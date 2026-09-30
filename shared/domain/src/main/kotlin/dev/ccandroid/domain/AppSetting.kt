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

    public companion object {
        /**
         * The key half of the invariant, as a named check: a key whose name
         * suggests it would store a secret is refused before a caller builds
         * the entity and trips the require above.
         */
        public fun isSecretShapedKey(key: String): Boolean {
            val lowerKey = key.lowercase()
            return lowerKey.contains("secret") ||
                lowerKey.contains("password") ||
                lowerKey.contains("api_key") ||
                lowerKey.contains("apikey") ||
                lowerKey.contains("token") ||
                lowerKey.contains("private_key")
        }
    }
}
