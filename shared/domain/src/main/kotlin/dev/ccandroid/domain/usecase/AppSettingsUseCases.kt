package dev.ccandroid.domain.usecase

import dev.ccandroid.core.AppError
import dev.ccandroid.core.ErrorCode
import dev.ccandroid.core.Outcome
import dev.ccandroid.domain.AppSetting
import kotlinx.coroutines.flow.Flow

/**
 * The keys the app writes into `AppSetting`, per
 * `docs/02-architecture/data-model.md` §AppSetting. A key not in this list is
 * a schema decision and needs a doc change first, not a new string at a call
 * site.
 *
 * The `AppSetting` invariant forbids secret-shaped values, and the keys here
 * are the guarantee's other half: a fixed vocabulary means no code path can
 * invent a `stored_api_key` spelling that slips past the check.
 */
public object SettingsKeys {
    public const val LAST_USED_PROJECT_ID: String = "last_used_project_id"
    public const val ONBOARDING_STATE: String = "onboarding_state"
    public const val TERMINAL_FONT_SIZE: String = "terminal_font_size"
    public const val THEME_MODE: String = "theme_mode"
    public const val LANGUAGE: String = "language"
    public const val NOTIFICATION_BATTERY_THRESHOLD: String = "notification_battery_threshold"
    public const val DIAGNOSTICS_BUNDLE_VERSION: String = "diagnostics_bundle_version"
    public const val UPDATE_CHECK_TIMESTAMP: String = "update_check_timestamp"
}

/** The defaults, so "unset" and "default" are one concept, not two. */
public object SettingDefaults {
    /** German is the default language, per the i18n contract; English is the alternative. */
    public const val LANGUAGE: String = "de"
    public const val LANGUAGE_EN: String = "en"
    public const val THEME_MODE: String = "system"
    public const val TERMINAL_FONT_SIZE: Int = 14

    public fun supportedLanguages(): List<String> = listOf(LANGUAGE, LANGUAGE_EN)
}

/**
 * The app-wide key-value store: last used project, onboarding state, terminal
 * font size, theme mode, language — anything not worth a column.
 *
 * Secrets are never here (`AppSetting`'s invariant); they are `SecretProfile`
 * rows behind a `SecretRef`. The repository is deliberately tiny: settings
 * have no lifecycle, so there is nothing for a use case layer to add between
 * a screen and this interface except the invariant check.
 */
public interface AppSettingsRepository {

    public fun observe(key: String): Flow<AppSetting?>

    public fun observeAll(): Flow<List<AppSetting>>

    public suspend fun get(key: String): Outcome<AppSetting?>

    public suspend fun set(key: String, value: String): Outcome<Unit>
}

/**
 * Reads one setting, returning the documented default when it was never set.
 *
 * The default is applied here and not at the call sites, so a screen cannot
 * invent its own value for an unset language or theme.
 */
public class GetAppSetting(private val repository: AppSettingsRepository) {

    public suspend operator fun invoke(key: String): Outcome<AppSetting?> = repository.get(key)

    public suspend fun string(key: String, default: String): Outcome<String> =
        repository.get(key).map { (it?.value ?: default) }

    public suspend fun int(key: String, default: Int): Outcome<Int> =
        repository.get(key).map { setting ->
            setting?.value?.toIntOrNull() ?: default
        }
}

/**
 * Writes one setting, after the invariant from `data-model.md` §AppSetting.
 *
 * A key-shaped value is refused with a typed error, because a value that
 * looks like a credential here is either a leak in progress or a mistake —
 * both are better answered before the row exists.
 */
public class SetAppSetting(private val repository: AppSettingsRepository) {

    public suspend operator fun invoke(key: String, value: String): Outcome<Unit> {
        if (looksLikeSecret(value)) {
            return Outcome.Failure(
                AppError.Conflict(
                    code = ErrorCode.SETTINGS_SECRET_SHAPED,
                    messageDe = "Dieser Wert sieht nach einem Schlüssel aus und wird nicht in den Einstellungen gespeichert.",
                    messageEn = "This value looks like a credential and is not stored in settings.",
                    details = REASON_SECRET_SHAPED,
                ),
            )
        }
        return repository.set(key, value)
    }

    public companion object {
        public const val REASON_SECRET_SHAPED: String = "VALUE_LOOKS_LIKE_A_SECRET"

        /** The same shape families the log redactor covers, plus a bare `Bearer` header. */
        public fun looksLikeSecret(value: String): Boolean = SECRET_SHAPES.any { it.containsMatchIn(value) }

        private val SECRET_SHAPES = listOf(
            Regex("\\bsk-ant-[A-Za-z0-9_-]{10,}"),
            Regex("\\bsk-(proj-)?[A-Za-z0-9]{20,}"),
            Regex("\\bghp_[A-Za-z0-9]{20,}"),
            Regex("\\bgithub_pat_[A-Za-z0-9_]{30,}"),
            Regex("\\bgho_[A-Za-z0-9]{36}"),
            Regex("\\bAKIA[0-9A-Z]{16}"),
            Regex("\\bAIza[0-9A-Za-z_-]{30,}"),
            Regex("\\bxox[baprs]-[A-Za-z0-9-]{10,}"),
            Regex("[A-Za-z0-9_-]{16,}\\.[A-Za-z0-9_-]{16,}\\.[A-Za-z0-9_-]{16,}"),
            Regex("(?i)^bearer\\s+\\S{8,}$"),
            Regex("-----BEGIN [A-Z ]*PRIVATE KEY-----"),
        )
    }
}
