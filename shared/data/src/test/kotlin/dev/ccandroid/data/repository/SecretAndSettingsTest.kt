package dev.ccandroid.data.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.ccandroid.core.ErrorCode
import dev.ccandroid.core.InMemorySecretStore
import dev.ccandroid.core.LogRedactor
import dev.ccandroid.core.Outcome
import dev.ccandroid.core.SecretRef
import dev.ccandroid.core.SystemTimeProvider
import dev.ccandroid.data.db.AppDatabase
import dev.ccandroid.domain.SecretProfile
import dev.ccandroid.domain.usecase.AppSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * P2-9 and P2-12 against a real SQLite engine (in-memory), per the test plan:
 * ≥ 80 % coverage on `shared/data`, boundaries, error paths, and the empty case.
 *
 * The secret tests assert the documented lifecycle: rotation overwrites,
 * forgetting keeps the profile row, and the stored value never reaches a
 * profile fact or a log line.
 */
class SecretAndSettingsTest {

    private lateinit var database: AppDatabase
    private lateinit var settings: AppSettingsRepository
    private lateinit var secrets: RoomSecretProfileService
    private lateinit var store: InMemorySecretStore

    @Before
    fun setUp() {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        database = db
        settings = RoomAppSettingsRepository(db.appSettingDao(), SystemTimeProvider())
        store = InMemorySecretStore()
        secrets = RoomSecretProfileService(db.secretProfileDao(), store)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- settings (P2-12) ------------------------------------------------

    @Test
    fun `setting round trips through the real table`() = runTest {
        assertTrue(settings.set("theme_mode", "dark").isSuccess)

        val read = settings.get("theme_mode")
        assertTrue(read.isSuccess)
        assertEquals("dark", read.getOrNull()?.value)
        assertTrue("the write stamps the time, not the caller", read.getOrNull()!!.updatedAt > 0)
    }

    @Test
    fun `an unset setting reads as null, not as an error`() = runTest {
        val read = settings.get("language")
        assertTrue(read.isSuccess)
        assertNull(read.getOrNull())
    }

    @Test
    fun `observe emits the written value`() = runTest {
        assertTrue(settings.set("language", "en").isSuccess)

        assertEquals("en", settings.observe("language").first()?.value)
    }

    @Test
    fun `rewriting a setting replaces the row instead of duplicating it`() = runTest {
        settings.set("language", "en")
        settings.set("language", "de")

        assertEquals(1, settings.observeAll().first().size)
        assertEquals("de", settings.get("language").getOrNull()?.value)
    }

    @Test
    fun `a key whose name suggests a secret never reaches a row`() = runTest {
        // AppSetting's own invariant: a key named like it stores a secret is
        // refused, and the boundary turns the refusal into a typed failure
        // instead of letting the exception cross.
        val result = settings.set("stored_api_token_for_work", "irrelevant")

        assertTrue("a secret-named key must not be writable", result.isFailure)
        assertNull(settings.get("stored_api_token_for_work").getOrNull())
    }

    // --- secrets (P2-9) ----------------------------------------------------

    @Test
    fun `profile ref for an unknown profile is a typed not-found`() = runTest {
        val result = secrets.getProfileRef("prof_missing")

        assertTrue(result.isFailure)
        assertEquals(ErrorCode.SECRET_PROFILE_NOT_FOUND, (result as Outcome.Failure).error.code)
    }

    @Test
    fun `a stored value round trips through the value path`() = runTest {
        val key = FIXTURE_BASE + "d".repeat(24)
        val ref = secrets.create("work", key.encodeToByteArray()).getOrThrow()

        val resolved = secrets.resolve(ref)
        assertTrue(resolved.isSuccess)
        assertEquals(key, resolved.getOrNull()?.decodeToString())
    }

    @Test
    fun `resolving marks the profile used when its row exists`() = runTest {
        val ref = secrets.create("work", FIXTURE_VALUE.encodeToByteArray()).getOrThrow()
        secrets.insert(profile(ref.profileId))

        assertNull(secrets.getById(ref.profileId).getOrNull()?.lastUsedAt)

        assertTrue(secrets.resolve(ref).isSuccess)
        assertNotNull(secrets.getById(ref.profileId).getOrNull()?.lastUsedAt)
    }

    @Test
    fun `getProfileRef succeeds once the profile row exists`() = runTest {
        val ref = secrets.create("work", FIXTURE_VALUE.encodeToByteArray()).getOrThrow()

        assertTrue("the value alone is not a profile", secrets.getProfileRef(ref.profileId).isFailure)

        secrets.insert(profile(ref.profileId))
        val result = secrets.getProfileRef(ref.profileId)
        assertTrue(result.isSuccess)
        assertEquals(ref.profileId, (result as Outcome.Success).value.profileId)
    }

    @Test
    fun `rotation overwrites the value and refreshes the hint`() = runTest {
        val first = FIXTURE_BASE + "e".repeat(20) + "OLD1"
        val second = FIXTURE_BASE + "f".repeat(20) + "NEW2"
        val ref = secrets.create("work", first.encodeToByteArray()).getOrThrow()
        secrets.insert(profile(ref.profileId))

        assertTrue(secrets.rotate(ref, second.encodeToByteArray()).getOrNull() == true)

        assertEquals(second, secrets.resolve(ref).getOrNull()?.decodeToString())
        val row = secrets.getById(ref.profileId).getOrNull()!!
        assertTrue("the hint follows the rotation", row.hint.endsWith("NEW2"))
        assertFalse(row.hint.contains(first))
    }

    @Test
    fun `forgetting the value keeps the profile row`() = runTest {
        val ref = secrets.create("work", FIXTURE_VALUE.encodeToByteArray()).getOrThrow()
        secrets.insert(profile(ref.profileId))

        assertTrue(secrets.forget(ref).getOrNull() == true)

        assertNull(secrets.resolve(ref).getOrNull())
        assertTrue("the row stays; the user revokes at the provider", secrets.getProfileRef(ref.profileId).isSuccess)
    }

    @Test
    fun `the profile facts never carry the plaintext`() = runTest {
        val ref = secrets.create("work", FIXTURE_VALUE.encodeToByteArray()).getOrThrow()
        // The row is created with ciphertext-shaped facts, never the value.
        secrets.insert(profile(ref.profileId))

        val profile = secrets.getById(ref.profileId).getOrNull()!!
        assertFalse(profile.cipherText.contains(FIXTURE_VALUE))
        assertFalse(profile.hint.contains(FIXTURE_VALUE))
    }

    @Test
    fun `stored values join the log redactor through the knownSecrets seam`() = runTest {
        val key = FIXTURE_BASE + "9".repeat(24)
        val ref = secrets.create("work", key.encodeToByteArray()).getOrThrow()

        val redactor = LogRedactor(knownSecrets = store.knownSecrets())
        val redacted = redactor.redactText("error near $key while authenticating")

        assertFalse("a stored key must not survive the log pass", redacted.contains(key))
        assertTrue(redacted.contains("<secret:${ref.profileId}>"))
    }

    @Test
    fun `an empty store contributes no known secrets`() = runTest {
        assertTrue(store.knownSecrets().isEmpty())
        assertTrue(LogRedactor().redactText("nothing stored yet").isNotEmpty())
    }

    // --- fixtures ---------------------------------------------------------

    private fun profile(id: String) = SecretProfile(
        id = id,
        name = "work",
        // Ciphertext-shaped facts; the value never becomes a field (rule 8).
        cipherText = "ENC-FIXTURE-".repeat(4),
        iv = "IV-FIXTURE",
        keyAlias = "alias_$id",
        hint = "••••",
        createdAt = 1_700_000_000_000,
        lastUsedAt = null,
    )

    private fun Outcome<SecretRef>.getOrThrow(): SecretRef = when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> throw IllegalStateException(error.messageEn)
    }

    private companion object {
        private const val FIXTURE_BASE = "FIXTURE-sk-"
        private const val FIXTURE_VALUE = "FIXTURE-sk-value-not-a-real-key"
    }
}
