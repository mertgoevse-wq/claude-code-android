package dev.ccandroid.domain.usecase

import dev.ccandroid.core.AppError
import dev.ccandroid.core.Outcome
import dev.ccandroid.domain.AppSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P2-12. The invariant under test is the data model's: secrets are never in
 * `AppSetting` (`docs/02-architecture/data-model.md` §AppSetting). The map
 * double stores what a real store would, so a refused write is observable by
 * absence.
 */
class AppSettingsUseCasesTest {

    private class MapSettings : AppSettingsRepository {
        val state = MutableStateFlow<Map<String, AppSetting>>(emptyMap())

        override fun observe(key: String): Flow<AppSetting?> = state.map { it[key] }

        override fun observeAll(): Flow<List<AppSetting>> = state.map { it.values.sortedBy(AppSetting::key) }

        override suspend fun get(key: String): Outcome<AppSetting?> = Outcome.Success(state.value[key])

        override suspend fun set(key: String, value: String): Outcome<Unit> {
            state.value = state.value + (key to AppSetting(key, value, updatedAt = 0L))
            return Outcome.Success(Unit)
        }
    }

    @Test
    fun `unset key yields the documented default, not a screen's guess`() = runTest {
        val get = GetAppSetting(MapSettings())

        val language = get.string(SettingsKeys.LANGUAGE, SettingDefaults.LANGUAGE)
        val font = get.int(SettingsKeys.TERMINAL_FONT_SIZE, SettingDefaults.TERMINAL_FONT_SIZE)

        assertTrue(language.isSuccess)
        assertEquals("de", language.getOrNull())
        assertEquals(14, font.getOrNull())
    }

    @Test
    fun `non-numeric int value falls back to the default instead of crashing`() = runTest {
        val repo = MapSettings()
        repo.set(SettingsKeys.TERMINAL_FONT_SIZE, "bigger")
        val get = GetAppSetting(repo)

        assertEquals(14, get.int(SettingsKeys.TERMINAL_FONT_SIZE, SettingDefaults.TERMINAL_FONT_SIZE).getOrNull())
    }

    @Test
    fun `round trip - set then read returns the value`() = runTest {
        val repo = MapSettings()
        val set = SetAppSetting(repo)
        val get = GetAppSetting(repo)

        val result = set(SettingsKeys.THEME_MODE, "dark")

        assertTrue(result.isSuccess)
        assertEquals("dark", get.string(SettingsKeys.THEME_MODE, "system").getOrNull())
    }

    @Test
    fun `a key-shaped value is refused before it is stored`() = runTest {
        val repo = MapSettings()
        val set = SetAppSetting(repo)
        val secret = "FIXTURE-sk-" + "c".repeat(24)

        val result = set(SettingsKeys.LAST_USED_PROJECT_ID, secret)

        assertTrue("a credential-shaped value must not become a setting", result.isFailure)
        assertTrue("the refusal happens before the write", repo.state.value.isEmpty())
    }

    @Test
    fun `a jwt-shaped value is refused with the named reason`() = runTest {
        val set = SetAppSetting(MapSettings())
        val jwt = "eyJ" + "a".repeat(20) + "." + "b".repeat(20) + "." + "c".repeat(20)

        val result = set(SettingsKeys.ONBOARDING_STATE, jwt)

        assertTrue(result.isFailure)
        val error = (result as Outcome.Failure).error as AppError.Conflict
        assertEquals(SetAppSetting.REASON_SECRET_SHAPED, error.details)
    }

    @Test
    fun `an ordinary value passes the shape check`() = runTest {
        val set = SetAppSetting(MapSettings())

        assertTrue(set(SettingsKeys.LANGUAGE, "en").isSuccess)
        assertTrue(set(SettingsKeys.TERMINAL_FONT_SIZE, "16").isSuccess)
        assertTrue(set(SettingsKeys.LAST_USED_PROJECT_ID, "proj_01HW").isSuccess)
    }

    @Test
    fun `every key stays inside the data model's vocabulary`() {
        val declared = listOf(
            SettingsKeys.LAST_USED_PROJECT_ID,
            SettingsKeys.ONBOARDING_STATE,
            SettingsKeys.TERMINAL_FONT_SIZE,
            SettingsKeys.THEME_MODE,
            SettingsKeys.LANGUAGE,
            SettingsKeys.NOTIFICATION_BATTERY_THRESHOLD,
            SettingsKeys.DIAGNOSTICS_BUNDLE_VERSION,
            SettingsKeys.UPDATE_CHECK_TIMESTAMP,
        )
        val allowed = setOf(
            "last_used_project_id", "onboarding_state", "terminal_font_size", "theme_mode",
            "language", "notification_battery_threshold", "diagnostics_bundle_version", "update_check_timestamp",
        )

        assertTrue(declared.all { it in allowed })
        assertEquals("a duplicate key constant is a typo waiting to happen", allowed.size, declared.toSet().size)
    }

    @Test
    fun `supported languages are exactly german default and english`() {
        val languages = SettingDefaults.supportedLanguages()

        assertEquals(listOf("de", "en"), languages)
        assertEquals("German is the default per the i18n contract", "de", SettingDefaults.LANGUAGE)
    }

    @Test
    fun `the shape check recognises no value in the app's own vocabulary`() {
        val ordinary = listOf("de", "en", "dark", "system", "14", "proj_01HW", "0", "3")
        assertTrue(ordinary.all { !SetAppSetting.looksLikeSecret(it) })
        assertFalse(SetAppSetting.looksLikeSecret("-----BEGIN CERTIFICATE-----"))
    }
}
