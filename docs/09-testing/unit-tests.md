# Unit tests

The ~620 JVM tests, organised by subject. Every entry lists the specific cases that must exist, not a vague "test the parser".

Test rules that apply to everything here are in `test-strategy.md`. Fixtures are listed in `fixtures-and-test-data.md`.

## 1. Event parsing — 140 tests

`EventParser` turns Claude Code's `stream-json` output into `AgentEvent`. It is the single most fragile piece of the product, so it gets the most tests.

### 1.1 Well-formed events — 30 tests

One test per `AgentEvent` subtype in `02-architecture/event-protocol.md`, asserting the full field mapping: every optional field present, every field absent, and the enum parse of each string field. Explicitly:

| Case | Assertion |
|---|---|
| `system/init` with full session info | `SessionInit(sessionId, model, cwd, tools, permissionMode)` matches exactly |
| `system/init` with minimal fields | `tools` empty, `permissionMode` default, no crash |
| `assistant` text block | one `AssistantTextDelta` per content block, concatenated in order |
| `assistant` tool_use block | `ToolCallPending` with the id, name, and raw input JSON string preserved verbatim |
| `user` tool_result, success | `ToolCallResult(success=true)` |
| `user` tool_result, error | `ToolCallResult(success=false, error = first 200 chars of content)` |
| `user` tool_result, `is_error` absent | treated as success — the upstream default |
| `stream_event` wrapping `content_block_delta` | forwarded as `AssistantTextDelta`, no duplication with the parent `assistant` event |
| `stream_event` wrapping `message_delta` | `UsageReported` with cumulative token counts |
| `result` success, `subtype: success` | `RunCompleted(isError=false)` |
| `result` failure | `RunCompleted(isError=true, error)` |
| `result` with `duration_ms`, `num_turns`, `total_cost_usd` | all three carried into the run record |
| `result` with `usage` present | token counts taken from `usage`, not estimated |
| `api_retry` system event | `EngineRetrying(attempt, maxRetries, delayMs)` — displayed, never counted as our own retry |
| Unknown `type` | `UnknownEvent(rawJson)`, stream continues, no throw |
| Unknown subtype within a known type | same, the known parts still parsed |

### 1.2 Malformed input — 40 tests

| Case | Expected |
|---|---|
| Empty line | skipped silently |
| Whitespace-only line | skipped silently |
| Truncated JSON mid-object | `ParseFailed` emitted once, stream continues |
| Two JSON objects on one line | first parsed, second emitted as separate event, no data loss |
| `content_block_delta` before any `message_start` | delta still forwarded; the UI may not know the message id yet, so the id is null |
| `text_delta` with `text: ""` | no event emitted (empty deltas are noise) |
| Tool input arriving as a partial JSON string across three deltas | accumulated; the pending card shows a live preview |
| `NaN` or `Infinity` in a numeric field | field defaulted, event still emitted, a warning is attached |
| Integer where a float is expected | coerced |
| Float where an integer is expected (`num_turns: 3.0`) | truncated, no crash |
| `null` for a non-nullable field | field defaulted, event emitted, warning attached |
| Missing `type` | `ParseFailed` |
| Deeply nested JSON (10 000 levels) | `ParseFailed` via depth guard, no `StackOverflowError` |
| 5 MB single line | `ParseFailed` with "line too long"; the ring buffer keeps the first 2 KB |
| Invalid UTF-8 bytes | replacement characters, no crash |
| BOM at the start of a line | stripped |
| CRLF line endings | handled |
| A line that is a JSON array, not an object | `ParseFailed` |
| A line that is `null` literal | skipped |
| Repeated identical event ids | de-duplicated (some upstream versions resend on reconnect) |

### 1.3 Sequence handling — 35 tests

| Case | Expected |
|---|---|
| Partial message reassembly across 20 deltas | exactly one assembled message, in order |
| Interleaved subagent events with `parent_tool_use_id` | routed to the right subagent lane |
| `text_delta` after a `tool_use` in the same message | order preserved: text, tool, text |
| Two tools in one assistant message | two `ToolCallPending`, both resolved, both cards shown |
| A tool that never returns before `result` | card marked `unresolved`, run still completes |
| Stream ends without a `result` event | run marked `INTERRUPTED`, not `FAILED` |
| `result` arrives twice | second ignored, one completion |
| Session id changes mid-stream | treated as a session fork, a banner is shown |
| Token usage out of order (cumulative count decreases) | the higher value wins, warning attached |
| A `result` with `total_cost_usd: 0` while usage is non-zero | cost recomputed from the pricing table |

Plus 25 tests asserting the parser's **invariants** on randomised-but-seeded input: total text length in equals total text length out, no event is dropped, ordering is monotonic per stream.

### 1.4 Fuzzing — 35 cases

Property tests over three generators: random byte strings, mutated valid events (bit flips, truncations, insertions), and structured-but-nonsensical events. Properties: never throws an uncaught exception, never loops, terminates within 50 ms, and memory does not grow.

## 2. Permission evaluation — 120 tests

The evaluator from `08-orchestration/permissions.md`. Table-driven, exhaustive, and every row is a real scenario.

| Group | Rows | What varies |
|---|---|---|
| Hard block: deletion | 18 | `rm`, `rm -rf`, `git clean -fd`, `unlink`, `find -delete`, `shred`, a `rm` inside a script, `rm` with a glob, `rm` in a subdirectory, a Windows-style `del` in a shell string, `trash`-like tools, and the six near-miss spellings that must **not** trigger |
| Hard block: spending | 12 | `npm publish`, `pip upload`, `cargo publish`, Stripe/Shopify API calls, cloud `deploy` commands, a payment link, a crypto withdrawal address |
| Hard block: publishing | 10 | `git push` to `main`/`master`/default branch, `gh pr create`, `gh release create`, `git tag -f` on a released tag, npm/yarn publish aliases |
| Hard block: secrets | 12 | writing to `~/.ssh`, `~/.aws/credentials`, `.env` overwrite, keychain export, a commit containing a detected key pattern |
| Hard block: hiding | 10 | `git commit --amend` after a push, history rewrite on a shared branch, `git checkout .` discarding changes, log file truncation, CI log suppression |
| Allowed at level 1 | 15 | read, list, grep, status, diff, show, log, `git branch` (local), `cargo check`, `./gradlew test` |
| Allowed at level 2 | 10 | plus local edit, `git add`, `git commit` on a non-default local branch, formatters |
| Allowed at level 3 | 10 | plus `git checkout -b`, dependency add, migrations, running a repo's own scripts |
| Requires prompt (level 3+ only) | 8 | network fetch, install, anything with an unknown command name |
| Prohibited at every level | 5 | the five hard blocks, no exceptions, no configuration |
| Command chaining | 10 | `a && b`, `a ; b`, `a | b`, backticks, `$(...)`, subshells, heredocs that contain a blocked command, a blocked command aliased to an allowed name, a blocked command invoked through a script path |

Each row asserts three things: the verdict, the reason code, and the user-facing sentence in both German and English. The last one matters — a refusal the user cannot understand is a bug even when the verdict is right.

## 3. Verification judging — 90 tests

`VerificationJudge` from `05-features/verification.md` decides whether a run is `DONE`. A false `PASSED` is the worst defect this product can ship, so the suite is exhaustive.

| Group | Rows | Notes |
|---|---|---|
| Exit code mapping | 20 | 0 → passed; 1 → failed; 2 → error; 127/126 → error (command missing); 124 → failed (timeout); 128+128+n → signal n; 143 → cancelled, never failed |
| Output parsing | 25 | Gradle `BUILD SUCCESSFUL`/`BUILD FAILED`, Maven, npm, pytest, `cargo test`, plain `make`, and a project with no framework at all (exit code only) |
| Lint severity | 10 | errors block; warnings do not; a project configured to treat warnings as errors is honoured |
| Coverage gate | 8 | a configured minimum below, at, and above the measured value |
| Timeout | 8 | at 99 % of the limit, at 100 %, at 101 %; a killed-by-timeout run is `FAILED` with the time shown |
| Output truncation | 6 | a 10 MB log is judged on its head and tail, never truncated into a false pass |
| Absence of evidence | 8 | an empty log with exit 0 is `UNVERIFIED`, not `PASSED` — the core safety property |

## 4. Retry and self-healing — 70 tests

| Group | Rows | Cases |
|---|---|---|
| Budget accounting | 15 | per failure type, per run, across runs, budget exhausted mid-run |
| Backoff schedule | 12 | exact delays, jitter bounds, the ceiling |
| Anti-loop detection | 15 | same error signature N times, alternating A/B/A/B, increasing error counts, decreasing file count, and the near-miss cases that must **not** trip |
| Never-weaken-a-check | 10 | removing an assertion, `@Ignore`, lowering a coverage threshold, `|| true`, deleting a test — all refused, all produce the same hard-block reason |
| Escalation ladder | 10 | each of the 8 rungs, the transition condition, and the terminal stop |
| Exhaustion behaviour | 8 | what the run state becomes, what the user sees, what is preserved |

## 5. Event stream and session state — 65 tests

| Group | Rows | Cases |
|---|---|---|
| `AgentEventFlow` | 20 | backpressure, buffer bound, drop policy (drop partial deltas first, never drop state events), collector cancellation |
| Session state machine | 20 | every transition in `02-architecture/state-machines.md`, including the illegal ones |
| Run state machine | 15 | every transition, `DONE` reachable only through verification |
| Resumption | 10 | `CLAUDE_CODE_RESUME_INTERRUPTED_TURN`, exit 143 handling, resumed transcript integrity |

## 6. Cost and token arithmetic — 55 tests

| Group | Rows | Cases |
|---|---|---|
| Pricing table lookup | 12 | per model, per region, unknown model → refuse to estimate rather than guess |
| Cache pricing | 10 | input, cache write, cache read, 5-minute TTL vs 1-hour TTL |
| Streaming cost accumulation | 12 | partial usage, cumulative deltas, out-of-order counters, mid-run model switch |
| Currency and formatting | 8 | USD formatting, locale separators, values below 0.01, values above 100 |
| Budget display | 13 | no cap (per the product decision) but a visible running total and a per-run total |

## 7. Persistence and repositories — 60 tests

Room, against an in-memory database, on the JVM via Robolectric where the annotation processor is needed.

| Group | Rows |
|---|---|
| Schema and migrations | 18 — every migration path from every shipped version, forward and (for dev builds) backward |
| Repository CRUD | 20 |
| Queries used by the UI | 14 — each screen's query tested with the exact data shape it renders |
| Deletion | 8 — **the project, session, and run delete paths assert that nothing user-authored is removed**; the only permitted deletes are caches and the transparency log's own rotation |

## 8. Support and pure logic — 80 tests

| Subject | Rows | Cases |
|---|---|---|
| `PathSafety` — refusing paths that escape the project root, including `..`, absolute paths, and symlinks pointing out | 15 |
| Redaction — keys, tokens, and personal paths in arbitrary strings | 12 |
| Provider config parsing and validation | 12 |
| Model capability lookup | 8 |
| Skill manifest parsing and validation | 12 |
| Error message localisation — every error code has a German and an English string, neither empty, neither containing a format placeholder that does not exist | 9 |

## Test naming

```kotlin
@Test
fun `stream_event content_block_delta with empty text emits nothing`()

@Test
fun `permission evaluator refuses rm -rf inside a subshell with reason HARD_BLOCK_DELETION`()

@Test
fun `verification judge treats exit 0 with an empty log as UNVERIFIED`()
```

Backtick names describe behaviour, not implementation. A test called `testParser3` has told us nothing and will outlive the code it tests.
