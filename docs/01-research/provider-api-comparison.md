# Provider API comparison

The app speaks to whatever provider the user configures. This document records what actually differs, because the differences drive `ProviderKind` in the code.

**Verified 2026-09-27.** API shapes change; re-verify before adding a new kind.

## Why the app is a provider client at all

Claude Code has a first-class relationship with the Anthropic API. Talking to OpenAI or a self-hosted model means either a compatibility gateway or teaching the app to speak both dialects. We chose the second: the app is the client, and it presents a single internal event stream to the UI regardless of dialect.

This is a real cost — two streaming formats, two tool-call formats, two error taxonomies. It is worth it because the alternative locks the user to one vendor, and one of the two personas would consider that a defect.

## The shapes

| | Anthropic Messages | OpenAI Chat Completions | OpenAI Responses |
|---|---|---|---|
| Path | `/v1/messages` | `/v1/chat/completions` | `/v1/responses` |
| Streaming | SSE, typed event blocks | SSE, `data:` frames with `delta` | SSE, typed semantic events |
| Text delta | `content_block_delta` → `text_delta` | `choices[].delta.content` | `response.output_text.delta` |
| Tool call | `tool_use` block, streamed `input_json_delta` | `tool_calls[]` with `function.arguments` string | `function_call` items |
| Tool result | a `tool_result` block in the next user message | a `role: "tool"` message | `function_call_output` item |
| System prompt | top-level `system` parameter | `messages[0]` with `role: "system"` | `instructions` |
| Max tokens | required | optional | `max_output_tokens` |
| Stop reason | `end_turn`, `max_tokens`, `stop_sequence`, `tool_use` | `stop`, `length`, `tool_calls`, `content_filter` | `completed`, `incomplete` |
| Errors | `{"type":"error","error":{"type":...}}` | `{"error":{"message","type","code"}}` | `{"error":{...}}` |
| Rate limit headers | `anthropic-ratelimit-*` | `x-ratelimit-*` | `x-ratelimit-*` |
| Cache | explicit `cache_control` breakpoints | implicit, provider-specific | implicit |

## Mapping to one internal event

Everything above collapses into `AgentEvent`. The mapping lives in `AgentEventMapper` and is the only place a provider difference may appear.

| Internal event | Anthropic source | OpenAI source |
|---|---|---|
| `TextDelta(text)` | `content_block_delta` with `text_delta` | `choices[].delta.content` |
| `ThinkingDelta(text)` | `content_block_delta` with `thinking_delta` | absent (reasoning models expose it differently) |
| `ToolStart(id, name)` | `content_block_start` with `tool_use` | `tool_calls[].function.name` arriving |
| `ToolArgsDelta(id, json)` | `input_json_delta.partial_json` | accumulated `function.arguments` string |
| `ToolEnd(id)` | `content_block_stop` | `finish_reason: "tool_calls"` |
| `Stop(reason)` | `message_delta` → `stop_reason` | `choices[].finish_reason` |
| `Usage(input, output, cacheRead, cacheCreate)` | `message_delta.usage` and `message_start.usage` | `usage` in the final frame, when streamed |
| `RateLimit(...)` | `anthropic-ratelimit-*` headers | `x-ratelimit-*` headers |

### The two hard problems

**Partial JSON.** Anthropic streams tool arguments as JSON fragments that are not valid JSON until complete. OpenAI streams them as one concatenated string with the same property. We accumulate fragments and parse once, at the end. A tool card therefore shows a spinner, not a half-rendered argument object.

**Usage timing.** Anthropic reports input and output tokens in different events and can be asked for cache token counts separately. OpenAI reports usage in the final frame and only if the server streams it. Consequence: the cost meter can be wrong mid-run and correct at the end. We show a live estimate and replace it with the reported figure when it arrives. We never show a cost and then silently change it — the meter animates from estimate to actual, visibly.

## Error mapping

| Class | Anthropic | OpenAI | Retryable | User-facing |
|---|---|---|---|---|
| Auth | 401 `authentication_error` | 401 `invalid_api_key` | No | "Schlüssel nicht gültig" |
| Permission | 403 | 403 | No | "Zugriff verweigert" |
| Rate limit | 429 `rate_limit_error` | 429 | Yes, back off | "Zu viele Anfragen, Versuch N" |
| Overloaded | 529 / `overloaded` | 503 | Yes | "Dienst überlastet, Versuch N" |
| Bad request | 400 `invalid_request_error` | 400 | No | "Anfrage abgelehnt: <message>" |
| Model missing | 404 `model_not_found` | 404 | No | "Modell nicht gefunden" |
| Server | 500 / 529 | 500 / 502 / 503 | Yes | "Serverfehler, Versuch N" |
| Billing | `billing_error` | 402 | No | "Guthaben aufgebraucht" |
| Network | connection error | connection error | Yes | "Keine Verbindung" |

Retryable errors are surfaced with the attempt number from the `api_retry` event, not from our own counter, when the provider gives us one. Our counter is the fallback.

## Model discovery

Anthropic exposes `/v1/models`. OpenAI exposes `/v1/models`. Self-hosted runtimes vary: llama.cpp exposes `/v1/models`; some servers return a static list or nothing at all.

So: the app tries discovery, and if the endpoint does not support it, falls back to a manual model list where the user types the model id. The connection test reports which path was taken. This must not be a failure state — plenty of perfectly good servers have no model endpoint.

## Custom providers

A `CUSTOM` provider is: name, base URL, kind (which dialect), an optional path template, an API key, and a manual model list. This covers:

- OpenRouter, Groq, Together, Fireworks — OpenAI-compatible
- Bedrock and Vertex Anthropic-compatible gateways
- llama.cpp server, LM Studio, vLLM, Ollama's OpenAI shim, text-generation-webui
- An LLM gateway or proxy in front of several of the above
- Anything else that speaks one of the two dialects

Optional per-provider HTTP headers exist for gateways that require a route key or a project id.

**Model id versus model name.** Providers disagree about whether to send `claude-sonnet-4` or a display name. We send exactly what the user typed and never rewrite it. A provider that needs something else fails with a clear message, which is more honest than a silent rewrite.

## What we deliberately do not implement

| Not implemented | Reason |
|---|---|
| Batch API | A phone does not benefit from asynchronous batch pricing |
| Prompt caching controls | Handled by the engine; exposing it invites misuse |
| Fine-tuning | Different product |
| Embeddings | Not a coding agent concern |
| File upload API | We pass files as `@` references, which is what the engine expects |
| Streaming web search tool | Provider-specific, and the app has its own fetch path for the user |
| Multi-provider fallback within a run | A run is bound to one provider. Switching mid-run changes the answer's provenance, and the cost accounting gets murky. The user switches deliberately for the next run. |

## Open items

- Verify whether `thinking_delta` is exposed by current OpenAI reasoning models in a shape worth mapping. If yes, the reasoning UI works on both dialects. **TBD — verify at build time.**
- Verify cache token fields on third-party Anthropic-compatible gateways. Many omit them, so cache savings will read as zero. **TBD — verify at build time.**
- Decide whether a local llama.cpp on the phone is realistic under Profile B, or whether that is better routed to the home-PC runner. Leaning towards the runner. **TBD.**
