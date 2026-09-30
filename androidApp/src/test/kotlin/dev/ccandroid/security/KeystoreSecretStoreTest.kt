package dev.ccandroid.security

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.ccandroid.core.CryptoGateway
import dev.ccandroid.data.db.AppDatabase
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
 * The composition behind the production [SecretStore], tested on the JVM
 * with a deterministic fake gateway. The Keystore itself is exercised on the
 * device; what is tested here is the contract that holds it together:
 * round trip, rotation-overwrites, forget-keeps-the-row, and that the stored
 * value never appears in a profile fact.
 */
class KeystoreSecretStoreTest {

    private lateinit var database: AppDatabase
    private lateinit var store: KeystoreSecretStore
    private lateinit var crypto: FakeCryptoGateway

    @Before
    fun setUp() {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        database = db
        crypto = FakeCryptoGateway()
        store = KeystoreSecretStore(crypto, db.secretProfileDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `key round trips - stored, resolved, never in a row fact`() = runTest {
        val ref = store.create("work", FIXTURE_KEY.encodeToByteArray())

        assertEquals(FIXTURE_KEY, store.resolve(ref)?.decodeToString())

        val row = database.secretProfileDao().getSecretProfileById(ref.profileId)!!
        assertFalse("the row carries ciphertext, not the value", row.cipherText.contains(FIXTURE_KEY))
        assertFalse(row.hint.contains(FIXTURE_KEY))
        assertTrue(row.keyAlias.startsWith("cca_secret_"))
    }

    @Test
    fun `the gateway contract round trips bytes and binds them to the alias`() = runTest {
        val gateway = AndroidContractGateway()
        val payload = FIXTURE_KEY.encodeToByteArray()

        val encrypted = gateway.encrypt("alias_one", payload)
        assertFalse("the ciphertext differs from the plaintext", encrypted.contentEquals(payload))
        assertEquals(FIXTURE_KEY, gateway.decrypt("alias_one", encrypted).decodeToString())
    }

    @Test
    fun `rotation overwrites - no old value, no old ciphertext`() = runTest {
        val ref = store.create("work", FIXTURE_KEY.encodeToByteArray())
        val before = database.secretProfileDao().getSecretProfileById(ref.profileId)!!.cipherText

        assertTrue(store.rotate(ref, SECOND_FIXTURE_KEY.encodeToByteArray()))

        assertEquals(SECOND_FIXTURE_KEY, store.resolve(ref)?.decodeToString())
        val after = database.secretProfileDao().getSecretProfileById(ref.profileId)!!.cipherText
        assertFalse("rotation replaced the ciphertext", after == before)
    }

    @Test
    fun `forget keeps the row and drops the value and the keystore key`() = runTest {
        val ref = store.create("work", FIXTURE_KEY.encodeToByteArray())
        val alias = database.secretProfileDao().getSecretProfileById(ref.profileId)!!.keyAlias

        assertTrue(store.forget(ref))

        assertNull(store.resolve(ref))
        assertNotNull("the row stays", database.secretProfileDao().getSecretProfileById(ref.profileId))
        assertTrue(database.secretProfileDao().getSecretProfileById(ref.profileId)!!.cipherText.isEmpty())
        assertTrue("the keystore key was deleted", crypto.deleted.contains(alias))
    }

    @Test
    fun `hint is masked and keeps only the last four characters`() = runTest {
        val ref = store.create("work", FIXTURE_KEY.encodeToByteArray())

        assertEquals("••••KEY9", store.hint(ref))
    }

    @Test
    fun `resolve of a forgotten profile is null, not an error`() = runTest {
        assertNull(store.resolve(dev.ccandroid.core.SecretRef("prof_missing")))
    }

    private companion object {
        private val FIXTURE_KEY = "FIXTURE-sk-" + "a".repeat(20) + "KEY9"
        private val SECOND_FIXTURE_KEY = "FIXTURE-sk-" + "b".repeat(24)
    }
}

/** Deterministic stand-in for the Keystore: alias-prefixed bytes, tracked deletions. */
private class FakeCryptoGateway : CryptoGateway {
    val deleted = mutableSetOf<String>()
    private val aliases = mutableSetOf<String>()

    override suspend fun encrypt(alias: String, plainText: ByteArray): ByteArray {
        aliases.add(alias)
        return alias.encodeToByteArray() + plainText
    }

    override suspend fun decrypt(alias: String, cipherText: ByteArray): ByteArray {
        val prefix = alias.encodeToByteArray()
        require(cipherText.size >= prefix.size) { "ciphertext shorter than its alias prefix" }
        return cipherText.copyOfRange(prefix.size, cipherText.size)
    }

    override suspend fun deleteKey(alias: String): Boolean {
        deleted.add(alias)
        return aliases.remove(alias)
    }

    override suspend fun hasKey(alias: String): Boolean = alias in aliases
}

/** The byte contract [AndroidCryptoGateway] implements: iv || body, reversed on decrypt. */
private class AndroidContractGateway : CryptoGateway {
    override suspend fun encrypt(alias: String, plainText: ByteArray): ByteArray =
        ByteArray(IV_LENGTH) { it.toByte() } + plainText

    override suspend fun decrypt(alias: String, cipherText: ByteArray): ByteArray =
        cipherText.copyOfRange(IV_LENGTH, cipherText.size)

    override suspend fun deleteKey(alias: String): Boolean = true

    override suspend fun hasKey(alias: String): Boolean = true

    private companion object {
        const val IV_LENGTH = 12
    }
}
