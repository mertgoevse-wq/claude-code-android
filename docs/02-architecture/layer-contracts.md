# Layer contracts

What each layer is allowed to do, and the specific mistakes it must never make. Most of this file is a list of forbidden mistakes, because those are the ones that happen.

## The ladder

Data flows **down**; events and results flow **up**. A layer may call the layer below it and nothing else. There are no sideways calls between siblings; shared behaviour moves down to a layer both can see.

```
UI  →  Presentation  →  Domain  →  Core
                     ↘  Orchestration → Execution
                     ↘  Data / Vcs / Skills / Providers
```

## Layer by layer

### UI

**May:** read a `UiState`, emit events, navigate, animate, read `DesignTokens`, open the terminal, and call an `actual` provided by `androidApp` for something genuinely platform-shaped (haptics, a share sheet, a file picker).

**May never:**

- Construct a domain object that the domain did not create. A `Project` is built by a use case.
- Touch a repository, a DAO, or a `GitClient`.
- Know the format of a CLI event line.
- Contain a `when` over an engine state. That is the ViewModel's job, and the ViewModel's job exists so it can be tested.
- Do blocking work. No `runBlocking`, no `Thread.sleep`, no `Future.get`.
- Contain business rules disguised as layout logic. "Show the push button only when the branch is not the default" is a rule; it belongs in a use case that puts a boolean in the state.

**The pragmatic exception:** the terminal. It owns a scroll buffer, a selection, and a cursor. It keeps that state locally and says so. A screen that is genuinely a view onto a byte stream is allowed to hold the stream's view state.

### Presentation

**May:** combine repositories and use cases into a `UiState`, hold a coroutine scope tied to the screen, and emit one-shot UI events (navigate, snackbar, permission sheet).

**May never:**

- Perform I/O outside a use case. "Load the project list" is a use case; the ViewModel collects its flow.
- Use a raw `viewModelScope` for something that must survive a configuration change without re-fetching; that is `stateIn` with a sensible policy.
- Emit a `StateFlow` that contains a resource the screen must close. If it owns a stream, it closes it in `onCleared`.
- Know the concrete backend. It asks for an `AgentSession`, not for a subprocess.

### Domain

**May:** be pure. Decide things. Own the vocabulary.

**May never:** import anything from Android, Room, Ktor, Koin, Compose, or the CLI. `shared/domain` builds against the Kotlin standard library and `kotlinx.serialization` annotations, and that is the entire dependency list.

**The test for whether something belongs here:** if it can be written as a function from data to a decision, with no I/O, it belongs here. `HardBlockPolicy`, `PermissionResolver`, `BudgetPolicy`, `Judge`, and the retry arithmetic are all here, which is why the hard rules are testable without a device.

### Orchestration

**May:** coordinate. Hold a long-running operation's state. Decide what happens next.

**May never:**

- Talk to a UI. It emits events; the ViewModel maps them into state.
- Reach into a ViewModel or a screen.
- Assume it is running on the phone. It receives an `ExecutionBackend` and asks it.
- Perform a git operation directly. It asks `vcs`.
- Hide a hard block. It cannot grant one.
- Depend on wall-clock time. Time arrives through `TimeProvider`; this is why retry and loop logic are testable without sleeping.

**Ordering rule:** the sequence is `plan → execute → verify → judge → act`. Every branch of a run passes through `judge`. There is no path that reaches "done" without it. This is enforced by a test that walks every path of `RetryController` and asserts none of them can produce a `DONE` state without a passing `VerificationRun`.

### Data

**May:** read and write, cache, call the network, and translate between storage shapes and domain types.

**May never:**

- Contain a business rule. A repository that decides something is a use case wearing a disguise.
- Return a nullable "just in case". It returns `Outcome<T>`, so the failure is part of the type.
- Do work off the main thread by accident. Suspending functions do not move themselves; the dispatcher is part of the gateway they call.
- Hold mutable state that outlives a call.

### Runtime

**May:** install, launch, supervise, and diagnose. It is the only layer allowed to spawn processes.

**May never:**

- Spawn a process without registering it in `ProcessSupervisor`, or it cannot be cancelled.
- Assume an install step is atomic. Every step is resumable.
- Swallow an error. Each failure maps to an `AppError` with a user-facing message; see `02-architecture/error-taxonomy.md`.
- Continue with a partially installed runtime. Half an environment is worse than none, because it fails later and further away.

### Execution

**May:** translate an `AgentRequest` into a process invocation or a network call, and translate raw output into `AgentEvent`.

**May never:**

- Know about projects, chat, or plans. It gets a directory and a prompt.
- Make a decision. A permission request is an event; the orchestrator decides how to answer it.
- Swallow a malformed line silently. An unparseable line is logged raw and surfaced as a `MALFORMED_OUTPUT` event; the run continues, because a single bad line should not lose an hour of work.

### Vcs

**May:** run git, talk to GitHub, enforce push policy.

**May never:**

- Build a command by concatenating a string. Arguments are typed and serialised. This is a security property, not a style preference.
- Push to a default branch. `PushPolicy` throws before the call is made.
- Delete anything. The command allowlist simply has no delete subcommand, so there is nothing to filter at runtime — the capability is absent by construction.

### Skills

**May:** parse, validate, install, generate, and list.

**May never:**

- Write to disk without a confirmed plan.
- Execute anything it installs. A skill is data; what runs it is the engine, under the engine's permissions.
- Trust a name. A skill's directory name and its frontmatter name must agree, or installation fails with a clear message.

### Core

**May:** be boring. Types, dispatchers, gateways, and redaction.

**May never:** contain a domain concept. `core` does not know what a project is.

## Cross-cutting contracts

### Error handling

Every fallible call returns `Outcome<T>`. No exceptions cross a module boundary except programming errors, which crash in development and become a recorded failure in release. See `02-architecture/error-taxonomy.md`.

### Cancellation

Cancellation is structured. A ViewModel's scope cancels its children; a run's scope is a child of the session scope; the process is killed by `ProcessSupervisor` in the scope's `finally`. There is no fire-and-forget coroutine that outlives its screen. This is what makes "the app was killed mid-run" a recoverable state rather than a corrupted one.

### Threads

| Work | Dispatcher |
|---|---|
| Composables, state | Main |
| Repository reads | IO |
| Pure computation | Default |
| git subprocesses | IO, with a serialized queue per project |
| Network streaming | IO, never buffered on Main |

Two git operations on the same project never run concurrently. A per-project `Mutex` enforces it, because two concurrent commits on one worktree produce a state neither operation intended.

### One-shot UI events

A navigation or a snackbar is an event, not state. It is delivered through a `Channel` consumed once by the screen, so a rotation does not replay a navigation. Anything that must survive rotation is state.

### Time

`TimeProvider` everywhere that measures or waits. Retry backoff, elapsed-time display, the daily update check, and the anti-loop window all read the injected clock. A test for backoff asserts the delays, and runs in milliseconds.

## The contract tests

| Test | What it proves |
|---|---|
| `domain purity` | `shared/domain` has no forbidden imports |
| `shared purity` | `shared/core` and `shared/domain` have no Android imports |
| `no done without judge` | No path from any run state to `DONE` bypasses a passing verification |
| `hard blocks hold` | Each of the five rules refuses, at every autonomy level |
| `no delete capability` | The git command builder has no delete path, by type |
| `no analytics` | No analytics SDK or host appears anywhere |
| `event schema` | `AgentEvent` matches the recorded fixture contract |
