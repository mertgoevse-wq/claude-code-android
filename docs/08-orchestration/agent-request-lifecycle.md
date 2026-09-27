# Agent request lifecycle

Everything between "the user pressed send" and "the run is in a terminal state", in order.

## The full sequence

```
1. Accept
2. Validate
3. Resolve policy
4. Resolve the backend
5. Prepare the working state
6. Build the request
7. Stream
8. Persist, concurrently
9. Verify
10. Judge
11. Act
12. Notify
13. Close
```

Each step is a function with one job and one failure mode. Nothing is skipped, and nothing happens out of order.

## 1. Accept

| What | Detail |
|---|---|
| Input | The task text, the project, the conversation, the run overrides |
| Validation | Non-empty after trimming. Attachments resolved. The provider is configured. The runtime is ready. |
| Creates | A `Run` row in `CREATED`, and a `Turn` in `PLANNING` |
| On failure | The message is **already in the transcript**, optimistic, per `04-screens/03-new-chat.md`. The run carries the error, and the user can edit and resend. |
| Timing | Under 50 ms, local database writes only |

An invalid request never loses the user's text. That is the one rule at this step.

## 2. Validate

The checks that stop a run before it costs anything.

| Check | Failure |
|---|---|
| A provider is configured | `NO_PROVIDER_CONFIGURED`, a link to the setup |
| The key resolves | The provider's screen |
| The model is set | The model sheet, defaulted to the project's |
| The runtime is `READY` | The runtime setup, or the run is not started at all |
| The project path exists | `PROJECT_PATH_MISSING` |
| The project is writable | The specific filesystem error |
| No run is active for this project | A link to the running one |
| The autonomy level is valid | A programming error, so a crash, because a stored enum cannot be invalid |
| Free storage is sufficient | The shortfall. The estimate is the project's size times 3, plus 200 MB |

**Validation happens before the first token is spent.** A run that cannot possibly work should say so in under a second, not after a plan has been generated.

## 3. Resolve policy

`PermissionResolver` decides what this run may do unattended.

```kotlin
data class ResolvedPolicy(
    val level: AutonomyLevel,
    val permissionMode: String,          // the CLI mode
    val allowedTools: Set<String>,
    val deniedTools: Set<String>,        // always ∪ HardBlockPolicy
    val checkpoints: Set<CheckpointKind>,
    val retryBudget: Int,                // -1 = unlimited
    val offloadPolicy: OffloadPolicy,
)
```

| Input | Effect |
|---|---|
| The project's autonomy level | The base |
| The run's override | Wins over the project, for this run only |
| The project's denied tools | Unioned in |
| `HardBlockPolicy` | Unioned in. **Always. At every level. With no override.** |
| The project's retry budget | The base |
| The project's offload policy | The base |

The resolved policy is stored on the run. A run's behaviour is reconstructable from its record, which is what makes the transparency log meaningful.

## 4. Resolve the backend

Per `06-runtime/execution-backends.md`. The rules, in order:

1. A run override.
2. The project is pinned to a runner, and it probes online.
3. The offload policy is `ALWAYS`, and a runner is available.
4. A required toolchain is missing on the device, and a runner has it.
5. Otherwise, the device.

| Outcome | Behaviour |
|---|---|
| Resolved | The backend id and the reason are stored on the run and shown in the header |
| Pinned but offline | **Refused**, with the reason. Not moved to the phone. |
| Needed a toolchain, no runner has it | The device, with a note: "Gradle fehlt auch auf {host}." |
| No runner configured | The device. |

## 5. Prepare the working state

| Situation | Action |
|---|---|
| Clean tree, on any branch | A feature branch from HEAD: `{prefix}{slug}-{shortId}` |
| On the default branch | The same. The default branch is never committed to. |
| Uncommitted changes | A branch from the current state. The changes come along and stay uncommitted. |
| Detached head | A branch at the current commit |
| A merge or rebase in progress | **Refused**, with a link to the terminal |
| The branch name is taken | A different short id. Never a force-replace. |
| Worktree isolation is on | A `git worktree` under the app's own directory, removed only by the audited erase path |
| A permission request is pending for this project | Refused, with a link to it |

| Never | Reason |
|---|---|
| A stash | A stash is a place changes go to be forgotten. |
| A reset, ever | A hard block |
| A clean, ever | A hard block |
| A checkout that would discard | Refused |

## 6. Build the request

| Field | Source |
|---|---|
| The prompt | The task text, plus the conversation history, plus the project's `CLAUDE.md` awareness, plus the active skills |
| The working directory | The project, or the worktree |
| The model | The run's override, else the project's, else the global default |
| The provider | The project's, else the global default |
| The permission mode | From the resolved policy |
| The allowed tools | From the resolved policy |
| `--output-format stream-json --verbose --include-partial-messages` | Always |
| The session id | If resuming |
| `CLAUDE_CODE_RESUME_INTERRUPTED_TURN=1` | On a resume |
| The environment | Built explicitly, per `06-runtime/process-supervision.md`. Never inherited wholesale. |
| `--add-dir` | Only for a directory the run genuinely needs beyond the project, and only from the project's configuration |
| `--mcp-config` | Only an ad-hoc configuration the user confirmed for this run |

**The environment is built, not inherited.** The app's process environment contains Android internals, and passing all of it to a subprocess is both unnecessary and a small information leak to a third-party binary. A whitelist, assembled deliberately.

## 7. Stream

The main flow. Per `02-architecture/event-protocol.md`.

| Concern | Behaviour |
|---|---|
| Reading | Line by line, off the main thread |
| Mapping | `AgentEventMapper`, the only place that knows the wire format |
| A malformed line | Recorded raw, skipped, the run continues |
| A partial line | Held until a newline or stream close |
| Backpressure | A bounded buffer. A slow UI does not grow memory; it slows the pipeline, which is correct. |
| Persistence | Every event, concurrently, per step 8 |
| Interruption | SIGINT, 2 s, SIGTERM, 2 s, SIGKILL, verify, per `06-runtime/process-supervision.md` |
| A permission request | Emitted as an event. The app answers it. The engine does not block on a dialog. |
| Process death | The flow completes exceptionally; the run becomes `INTERRUPTED` |

## 8. Persist, concurrently

Four consumers of one flow, per `02-architecture/concurrency-model.md`. Started before the first event arrives, so nothing is missed.

| Consumer | What it writes | Failure behaviour |
|---|---|---|
| The transcript | Messages, tool invocations, plan steps | A database failure is fatal to the run. A run whose transcript is incomplete is worse than a failed run. |
| The log | Every category | A failure is logged to the crash handler and retried once. The log is append-only and a missing entry is bad but not fatal. |
| The cost meter | Usage events | A failure means the cost is estimated. The UI says so. Never a silent zero. |
| The anti-loop detector | In memory | A failure is a programming error |

**Writes are on every event, not at the end of the run.** A process death at minute eighteen must not lose the first seventeen.

## 9. Verify

Per `05-features/verification.md`.

| Situation | Behaviour |
|---|---|
| The run produced file changes | Verify. This is the case that matters. |
| The run produced no changes and said it was a question | No verification. The run is an answer, not a change. It is labelled as such, and no green tick is shown, because nothing was changed to be correct about. |
| The run was interrupted | No verification. There is nothing complete to verify. |
| The run failed | No verification. A failed run has no result to check. |
| No commands are configured | `UNVERIFIED`. Never `PASSED`. |
| Commands are configured | Run them, sequentially, stopping at the first failure, each with its own timeout |

The second row matters and is easy to get wrong. Somebody asks "why does this test fail?" and gets an explanation. There is no diff, so there is nothing to verify, and showing a green tick on a conversation would be a lie about what happened.

## 10. Judge

Per `05-features/verification.md`. Deterministic, from exit codes.

| Result | Next |
|---|---|
| `PASSED` | Step 11 |
| `FAILED` or `ERROR` | The retry controller |
| `UNVERIFIED` | Step 11, and the run is labelled unverified everywhere it appears |
| No verification was run, because there were no changes | Step 11, labelled as an answer, not as verified work |

`judgedAt` is recorded. A summary says when the decision was made, not just what it was.

## 11. Act

| Condition | Action | Failure |
|---|---|---|
| Changes exist and the run is not cancelled | Stage and commit on the feature branch | The run continues and reports; a failed commit is not a failed task |
| Tests passed, or the run is unverified, or there are no changes | Push the branch | A push failure is reported with the reason, and the commit is kept |
| The push succeeded | Open a pull request | A failure links to the existing one if there is one |
| Offload policy says the runner | The same, on the runner | As above |
| Nothing to do | Nothing | — |

| Never | Reason |
|---|---|
| Push to the default branch | `PushPolicy`, per ADR-006 |
| Push with a failing verification | The gate is the point |
| Delete anything | A hard block |
| Modify the pull request of a previous run | Each run gets its own branch and its own pull request |

**An unverified run is pushed.** The gate is "verified, or honestly labelled unverified". Refusing to push unverified work would make the app useless for projects without tests, which is most personal projects.

## 12. Notify

Per `07-integrations/notifications.md`. One event, mapped to one notification, and the notification is skipped entirely if the app is in the foreground.

The notification is posted **after** step 13's state write, so a notification that leads the database is impossible.

## 13. Close

| Action | Detail |
|---|---|
| The run's state | Written as terminal, with `endedAt` and `durationMs` |
| The final events | Flushed, so a `RunFinished` cannot be lost to a scope cancellation |
| The backend | Released, without killing anything still wanted |
| The wake lock | Released |
| The foreground service | Stopped if this was the last run |
| The session | Kept, for a resume. A session is cheap and resumability is a feature. |
| The working tree | **Not touched.** The user decides what happens to the changes. |
| The log | A final entry with the summary |
| The retention check | Whether this run has aged past the retention window |

## What happens when a step fails

| Step | Failure behaviour |
|---|---|
| 1–6 | `FAILED` before any token is spent. The message is in the transcript. |
| 7 | `INTERRUPTED` if the process died, `FAILED` if the engine reported a terminal failure |
| 8 | `FAILED`. A run with a broken transcript is not trustworthy. |
| 9 | `FAILED` with the verification results attached |
| 10 | A programming error. It crashes in development and is recorded in production. |
| 11 | The run reaches `DONE` with the action's failure reported. A failed push does not un-write the work. |
| 12 | Logged. The state is already correct in the database. |
| 13 | Every step is in a `finally`. A leak here is a battery bug, and there are tests for each. |

## Testing

| Test | Type |
|---|---|
| `FullSequence` | E2E — a complete successful run, with every step observed in order |
| `ValidationBeforeSpend` | E2E — a run with no provider configured produces zero model requests |
| `OptimisticMessage` | E2E — the task text is in the transcript within 100 ms, before the run exists |
| `PolicyResolvedAndStored` | E2E — the run's record contains the full resolved policy |
| `PolicyHardBlocksAlways` | Unit — the resolved policy's denied set contains the hard blocks at every level |
| `BackendReasonStored` | E2E — the backend and the reason are on the run and in the header |
| `PinnedOfflineRefuses` | E2E — a pinned, offline runner refuses rather than falling back |
| `WorkingState` | E2E — every tree state per `05-features/project-lifecycle.md` |
| `MergeInProgressRefuses` | E2E — refused with a terminal link |
| `EnvironmentWhitelisted` | Static — the process environment is built from a whitelist; nothing is inherited wholesale |
| `MalformedLineContinues` | E2E — a corrupt line mid-stream does not end the run |
| `PartialLineAssembled` | E2E — output without newlines produces correct events |
| `PersistEveryEvent` | Integration — the transcript's event count equals the stream's |
| `WriteBeforeNotify` | Integration — the terminal state is in the database before the notification is posted |
| `VerifyOnlyWithChanges` | E2E — an answer with no changes is not verified and is not shown as verified |
| `JudgeDeterministic` | Unit — the same verification results always produce the same judgement |
| `PushGate` | E2E — a failing verification blocks the push; an unverified run does not |
| `PushDefaultBlocked` | E2E — a push to the default branch is refused |
| `UnverifiedPushedAndLabelled` | E2E — an unverified run pushes, and the run, the commit, and the pull request all say so |
| `NoForegroundNotification` | E2E — no notification while the app is in the foreground |
| `CloseInFinally` | Integration — every cleanup runs on every failure path, asserted for each |
| `TreeUntouchedAtClose` | E2E — the working tree is exactly as the run left it |
| `ResumeSameSession` | E2E — a resume uses the same session id and continues the conversation |
| `InterruptionPath` | E2E — an interrupt produces `INTERRUPTED` within 3 s, with a resumable session |
