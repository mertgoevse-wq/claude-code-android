# Providers

Talking to whatever AI service the user has, in whatever dialect it speaks, without pretending they are the same.

## The dialects

| Kind | Path | Used for |
|---|---|---|
| `ANTHROPIC` | `/v1/messages` | Anthropic, and any gateway that speaks it: OpenRouter, Bedrock, Vertex, a local proxy |
| `OPENAI_CHAT` | `/v1/chat/completions` | OpenAI, Groq, Together, Fireworks, LM Studio, llama.cpp, Ollama's shim, most gateways |
| `OPENAI_RESPONSES` | `/v1/responses` | Newer OpenAI models, and providers adopting that shape |
| `CUSTOM` | User-configured | Anything that speaks one of the above, at any path |

The differences are in `01-research/provider-api-comparison.md`. This document is the implementation.

## The configuration

```kotlin
data class ProviderConfig(
    val id: ProviderId,
    val name: String,
    val kind: ProviderKind,
    val baseUrl: String,
    val pathTemplate: String?,      // CUSTOM only
    val secretProfileId: SecretRef,
    val models: List<ModelSpec>,
    val headers: Map<String, String>,
    val isEnabled: Boolean,
    val isDefault: Boolean,
    val lastTestedAt: Long?,
    val lastTestResult: TestResult?,
)
```

| Field | Validation |
|---|---|
| `name` | Non-empty, unique among providers |
| `kind` | One of the four. Changing it resets `pathTemplate` and warns, because the two are meaningless to each other. |
| `baseUrl` | A valid https URL, or http for `localhost` and a private range only. A plain http URL to a public host is refused with a reason. |
| `pathTemplate` | For `CUSTOM`. Rendered with the placeholders the kind implies, and the result is shown in a preview. |
| `secretProfileId` | Must resolve. A provider with a dangling reference is shown as broken, not silently disabled. |
| `models` | Manual entries, plus discovered ones. Both are kept; discovery never overwrites a manual entry. |
| `headers` | Key-value. A test asserts the `Authorization` header cannot be set here, because it would shadow the key. |

## Which host may be plain http

| Allowed http | Refused |
|---|---|
| `localhost` | Any public host |
| `127.0.0.1`, `::1` | A private RFC 1918 address the user has not confirmed |
| A private range, after a one-time confirm: "Diese Adresse ist in deinem lokalen Netz. Verbindung ohne Verschlüsselung." | Anything else |

A local model on the home network is a real and expected use. So is a phone-to-phone setup. What is not acceptable is silently sending a key in clear text to an arbitrary host, and the confirmation is the mechanism for the legitimate case.

## The client

| Aspect | Behaviour |
|---|---|
| Transport | Ktor, `OkHttp` on Android, `CIO` elsewhere |
| Timeouts | Connect 15 s, request 120 s for a normal call, and a separate long timeout for a streaming turn, which can run for minutes |
| Streaming | Server-sent events, parsed incrementally. A response is never buffered whole, because a streaming turn can be 2 MB of deltas. |
| Retries | The app does not retry a request. Claude Code's own `api_retry` events are surfaced, and the app's own retry is at the *turn* level, per `05-features/retry-and-self-healing.md`. Two retry layers multiplying each other is how a rate limit becomes a ban. |
| Compression | Request and response, when the server offers it |
| Keep-alive | On. Reusing a connection matters on a phone, where a new TLS handshake is expensive. |
| Proxy | Android's system proxy is honoured through the platform's default. A user on a corporate proxy should not have to configure anything. |
| Certificate pinning | Not used. See `07-integrations/github-auth.md` for the reasoning; it applies to every host. |
| A custom CA | Android's system trust store. A user with a corporate CA is covered by the system. |

## The request

| Field | Handling |
|---|---|
| The key | Resolved from the Keystore at the moment of the request, held in a header, and the header object is discarded afterwards |
| `anthropic-version` | Set for the `ANTHROPIC` kind. Omitted for a `CUSTOM` kind whose server does not want it, which some proxies do not. |
| `anthropic-beta` | Passed through if a feature needs it, and never invented |
| `x-api-key` versus `Authorization: Bearer` | Chosen by kind. `ANTHROPIC` uses the header form; `OPENAI_CHAT` uses the bearer form. A `CUSTOM` provider declares which, in a field, because some proxies accept only one. |
| The system prompt | Kind-specific placement: a top-level parameter for Anthropic, a message for OpenAI, an `instructions` field for Responses |
| Tools | Mapped per kind, from the app's internal tool definitions |
| Streaming | Always for a turn. Never for a test, where a whole response is what is being checked. |
| `max_tokens` | Required by Anthropic, optional elsewhere. Set from the model's metadata where known, and a conservative default where it is not. |

## The response

Parsed into the app's own types, then into `AgentEvent`. Per `02-architecture/event-protocol.md`.

| Aspect | Handling |
|---|---|
| Text deltas | Accumulated and emitted as `TextDelta` |
| Thinking deltas | Anthropic and Responses. OpenAI's reasoning models differ, and if they cannot be mapped cleanly, the feature is not exposed rather than shown wrong. **TBD: verify the current shape.** |
| Tool calls | Fragments accumulated, parsed once, emitted as `ToolStarted` then `ToolFinished` |
| Usage | Anthropic reports it in two places and can report cache tokens separately; OpenAI reports it in the final frame. Both feed `CostRecord` with an `isEstimated` flag. |
| Stop reasons | Mapped to one internal enum, with the provider's own value preserved in the log |
| An unparseable line | `MalformedEvent`, recorded raw, the run continues |
| An empty response | An `AppError` with the provider's status and the raw body in the log. Never a blank screen. |
| A non-JSON body from a `CUSTOM` provider | The common proxy misconfiguration. The error says: "Die Antwort ist kein JSON. Vielleicht steht eine Fehlerseite davor." with the first 200 characters. |

## The connection test

The feature that makes provider configuration bearable. Run automatically when a provider is complete, and on demand.

```kotlin
suspend fun test(config: ProviderConfig): TestResult
```

| Step | What it does | Failure reported as |
|---|---|---|
| 1. URL | Parses, the scheme is acceptable | "Die Adresse ist ungültig." |
| 2. Reachability | A HEAD or a cheap GET, 10 s | The specific network diagnosis from `06-runtime/environment-diagnostics.md` |
| 3. TLS | The handshake succeeds | "Das Zertifikat wird nicht akzeptiert." with the date and time as the first hint |
| 4. Authentication | A minimal authenticated request | "Der Schlüssel wurde abgelehnt." |
| 5. Model discovery | `GET /v1/models`, optional | Not a failure. Reported as "nicht unterstützt", and manual entry is offered. |
| 6. A minimal completion | The selected model, "reply with OK", 20 tokens | The specific model or format error |
| 7. Vision, if the model claims it | A 4×4 test image | "Das Modell kann keine Bilder.", downgrading the capability claim |
| 8. Streaming | A streaming request | "Die Antwort kommt nicht gestreamt." — a provider that ignores the streaming flag works, but slowly, and the user should know |

| Result | Shown as |
|---|---|
| All passed | "Verbindung ok · {model} · 340 ms" |
| Partially | Each step's outcome, and which one failed. Not a single "failed". |
| Failed | The specific reason and the fix, per `06-runtime/environment-diagnostics.md` |
| Not tested | A clear "noch nicht getestet" state, not a green check |

**Every step is reported separately.** A single "connection failed" for a provider that is reachable but has the wrong model is the least useful message in this domain, and it is what every other tool produces.

## Model discovery

| Endpoint | Handling |
|---|---|
| `GET /v1/models` | The response is a list, and the format varies: Anthropic's is `data[].id`, OpenAI's is `data[].id`, some proxies return a bare array |
| Parsing | Three shapes are attempted: `data[].id`, a bare array of strings, a bare array of objects with `id` or `name`. Beyond that, the app does not guess. |
| Failure | Not an error. "Der Server bietet keine Modelliste an. Modell selbst eintragen." |
| Merging | Discovered models are added; a manual entry with the same id is kept as the manual one |
| Caching | Until the next test, or 7 days |
| A huge list | Filtered in the UI, not truncated silently. "Zeige 40 von 3.200 Modellen." |
| The default | The first recommended-looking one, chosen by a short built-in list of known ids, and otherwise the first from the server. Never "latest". |

## Adding a provider

| Step | Detail |
|---|---|
| 1 | A name |
| 2 | A kind. Four cards, each naming what it is for |
| 3 | A base URL, prefilled per kind, editable, with a preview of the full endpoint |
| 4 | A key, or a key profile |
| 5 | A model, discovered or manual |
| 6 | A test, run automatically |
| 7 | Set as default, optionally |

Everything is editable afterwards. Nothing is applied before the test passes, except that a provider can be saved in a broken state deliberately, so a user configuring offline does not lose their typing.

## Key profiles

| Aspect | Behaviour |
|---|---|
| Purpose | Several keys for one provider: privat, Arbeit, Test |
| Storage | Each encrypted in the Keystore, per `07-integrations/secrets.md` |
| Selection | Per provider, and switchable at any time |
| Testing one | A cheap request against that key, so switching is safe |
| Last used | Tracked, and shown in the profile list |
| Deleting | Only the ciphertext. There is no API call, because the app does not manage keys at the provider. The confirmation says: "Der Schlüssel wird von dieser App vergessen. Ob er bei {provider} noch gilt, kann sie nicht wissen." |

## Failover

**Not implemented, and this is a decision rather than an omission.**

| Why not | Detail |
|---|---|
| Provenance | A run's answer is the work of one model. Silently switching changes the answer's origin, and the cost record becomes a mixture. |
| Cost | Switching to a cheaper model after a failure would look like a feature and would be a surprise on a bill. |
| Confusion | "Why did it answer differently this time?" is a question the user cannot answer from the app's own state. |

What the app does instead: on a provider failure, it names the error, and the settings screen offers the other configured providers as an explicit choice for the next run. A human decides. See `05-features/model-selection.md`, which is the same principle.

## Testing

| Test | Type |
|---|---|
| `ConfigValidation` | Unit — every field's rules, including a dangling secret reference |
| `HttpRestriction` | Unit — localhost and private ranges allowed, public http refused |
| `PrivateRangeConfirm` | UI — the one-time confirmation for a private range, naming the address |
| `AuthHeaderForm` | Unit — `x-api-key` for Anthropic, bearer for OpenAI, configurable for `CUSTOM` |
| `SystemPromptPlacement` | Unit — the correct placement per kind |
| `StreamingParsed` | Unit — a real recorded response per kind produces the correct `AgentEvent` sequence |
| `ToolCallAssembly` | Unit — fragmented JSON assembled and parsed once |
| `UsageAccounting` | Unit — per kind, feeding `CostRecord` with the correct `isEstimated` |
| `NoRetryAtClient` | Static — the provider client has no retry logic, asserted so a second retry layer cannot be added |
| `TestSteps` | Integration — each connection-test step fails independently and reports itself, exhaustively |
| `TestUntestedIsNotGreen` | UI — an untested provider shows a neutral state, never a check |
| `TestStreamingStep` | E2E — a provider that ignores the streaming flag is reported as "nicht gestreamt" |
| `DiscoveryShapes` | Unit — the three response shapes parse, and an unknown shape is not guessed at |
| `DiscoveryFailureGraceful` | E2E — a server without `/v1/models` offers manual entry without an error state |
| `DiscoveryMerge` | Unit — a manual entry survives a discovery run with the same id |
| `NeverLatest` | Unit — no default is ever the string "latest" or an unpinned alias |
| `KeyProfiles` | E2E — three profiles per provider, switchable, each testable |
| `KeyDeleteHonest` | UI — the confirmation states the app cannot know whether the key still exists at the provider |
| `NoFailover` | E2E — a provider failure never switches providers automatically |
| `MalformedBody` | E2E — an HTML error page from a proxy produces the "kein JSON" message with the first 200 characters |
| `KeyNeverInClient` | Static — the provider client receives a `SecretRef`, never a key value; the key is resolved at the boundary |
| `TimeoutSeparation` | Unit — the streaming timeout differs from the request timeout, and a long turn is not killed |
