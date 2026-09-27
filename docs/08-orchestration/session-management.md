# Session management

A session is a conversation with the engine, persisted on disk, that can be continued. Managing them is what makes a phone usable: a run that dies must be resumable, and resuming must be free of ambiguity.

## What a session is

| Property | Detail |
|---|---|
| Identity | The engine's session id, stored per conversation |
| Storage | A file on disk, in the runtime profile's location, written by the engine |
| The app's copy | The session id in the database, plus the full event transcript in Room |
| Lifetime | Until the conversation is erased, or the user erases the local project data |
| Cost | Small. A session file is kilobytes. |

**The app keeps two things and they are not redundant.** The session id is what lets the engine continue; the transcript is what lets a person read what happened. The transcript is authoritative for display; the session is authoritative for the engine's memory. A transcript that is complete and a session that is missing is a readable history that cannot be continued. Both are kept, both are checked.

## States

Per `02-architecture/state-machines.md`, section 2.

| State | Meaning | Who moves it |
|---|---|---|
| `IDLE` | No session, or the session is not attached | The user |
| `CREATING` | Spawning the process | The session manager |
| `STARTING` | Waiting for the first event | The session manager |
| `READY` | Accepting input | The session manager |
| `STREAMING` | A turn is running | Events |
| `AWAITING_PERMISSION` | Blocked on a decision | Events |
| `COMPACTING` | Context compaction | Events |
| `INTERRUPTED` | The process is gone, the conversation is preserved | The process watcher |
| `RESUMING` | Restarting with the stored id | The user or the session manager |
| `FAILED` | With an error | The session manager |

## Creating

| Step | Detail |
|---|---|
| 1 | A new conversation has no session id |
| 2 | The first turn starts a session, and the id arrives in the first system event |
| 3 | It is written to the database immediately |
| 4 | A failure before the id arrives leaves the conversation without one. The transcript is still there, and the next turn starts a new session with the transcript as context. |

**A conversation without a session is a valid state.** It is shown as such, and the next turn recovers. What does not happen is a conversation that appears to be resumable and is not.

## Resuming

| Situation | Behaviour |
|---|---|
| The app was killed | On next launch, the run is `INTERRUPTED` and a resume is **offered**, never performed. A silent restart can duplicate a commit. |
| The device rebooted | The same, with the reason `DEVICE_REBOOT` |
| The app was updated | The same, with the reason `APP_UPDATED` |
| The user stopped it | The same, deliberately. "Fortsetzen" and "Verwerfen", and Verwerfen ends the turn and keeps the branch. |
| The user came back later | A resume from the chat screen |
| An interrupted turn | `CLAUDE_CODE_RESUME_INTERRUPTED_TURN=1` is set, so the resume continues what was in progress. This is what somebody who pressed stop and came back an hour later expects. |

### Resuming versus starting fresh

Both are offered, because they are genuinely different and the user is the one who knows which they want.

| | Fortsetzen | Neue Unterhaltung |
|---|---|---|
| Context | The existing session | A new one |
| The transcript | Appended to | A new conversation, with the previous text as context |
| The working tree | Untouched | Untouched |
| The session id | The same | A new one |
| For | "Carry on where it was" | "Different approach, same starting point" |

A new conversation carries the previous task text as context, explicitly marked as such in the first message, so the engine is not silently given a transcript it did not produce.

## Process death

| What | Survives | Mechanism |
|---|---|---|
| The session id | Yes | The database, written on receipt |
| The transcript | Yes | Room, written per event |
| The run state | Yes | Room, written per event |
| The engine's own session file | Yes | On disk, written by the engine |
| The process | No | A new one, on resume |
| The PTY | No | A new one |
| The terminal scrollback | Yes | The file |
| The exit code | No | Recorded as `UNKNOWN`. **Never guessed.** |

**`UNKNOWN` is the honest value and it is shown as such.** A run whose process was killed has an exit code nobody observed, and writing `137` because SIGKILL is a plausible guess would be writing a fiction into a transparency log.

## Interruption

The graceful sequence, per `06-runtime/process-supervision.md`:

| Step | Action | Wait | Why |
|---|---|---|---|
| 1 | The UI says "Wird gestoppt" | Immediately | The user must see that the tap registered |
| 2 | SIGINT to the process group | 2 s | The clean way to end a turn. The session stays coherent. |
| 3 | SIGTERM | 2 s | For a tool that ignores SIGINT |
| 4 | SIGKILL | 0.5 s | Nothing survives this |
| 5 | A tree walk verifies | — | "Gestoppt" appears only when the walk finds nothing |
| 6 | The session's state is written | — | `IDLE` if the process exited cleanly, `INTERRUPTED` otherwise |
| 7 | Uncommitted work is committed to `wip/` | — | So nothing is lost |
| 8 | The queue of pending messages is preserved | — | So a resume delivers them |

**The distinction between `IDLE` and `INTERRUPTED` after a stop is not cosmetic.** SIGINT ends the turn cleanly: the engine's context is intact, and a resume continues the conversation. SIGKILL does not, and a resume may find a partially-written context. The app says which happened, so the user knows whether a resume will feel continuous.

## Multiple conversations

| Aspect | Behaviour |
|---|---|
| One active session per conversation | Yes |
| Several active sessions in one app | Yes. A foreground service, a wake lock for the one that is executing, and a tab bar of running sessions. |
| A turn in conversation A while B is idle | Fine. B's session file is on disk, and nothing is held open for it. |
| A turn in A while B is streaming | B's run continues. The app does not queue B behind A. Two projects, two runs, two services' worth of work, bounded by the concurrency limit. |
| The same project twice | Refused, with a link to the running one. Two runs in one working directory produce a state neither intended. |
| Memory | Each streaming session holds a replay buffer. The buffers are bounded, and a session that is idle for 10 minutes is released to disk. |

## Session files on disk

| Location | Profile |
|---|---|
| Native | `filesDir/cca/home/.claude/projects/…` |
| proot | The guest's `~/.claude/projects/…`, inside the image |
| SSH | The host's `~/.claude/projects/…` |
| Actions | Not applicable between jobs; the id is in a repository variable |

**The app never reads or writes a session file directly.** The engine owns that format. The app stores the id and the transcript, and it never parses a session file. This is what makes a format change the engine's problem rather than a corruption bug in the app.

## Erasure

| Action | Session files | The database's transcript |
|---|---|---|
| A conversation is archived | Kept | Kept |
| A project is archived | Kept | Kept |
| A project is erased | **Kept on disk.** The app does not delete files it did not create in this path. | Removed |
| The app is uninstalled | Removed, with everything else | Removed |
| Retention prunes a run | Kept. The engine's own files are not ours to prune. | The run's index rows, per the retention rule |

The third row is the deliberate limit. Erasing a project's local data removes the app's record of the work, and the engine's session files stay. They are small, they are the engine's, and deleting them would be the app deleting something it does not own. The Settings screen says so when it happens: "Die Sitzungsdateien von Claude Code bleiben auf dem Gerät. Sie werden mit der App entfernt."

## Aging and cleanup

| Concern | Behaviour |
|---|---|
| Session files growing | The storage screen shows their total size, attributed separately from projects |
| A very old conversation | Never pruned by default. Retention applies to the app's index, not to the engine's files. |
| A session whose id is invalid | `ENGINE_SESSION_NOT_FOUND`. The conversation becomes a new one, with the transcript carried forward as context, and the user is told. |
| A session from a different engine version | Not resumed, per `02-architecture/data-migrations.md`. The old file is left on disk. |
| The engine's own cleanup | Left to the engine, which manages its own files |

## Concurrency inside one session

| Situation | Behaviour |
|---|---|
| A prompt sent while streaming | Queued, delivered at the next turn boundary. The transcript shows it as queued, so the user knows it has not been lost. |
| Several queued | Delivered in order |
| A permission while messages are queued | The messages wait. The user deals with the sheet first. |
| An interrupt with messages queued | The queue is preserved, and a resume delivers them |
| A cancel with messages queued | The queue is dropped, and the transcript says so |

## Testing

| Test | Type |
|---|---|
| `CreateStoresIdImmediately` | E2E — the session id is in the database before the first turn completes |
| `NoSessionIsValidState` | E2E — a conversation whose session never arrived is readable, and the next turn recovers |
| `ResumeUsesSameId` | E2E — a resume passes the stored id and the engine continues the conversation |
| `ResumeInterruptedTurn` | E2E — with `CLAUDE_CODE_RESUME_INTERRUPTED_TURN=1`, a resume continues the interrupted turn |
| `ResumeOfferedNotPerformed` | E2E — after a process death, nothing restarts without a tap |
| `NoDuplicateCommitOnResume` | E2E — a resumed run does not produce a second commit or a second push |
| `FreshConversationCarriesContext` | E2E — a new conversation includes the previous text, explicitly marked as carried context |
| `DeathRecovery` | E2E — killed mid-turn, the transcript, the run state, and the session id all survive |
| `ExitCodeUnknown` | E2E — a killed process records `UNKNOWN` and never a guessed value |
| `SigintCoherent` | E2E — a SIGINT produces a coherent session that resumes continuously |
| `SigkillFlagged` | E2E — a SIGKILL is reported as less coherent, and the app says so |
| `StopUnderThreeSeconds` | E2E — verified in three seconds, and "Gestoppt" appears only after the tree walk |
| `WipCommit` | E2E — uncommitted work is committed to `wip/` on an interrupt, and nothing is deleted |
| `QueuePreserved` | E2E — a queued message survives an interrupt and is delivered on resume |
| `QueueOrdered` | E2E — three queued messages are delivered in order |
| `ParallelSessions` | E2E — two projects run concurrently, and the same project twice is refused |
| `ConcurrentLimit` | E2E — the third concurrent run is refused, naming the two that are running |
| `IdleSessionReleased` | Integration — a session idle 10 minutes releases its buffer and the transcript is unaffected |
| `AppNeverReadsSessionFiles` | Static — no code in the app opens a file under `.claude/projects/`
| `SessionNotFound` | E2E — an invalid id produces the documented recovery, and the old file stays on disk
| `EraseKeepsSessionFiles` | E2E — erasing a project removes the transcript and leaves the engine's files
| `RetentionDoesNotTouchFiles` | E2E — retention prunes index rows and leaves the engine's files alone
