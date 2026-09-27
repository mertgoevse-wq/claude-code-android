# Concurrency model

Which thread does what, who owns which scope, and what happens when two things want the same thing at once.

## Dispatchers

Declared once in `shared/core`, injected everywhere, never referenced as `Dispatchers.IO` outside `core`.

| Dispatcher | Used for | Why |
|---|---|---|
| `Main` | Composables, state updates, notification building on the main process | Framework requirement |
| `IO` | Room reads and writes, network streaming, git subprocesses, filesystem, the bootstrap download | Blocking or slow, unbounded parallelism |
| `Default` | Diff computation, plan parsing, cost aggregation, cost estimation, string processing | CPU-bound, bounded by the core count |
| `Unconfined` | Only inside `StateFlow` collectors that must not buffer, and never for I/O | Easy to misuse; used in exactly three places, each with a comment |

**`limitedParallelism(2)` for git.** Two git operations on different projects can run in parallel; on the same project they cannot, because two concurrent commits in one worktree produce a state neither intended. The repository serialises per project with a `Mutex`, and the git dispatcher is limited to a small number so a large import does not starve the UI of IO capacity.

## Scopes

| Scope | Owner | Lifetime | Cancelled by |
|---|---|---|---|
| `appScope` | `App` | The process | Nothing; it is the root |
| `sessionScope` | `AgentSessionManager`, one per running session | The session | Session end, cancel, or a fatal error |
| `runScope` | A `Run` | The run | Turn completion, cancel, retry |
| `verifyScope` | A `VerificationRun` | The verification | Verification completion |
| `screenScope` | A ViewModel | The screen, across configuration changes | `onCleared` |
| `serviceScope` | The foreground service | The service | `onDestroy` |
| `pollerScope` | The GitHub notification poller | The app, while enabled | Disabling GitHub, or a low-battery stop |

**The rule:** a scope is created by the thing that owns the work, and cancelled by that same thing in its `finally`. There is no `GlobalScope`, no fire-and-forget coroutine, and no coroutine launched from a composable.

## Structured concurrency rules

1. **A child never outlives its parent.** If a screen starts work, the screen's scope owns it.
2. **Cancellation is a signal, not an error.** Cancellation exceptions are rethrown, never swallowed, and never converted into a user-facing "something went wrong".
3. **Failures are values.** Every fallible call returns `Outcome<T>`, so an error crosses a `Flow` as data rather than as an exception that kills a collector.
4. **A flow that represents a resource owns closing it.** `ConversationRepository.observeTurns()` closes its cursor in `awaitClose`.
5. **Mutex, not a lock.** No `synchronized`, no `ReentrantLock`. Suspending code holds a thread hostage if it blocks.

## Streams

The event pipeline, from the engine to the screen:

```
CLI stdout
  │  read on IO, line by line
  ▼
AgentStreamClient ── parses JSON, emits raw lines
  │
  ▼
AgentEventMapper ── raw line → AgentEvent | MalformedEvent
  │
  ├──► ConversationRepository (IO) ──► Room
  ├──► LogRepository (IO) ──► append-only log
  ├──► ChatViewModel (Main) ──► UiState
  └──► CostMeter (Default) ──► aggregated cost
```

Four consumers of one flow. `shareIn` with a replay buffer of the events not yet persisted, scoped to `runScope`, so a ViewModel that attaches late still sees the beginning of the run.

**Backpressure:** the engine is faster than the UI. The flow is buffered, and the buffer is bounded. When it fills, the engine's stdout read blocks — which is correct, because the pipe applies backpressure too, and an unbounded buffer would be a memory leak with extra steps.

**Ordering:** events are emitted in the order the engine produced them, and persisted in that order. A malformed line does not reorder anything: it is recorded as a `MalformedEvent` in sequence and skipped.

## The main thread

Hard rule: no blocking work on Main, enforced by a StrictMode-like assertion in debug builds that throws if a database read, a filesystem call, or a subprocess is initiated from Main.

| Allowed on Main | Forbidden on Main |
|---|---|
| Compose composition and layout | Room queries |
| StateFlow updates | Network calls |
| Animation frames | File reads and writes |
| Reading small in-memory caches | `ProcessBuilder` |
| Notification tap handling | JSON parsing of a large output |

## Writing to disk

The app writes in a few places, and they have different durability needs.

| What | When | Durability |
|---|---|---|
| Project files | As the engine writes them | The engine's business, not ours |
| Run and tool state | On every event | Immediate. A crash must not lose the run. |
| Terminal scrollback | Continuously, to a file | Buffered; the tail can be lost, the session cannot |
| Log entries | On write | Immediate for `ERROR`, buffered for `INFO` |
| Cost records | On every usage event | Immediate. A wrong cost is worse than a slow write. |
| Bootstrap state | On every state transition | Immediate. This is what makes the bootstrap resumable. |

**Serialising writes.** SQLite handles its own transactions. Files do not: two goroutines writing the same terminal log would interleave. Each file has a single owning writer with a queue.

## Shutdown and restart

The states where the process can die unexpectedly, and what we guarantee:

| Event | What survives | What does not |
|---|---|---|
| User swipes the app away | Everything in the database, including the run state and the session id | The process; the run is `INTERRUPTED` |
| Android kills the process under memory pressure | The same, plus the foreground-service notification | The process; the run resumes or is marked interrupted |
| Device reboot | Everything; a boot receiver restores active sessions | The process; the run is `INTERRUPTED` with reason `DEVICE_REBOOT` |
| App update | Everything, after migration | The process; the run is `INTERRUPTED` with reason `APP_UPDATED` |
| Power loss | Everything already committed to the database | The process |
| The engine crashing | Everything, including the run state and the log | The engine's in-flight turn; recorded as a crash with the raw output |

**The recovery contract:** on launch, the app finds any run in a non-terminal state, checks whether its backend is still alive, and either offers a resume or marks it interrupted with a plain-language reason. It never silently restarts a run. Restarting without asking would risk doing something twice.

## Concurrency hazards we have identified

| Hazard | Mitigation |
|---|---|
| Two runs on one project | A per-project `Mutex` around the working directory. A second run in a directory with a run in progress is refused with a clear message, not queued silently. |
| Reading a run's state while it is being written | The database is the single writer; the UI observes a flow. No in-memory copy that could diverge. |
| A ViewModel collecting a flow that has no terminal state | Every long-running flow has a terminal event, including on cancellation, so collectors complete. |
| A screen surviving rotation mid-stream | `stateIn` with `WhileSubscribed(5_000)` on the run's event flow, and a replay buffer covering the gap. |
| A toast or navigation firing twice | One-shot UI events go through a `Channel`, not a `StateFlow`. |
| A run finishing while the user is looking at a stale screen | The screen observes the run's state from the database, not from a local copy. |
| The cost meter being read from two places | One `CostMeter` per run, in the orchestration layer, exposed as a flow. |

## Instrumentation

Debug builds assert:

- No database or filesystem call from Main.
- No `GlobalScope` usage anywhere (a lint rule, not a comment).
- Every `CoroutineScope` created with an explicit `Job`, so cancellation is a property of the type.
- Dispatcher access only inside `core` (a lint rule on `Dispatchers.` references).

Release builds expose a live trace in the transparency view: every scope, its children, and its state. When a user reports a stuck run, that trace is in the diagnostics bundle and it is usually the whole answer.
