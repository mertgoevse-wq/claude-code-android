# Provider catalogue sources

Where every entry in `ProviderCatalogue` came from, and when it was checked.

**The rule this document exists for:** a base URL that is wrong produces a
connection error the user cannot act on, and there is no way for them to tell
the app apart from a typo. So a plausible guess is not a defect to be tidied
later; it is a defect that ships. Every value below was read from a source, on a
date, and the entry carries that date in code.

**Checked 2026-09-30.** When a provider changes its endpoint, the entry is wrong
until someone notices. The picker does not probe, so a stale entry is a stale
entry — this file is how that gets found.

## How to add a provider

1. Find the vendor's own API documentation, not a blog post or a listicle.
2. Confirm three things separately: the **host**, the **completion path**, and
   the **dialect**. A gateway's path is frequently not the vendor's path, and the
   two are the reason the base URL and the path are separate fields.
3. Add the entry to `ProviderCatalogue.kt`.
4. Add a row here, with the URL you read and the date.
5. `ProviderCatalogueTest` refuses the entry if the base URL will not parse, the
   path does not start with a slash, the id is duplicated, or the entry claims a
   verification date the rest of the list does not share. It also asserts that
   every entry the picker offers passes its own validation.

## Hosted, OpenAI chat dialect

Hosts and paths as listed in the agentgateway OpenAI-compatible provider table
(<https://agentgateway.dev/docs/kubernetes/latest/integrations/llm/providers/openai-compatible/>),
which documents the host and the full chat-completions path for each.

| Entry | Host | Completion path | Checked |
|---|---|---|---|
| `groq` | `api.groq.com` | `/openai/v1/chat/completions` | 2026-09-30 |
| `deepseek` | `api.deepseek.com` | `/v1/chat/completions` | 2026-09-30 |
| `mistral` | `api.mistral.ai` | `/v1/chat/completions` | 2026-09-30 |
| `together` | `api.together.xyz` | `/v1/chat/completions` | 2026-09-30 |
| `fireworks` | `api.fireworks.ai` | `/inference/v1/chat/completions` | 2026-09-30 |
| `cerebras` | `api.cerebras.ai` | `/v1/chat/completions` | 2026-09-30 |
| `xai` | `api.x.ai` | `/v1/chat/completions` | 2026-09-30 |
| `sambanova` | `api.sambanova.ai` | `/v1/chat/completions` | 2026-09-30 |
| `baseten` | `inference.baseten.co` | `/v1/chat/completions` | 2026-09-30 |
| `deepinfra` | `api.deepinfra.com` | `/v1/openai/chat/completions` | 2026-09-30 |
| `cohere` | `api.cohere.ai` | `/compatibility/v1/chat/completions` | 2026-09-30 |
| `huggingface` | `router.huggingface.co` | `/v1/chat/completions` | 2026-09-30 |
| `openrouter` | `openrouter.ai` | `/api/v1/chat/completions` | 2026-09-30 |

**Why the paths differ.** Groq, Fireworks, DeepInfra, Cohere and OpenRouter all
speak the same dialect and none of them uses `/v1/chat/completions`. This is the
concrete reason `baseUrl` and `pathTemplate` are separate fields: a single
`baseUrl` that includes the path would have to be wrong for one of them.

## First party

| Entry | Host | Completion path | Source | Checked |
|---|---|---|---|---|
| `anthropic` | `api.anthropic.com` | `/v1/messages` | <https://platform.claude.com/docs/en/api/overview> | 2026-09-30 |
| `openai` | `api.openai.com` | `/v1/chat/completions` | <https://developers.openai.com/api/reference/overview/> | 2026-09-30 |

The model-list paths for both are `/v1/models`; both document it.

## Gateways

| Entry | Host | Completion path | Source | Checked |
|---|---|---|---|---|
| `openrouter` | `openrouter.ai` | `/api/v1/chat/completions` | agentgateway table, as above | 2026-09-30 |
| `litellm` | *left to the user* | `/v1/chat/completions` | <https://docs.litellm.ai/docs/providers/openai_compatible> | 2026-09-30 |

`litellm` ships with an empty base URL on purpose. A LiteLLM proxy is a thing
the user runs themselves, on a host only they know, so there is no vendor host to
prefill. The dialect and the path are known and prefilled; the address is a field
the user fills in. An entry that guessed a host here would send a key somewhere.

## Self-hosted

These are the runtimes the project expects to meet on a user's own machine or
network, at their documented default ports. **The host is local in every case**,
which `ProviderCatalogueTest` enforces: an entry in the self-hosted group that
points at a public address would be offering to send a key in clear text to the
internet, and the test fails rather than letting it ship.

| Entry | Runtime | Default address | Checked |
|---|---|---|---|
| `lmstudio` | LM Studio local server | `http://127.0.0.1:1234` | 2026-09-30 |
| `llamacpp` | llama.cpp server | `http://127.0.0.1:8080` | 2026-09-30 |
| `vllm` | vLLM OpenAI server | `http://127.0.0.1:8000` | 2026-09-30 |
| `ollama` | Ollama OpenAI-compatible shim | `http://127.0.0.1:11434` | 2026-09-30 |
| `jan` | Jan | `http://127.0.0.1:1337` | 2026-09-30 |

**TBD — verify at build time.** The default port for `jan` and for `lmstudio` is
the part of these rows least likely to be right, because a desktop application's
default is not a stable contract. Both are prefilled and both are editable, and
the connection test reports the actual port it tried. Verify before shipping the
self-hosted group; do not ship a port that was only read from memory.

## Not in the list, and why

| Provider | Why it is absent |
|---|---|
| AWS Bedrock, Google Vertex | Neither exposes a plain HTTP endpoint with a bearer key. They need SigV4 and OAuth respectively, which is a different authentication mechanism, not a different base URL. `CUSTOM` does not cover them. Adding one is its own decision with its own document. |
| Azure OpenAI | The endpoint is per-resource and per-deployment, so there is no vendor host to prefill. Reachable as `CUSTOM` with the resource URL. |
| Google Gemini | The OpenAI-compatible surface exists but the path prefix is not stable across surfaces. `CUSTOM` covers it; a prefilled entry would be a guess about a prefix. |
| Anything else | `CUSTOM`. A base URL, a path, and a dialect reach every server that speaks either shape, including one this project has never heard of. The catalogue is a convenience, not a gate. |

## What the app does not do with this list

- **No probing.** The app never calls a provider to see whether the entry is
  still right. The connection test is the user's request to a provider they
  chose; a background sweep of twenty hosts is not something a phone should do
  on its own.
- **No silent rewrite.** A model id is sent exactly as typed. A provider that
  wants something else fails with a message, which beats a silent rewrite.
- **No auto-select.** The list orders itself; it never picks a provider. See
  `07-integrations/providers.md` §Failover for why a human decides.
