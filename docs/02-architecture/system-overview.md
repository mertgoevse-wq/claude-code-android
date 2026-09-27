# System overview

One page. If you read nothing else in this folder, read this.

## The shape

```
┌──────────────────────────────────────────────────────────────────┐
│  UI  (Compose Multiplatform)                                    │
│  chat · projects · skills · terminal · diff · settings · github  │
└──────────────────────────────────────────────────────────────────┘
                                ↕ UiState / events
┌──────────────────────────────────────────────────────────────────┐
│  PRESENTATION                                                    │
│  ViewModels · sealed UiState · one-shot UI events                │
└──────────────────────────────────────────────────────────────────┘
                                ↕ use cases
┌──────────────────────────────────────────────────────────────────┐
│  DOMAIN                                                          │
│  models · policies (permissions, hard blocks, budget) · use cases │
│  pure Kotlin, no framework, no Android                           │
└──────────────────────────────────────────────────────────────────┘
           ↕                    ↕                     ↑
┌────────────────────┐ ┌────────────────────┐ ┌────────────────────┐
│ ORCHESTRATION      │ │ RUNTIME            │ │ VCS                │
│ plan · execute ·   │ │ bootstrap ·        │ │ git wrapper ·      │
│ verify · judge ·   │ │ supervise · pty ·  │ │ branch · commit ·  │
│ retry · budget     │ │ diagnose           │ │ push · PR · GitHub │
└────────────────────┘ └────────────────────┘ └────────────────────┘
           ↕                    ↕                     ↑
┌────────────────────┐ ┌────────────────────┐ ┌────────────────────┐
│ DATA               │ │ SKILLS             │ │ PROVIDERS          │
│ Room · repos ·     │ │ parse · validate · │ │ Anthropic ·        │
│ Ktor · cost meter  │ │ install · generate │ │ OpenAI · custom    │
└────────────────────┘ └────────────────────┘ └────────────────────┘
                                ↕
┌──────────────────────────────────────────────────────────────────┐
│  CORE   Result · AppError · dispatchers · crypto · fs · platform  │
└──────────────────────────────────────────────────────────────────┘
                                ↕
┌──────────────────────────────────────────────────────────────────┐
│  EXECUTION BACKENDS (the only platform-specific seam)             │
│  AndroidLocalBackend · SshBackend · OracleBackend · ActionsBackend│
└──────────────────────────────────────────────────────────────────┘
```

## The three decisions that shaped this

### 1. One seam: `ExecutionBackend`

Everything above the backend layer is platform-agnostic. The backend is the only place that knows whether Claude Code is running on the phone, on a home PC, or on a free cloud instance.

This is what makes "local first, server later" an additive change rather than a rewrite. It is also what keeps `shared/` importable from iOS: a phone-local backend is Android-only, and everything above it does not know that.

**The cost of the abstraction is real and we accept it:** some features are genuinely local-only. Building an APK on the phone is local-only. A home-PC runner has a different filesystem. The interface models that with `BackendCapabilities` rather than pretending the platforms are identical.

### 2. The event stream is the contract

The engine speaks newline-delimited JSON. We map it once, in `AgentEventMapper`, into a closed set of `AgentEvent` values. Everything downstream — the chat UI, the notification system, the log, the cost meter, the judge — consumes only that set.

Consequences, all of them good:

- A provider change or a CLI version change is contained to one file and one contract test.
- The UI is testable without an engine: fixtures replay real output.
- Remote and local runs are indistinguishable to every layer above the backend.

### 3. Verification decides success

The agent saying "done" is a claim, not a result. `Verifier` runs the project's own commands; `Judge` decides pass or fail from the exit codes; only then does the app say finished. A project with no verification commands gets labelled **unverified**, never **done**.

This is the difference between this app and every other mobile client, and it is why `05-features/verification.md` is a long document.

## The data flow of one task

```
User taps send
  │
  ├─ PermissionResolver decides what needs asking, from the project's autonomy level
  │     and HardBlockPolicy vetoes unconditionally
  │
  ├─ AgentRequestBuilder assembles: prompt, working dir, tools, model, provider env
  │
  ├─ ExecutionBackend.stream(request) → Flow<AgentEvent>
  │
  ├─ AgentEventMapper maps CLI JSON → AgentEvent, writing each raw line to the log
  │
  ├─ ConversationRepository persists every event (so the log is reconstructable)
  │
  ├─ ChatViewModel reduces events into UiState; the mark's state machine subscribes
  │
  ├─ On stop: Judge evaluates the last VerificationRun
  │     ├─ pass → CommitBuilder → branch → push (if green) → PullRequestService → notify
  │     └─ fail → RetryController: diagnose → fix → re-verify, within the budget
  │               └─ budget exhausted → write report, notify, stop
  │
  └─ LogRepository receives every command, decision, error, and cost. Append-only.
```

## Where state lives

| State | Home | Why |
|---|---|---|
| Conversations, turns, tool calls | Room | Queryable, survives process death, drives history |
| Projects, settings, autonomy | Room | Same |
| Cost records | Room | Aggregation across runs |
| Activity log | Room, append-only | Never edited, never deleted |
| Skills registry | Room + files on disk | Metadata in DB, content on disk where git can see it |
| Secrets | Android Keystore | Not in the database, not in the log, not in backups |
| Session IDs | Room | Needed to resume |
| Files of a project | The filesystem, inside the project folder | Git must see them; the agent must edit them |
| Terminal scrollback | Files | Large; not worth a table |

## The five hard rules in code

They are not a policy page. They are `HardBlockPolicy`, consulted by `PermissionResolver` before any tool dispatch, tested with a test per rule, and additionally denied in `.claude/settings.json` so the build cannot take a shortcut either.

| Rule | Enforced in | Also |
|---|---|---|
| Never delete | `HardBlockPolicy.blockIfDestructive()` | Command sanitizer, permission deny-list |
| Never spend money | `HardBlockPolicy.blockIfPaid()` | No paid path exists in the UI at all |
| Never make public | `HardBlockPolicy.blockIfPublic()` | Repository creation forces `private: true` |
| Never push to default | `PushPolicy` | A guard between `CommitBuilder` and the git call |
| Never hide | `LogRepository` is append-only | Transparency view, export |

Raising autonomy to `FULL_AUTO` does not relax any of them. There is no code path that does.

## Failure posture

| Failure | The app's behaviour |
|---|---|
| Network drops mid-run | Run is marked interrupted, not failed. Resume is offered. Partial work is committed to `wip/`. |
| The engine binary breaks after an update | Rollback to the last known-good version, user told why |
| Verification fails repeatedly | Retry within the budget, then a plain-language report and a stop |
| A step repeats without progress | Anti-loop detector aborts it and records why |
| The process is killed by Android | The foreground service plus a boot receiver restores the session; the user is told it was interrupted |
| A provider is overloaded | The `api_retry` event is surfaced as "overloaded, attempt 2 of 5" — never a silent stall |
| Out of storage | Report exactly how much is needed. Never delete anything to make room. |
| A key is wrong | Say so on the connection test, before a run starts |

## What is deliberately not in this architecture

- No push server, no sync service, no account. Nothing to run.
- No analytics, no tracking, no advertising identifier.
- No pluggable model beyond the provider dialects; the engine is the model loop.
- No general plugin runtime for *our* app. Skills extend Claude Code, not the app itself — the one exception being the documented set of skills the app can install.
- No shared state between app instances. One process, one user, one device.
