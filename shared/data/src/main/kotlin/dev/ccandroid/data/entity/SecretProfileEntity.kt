package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.ccandroid.domain.SecretProfile

@Entity(tableName = "secret_profiles")
data class SecretProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cipherText: String,
    val iv: String,
    val keyAlias: String,
    val hint: String,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
) {
    companion object {
        fun fromDomain(profile: SecretProfile): SecretProfileEntity = SecretProfileEntity(
            id = profile.id,
            name = profile.name,
            cipherText = profile.cipherText,
            iv = profile.iv,
            keyAlias = profile.keyAlias,
            hint = profile.hint,
            createdAt = profile.createdAt,
            lastUsedAt = profile.lastUsedAt,
        )
    }

    fun toDomain(): SecretProfile = SecretProfile(
        id = id,
        name = name,
        cipherText = cipherText,
        iv = iv,
        keyAlias = keyAlias,
        hint = hint,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
    )
}
