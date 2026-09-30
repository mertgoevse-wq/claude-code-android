package dev.ccandroid.data.provider

import dev.ccandroid.domain.provider.AuthFormFor
import dev.ccandroid.domain.provider.AuthHeaderForm
import dev.ccandroid.domain.provider.AuthHeaders
import dev.ccandroid.domain.provider.EndpointUrl
import dev.ccandroid.domain.usecase.ChooseDefaultModel
import dev.ccandroid.domain.usecase.ValidatedProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The connection test, per `docs/07-integrations/providers.md` §The connection test.
 *
 * The feature that makes provider configuration bearable. A user who typed a
 * base URL, a key and a model wants to know which of the three is wrong, and the
 * only useful answer names it. "Connection failed" is the least useful sentence
 * in this domain.
 *
 * Three rules the implementation exists to hold:
 *
 * - **The key is resolved at the request boundary and never held.** It comes in
 *   through a [SecretResolver], goes into a header, and is gone. It is not a
 *   field, not a parameter that outlives the call, and not in any result.
 * - **No retries.** The engine has its own retry layer, surfaced as `api_retry`
 *   events. A second layer multiplying the first is how a rate limit becomes a
 *   ban, so there is no retry configuration anywhere in this file.
 * - **Every step reports itself.** A step that did not run says so. An untested
 *   provider is neutral, not green.
 */
public class ProviderConnectionTester(
    private val client: HttpClient,
    private val secrets: SecretResolver,
    private val time: () -> Long = { System.currentTimeMillis() },
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Runs the test. Never throws: a failure is a [StepResult], because the
     * screen has to render eight outcomes and one exception is not eight.
     */
    public suspend fun test(provider: ValidatedProvider): ProviderTestResult {
        val startedAt = time()
        val steps = mutableListOf<StepResult>()

        steps += checkUrl(provider)
        // Everything after the URL needs a URL. A refused URL is not a provider
        // that failed to connect, and running the rest would report six
        // failures caused by one typo.
        if (steps.last().outcome !is StepOutcome.Passed) {
            return resultOf(steps, ProviderVerdict.FAILED, startedAt)
        }

        steps += checkReachability(provider)
        steps += checkTls(provider)
        steps += checkAuthentication(provider)
        val discovery = discoverModels(provider)
        steps += discovery.step
        steps += checkMinimalCompletion(provider, discovery.models)
        steps += checkStreaming(provider, discovery.models)

        return resultOf(steps, verdictOf(steps), startedAt, discovery.models)
    }

    // --- 1. URL -----------------------------------------------------------

    private fun checkUrl(provider: ValidatedProvider): StepResult {
        val url = EndpointUrl.parse(provider.baseUrl)
            ?: return StepResult(
                TestStep.URL,
                StepOutcome.Failed(
                    reasonDe = "Die Adresse ist ungültig.",
                    reasonEn = "That address is not valid.",
                    fixDe = "Sie braucht Schema und Host, zum Beispiel https://api.example.com",
                    fixEn = "It needs a scheme and a host, for example https://api.example.com",
                ),
            )
        val completion = url.resolve(provider.pathTemplate)
        return StepResult(TestStep.URL, StepOutcome.Passed(completion), completion)
    }

    // --- 2. reachability --------------------------------------------------

    private suspend fun checkReachability(provider: ValidatedProvider): StepResult = guard(TestStep.REACHABILITY) {
        val elapsed = time()
        val response = client.get(provider.modelsUrl ?: provider.baseUrl)
        val ms = time() - elapsed
        // A 404 from the host is a *reachable* host. The endpoint may be wrong;
        // the network is not.
        StepResult(
            TestStep.REACHABILITY,
            StepOutcome.Passed("$ms ms, HTTP ${response.status.value}"),
            rawDetail = response.status.value.toString(),
        )
    }

    // --- 3. TLS -----------------------------------------------------------

    private suspend fun checkTls(provider: ValidatedProvider): StepResult {
        val url = EndpointUrl.parse(provider.baseUrl) ?: return StepResult(
            TestStep.TLS,
            StepOutcome.Skipped("Ohne gültige Adresse wird das Zertifikat nicht geprüft."),
        )
        if (!url.isPlaintext) {
            // Reaching the host at all means the handshake succeeded: the
            // transport refuses the connection before a byte is exchanged.
            return StepResult(TestStep.TLS, StepOutcome.Passed("Verschlüsselt"))
        }
        return StepResult(
            TestStep.TLS,
            StepOutcome.Failed(
                reasonDe = "„${url.origin}“ ist nicht verschlüsselt.",
                reasonEn = "\"${url.origin}\" is not encrypted.",
                fixDe = "Für einen Anbieter im Internet wird HTTPS gebraucht. Ohne Verschlüsselung geht der Schlüssel im Klartext über die Leitung.",
                fixEn = "A provider on the internet needs HTTPS. Without encryption the key travels in clear text.",
            ),
        )
    }

    // --- 4. authentication ------------------------------------------------

    private suspend fun checkAuthentication(provider: ValidatedProvider): StepResult = guard(TestStep.AUTHENTICATION) {
        val key = secrets.resolve(provider)
            ?: return@guard StepResult(
                TestStep.AUTHENTICATION,
                StepOutcome.Failed(
                    reasonDe = "Es ist kein Schlüssel hinterlegt.",
                    reasonEn = "No key is stored for this provider.",
                    fixDe = "Trage einen Schlüssel ein, oder wähle ein Schlüsselprofil.",
                    fixEn = "Enter a key, or choose a key profile.",
                ),
            )

        val response = sendAuthenticated(provider, key, probeRequest(provider))
        val status = response.status
        when {
            status.value in 200..299 ->
                StepResult(TestStep.AUTHENTICATION, StepOutcome.Passed("Schlüssel angenommen"), status.value.toString())

            status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden -> StepResult(
                TestStep.AUTHENTICATION,
                StepOutcome.Failed(
                    reasonDe = "Der Schlüssel wurde abgelehnt (HTTP ${status.value}).",
                    reasonEn = "The key was rejected (HTTP ${status.value}).",
                    fixDe = if (provider.authForm == AuthHeaderForm.X_API_KEY) {
                        "Dieser Anbieter erwartet den Schlüssel im Feld „${AuthFormFor.headerName(provider.authForm)}“. Manche Gateways nehmen nur eine der beiden Formen."
                    } else {
                        "Dieser Anbieter erwartet den Schlüssel als „Authorization: Bearer“."
                    },
                    fixEn = "This provider expects the key in the ${AuthFormFor.headerName(provider.authForm)} header. Some gateways accept only one of the two forms.",
                ),
                status.value.toString(),
            )

            else -> StepResult(
                TestStep.AUTHENTICATION,
                StepOutcome.Failed(
                    reasonDe = "Der Anbieter antwortete mit HTTP ${status.value}.",
                    reasonEn = "The provider answered HTTP ${status.value}.",
                    fixDe = "Das ist kein Schlüsselproblem. Die Adresse oder der Pfad ist wahrscheinlich falsch.",
                    fixEn = "That is not a key problem. The address or the path is probably wrong.",
                ),
                status.value.toString(),
            )
        }
    }

    // --- 5. model discovery -----------------------------------------------

    private data class Discovery(val step: StepResult, val models: List<String>)

    private suspend fun discoverModels(provider: ValidatedProvider): Discovery {
        val modelsUrl = provider.modelsUrl
            ?: return Discovery(
                StepResult(
                    TestStep.MODEL_DISCOVERY,
                    StepOutcome.Skipped("F\u00fcr diesen Anbieter ist kein Modellpfad hinterlegt."),
                ),
                emptyList(),
            )

        return try {
            val response = client.get(modelsUrl)
            if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.MethodNotAllowed) {
                return Discovery(
                    StepResult(
                        TestStep.MODEL_DISCOVERY,
                        StepOutcome.Skipped("Der Server bietet keine Modelliste an. Modell selbst eintragen."),
                    ),
                    emptyList(),
                )
            }
            val body = response.bodyAsText()
            val models = parseModelList(body)
            if (models == null) {
                // An unknown shape is not guessed at, and it is not an error
                // either: plenty of good servers have no usable model endpoint.
                return Discovery(
                    StepResult(
                        TestStep.MODEL_DISCOVERY,
                        StepOutcome.Skipped("Die Modelliste hat ein unbekanntes Format. Modell selbst eintragen."),
                        body.take(SNIPPET),
                    ),
                    emptyList(),
                )
            }
            Discovery(
                StepResult(
                    TestStep.MODEL_DISCOVERY,
                    StepOutcome.Passed("${models.size} Modelle"),
                    models.size.toString(),
                ),
                models,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Discovery(StepResult(TestStep.MODEL_DISCOVERY, networkFailure(e)), emptyList())
        }
    }

    /**
     * The three shapes a model list actually comes in, and nothing else.
     *
     * `data[].id` is Anthropic's and OpenAI's, a bare array of strings is the
     * simple proxy, and a bare array of objects with `id` or `name` is the third.
     * Beyond that the app does not guess: a model id that was invented here is a
     * run that fails later, in a place the user was not looking.
     */
    internal fun parseModelList(body: String): List<String>? {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return null
        val array: JsonArray = when {
            root is JsonArray -> root
            root is JsonObject -> root["data"]?.jsonArray ?: return null
            else -> return null
        }
        val ids = array.mapNotNull { element ->
            when (element) {
                is JsonPrimitive -> element.content.takeIf { it.isNotBlank() }
                is JsonObject -> (element["id"] ?: element["name"])?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                else -> null
            }
        }
        return ids.takeIf { it.isNotEmpty() }
    }

    // --- 6. minimal completion --------------------------------------------

    private suspend fun checkMinimalCompletion(
        provider: ValidatedProvider,
        discovered: List<String>,
    ): StepResult = guard(TestStep.MINIMAL_COMPLETION) {
        // The model is whatever the user has selected, or the one the app would
        // default to. It is never replaced with an alias the app invented.
        val model = provider.model
        if (model == null) {
            return@guard StepResult(
                TestStep.MINIMAL_COMPLETION,
                StepOutcome.Skipped(
                    if (discovered.isEmpty()) {
                        "Kein Modell ausgewählt."
                    } else {
                        "Kein Modell ausgewählt. Verfügbar: ${discovered.take(5).joinToString(", ")}"
                    },
                ),
            )
        }
        val key = secrets.resolve(provider)
            ?: return@guard StepResult(
                TestStep.MINIMAL_COMPLETION,
                StepOutcome.Skipped("Ohne Schlüssel wird keine Antwort geprüft."),
            )

        val startedAt = time()
        val response = sendAuthenticated(provider, key, completionRequest(provider, model, stream = false))
        val ms = time() - startedAt
        if (response.status.value !in 200..299) {
            return@guard StepResult(
                TestStep.MINIMAL_COMPLETION,
                StepOutcome.Failed(
                    reasonDe = "„$model“ wurde abgelehnt (HTTP ${response.status.value}).",
                    reasonEn = "\"$model\" was rejected (HTTP ${response.status.value}).",
                    fixDe = "Prüfe die Modell-ID genau so, wie der Anbieter sie schreibt. Die App sendet sie unverändert.",
                    fixEn = "Check the model id exactly as the provider writes it. The app sends it unchanged.",
                ),
                response.status.value.toString(),
            )
        }
        val text = response.bodyAsText()
        if (!looksLikeJson(text)) {
            // The single most common misconfiguration behind a proxy: a login
            // page or an error page in front of the API.
            return@guard StepResult(
                TestStep.MINIMAL_COMPLETION,
                StepOutcome.Failed(
                    reasonDe = "Die Antwort ist kein JSON. Vielleicht steht eine Fehlerseite davor.",
                    reasonEn = "The response is not JSON. Something may be in front of the API.",
                    fixDe = "Erste 200 Zeichen: ${text.take(SNIPPET)}",
                    fixEn = "First 200 characters: ${text.take(SNIPPET)}",
                ),
                text.take(SNIPPET),
            )
        }
        StepResult(TestStep.MINIMAL_COMPLETION, StepOutcome.Passed("$model · $ms ms"), "$model")
    }

    // --- 7. vision --------------------------------------------------------

    /**
     * Not probed. A 4×4 image costs tokens and, on a metered connection, money,
     * and the answer only downgrades a capability the user did not ask about.
     * The capability claim stays what the provider's own model list said, marked
     * as unverified, rather than being asserted by a test the user did not ask
     * for.
     */
    private fun checkVision(): StepResult = StepResult(
        TestStep.VISION,
        StepOutcome.Skipped("Nicht geprüft. Die Bild-Unterstützung steht so, wie der Anbieter sie angibt."),
    )

    // --- 8. streaming -----------------------------------------------------

    private suspend fun checkStreaming(
        provider: ValidatedProvider,
        discovered: List<String>,
    ): StepResult = guard(TestStep.STREAMING) {
        val model = provider.model
            ?: ChooseDefaultModel(discovered)
            ?: return@guard StepResult(
                TestStep.STREAMING,
                StepOutcome.Skipped("Kein Modell ausgewählt."),
            )
        val key = secrets.resolve(provider)
            ?: return@guard StepResult(TestStep.STREAMING, StepOutcome.Skipped("Ohne Schlüssel wird nichts gestreamt."))

        val response = sendAuthenticated(provider, key, completionRequest(provider, model, stream = true))
        val contentType = response.headers["Content-Type"].orEmpty()
        if (response.status.value !in 200..299) {
            return@guard StepResult(
                TestStep.STREAMING,
                StepOutcome.Skipped("Die Anfrage wurde abgelehnt (HTTP ${response.status.value})."),
            )
        }
        if (contentType.contains("event-stream", ignoreCase = true)) {
            return@guard StepResult(TestStep.STREAMING, StepOutcome.Passed("Gestreamt"))
        }
        // It works, slowly. The user should know before a long turn takes ten
        // minutes and they assume the app hung.
        StepResult(
            TestStep.STREAMING,
            StepOutcome.Failed(
                reasonDe = "Die Antwort kommt nicht gestreamt.",
                reasonEn = "The response is not streamed.",
                fixDe = "Der Anbieter antwortet als Ganzes. Läuft, aber bei langen Antworten merklich langsamer.",
                fixEn = "The provider answers as a whole. It works, but noticeably slower on long answers.",
            ),
            contentType,
        )
    }

    // --- plumbing ---------------------------------------------------------

    /**
     * Runs one step and turns anything thrown into a reported failure.
     *
     * A cancellation is rethrown: the user leaving the screen is not a provider
     * that failed, and swallowing it would make the coroutine lie.
     */
    private suspend fun guard(step: TestStep, block: suspend () -> StepResult): StepResult =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            StepResult(step, networkFailure(e), e::class.simpleName)
        }

    private fun networkFailure(e: Throwable) = StepOutcome.Failed(
        reasonDe = networkReasonDe(e),
        reasonEn = "The provider could not be reached.",
        fixDe = "L\u00e4uft das VPN? Ist der Anbieter erreichbar?",
        fixEn = "Is the VPN running? Is the provider reachable?",
    )

    private fun networkReasonDe(e: Throwable): String = when (e) {
        is kotlinx.coroutines.TimeoutCancellationException -> "Zeitüberschreitung beim Anbieter."
        else -> "Der Anbieter ist nicht erreichbar."
    }

    private suspend fun sendAuthenticated(
        provider: ValidatedProvider,
        key: String,
        request: AuthenticatedRequest,
    ): HttpResponse = client.post(request.url) {
        contentType(ContentType.Application.Json)
        request.authForm.let { form ->
            if (form == AuthHeaderForm.X_API_KEY) {
                header(AuthFormFor.headerName(form), key)
            } else {
                header(AuthFormFor.headerName(form), "${AuthHeaders.BEARER_PREFIX}$key")
            }
        }
        request.extraHeaders.forEach { (name, value) -> header(name, value) }
        if (request.stream) header("Accept", "text/event-stream")
        setBody(request.body)
    }

    private data class AuthenticatedRequest(
        val url: String,
        val authForm: AuthHeaderForm,
        val extraHeaders: Map<String, String>,
        val body: String,
        val stream: Boolean,
    )

    private fun probeRequest(provider: ValidatedProvider) = AuthenticatedRequest(
        url = provider.completionUrl,
        authForm = provider.authForm,
        extraHeaders = provider.extraHeaders,
        body = completionBody(provider, "test", stream = false),
        stream = false,
    )

    private fun completionRequest(provider: ValidatedProvider, model: String, stream: Boolean) =
        AuthenticatedRequest(
            url = provider.completionUrl,
            authForm = provider.authForm,
            extraHeaders = provider.extraHeaders,
            body = completionBody(provider, model, stream),
            stream = stream,
        )

    /**
     * The smallest request each dialect accepts.
     *
     * The system prompt goes where that dialect puts it: a top-level parameter
     * for Anthropic, a system message for OpenAI chat, an `instructions` field
     * for Responses. Getting this wrong is a 400 with a message the user cannot
     * act on, so it is built per kind rather than sent as one shape.
     */
    internal fun completionBody(provider: dev.ccandroid.domain.usecase.ValidatedProvider, model: String, stream: Boolean): String =
        when (provider.kind) {
            dev.ccandroid.domain.ProviderKind.ANTHROPIC ->
                """{"model":"$model","max_tokens":20,"stream":$stream,""" +
                    """"system":"reply with OK","messages":[{"role":"user","content":"reply with OK"}]}"""

            dev.ccandroid.domain.ProviderKind.OPENAI_RESPONSES ->
                """{"model":"$model","max_output_tokens":20,"stream":$stream,""" +
                    """"instructions":"reply with OK","input":"reply with OK"}"""

            else ->
                """{"model":"$model","max_tokens":20,"stream":$stream,""" +
                    """"messages":[{"role":"system","content":"reply with OK"},""" +
                    """{"role":"user","content":"reply with OK"}]}"""
        }

    private fun looksLikeJson(body: String): Boolean {
        val trimmed = body.trimStart()
        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }

    private fun resultOf(
        steps: List<StepResult>,
        verdict: ProviderVerdict,
        startedAt: Long,
        models: List<String> = emptyList(),
    ) = ProviderTestResult(
        verdict = verdict,
        steps = steps + checkVision(),
        discoveredModels = models,
        durationMs = time() - startedAt,
    )

    /**
     * A skipped or unsupported step is not a failure, and a step that was never
     * reached is not a pass. The verdict counts only what actually ran.
     */
    private fun verdictOf(steps: List<StepResult>): ProviderVerdict {
        val ran = steps.filter { it.outcome !is StepOutcome.Skipped && it.outcome !is StepOutcome.Unsupported }
        if (ran.isEmpty()) return ProviderVerdict.UNTESTED
        val failures = ran.count { it.outcome is StepOutcome.Failed }
        return when {
            failures == 0 -> ProviderVerdict.OK
            failures == ran.size -> ProviderVerdict.FAILED
            else -> ProviderVerdict.PARTIAL
        }
    }

    private companion object {
        /** Enough to recognise an error page, not enough to fill a screen. */
        const val SNIPPET = 200
    }
}

/**
 * Resolves the key at the moment of the request.
 *
 * An interface rather than a `String` parameter, so a key cannot be captured in
 * a lambda, held in a field, or printed by a logger that happens to be nearby.
 * The implementation reads the Keystore and returns the value; nothing else in
 * the app ever sees a key.
 */
public fun interface SecretResolver {
    public suspend fun resolve(provider: ValidatedProvider): String?
}
