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

## The provider list

The user picks a provider from a list rather than typing a base URL, because
typing a base URL is where this usually goes wrong. The list is
`ProviderCatalogue`, twenty-one entries across four groups, and every entry's
host, path and dialect was looked up on a recorded date in
`10-build/provider-catalogue-sources.md`.

| Group | What is in it | Why it is a group |
|---|---|---|
| `FIRST_PARTY` | Anthropic | The vendor the engine was written for |
| `GATEWAY` | OpenRouter, LiteLLM Proxy | One key in front of several vendors |
| `HOSTED` | OpenAI, Groq, DeepSeek, Mistral, Together, Fireworks, Cerebras, xAI, SambaNova, Baseten, DeepInfra, Cohere, Hugging Face Router | A vendor, one key, one dialect |
| `SELF_HOSTED` | LM Studio, llama.cpp, vLLM, Ollama, Jan | The user's own machine, usually plain http |

**`baseUrl` and `pathTemplate` are separate fields, and that is not tidiness.**
Groq serves `/openai/v1/chat/completions`, Fireworks serves
`/inference/v1/chat/completions`, DeepInfra serves
`/v1/openai/chat/completions`, and DeepSeek serves `/v1/chat/completions` — all
the same dialect. A single base URL that included the path would have to be
wrong for one of them.

The same dialect does not mean the same auth header, either: `ANTHROPIC` uses
`x-api-key`, the OpenAI shapes use `Authorization: Bearer`, and a `CUSTOM`
provider declares which, because a proxy that accepts only one of them is a real
configuration and guessing wrong produces a 401 the user cannot diagnose.

A provider that is not in the list is not a dead end. `CUSTOM` takes a base URL,
a path and a dialect, and that is the supported path for a server this project
has never heard of. The catalogue is a convenience, not a gate.

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
| `pathTemplate` | Filled from the kind, editable. A `CUSTOM` template may carry `{version}`, which is taken from the base URL's own path rather than guessed. The full endpoint is shown as a preview before anything is saved. |
| `secretProfileId` | Must resolve. A provider with a dangling reference is shown as broken, not silently disabled. |
| `models` | Manual entries, plus discovered ones. Both are kept; discovery never overwrites a manual entry. A model id ending in `-latest` is refused: it is an alias, not a model, and a run recorded against one cannot be reproduced or costed. |
| `headers` | Key-value. A header the app sets itself — `Authorization`, `x-api-key`, `Host`, `Content-Length` — is refused, case-insensitively, because it would shadow the key the Keystore resolved and the failure would look like a rejected key. |

### The rules, and the failure each one prevents

| Rule | What it prevents | Where |
|---|---|---|
| Plain http is refused for a public host | A key crossing the internet in clear text | `EndpointUrl.isLocal` |
| Plain http for a local host is **asked about**, not refused | A local model being impossible to configure, while the one case that is legitimate still gets said out loud | `ValidateProviderDraft`, `REASON_LOCAL_PLAINTEXT` |
| `172.16.0.0/12` is read octet by octet | `172.15` and `172.32`, which are on the internet, being treated as local by a prefix check | `EndpointUrl.isIn172PrivateRange` |
| A userinfo section is refused | `https://api.groq.com@evil.example`, which a careless parser reads as Groq and everything else reads as the attacker's host | `EndpointUrl.parse` |
| A port outside 1–65535 is refused | A key sent to a port nobody meant | `EndpointUrl.parse` |
| The parser refuses anything it does not recognise | A key sent to a host the user did not choose | `EndpointUrl.parse` |
| A reserved header is refused | A shadowed key, which surfaces at the provider as a rejected key and sends the user to the wrong page | `ValidateProviderDraft` |
| An unpinned model alias is refused | A cost record and a run that cannot be reproduced or compared | `UnpinnedModelAliases` |
| Changing the kind resets the path and says so | A path from one dialect silently applied to another, which is a 404 nobody reads | `ChangeProviderKind` |
| A catalogue entry that fails its own validation fails the test | A provider the user picks from the list being then told it is invalid | `ProviderCatalogueTest` |

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

Steps 2 and 3 are prefilled from the catalogue when the provider came from the
list, and empty when it was typed. A step that cannot satisfy the rules under
§The configuration does not advance, and the message says what would have gone
wrong rather than that the field is invalid.

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

| Test | Type | State |
|---|---|---|
| `ConfigValidation` | Unit — every field's rules, including a dangling secret reference | With P2-8, for the secret reference |
| `HttpRestriction` | Unit — localhost and private ranges allowed, public http refused | Done — `EndpointUrlTest`, `ProviderConfigurationTest` |
| `PrivateRangeConfirm` | UI — the one-time confirmation for a private range, naming the address | Phase 4, with the screen. The domain half is done: `REASON_LOCAL_PLAINTEXT` |
| `AuthHeaderForm` | Unit — `x-api-key` for Anthropic, bearer for OpenAI, configurable for `CUSTOM` | Done — `AuthFormForTest` |
| `CatalogueInvariants` | Unit — every entry parses, has a unique id, a dated check, and passes its own validation | Done — `ProviderCatalogueTest` |
| `NeverLatest` | Unit — no default is ever the string "latest" or an unpinned alias | Done — `UnpinnedModelAliases`, `ChooseDefaultModel` |
| `SystemPromptPlacement` | Unit — the correct placement per kind | With the client |
| `StreamingParsed` | Unit — a real recorded response per kind produces the correct `AgentEvent` sequence | With the client |
| `ToolCallAssembly` | Unit — fragmented JSON assembled and parsed once | With the client |
| `UsageAccounting` | Unit — per kind, feeding `CostRecord` with the correct `isEstimated` | With the client |
| `NoRetryAtClient` | Static — the provider client has no retry logic, asserted so a second retry layer cannot be added | With the client |
| `TestSteps` | Integration — each connection-test step fails independently and reports itself, exhaustively | Next |
| `TestUntestedIsNotGreen` | UI — an untested provider shows a neutral state, never a check | Phase 4 |
| `TestStreamingStep` | E2E — a provider that ignores the streaming flag is reported as "nicht gestreamt" | Phase 5 |
| `DiscoveryShapes` | Unit — the three response shapes parse, and an unknown shape is not guessed at | With the client |
| `DiscoveryFailureGraceful` | E2E — a server without `/v1/models` offers manual entry without an error state | Phase 5 |
| `DiscoveryMerge` | Unit — a manual entry survives a discovery run with the same id | With the client |
| `KeyProfiles` | E2E — three profiles per provider, switchable, each testable | Phase 5 |
| `KeyDeleteHonest` | UI — the confirmation states the app cannot know whether the key still exists at the provider | Phase 5 |
| `NoFailover` | E2E — a provider failure never switches providers automatically | Phase 5 |
| `MalformedBody` | E2E — an HTML error page from a proxy produces the "kein JSON" message with the first 200 characters | With the client |
| `KeyNeverInClient` | Static — the provider client receives a `SecretRef`, never a key value; the key is resolved at the boundary | With the client |
| `TimeoutSeparation` | Unit — the streaming timeout differs from the request timeout, and a long turn is not killed | With the client |
