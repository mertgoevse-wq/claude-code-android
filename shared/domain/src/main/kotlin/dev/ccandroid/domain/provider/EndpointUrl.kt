package dev.ccandroid.domain.provider

import dev.ccandroid.domain.ProviderKind

/**
 * A base URL, parsed by hand.
 *
 * `shared/domain` imports nothing JVM-only, so `java.net.URI` is not available
 * here and would not be available on the iOS target either. The parser is
 * deliberately small: it reads the four parts provider configuration actually
 * uses and refuses everything it does not understand, because a URL the app
 * half-understands is a key sent somewhere the user did not intend.
 *
 * Refusing is the point. A parser that guesses produces a request to a host
 * nobody chose.
 */
public data class EndpointUrl(
    val scheme: String,
    val host: String,
    val port: Int?,
    /** Always starts with `/`, never ends with one, never empty. */
    val path: String,
) {

    /** The origin, as a browser would write it. No trailing slash. */
    val origin: String
        get() = buildString {
            append(scheme).append("://").append(if (host.contains(':')) "[$host]" else host)
            if (port != null) append(':').append(port)
        }

    public fun resolve(pathTemplate: String): String {
        val suffix = normalizePath(pathTemplate)
        return if (suffix.isEmpty()) origin else origin + suffix
    }

    public val isPlaintext: Boolean get() = scheme == "http"

    /**
     * True for a host that is the device itself or on the user's own network.
     *
     * A local model is a real and expected use, and so is a second machine in
     * the house. What is not acceptable is sending a key in clear text to a host
     * on the internet, which is why plain http is refused everywhere else and
     * confirmed here rather than the other way round.
     */
    public val isLocal: Boolean
        get() = host == "localhost" ||
            host == "::1" ||
            host == "0.0.0.0" ||
            host.startsWith("127.") ||
            host.startsWith("10.") ||
            host.startsWith("192.168.") ||
            isIn172PrivateRange() ||
            host.endsWith(".local") ||
            host.endsWith(".internal") ||
            host.endsWith(".home.arpa")

    /**
     * 172.16.0.0/12 is the one private range that is not a clean prefix, so it
     * has to be read octet by octet. `172.15` and `172.32` sit just outside it
     * and are on the internet, which is the whole reason this is a range check
     * and not a `startsWith`.
     */
    private fun isIn172PrivateRange(): Boolean {
        val octets = host.split('.')
        if (octets.size != 4) return false
        if (octets[0].toIntOrNull() != 172) return false
        return octets[1].toIntOrNull() in 16..31
    }

    public companion object {

        private val SCHEME = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*)://")
        private val HOST_WITH_PORT = Regex("^(\\[[0-9A-Fa-f:]+]|[^/:?#]+)(?::(\\d{1,5}))?")

        private fun normalizePath(raw: String): String {
            val trimmed = raw.trim().trim('/')
            return if (trimmed.isEmpty()) "" else "/$trimmed"
        }

        /** Null when the string is not a URL the app is willing to use. */
        public fun parse(raw: String): EndpointUrl? {
            val input = raw.trim()
            if (input.isEmpty()) return null

            val schemeMatch = SCHEME.find(input) ?: return null
            val scheme = schemeMatch.groupValues[1].lowercase()
            if (scheme != "https" && scheme != "http") return null

            val afterScheme = input.substring(schemeMatch.value.length)
            val authorityEnd = afterScheme.indexOfFirst { it == '/' || it == '?' || it == '#' }
                .let { if (it == -1) afterScheme.length else it }
            val authority = afterScheme.substring(0, authorityEnd)
            if (authority.isEmpty()) return null

            val hostMatch = HOST_WITH_PORT.find(authority) ?: return null
            // A userinfo section would let "https://evil.com@good.com" read as good.com.
            if (hostMatch.range.first != 0) return null
            if (authority.contains('@')) return null

            val host = hostMatch.groupValues[1].removePrefix("[").removeSuffix("]").lowercase()
            if (host.isEmpty() || host.contains(' ')) return null

            val portText = hostMatch.groupValues[2]
            val port = if (portText.isEmpty()) null else portText.toIntOrNull() ?: return null
            if (port != null && (port < 1 || port > 65535)) return null

            val rawPath = if (authorityEnd == afterScheme.length) "" else afterScheme.substring(authorityEnd)
            val pathOnly = rawPath.substringBefore('?').substringBefore('#')

            return EndpointUrl(scheme, host, port, normalizePath(pathOnly))
        }

        /** The endpoint preview shown while the user types, never a live request. */
        public fun preview(raw: String, pathTemplate: String): String? =
            parse(raw)?.resolve(pathTemplate)
    }
}

/**
 * Renders a path template for a dialect.
 *
 * Anthropic and the two OpenAI shapes have fixed paths, so the template is
 * shown as written. A `CUSTOM` provider may have a version segment in an
 * unexpected place, so `{version}` is filled from the base URL's own path
 * rather than guessed.
 */
public object PathTemplates {

    public fun defaultFor(kind: ProviderKind): String = when (kind) {
        ProviderKind.ANTHROPIC -> "/v1/messages"
        ProviderKind.OPENAI_CHAT -> "/v1/chat/completions"
        ProviderKind.OPENAI_RESPONSES -> "/v1/responses"
        ProviderKind.CUSTOM -> "/v1/chat/completions"
    }

    /**
     * Fills the one placeholder a custom path may use, from the base URL.
     *
     * `{version}` comes from the last segment of the base path, so a base of
     * `https://gw.example/v2` yields `/v2/chat/completions`. A base with no
     * path yields the template with the placeholder removed, because a request
     * to `/chat/completions` is a real endpoint on more servers than one to
     * `/{version}/chat/completions` ever is.
     */
    public fun render(template: String, baseUrl: EndpointUrl?): String {
        if (!template.contains(PLACEHOLDER)) return template.trim().trimEnd('/').ifEmpty { "/" }
        val version = baseUrl?.path?.trim('/')?.substringAfterLast('/')?.takeIf { it.isNotEmpty() }
            ?: return template.replace(PLACEHOLDER, "").replace("//", "/")
        return template.replace(PLACEHOLDER, version)
    }

    private const val PLACEHOLDER = "{version}"
}

/**
 * The header the key goes in, per dialect.
 *
 * A `CUSTOM` provider declares its own because a proxy that accepts only one
 * form is a real configuration, and guessing wrong produces a 401 the user
 * cannot diagnose from the app.
 */
public object AuthFormFor {

    public fun forKind(kind: ProviderKind, declared: AuthHeaderForm? = null): AuthHeaderForm = when (kind) {
        ProviderKind.ANTHROPIC -> AuthHeaderForm.X_API_KEY
        ProviderKind.OPENAI_CHAT, ProviderKind.OPENAI_RESPONSES -> AuthHeaderForm.BEARER
        ProviderKind.CUSTOM -> declared ?: AuthHeaderForm.BEARER
    }

    /** The header name the key goes in. */
    public fun headerName(form: AuthHeaderForm): String = when (form) {
        AuthHeaderForm.X_API_KEY -> AuthHeaders.X_API_KEY
        AuthHeaderForm.BEARER -> AuthHeaders.AUTHORIZATION
    }

    /**
     * The header value. The key itself is never an argument: it is resolved from
     * the Keystore at the request boundary and this only decides the shape, so a
     * key cannot end up in a log, an exception message, or a test fixture.
     */
    public fun headerValueShape(form: AuthHeaderForm): String = when (form) {
        AuthHeaderForm.X_API_KEY -> "<key>"
        AuthHeaderForm.BEARER -> "${AuthHeaders.BEARER_PREFIX}<key>"
    }
}
