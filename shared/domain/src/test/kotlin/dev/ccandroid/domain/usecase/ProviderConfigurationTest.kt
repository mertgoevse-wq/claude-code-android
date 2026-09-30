package dev.ccandroid.domain.usecase

import dev.ccandroid.core.Outcome
import dev.ccandroid.domain.ProviderKind
import dev.ccandroid.domain.provider.AuthHeaderForm
import dev.ccandroid.domain.provider.AuthHeaders
import dev.ccandroid.domain.provider.ProviderCatalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Provider configuration, per `docs/07-integrations/providers.md` §The
 * configuration and §Adding a provider.
 *
 * Each test names the failure it prevents, because a refused field that does not
 * say what would have happened sends the user to the wrong page.
 */
class ProviderConfigurationTest {

    private fun draft(
        name: String = "Rechner daheim",
        kind: ProviderKind = ProviderKind.OPENAI_CHAT,
        baseUrl: String = "http://192.168.1.20:1234",
        pathTemplate: String? = null,
        authForm: AuthHeaderForm? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        model: String? = null,
        catalogueId: String? = null,
        localNetworkConfirmed: Boolean = true,
    ) = ProviderDraft(
        name = name,
        kind = kind,
        baseUrl = baseUrl,
        pathTemplate = pathTemplate,
        authForm = authForm,
        extraHeaders = extraHeaders,
        model = model,
        catalogueId = catalogueId,
        localNetworkConfirmed = localNetworkConfirmed,
    )

    private fun validate(draft: ProviderDraft) = ValidateProviderDraft(draft)

    /** The machine-readable reason, which is what the screen branches on. */
    private fun detailOf(outcome: Outcome<*>): String? = when (val error = outcome.errorOrNull()) {
        is dev.ccandroid.core.AppError.Simple -> error.details
        else -> null
    }

    // --- names ------------------------------------------------------------

    @Test
    fun `a name is required, and the message says so in both languages`() {
        val outcome = validate(draft(name = "   "))

        assertTrue(outcome.isFailure)
        assertEquals("PROVIDER_NAME_EMPTY", detailOf(outcome))
        assertEquals("Gib dem Anbieter einen Namen.", outcome.errorOrNull()?.messageDe)
        assertNotNull(outcome.errorOrNull()?.messageEn)
    }

    @Test
    fun `a name is trimmed, because a trailing space makes two providers look alike`() {
        val result = validate(draft(name = "  Rechner daheim  ")).getOrNull()

        assertEquals("Rechner daheim", result?.name)
    }

    // --- url --------------------------------------------------------------

    @Test
    fun `a base url that is not a url is refused with an example, not a shrug`() {
        val outcome = validate(draft(baseUrl = "mein-server"))

        assertEquals("PROVIDER_URL_INVALID", detailOf(outcome))
        assertTrue(
            "The message must show what a valid one looks like, was: ${outcome.errorOrNull()?.messageDe}",
            outcome.errorOrNull()?.messageDe?.contains("https://api.example.com") == true,
        )
    }

    @Test
    fun `plaintext http to a public host is refused, naming the host`() {
        val outcome = validate(draft(baseUrl = "http://api.example.com"))

        assertEquals("PROVIDER_URL_PLAINTEXT_PUBLIC", detailOf(outcome))
        assertTrue(outcome.errorOrNull()?.messageDe?.contains("api.example.com") == true)
    }

    @Test
    fun `plaintext http to a local host is allowed once the user has been told`() {
        val result = validate(draft(baseUrl = "http://192.168.1.20:1234", localNetworkConfirmed = true)).getOrNull()

        assertEquals("http://192.168.1.20:1234", result?.baseUrl)
    }

    @Test
    fun `plaintext http to a local host without the confirmation is not an error, it is a question`() {
        val outcome = validate(draft(baseUrl = "http://192.168.1.20:1234", localNetworkConfirmed = false))

        assertTrue(outcome.isFailure)
        assertEquals(ValidateProviderDraft.REASON_LOCAL_PLAINTEXT, detailOf(outcome))
        assertTrue(
            "The question must name the address, was: ${outcome.errorOrNull()?.messageDe}",
            outcome.errorOrNull()?.messageDe?.contains("192.168.1.20") == true,
        )
    }

    // --- headers ----------------------------------------------------------

    @Test
    fun `a custom Authorization header is refused, because it would shadow the key`() {
        val outcome = validate(draft(extraHeaders = mapOf("Authorization" to "Bearer something")))

        assertEquals("PROVIDER_HEADER_RESERVED", detailOf(outcome))
    }

    @Test
    fun `the reserved header check is case insensitive, because HTTP header names are`() {
        val outcome = validate(draft(extraHeaders = mapOf("X-API-Key" to "something")))

        assertEquals("PROVIDER_HEADER_RESERVED", detailOf(outcome))
    }

    @Test
    fun `a header the app does not set itself is allowed through`() {
        val result = validate(draft(extraHeaders = mapOf("X-Project-Id" to "team-7"))).getOrNull()

        assertEquals("team-7", result?.extraHeaders?.get("X-Project-Id"))
    }

    // --- endpoint ---------------------------------------------------------

    @Test
    fun `the completed endpoint is what the request will actually go to`() {
        val result = validate(
            draft(kind = ProviderKind.OPENAI_CHAT, baseUrl = "https://api.groq.com", pathTemplate = "/openai/v1/chat/completions"),
        ).getOrNull()

        assertEquals("https://api.groq.com/openai/v1/chat/completions", result?.completionUrl)
    }

    @Test
    fun `a provider with no path gets the one its dialect uses`() {
        val result = validate(
            draft(kind = ProviderKind.ANTHROPIC, baseUrl = "https://api.anthropic.com", pathTemplate = null),
        ).getOrNull()

        assertEquals("https://api.anthropic.com/v1/messages", result?.completionUrl)
    }

    @Test
    fun `the base is stored as an origin, so a trailing path is not saved twice`() {
        val result = validate(
            draft(kind = ProviderKind.OPENAI_CHAT, baseUrl = "https://api.groq.com/", pathTemplate = "/openai/v1/chat/completions"),
        ).getOrNull()

        assertEquals("https://api.groq.com", result?.baseUrl)
    }

    // --- auth form --------------------------------------------------------

    @Test
    fun `the auth form follows the dialect`() {
        assertEquals(
            AuthHeaderForm.X_API_KEY,
            validate(draft(kind = ProviderKind.ANTHROPIC, baseUrl = "https://api.anthropic.com")).getOrNull()?.authForm,
        )
        assertEquals(
            AuthHeaderForm.BEARER,
            validate(draft(kind = ProviderKind.OPENAI_CHAT, baseUrl = "https://api.openai.com")).getOrNull()?.authForm,
        )
    }

    @Test
    fun `a custom provider keeps the form the user declared`() {
        val result = validate(
            draft(kind = ProviderKind.CUSTOM, baseUrl = "https://gw.example", authForm = AuthHeaderForm.X_API_KEY),
        ).getOrNull()

        assertEquals(AuthHeaderForm.X_API_KEY, result?.authForm)
    }

    // --- model ------------------------------------------------------------

    @Test
    fun `an unpinned model alias is refused, because the run would not be reproducible`() {
        val outcome = validate(draft(model = "gpt-4o-latest"))

        assertEquals("PROVIDER_MODEL_UNPINNED", detailOf(outcome))
    }

    @Test
    fun `a pinned model id is accepted`() {
        assertEquals("gpt-4.1", validate(draft(model = "gpt-4.1")).getOrNull()?.model)
    }

    // --- catalogue --------------------------------------------------------

    @Test
    fun `a provider from the catalogue gets the models path the catalogue says`() {
        val result = validate(
            draft(
                name = "OpenRouter",
                kind = ProviderKind.OPENAI_CHAT,
                baseUrl = "https://openrouter.ai",
                pathTemplate = "/api/v1/chat/completions",
                catalogueId = "openrouter",
            ),
        ).getOrNull()

        assertEquals("https://openrouter.ai/api/v1/models", result?.modelsUrl)
    }

    @Test
    fun `a provider typed by hand has no models url until it is tested`() {
        val result = validate(draft(catalogueId = null)).getOrNull()

        assertNull("Discovery has not run yet, so there is nothing to point at.", result?.modelsUrl)
    }

    // --- changing the kind ------------------------------------------------

    @Test
    fun `changing the dialect resets the path, because the old one means nothing to the new one`() {
        val change = ChangeProviderKind(from = ProviderKind.ANTHROPIC, to = ProviderKind.OPENAI_CHAT).getOrNull()

        assertEquals(ProviderKind.OPENAI_CHAT, change?.kind)
        assertEquals("/v1/chat/completions", change?.pathTemplate)
    }

    @Test
    fun `changing the dialect names the header the user has to remove`() {
        val change = ChangeProviderKind(from = ProviderKind.OPENAI_CHAT, to = ProviderKind.ANTHROPIC).getOrNull()

        assertEquals(listOf(AuthHeaders.X_API_KEY), change?.headersToDrop)
    }

    @Test
    fun `a dialect change to the same dialect drops nothing`() {
        val change = ChangeProviderKind(from = ProviderKind.CUSTOM, to = ProviderKind.CUSTOM).getOrNull()

        assertTrue(change?.headersToDrop?.isEmpty() == true)
    }

    // --- default model ----------------------------------------------------

    @Test
    fun `the default model is a known id when the server offers one`() {
        assertEquals("llama-3.3-70b-versatile", ChooseDefaultModel(listOf("some-other-model", "llama-3.3-70b-versatile")))
    }

    @Test
    fun `the default model is the first from the server when none is known`() {
        assertEquals("house/custom-v2", ChooseDefaultModel(listOf("house/custom-v2", "house/custom-v1")))
    }

    @Test
    fun `the default model is never an unpinned alias, even first in the list`() {
        assertEquals("gpt-4.1", ChooseDefaultModel(listOf("gpt-4o-latest", "gpt-4.1")))
    }

    @Test
    fun `a list of nothing but aliases yields no default at all, rather than a wrong one`() {
        assertNull(ChooseDefaultModel(listOf("latest", "gpt-4o-latest")))
        assertNull(ChooseDefaultModel(emptyList()))
    }

    @Test
    fun `a catalogue entry the picker offers is never a hard failure`() {
        // The catalogue and the validator have to agree. If one entry drifts, the
        // user picks a provider from the list and is then told it is invalid.
        // A self-hosted entry is allowed to ask for the one-time plaintext
        // confirmation — that is the question, not a refusal.
        ProviderCatalogue.entries.forEach { entry ->
            if (entry.baseUrl.isEmpty()) return@forEach
            val outcome = validate(
                draft(
                    name = entry.displayName,
                    kind = entry.kind,
                    baseUrl = entry.baseUrl,
                    pathTemplate = entry.pathTemplate,
                    catalogueId = entry.id,
                    localNetworkConfirmed = false,
                ),
            )
            val detail = detailOf(outcome)
            assertTrue(
                "Catalogue entry '${entry.id}' is offered in the picker but is refused: " +
                    "${outcome.errorOrNull()?.messageDe}",
                outcome.isSuccess || detail == ValidateProviderDraft.REASON_LOCAL_PLAINTEXT,
            )
        }
    }

    @Test
    fun `every self-hosted catalogue entry is reachable once the user accepts the warning`() {
        ProviderCatalogue.entries
            .filter { it.baseUrl.startsWith("http://") }
            .forEach { entry ->
                val result = validate(
                    draft(
                        name = entry.displayName,
                        kind = entry.kind,
                        baseUrl = entry.baseUrl,
                        pathTemplate = entry.pathTemplate,
                        catalogueId = entry.id,
                        localNetworkConfirmed = true,
                    ),
                ).getOrNull()
                assertNotNull(
                    "Catalogue entry '${entry.id}' cannot be configured at all, even after the " +
                        "user accepts the plaintext warning: ${detailOf(validate(draft(baseUrl = entry.baseUrl, kind = entry.kind, pathTemplate = entry.pathTemplate, catalogueId = entry.id, localNetworkConfirmed = true)))}",
                    result,
                )
            }
    }
}
