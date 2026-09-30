package dev.ccandroid.domain.provider

/**
 * Which header carries the key.
 *
 * `ANTHROPIC` uses the header form, `OPENAI_CHAT` and `OPENAI_RESPONSES` the
 * bearer form, and `CUSTOM` is declared per provider because some proxies accept
 * only one of them. A proxy that accepts only the other form is a real and
 * common configuration, so this is a field and not a guess.
 */
public enum class AuthHeaderForm {
    X_API_KEY,
    BEARER,
}

/** The header names, kept next to the choice so the two cannot drift apart. */
public object AuthHeaders {
    public const val X_API_KEY: String = "x-api-key"
    public const val AUTHORIZATION: String = "Authorization"
    public const val BEARER_PREFIX: String = "Bearer "

    /**
     * Headers the app sets itself. A custom header with one of these names would
     * shadow the key the Keystore resolved, and the failure would look like a
     * rejected key rather than a shadowed one. `ProviderConfigurationTest`
     * refuses them at configuration time.
     */
    public val RESERVED: Set<String> = setOf(
        X_API_KEY.lowercase(),
        AUTHORIZATION.lowercase(),
        "host",
        "content-length",
    )
}
