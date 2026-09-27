# Event protocol

`AgentEvent` is the contract between the engine, every consumer, and every backend. It is a closed set. If something is not in it, the mapper is wrong or the domain model is missing a concept.

## The set

```kotlin
sealed interface AgentEvent {
    val timestamp: Long

    // Lifecycle
    data class RunStarted(...) : AgentEvent
    data class PlanProposed(val plan: Plan) : AgentEvent
    data class PlanStepChanged(val stepId, val state: PlanStepState) : AgentEvent
    data class RunFinished(val state: RunState, val summary: RunSummary) : AgentEvent

    // Content
    data class TextDelta(val text: String) : AgentEvent
    data class ThinkingDelta(val text: String) : AgentEvent
    data class MessageCompleted(val messageId: String, val rendered: String) : AgentEvent

    // Tool activity
    data class ToolStarted(val invocation: ToolInvocation) : AgentEvent
    data class ToolOutputDelta(val toolUseId: String, val chunk: String) : AgentEvent
    data class ToolFinished(val toolUseId: String, val result: ToolResult) : AgentEvent

    // Subagents
    data class SubagentStarted(val toolUseId: String, val name: String) : AgentEvent
    data class SubagentFinished(val toolUseId: String) : AgentEvent

    // Control
    data class PermissionRequested(val request: PermissionRequest) : AgentEvent
    data class PermissionResolved(val toolUseId: String, val decision: PermissionDecision) : AgentEvent
    data class CheckpointReached(val checkpoint: Checkpoint) : AgentEvent
    data class VerificationUpdate(val state: VerificationState) : AgentEvent
    data class CostUpdate(val cost: CostSnapshot) : AgentEvent

    // Diagnostics
    data class ApiRetry(val attempt: Int, val maxRetries: Int, val delayMs: Long, val reason: RetryReason) : AgentEvent
    data class MalformedOutput(val rawLine: String, val reason: String) : AgentEvent
    data class Info(val code: String, val detail: Map<String, String>) : AgentEvent
    data class Failed(val error: AppError) : AgentEvent
}
```

**One event, one meaning, one owner.** `TextDelta` is a delta; a complete message is `MessageCompleted`. Nothing is both.

## Outbound

What the app sends to a backend.

```kotlin
sealed interface OutboundMessage {
    data class Prompt(val text: String, val attachments: List<Attachment>) : OutboundMessage
    data class AnswerPermission(val toolUseId: String, val decision: PermissionDecision) : OutboundMessage
    data class Interrupt(val reason: InterruptReason) : OutboundMessage
    data class Compact(val keepPlan: Boolean) : OutboundMessage
    data class Cancel(val reason: CancelReason) : OutboundMessage
}
```

`PermissionDecision` is `ALLOW_ONCE`, `ALLOW_ALWAYS`, `DENY`. `ALLOW_ALWAYS` is scoped to the run, never persisted as a project setting, and never survives a session. Making a permission permanent is the user's decision to make every time, which is the point of a permission.

## Mapping from the CLI

`AgentEventMapper` is the only file that knows the CLI's output format.

| CLI output | `AgentEvent` |
|---|---|
| `stream_event` + `text_delta` | `TextDelta` |
| `stream_event` + `thinking_delta` | `ThinkingDelta` |
| `content_block_start` + `tool_use` | `ToolStarted` |
| `input_json_delta.partial_json` | accumulated, parsed at `ToolStarted` completion, never emitted as a partial parse |
| `content_block_stop` for a tool | `ToolFinished` |
| `user` message with a non-null `parent_tool_use_id` | `SubagentStarted` |
| `assistant`/`user` message with a parent id, after the first | `SubagentFinished` or a nested content event |
| `system` + `subtype: api_retry` | `ApiRetry` |
| `result` message | `RunFinished`, plus `CostUpdate` if usage was present |
| A line that does not parse | `MalformedOutput` — recorded, skipped, run continues |

**The `--verbose` trap.** `stream-json` output is not what we expect without `--verbose`. A contract test asserts that the invocation always includes it, and a fixture test proves the mapping is still correct if it is ever removed.

**Subagent flattening is a legal mapping.** If a version does not forward subagent text, the mapper emits subagent lifecycle events without their content. The UI renders a subagent as a card with its tool calls, which is correct and complete enough. Forwarding is an enhancement, not a requirement.

## Compatibility rules

1. **Adding a case is a minor change.** Consumers must handle unknown events by ignoring them. Every `when` over `AgentEvent` in the codebase has an `else` branch that logs and continues.
2. **Removing or renaming a case is a major change.** It requires a mapping layer, a fixture update, and a migration if anything was persisted. We have not done this yet, and the first time we do, the ADR log gets an entry.
3. **Adding a required field to an existing case is a breaking change.** Add it nullable, ship it, make it non-null in the next major.
4. **The wire format is versioned separately from the class.** Persisted events carry a `schemaVersion`. A reader that meets a higher version reads what it understands and records that it did not understand the rest.
5. **Unknown fields are ignored**, never rejected. The parser is lenient by design; the engine may add fields without warning.

## Persistence

| Consumer | Where | Why |
|---|---|---|
| Chat UI | In memory, with a replay buffer | Live rendering |
| `ConversationRepository` | Room, per turn | The transcript |
| `LogRepository` | `SessionLogEntry`, append-only | The transparency record |
| `CostMeter` | `CostRecord` | Aggregation |
| `AntiLoopDetector` | In memory, per run | A step repeating |
| `ContextBudgeter` | In memory, per run | Token accounting |

**Events are persisted, not derived.** The transcript is a record of what happened, not a re-render of what the UI displayed. If the mapping changes, old transcripts still render, because they were stored as domain types.

## Ordering guarantees

1. Events reach consumers in the order the engine produced them.
2. A `ToolStarted` is always followed by exactly one `ToolFinished` or a `PermissionRequested` then a `ToolFinished`.
3. `RunFinished` is always last. A run that is killed has its `RunFinished` synthesised by the session manager, never omitted, so no consumer waits forever for it.
4. `CostUpdate` may arrive after `RunFinished` when the provider reports usage in a final frame. The cost meter accepts late updates; the summary waits briefly for one before rendering.
5. Concurrent tool calls are interleaved in engine order and correlated by `toolUseId`. The UI renders them as a list, not a tree, unless a parent id exists.

## Rate and size limits

| Concern | Limit | Behaviour when exceeded |
|---|---|---|
| `TextDelta` size | 16 KB per chunk | Larger payloads are split. A single delta is never dropped. |
| `ToolOutputDelta` | 8 KB per chunk, streamed to a file, preview capped at 2 KB | The preview is truncated with an explicit marker |
| Buffered events while the UI is gone | 2000, then the oldest non-critical are dropped | `RunStarted`, `RunFinished`, `PermissionRequested`, and `CostUpdate` are never dropped; the log has the rest |
| `ThinkingDelta` accumulation | 64 KB per message | The rest is not accumulated. It is in the log. |

**Nothing critical is ever dropped for size.** The dropped-events counter is displayed in the transparency view, so a gap is visible rather than silent.

## A run, as a stream

```
RunStarted(projectId, backendId, profile, autonomyLevel, toolsAllowed)
PlanProposed(Plan(3 steps, 1 checkpoint))
ToolStarted(Read, "build.gradle.kts")
ToolFinished(ok)
ThinkingDelta("The build file pins AGP 8.2 but the project uses 8.7 …")
TextDelta("I'll start by …")
ToolStarted(Edit, "build.gradle.kts")
PermissionRequested(Edit — modifies a build file)
PermissionResolved(ALLOW_ONCE)
ToolFinished(ok)
ApiRetry(attempt=1, maxRetries=5, reason=Overloaded, delayMs=2000)
TextDelta("Retrying …")
CheckpointReached(beforePush)
CostUpdate(input=14200, output=3100, usd=0.0412, isEstimated=true)
RunFinished(DONE, summary)
```

The UI renders each of those without special-casing the backend, the provider, or the machine. That is the point of this document.

## Versioning the protocol

`EventSchema.VERSION` increments on a breaking change. A run records the version it started with, so a transcript from an older app version is readable and a transcript from a newer one is honestly reported as partially understood rather than mis-rendered.
