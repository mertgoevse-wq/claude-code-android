package dev.ccandroid.data.repository

import dev.ccandroid.core.AppError
import dev.ccandroid.core.ErrorCode
import dev.ccandroid.core.Outcome
import dev.ccandroid.core.TimeProvider
import dev.ccandroid.core.tryCatch
import dev.ccandroid.data.dao.AppSettingDao
import dev.ccandroid.data.entity.AppSettingEntity
import dev.ccandroid.domain.AppSetting
import dev.ccandroid.domain.usecase.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The app-wide settings on Room rather than on the DataStore.
 *
 * The DataStore stays out of `shared/data` on purpose: its Preferences API is
 * Android/JVM-shaped, while Room runs on the bundled driver the tests already
 * use. The DataStore arrives in `androidApp` as its own [AppSettingsRepository]
 * behind the same interface; the class name records the decision so nobody
 * "fixes" the name without reading `docs/02-architecture/data-model.md`
 * §AppSetting.
 *
 * The secret-shape invariant is enforced by the caller (`SetAppSetting`), so
 * the adapter stays thin and there is exactly one place that decides what may
 * become a setting.
 */
public class RoomAppSettingsRepository(
    private val dao: AppSettingDao,
    private val time: TimeProvider,
) : AppSettingsRepository {

    override fun observe(key: String): Flow<AppSetting?> =
        dao.observeSetting(key).map { it?.toDomain() }

    override fun observeAll(): Flow<List<AppSetting>> =
        dao.observeAllSettings().map { list -> list.map { it.toDomain() } }

    override suspend fun get(key: String): Outcome<AppSetting?> = tryCatch {
        dao.getSetting(key)?.toDomain()
    }

    override suspend fun set(key: String, value: String): Outcome<Unit> {
        // AppSetting's constructor refuses a secret-named key with a bare
        // exception; here it becomes a typed failure before the entity is
        // built, so the boundary stays exception-free (code rule 4).
        if (AppSetting.isSecretShapedKey(key)) {
            return Outcome.Failure(
                AppError.Simple(
                    code = ErrorCode.SETTINGS_SECRET_SHAPED,
                    messageDe = "Unter diesem Namen werden keine Einstellungen gespeichert.",
                    messageEn = "Settings are not stored under a name like this.",
                    retryable = false,
                    details = REASON_SECRET_NAMED_KEY,
                ),
            )
        }
        return tryCatch {
            dao.insertOrUpdateSetting(
                AppSettingEntity.fromDomain(
                    AppSetting(key = key, value = value, updatedAt = time.currentTimeMillis()),
                ),
            )
            Unit
        }
    }

    public companion object {
        public const val REASON_SECRET_NAMED_KEY: String = "KEY_NAME_SUGGESTS_A_SECRET"
    }
}
