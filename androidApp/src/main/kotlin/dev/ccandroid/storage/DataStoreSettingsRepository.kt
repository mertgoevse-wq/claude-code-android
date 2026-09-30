package dev.ccandroid.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.ccandroid.core.Outcome
import dev.ccandroid.core.TimeProvider
import dev.ccandroid.core.tryCatch
import dev.ccandroid.domain.AppSetting
import dev.ccandroid.domain.usecase.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The DataStore-backed settings store, the Android-side sibling of
 * `RoomAppSettingsRepository`: same [AppSettingsRepository] interface, its own
 * file. The DataStore carries the app's own non-synced preferences; Room
 * carries the settings that belong to the data model.
 *
 * The private Context DataStore delegate must live at the top level — the
 * singleton-per-file guarantee is what stops two instances from corrupting
 * the file. The secret-shape invariant is enforced by `SetAppSetting`, so
 * this store stays a dumb key-value sink and there is one place that decides
 * what may be written.
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ccandroid_settings",
)

public class DataStoreSettingsRepository(
    private val context: Context,
    private val time: TimeProvider,
) : AppSettingsRepository {

    override fun observe(key: String): Flow<AppSetting?> =
        context.settingsDataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)]?.let { value ->
                AppSetting(key = key, value = value, updatedAt = time.currentTimeMillis())
            }
        }

    override fun observeAll(): Flow<List<AppSetting>> =
        context.settingsDataStore.data.map { preferences ->
            preferences.asMap().map { (preferenceKey, value) ->
                AppSetting(
                    key = preferenceKey.name,
                    value = (value as? String) ?: "",
                    updatedAt = time.currentTimeMillis(),
                )
            }
        }

    override suspend fun get(key: String): Outcome<AppSetting?> = tryCatch {
        context.settingsDataStore.data.first()[stringPreferencesKey(key)]?.let { value ->
            AppSetting(key = key, value = value, updatedAt = time.currentTimeMillis())
        }
    }

    override suspend fun set(key: String, value: String): Outcome<Unit> = tryCatch {
        context.settingsDataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
        Unit
    }
}
