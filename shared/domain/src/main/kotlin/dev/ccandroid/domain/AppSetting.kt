package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public data class AppSetting(
    val key: String,
    val value: String,
    val updatedAt: Long,
) {
    init {
        // docs/02-architecture/data-model.md §AppSetting:
        // Invariant: secrets are never in AppSetting
        val lowerKey = key.lowercase()
        require(
            !lowerKey.contains("secret") &&
                !lowerKey.contains("password") &&
                !lowerKey.contains("api_key") &&
                !lowerKey.contains("apikey") &&
                !lowerKey.contains("token") &&
                !lowerKey.contains("private_key"),
        ) { "Secrets must never be stored in AppSetting. Use SecretProfile." }
    }
}
