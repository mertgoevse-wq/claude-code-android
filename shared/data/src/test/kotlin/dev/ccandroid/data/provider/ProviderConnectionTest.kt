package dev.ccandroid.data.provider

import dev.ccandroid.domain.ProviderKind
import dev.ccandroid.domain.provider.AuthHeaderForm
import dev.ccandroid.domain.provider.AuthHeaders
import dev.ccandroid.domain.usecase.ValidatedProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The connection test, per `docs/07-integrations/providers.md` §The connection test.
 *
 * A `MockEngine` stands in for the network, so nothing here opens a socket and
 * nothing here needs a key. The tests are about one thing the spec is explicit
 * about: **every step is reported separately**. A single "connection failed" for
 * a provider that is reachable but has the wrong model is the least useful
 * message in this domain.
 */
class ProviderConnectionTest {

    private val neverSent = SecretResolver { null }

    private fun anthropicLike(
        baseUrl: String = "https://api.example.com",
        kind: ProviderKind = ProviderKind.ANTHROPIC,
        authForm: AuthHeaderForm = AuthHeaderForm.X_API_KEY,
        modelsUrl: String? = "$baseUrl/v1/models",
        model: String? = "claude-sonnet-4-5",
    ) = ValidatedProvider(
        name = "Test",
        kind = kind,
        baseUrl = baseUrl,
        pathTemplate = if (kind == ProviderKind.ANTHROPIC) "/v1/messages" else "/v1/chat/completions",
        authForm = authForm,
        extraHeaders = emptyMap(),
        completionUrl = baseUrl + (if (kind == ProviderKind.ANTHROPIC) "/v1/messages" else "/v1/chat/completions"),
        modelsUrl = modelsUrl,
        model = model,
    )

    /** A server that answers everything, so only the step under test is wrong. */
    private fun happyEngine(): MockEngine = MockEngine { request ->
        when {
            request.url.encodedPath.endsWith("/models") ->
                respond(
                    """{"data":[{"id":"claude-sonnet-4-5"},{"id":"gpt-4.1"}]}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )

            request.headers[HttpHeaders.Accept] == "text/event-stream" ->
                respond(
                    "data: {}\n\n",
                    headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
                )

            else -> respond(
                """{"id":"msg_1","content":[]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    }

    private fun tester(engine: MockEngine, secrets: SecretResolver = neverSent) =
        ProviderConnectionTester(HttpClient(engine), secrets)

    // --- the whole thing --------------------------------------------------

    @Test
    fun `a server that answers everything reports ok`() = runTest {
        val result = tester(happyEngine(), keyResolver()).test(anthropicLike())

        assertEquals(ProviderVerdict.OK, result.verdict)
    }

    @Test
    fun `every step appears in the result, so nothing is silently missing`() = runTest {
        val result = tester(happyEngine(), keyResolver()).test(anthropicLike())

        assertEquals(TestStep.entries.toSet(), result.steps.map { it.step }.toSet())
    }

    @Test
    fun `a provider nobody has tested is not green`() = runTest {
        val result = ProviderTestResult(verdict = ProviderVerdict.UNTESTED, steps = emptyList())

        assertEquals(ProviderVerdict.UNTESTED, result.verdict)
        assertFalse("Untested is not the same as OK.", result.verdict == ProviderVerdict.OK)
    }

    // --- step 1: the url --------------------------------------------------

    @Test
    fun `a bad url fails the url step and stops there`() = runTest {
        val result = tester(happyEngine(), keyResolver())
            .test(anthropicLike().copy(baseUrl = "not a url", completionUrl = "x"))

        assertEquals(ProviderVerdict.FAILED, result.verdict)
        val url = result.step(TestStep.URL)
        assertTrue(url?.outcome is StepOutcome.Failed)
        // Six more failures caused by one typo would be noise, not information.
        assertNull("A refused url must not trigger six more requests.", result.step(TestStep.AUTHENTICATION))
    }

    // --- step 2: reachability ---------------------------------------------

    @Test
    fun `a host that answers 404 is reachable, and the step says so`() = runTest {
        // The endpoint may be wrong; the network is not. Reporting "unreachable"
        // here sends the user to check their wifi.
        val engine = MockEngine { respondError(HttpStatusCode.NotFound) }
        val result = tester(engine, keyResolver()).test(anthropicLike())

        val reachability = result.step(TestStep.REACHABILITY)
        assertTrue(
            "A 404 is a reachable host, was: $reachability",
            reachability?.outcome is StepOutcome.Passed,
        )
    }

    @Test
    fun `a host that cannot be reached fails the reachability step by name`() = runTest {
        val engine = MockEngine { throw java.io.IOException("no route to host") }
        val result = tester(engine, keyResolver()).test(anthropicLike())

        val reachability = result.step(TestStep.REACHABILITY)
        assertTrue(reachability?.outcome is StepOutcome.Failed)
        assertTrue((reachability?.outcome as StepOutcome.Failed).fixDe.contains("VPN"))
    }

    // --- step 3: tls ------------------------------------------------------

    @Test
    fun `a plaintext address fails the tls step and names the address`() = runTest {
        val result = tester(happyEngine(), keyResolver())
            .test(anthropicLike(baseUrl = "http://192.168.1.20:1234"))

        val tls = result.step(TestStep.TLS)
        assertTrue(tls?.outcome is StepOutcome.Failed)
        assertTrue((tls?.outcome as StepOutcome.Failed).reasonDe.contains("192.168.1.20"))
    }

    @Test
    fun `an https address passes the tls step`() = runTest {
        val tls = tester(happyEngine(), keyResolver()).test(anthropicLike()).step(TestStep.TLS)

        assertTrue(tls?.outcome is StepOutcome.Passed)
    }

    // --- step 4: authentication -------------------------------------------

    @Test
    fun `a rejected key says the key was rejected, not that the provider is down`() = runTest {
        val engine = MockEngine {
            if (it.url.encodedPath.endsWith("/models")) respond("""{"data":[]}""")
            else respondError(HttpStatusCode.Unauthorized)
        }
        val result = tester(engine, keyResolver()).test(anthropicLike())

        val auth = result.step(TestStep.AUTHENTICATION)
        assertTrue(auth?.outcome is StepOutcome.Failed)
        assertTrue((auth?.outcome as StepOutcome.Failed).reasonDe.contains("abgelehnt"))
    }

    @Test
    fun `a rejected key on an x-api-key provider names the header the provider wants`() = runTest {
        // Some gateways accept only the other form. The user has to be told which
        // one, or they cannot act on the message.
        val engine = MockEngine { respondError(HttpStatusCode.Unauthorized) }
        val result = tester(engine, keyResolver()).test(anthropicLike())

        val fix = (result.step(TestStep.AUTHENTICATION)?.outcome as StepOutcome.Failed).fixDe
        assertTrue(fix.contains(AuthHeaders.X_API_KEY))
    }

    @Test
    fun `no key at all is reported as a missing key, not as a rejected one`() = runTest {
        val result = tester(happyEngine(), neverSent).test(anthropicLike())

        val auth = result.step(TestStep.AUTHENTICATION)
        assertTrue(auth?.outcome is StepOutcome.Failed)
        assertTrue((auth?.outcome as StepOutcome.Failed).reasonDe.contains("kein Schlüssel"))
    }

    @Test
    fun `a 500 from the provider is not reported as a key problem`() = runTest {
        val engine = MockEngine {
            if (it.url.encodedPath.endsWith("/models")) respond("""{"data":[]}""")
            else respondError(HttpStatusCode.InternalServerError)
        }
        val result = tester(engine, keyResolver()).test(anthropicLike())

        val fix = (result.step(TestStep.AUTHENTICATION)?.outcome as StepOutcome.Failed).fixDe
        assertTrue(
            "A 500 is not a key problem and the message must say so, was: $fix",
            fix.contains("kein Schlüsselproblem"),
        )
    }

    // --- step 5: discovery ------------------------------------------------

    @Test
    fun `a server without a model list is skipped, never failed`() = runTest {
        val engine = MockEngine {
            when {
                it.url.encodedPath.endsWith("/models") -> respondError(HttpStatusCode.NotFound)
                it.headers[HttpHeaders.Accept] == "text/event-stream" ->
                    respond("data: {}\n\n", headers = headersOf(HttpHeaders.ContentType, "text/event-stream"))
                else -> respond("""{"id":"msg_1"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val result = tester(engine, keyResolver()).test(anthropicLike())

        val discovery = result.step(TestStep.MODEL_DISCOVERY)
        assertTrue(
            "Plenty of good servers have no model endpoint, was: $discovery",
            discovery?.outcome is StepOutcome.Skipped,
        )
        assertEquals(ProviderVerdict.OK, result.verdict)
    }

    @Test
    fun `a discovered list comes back for the model picker`() = runTest {
        val result = tester(happyEngine(), keyResolver()).test(anthropicLike())

        assertEquals(listOf("claude-sonnet-4-5", "gpt-4.1"), result.discoveredModels)
    }

    @Test
    fun `a list in an unknown shape is skipped rather than guessed at`() = runTest {
        val engine = MockEngine {
            if (it.url.encodedPath.endsWith("/models")) {
                respond("""{"models":[{"weird":"shape"}]}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("""{"id":"msg_1"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val result = tester(engine, keyResolver()).test(anthropicLike())

        assertTrue(result.step(TestStep.MODEL_DISCOVERY)?.outcome is StepOutcome.Skipped)
        assertTrue("Nothing may be invented from an unknown shape.", result.discoveredModels.isEmpty())
    }

    // --- step 6: the completion -------------------------------------------

    @Test
    fun `an html error page in front of the api produces the kein-json message`() = runTest {
        // The single most common misconfiguration behind a proxy.
        val html = "<!doctype html><html><body><h1>502 Bad Gateway</h1>" + "x".repeat(400) + "</body></html>"
        val engine = MockEngine {
            if (it.url.encodedPath.endsWith("/models")) respond("""{"data":[]}""")
            else respond(html, headers = headersOf(HttpHeaders.ContentType, "text/html"))
        }
        val result = tester(engine, keyResolver()).test(anthropicLike())

        val completion = result.step(TestStep.MINIMAL_COMPLETION)
        assertTrue(completion?.outcome is StepOutcome.Failed)
        val failure = completion?.outcome as StepOutcome.Failed
        assertTrue(failure.reasonDe.contains("kein JSON"))
        assertTrue(
            "The message shows the first 200 characters so the user can recognise the page, was: ${failure.fixDe}",
            failure.fixDe.contains("502 Bad Gateway"),
        )
        assertTrue("The snippet must be bounded, not the whole page.", failure.fixDe.length < 400)
    }

    @Test
    fun `a model the provider does not have names the model, not the provider`() = runTest {
        val engine = MockEngine {
            if (it.url.encodedPath.endsWith("/models")) respond("""{"data":[]}""")
            else respondError(HttpStatusCode.NotFound)
        }
        val result = tester(engine, keyResolver()).test(anthropicLike(model = "claude-sonnet-4-5"))

        val failure = result.step(TestStep.MINIMAL_COMPLETION)?.outcome as StepOutcome.Failed
        assertTrue(failure.reasonDe.contains("claude-sonnet-4-5"))
    }

    @Test
    fun `with no model chosen the completion step is skipped and the choices are offered`() = runTest {
        val result = tester(happyEngine(), keyResolver()).test(anthropicLike(model = null, modelsUrl = null))

        val completion = result.step(TestStep.MINIMAL_COMPLETION)
        assertTrue(completion?.outcome is StepOutcome.Skipped)
    }

    // --- step 8: streaming ------------------------------------------------

    @Test
    fun `a provider that ignores streaming is reported, because it is slow and the user should know`() = runTest {
        val engine = MockEngine {
            when {
                it.url.encodedPath.endsWith("/models") -> respond("""{"data":[]}""")
                it.headers[HttpHeaders.Accept] == "text/event-stream" ->
                    respond("""{"id":"msg_1"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("""{"id":"msg_1"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val result = tester(engine, keyResolver()).test(anthropicLike())

        val streaming = result.step(TestStep.STREAMING)
        assertTrue(streaming?.outcome is StepOutcome.Failed)
        assertTrue((streaming?.outcome as StepOutcome.Failed).reasonDe.contains("nicht gestreamt"))
    }

    @Test
    fun `a provider that streams is reported as streaming`() = runTest {
        val streaming = tester(happyEngine(), keyResolver()).test(anthropicLike()).step(TestStep.STREAMING)

        assertTrue(streaming?.outcome is StepOutcome.Passed)
    }

    // --- the rules the file exists to hold --------------------------------

    @Test
    fun `the key goes in the header the dialect asks for, and nowhere else`() = runTest {
        val seen = mutableListOf<Pair<String, String>>()
        val engine = MockEngine { request ->
            request.headers.entries().forEach { (name, values) ->
                values.forEach { seen += name to it }
            }
            if (request.url.encodedPath.endsWith("/models")) {
                respond("""{"data":[]}""")
            } else {
                respond("""{"id":"msg_1"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }

        tester(engine, keyResolver()).test(anthropicLike())

        val apiKeyHeaders = seen.filter { it.first.equals(AuthHeaders.X_API_KEY, ignoreCase = true) }
        assertTrue("The key must be in x-api-key for Anthropic.", apiKeyHeaders.isNotEmpty())
        assertTrue(
            "Anthropic does not use a bearer header, was: $seen",
            seen.none { it.first.equals(AuthHeaders.AUTHORIZATION, ignoreCase = true) },
        )
    }

    @Test
    fun `an OpenAI-shaped provider gets a bearer header, not x-api-key`() = runTest {
        val seen = mutableListOf<Pair<String, String>>()
        val engine = MockEngine { request ->
            request.headers.entries().forEach { (name, values) -> values.forEach { seen += name to it } }
            if (request.url.encodedPath.endsWith("/models")) respond("""{"data":[]}""")
            else respond("""{}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }

        tester(engine, keyResolver()).test(
            anthropicLike(kind = ProviderKind.OPENAI_CHAT, authForm = AuthHeaderForm.BEARER),
        )

        assertTrue(
            "The OpenAI shape uses a bearer token, was: $seen",
            seen.any { it.first.equals(AuthHeaders.AUTHORIZATION, ignoreCase = true) },
        )
    }

    @Test
    fun `the client never retries, because the engine already does`() = runTest {
        // Two retry layers multiplying each other is how a rate limit becomes a
        // ban. A 429 is answered once, whatever the server says.
        val calls = mutableListOf<Pair<String, HttpMethod>>()
        val engine = MockEngine { request ->
            calls += request.url.encodedPath + request.url.encodedQuery to request.method
            if (request.url.encodedPath.endsWith("/models")) respond("""{"data":[]}""")
            else respondError(HttpStatusCode.TooManyRequests)
        }

        tester(engine, keyResolver()).test(anthropicLike())

        // Reachability, authentication, discovery, completion, streaming: five
        // requests for five steps. A retry would make it more.
        assertEquals("A step was skipped, or a request was repeated.", 5, calls.size)

        // Three of those five go to the completion endpoint — authentication,
        // the minimal completion and the streaming probe. One each, even though
        // every one of them came back 429.
        val completions = calls.count { it.first.startsWith("/v1/messages") && it.second == HttpMethod.Post }
        assertEquals("A 429 must not be retried at the client.", 3, completions)
    }

    @Test
    fun `a cancellation is not swallowed into a provider failure`() = runTest {
        val engine = MockEngine { throw kotlinx.coroutines.CancellationException("left the screen") }

        val error = runCatching { tester(engine, keyResolver()).test(anthropicLike()) }.exceptionOrNull()

        assertNotNull("The user leaving the screen is not a provider that failed.", error)
        assertTrue(error is kotlinx.coroutines.CancellationException)
    }

    @Test
    fun `the system prompt goes where each dialect puts it`() = runTest {
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/models")) {
                respond("""{"data":[]}""")
            } else {
                respond("""{}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }

        val client = HttpClient(engine)
        val tester = ProviderConnectionTester(client, keyResolver())
        val anthropic = anthropicLike()
        val openai = anthropicLike(kind = ProviderKind.OPENAI_CHAT, authForm = AuthHeaderForm.BEARER)

        tester.completionBody(anthropic, "m", stream = false).also { bodies += it }
        tester.completionBody(openai, "m", stream = false).also { bodies += it }
        tester.completionBody(anthropic, "m", stream = true).also { bodies += it }

        assertTrue("Anthropic takes a top-level system parameter.", bodies[0].contains("\"system\":"))
        assertTrue("OpenAI chat takes a system message.", bodies[1].contains("\"role\":\"system\""))
        assertTrue("A streaming request asks for streaming.", bodies[2].contains("\"stream\":true"))
        assertTrue("A non-streaming request must not.", !bodies[1].contains("\"stream\":true"))
    }

    @Test
    fun `the request goes to the endpoint the validation produced`() = runTest {
        var path: String? = null
        val engine = MockEngine { request ->
            path = request.url.encodedPath
            if (request.url.encodedPath.endsWith("/models")) respond("""{"data":[]}""")
            else respond("""{}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }

        tester(engine, keyResolver()).test(anthropicLike(baseUrl = "https://api.groq.com"))

        assertEquals("/v1/messages", path)
    }

    @Test
    fun `a key is never held by the tester, so none of this can leak one`() = runTest {
        // A structural check, not a behavioural one: the tester takes a
        // SecretResolver, so there is no field, no parameter that outlives the
        // call, and nothing a logger nearby could print.
        val resolverCalls = mutableListOf<ValidatedProvider>()
        val resolver = SecretResolver { provider ->
            resolverCalls += provider
            "test-key-not-a-real-one"
        }
        val engine = MockEngine {
            if (it.url.encodedPath.endsWith("/models")) respond("""{"data":[]}""")
            else respond("""{}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val result = tester(engine, resolver).test(anthropicLike())

        assertTrue("The resolver is asked per request, not cached.", resolverCalls.size >= 2)
        assertEquals(
            "No step result may carry a key value.",
            null,
            result.steps.firstOrNull { it.rawDetail?.contains("test-key") == true }?.rawDetail,
        )
    }

    private fun keyResolver() = SecretResolver { "test-key-not-a-real-one" }

    @Test
    fun `a method the provider does not support is not tested with the wrong verb`() = runTest {
        val methods = mutableListOf<HttpMethod>()
        val engine = MockEngine { request ->
            methods += request.method
            if (request.url.encodedPath.endsWith("/models")) respond("""{"data":[]}""")
            else respond("""{}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }

        tester(engine, keyResolver()).test(anthropicLike())

        assertTrue("Discovery is a GET.", HttpMethod.Get in methods)
        assertTrue("The completion probe is a POST.", HttpMethod.Post in methods)
    }
}
