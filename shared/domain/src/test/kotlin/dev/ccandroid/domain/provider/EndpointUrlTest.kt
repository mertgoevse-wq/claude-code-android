package dev.ccandroid.domain.provider

import dev.ccandroid.domain.ProviderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The base URL parser, per `docs/07-integrations/providers.md` §Which host may be
 * plain http.
 *
 * A URL the app half-understands is a key sent somewhere the user did not
 * choose, so the parser refuses everything it does not recognise. These tests
 * are mostly about what it refuses.
 */
class EndpointUrlTest {

    @Test
    fun `an ordinary https base parses into its parts`() {
        val url = EndpointUrl.parse("https://api.groq.com")

        assertEquals("https", url?.scheme)
        assertEquals("api.groq.com", url?.host)
        assertNull(url?.port)
        assertEquals("https://api.groq.com", url?.origin)
    }

    @Test
    fun `a port is kept and an ipv6 host is unwrapped`() {
        assertEquals(1234, EndpointUrl.parse("http://127.0.0.1:1234")?.port)
        assertEquals("[::1]", EndpointUrl.parse("http://[::1]:8080")?.origin?.substringAfter("://")?.take(5))
    }

    @Test
    fun `a base path is normalised to a leading and no trailing slash`() {
        val url = EndpointUrl.parse("https://gw.example/api/v2/")

        assertEquals("/api/v2", url?.path)
        assertEquals("https://gw.example", url?.origin)
    }

    @Test
    fun `a query or a fragment is dropped rather than guessed at`() {
        assertEquals("/v1", EndpointUrl.parse("https://a.example/v1?key=secret")?.path)
        assertEquals("/v1", EndpointUrl.parse("https://a.example/v1#frag")?.path)
    }

    @Test
    fun `something that is not a URL at all is refused`() {
        assertNull(EndpointUrl.parse("api.groq.com"))
        assertNull(EndpointUrl.parse(""))
        assertNull(EndpointUrl.parse("   "))
        assertNull(EndpointUrl.parse("ftp://files.example"))
        assertNull(EndpointUrl.parse("https://"))
        assertNull(EndpointUrl.parse("https:///v1/messages"))
    }

    @Test
    fun `a userinfo section is refused, because the host it names is not the one dialled`() {
        // "https://api.groq.com@evil.example" reads as a Groq URL to a careless
        // parser and as an evil.example URL to everything else. Refuse it.
        assertNull(EndpointUrl.parse("https://api.groq.com@evil.example/v1"))
    }

    @Test
    fun `a port outside the legal range is refused`() {
        assertNull(EndpointUrl.parse("https://a.example:99999"))
        assertNull(EndpointUrl.parse("https://a.example:0"))
    }

    @Test
    fun `the local host set is the loopback and the private ranges`() {
        listOf(
            "http://localhost:1234",
            "http://127.0.0.1:8080",
            "http://127.1.2.3",
            "http://[::1]:11434",
            "http://10.0.0.4:8000",
            "http://192.168.1.20:1234",
            "http://172.16.0.9:1234",
            "http://172.31.255.1:1234",
            "http://mein-pc.local:1234",
        ).forEach { raw ->
            assertTrue("$raw should count as local", EndpointUrl.parse(raw)?.isLocal == true)
        }
    }

    @Test
    fun `a public address just outside a private range is not local`() {
        // 172.15 and 172.32 sit either side of RFC 1918. A prefix check that
        // ignores the boundary would call both of them local.
        assertFalse(EndpointUrl.parse("http://172.15.0.1:1234")?.isLocal == true)
        assertFalse(EndpointUrl.parse("http://172.32.0.1:1234")?.isLocal == true)
        assertFalse(EndpointUrl.parse("http://11.0.0.1:1234")?.isLocal == true)
        assertFalse(EndpointUrl.parse("http://api.groq.com")?.isLocal == true)
    }

    @Test
    fun `a resolved endpoint joins the base and the path exactly once`() {
        val url = EndpointUrl.parse("https://api.groq.com")!!

        assertEquals(
            "https://api.groq.com/openai/v1/chat/completions",
            url.resolve("/openai/v1/chat/completions"),
        )
        assertEquals("https://api.groq.com", url.resolve(""))
    }

    @Test
    fun `the preview is the endpoint, or null, never a guess`() {
        assertEquals(
            "https://api.mistral.ai/v1/chat/completions",
            EndpointUrl.preview("https://api.mistral.ai", "/v1/chat/completions"),
        )
        assertNull(EndpointUrl.preview("nonsense", "/v1/chat/completions"))
    }
}

/** The path a dialect uses, and the one placeholder a custom path may carry. */
class PathTemplatesTest {

    @Test
    fun `each dialect has the path its documentation gives`() {
        assertEquals("/v1/messages", PathTemplates.defaultFor(ProviderKind.ANTHROPIC))
        assertEquals("/v1/chat/completions", PathTemplates.defaultFor(ProviderKind.OPENAI_CHAT))
        assertEquals("/v1/responses", PathTemplates.defaultFor(ProviderKind.OPENAI_RESPONSES))
    }

    @Test
    fun `a custom path takes its version from the base url, not from a guess`() {
        val base = EndpointUrl.parse("https://gw.example/v2")!!

        assertEquals("/v2/chat/completions", PathTemplates.render("/{version}/chat/completions", base))
    }

    @Test
    fun `a base with no path yields a request the server can answer`() {
        val base = EndpointUrl.parse("https://gw.example")!!

        assertEquals("/chat/completions", PathTemplates.render("/{version}/chat/completions", base))
    }

    @Test
    fun `a path without the placeholder is left alone`() {
        val base = EndpointUrl.parse("https://gw.example/v2")!!

        assertEquals("/v1/chat/completions", PathTemplates.render("/v1/chat/completions", base))
    }
}

/** The header the key goes in, per dialect and per declaration. */
class AuthFormForTest {

    @Test
    fun `Anthropic uses the header form and the OpenAI shapes use bearer`() {
        assertEquals(AuthHeaderForm.X_API_KEY, AuthFormFor.forKind(ProviderKind.ANTHROPIC))
        assertEquals(AuthHeaderForm.BEARER, AuthFormFor.forKind(ProviderKind.OPENAI_CHAT))
        assertEquals(AuthHeaderForm.BEARER, AuthFormFor.forKind(ProviderKind.OPENAI_RESPONSES))
    }

    @Test
    fun `a custom provider declares its own, because a proxy may accept only one`() {
        assertEquals(
            AuthHeaderForm.X_API_KEY,
            AuthFormFor.forKind(ProviderKind.CUSTOM, AuthHeaderForm.X_API_KEY),
        )
        assertEquals(
            AuthHeaderForm.BEARER,
            AuthFormFor.forKind(ProviderKind.CUSTOM, AuthHeaderForm.BEARER),
        )
    }

    @Test
    fun `a custom provider with no declaration defaults to bearer, the common case`() {
        assertEquals(AuthHeaderForm.BEARER, AuthFormFor.forKind(ProviderKind.CUSTOM, null))
    }

    @Test
    fun `the header name follows the form`() {
        assertEquals("x-api-key", AuthFormFor.headerName(AuthHeaderForm.X_API_KEY))
        assertEquals("Authorization", AuthFormFor.headerName(AuthHeaderForm.BEARER))
    }

    @Test
    fun `the value shape carries no key, because no key is ever an argument`() {
        assertEquals("<key>", AuthFormFor.headerValueShape(AuthHeaderForm.X_API_KEY))
        assertEquals("Bearer <key>", AuthFormFor.headerValueShape(AuthHeaderForm.BEARER))
    }
}
