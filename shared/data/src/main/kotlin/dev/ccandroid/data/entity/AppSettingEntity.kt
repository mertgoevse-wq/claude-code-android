package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.ccandroid.domain.AppSetting

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long,
) {
    companion object {
        fun fromDomain(setting: AppSetting): AppSettingEntity = AppSettingEntity(
            key = setting.key,
            value = setting.value,
            updatedAt = setting.updatedAt,
        )
    }

    fun toDomain(): AppSetting = AppSetting(
        key = key,
        value = value,
        updatedAt = updatedAt,
    )
}
