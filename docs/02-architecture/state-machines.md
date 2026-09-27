# State machines

Four machines, fully specified. A state machine with an undefined transition is a crash waiting for a user to find it, so every transition here is either listed or explicitly impossible.

---

## 1. Bootstrap

The one-time setup of the runtime. Resumable at every step, because a phone will be interrupted.

| State | Meaning | Next |
|---|---|---|
| `IDLE` | Nothing attempted | `CHECKING_DEVICE` |
| `CHECKING_DEVICE` | Architecture, ABI, free space, OS version | `UNSUPPORTED` · `NEEDS_STORAGE` · `DOWNLOADING_RUNTIME` |
| `UNSUPPORTED` | 32-bit device or OS too old | terminal, with the reason |
| `NEEDS_STORAGE` | Not enough space | waits for space, then `DOWNLOADING_RUNTIME` |
| `DOWNLOADING_RUNTIME` | glibc-runner and patchelf | `VERIFYING_RUNTIME` · `FAILED` |
| `VERIFYING_RUNTIME` | Checksums for the runtime artifacts | `INSTALLING_RUNTIME` · `FAILED` |
| `INSTALLING_RUNTIME` | Writing into the app's prefix | `CHECKING_NETWORK` · `FAILED` |
| `CHECKING_NETWORK` | Can we reach the CDN and the checksum host | `DOWNLOADING_ENGINE` · `FAILED` |
| `DOWNLOADING_ENGINE` | The claude binary | `VERIFYING_ENGINE` · `FAILED` |
| `VERIFYING_ENGINE` | Checksum against Anthropic's published list | `PATCHING_ENGINE` · `FAILED` |
| `PATCHING_ENGINE` | Rewriting the ELF interpreter | `PROBING_ENGINE` · `FAILED` |
| `PROBING_ENGINE` | Launch once, expect a version banner | `READY` · `ROLLING_BACK` · `FAILED` |
| `ROLLING_BACK` | Restoring the last known-good version | `READY` · `FAILED` |
| `READY` | Runtime usable | terminal for this pass; re-enterable for an update |
| `FAILED` | With a specific `AppError` | retryable to the failed step, or `IDLE` |

**Properties:**

- **Idempotent.** Every step checks whether its work is already done and skips. Re-entering the app never restarts a completed step.
- **Resumable.** The current state is persisted on every transition, so a killed process resumes at the step it was on, not at step one.
- **Fail-closed on verification.** `VERIFYING_ENGINE` failing goes to `FAILED`, never to `PATCHING_ENGINE`. An unverified binary is never installed. This is not configurable.
- **No silent fallback.** A failed native profile offers the proot profile explicitly. It never switches on its own and pretends nothing happened.

**Onboarding is a client of this machine**, not a second implementation. The setup screen renders whatever state this machine is in.

---

## 2. Session

One conversation with the engine, including interruption and resume.

| State | Meaning | Next |
|---|---|---|
| `IDLE` | No session | `CREATING` |
| `CREATING` | Spawning the process | `STARTING` · `FAILED` |
| `STARTING` | Waiting for the first event | `READY` · `FAILED` |
| `READY` | Accepting input | `STREAMING` · `IDLE` |
| `STREAMING` | A turn is in progress | `READY` · `AWAITING_PERMISSION` · `INTERRUPTED` · `FAILED` |
| `AWAITING_PERMISSION` | Blocked on a decision | `STREAMING` · `IDLE` |
| `INTERRUPTED` | Process gone, conversation preserved | `RESUMING` · `IDLE` |
| `RESUMING` | Restart with the stored session id | `READY` · `FAILED` |
| `COMPACTING` | Context compaction in progress | `STREAMING` · `FAILED` |
| `FAILED` | With an `AppError` | `RESUMING` · `IDLE` |
| `IDLE` (terminal) | | |

**Properties:**

- **`INTERRUPTED` is not `FAILED`.** The distinction is the whole point: an interrupted run can resume and its work is not lost, a failed one needs a decision.
- **Interruption is graceful.** `INTERRUPTING` is an internal step: SIGINT, two-second wait, then SIGTERM. The turn is ended rather than killed, so the session stays resumable. `CLAUDE_CODE_RESUME_INTERRUPTED_TURN=1` is set for the resume.
- **At most one turn at a time.** Sending a prompt while streaming is queued, not interleaved.
- **A permission does not time out into an answer.** An unanswered permission holds the session in `AWAITING_PERMISSION` indefinitely, with a notification, until the user answers. Guessing "no" would silently change behaviour; guessing "yes" would be worse.

---

## 3. Run

A task from submission to result. This is where verification, judging, and the retry budget live.

| State | Meaning | Next |
|---|---|---|
| `CREATED` | Task accepted, nothing started | `PREPARING` · `CANCELLED` |
| `PREPARING` | Backend chosen, worktree or branch set up | `PLANNING` · `OFFLOADING` · `FAILED` |
| `OFFLOADING` | Moving to a remote runner | `PLANNING` · `FAILED` |
| `PLANNING` | Waiting for the plan | `AWAITING_PLAN_APPROVAL` · `RUNNING` · `FAILED` |
| `AWAITING_PLAN_APPROVAL` | User edits or approves | `RUNNING` · `CANCELLED` |
| `RUNNING` | Engine is working | `AWAITING_PERMISSION` · `VERIFYING` · `RETRYING` · `INTERRUPTED` · `FAILED` |
| `AWAITING_PERMISSION` | A tool needs a decision | `RUNNING` · `CANCELLED` |
| `VERIFYING` | Running the project's checks | `JUDGING` · `FAILED` |
| `JUDGING` | Deciding pass or fail | `COMMITTING` · `RETRYING` |
| `RETRYING` | Diagnosing and fixing | `RUNNING` · `RETRYING` · `FAILED` |
| `COMMITTING` | Staging and committing | `PUSHING` · `DONE` · `FAILED` |
| `PUSHING` | Pushing the branch, opening the PR | `DONE` · `FAILED` |
| `INTERRUPTED` | Process gone, work preserved | `RUNNING` (resume) · `CANCELLED` |
| `DONE` | Verified, or explicitly unverified | terminal |
| `FAILED` | With a report | terminal, resumable |
| `CANCELLED` | User stopped it deliberately | terminal |

### The invariant

> `DONE` is reachable only from `COMMITTING` or `PUSHING`, and only when the last `VerificationRun` is `PASSED` or `UNVERIFIED`.

There is no path from `RUNNING` to `DONE`. A run that the engine finished but whose verification failed goes to `RETRYING` or `FAILED`. A run with no verification commands goes to `COMMITTING` and is labelled **unverified**, which is a distinct visual state, not a green tick.

This is asserted by `noDoneWithoutJudgeTest`, which walks every transition table entry and proves no path reaches `DONE` without a passing verification.

### The retry budget

```
JUDGING → FAILED verification
  → attempt < budget?  → RETRYING → RUNNING → … → VERIFYING
  → attempt = budget?   → FAILED, with a report of what was tried
```

The budget is per project, configurable to 3, 10, 50, or unlimited. The counter is in the run state and visible in the UI throughout. `RETRYING` carries a reason (`CompileError`, `TestFailure`, `LintError`, `InstallError`, `Unknown`) so the UI can show what kind of fix is happening.

**Anti-loop:** inside `RETRYING`, if the same step fails the same way three times with no intervening file change, the run goes to `FAILED` regardless of budget, with the message "gleicher Fehler wiederholt". See `05-features/retry-and-self-healing.md`.

---

## 4. Skill install

| State | Meaning | Next |
|---|---|---|
| `IDLE` | | `FETCHING` |
| `FETCHING` | Getting the source | `PARSING` · `FAILED` |
| `PARSING` | Reading the manifest and content | `VALIDATING` · `FAILED` |
| `VALIDATING` | Name, frontmatter, description, structure | `AWAITING_CONFIRMATION` · `INVALID` |
| `INVALID` | Every problem listed at once | back to `IDLE` after the user corrects it |
| `AWAITING_CONFIRMATION` | Showing exactly what will be written | `INSTALLING` · `CANCELLED` |
| `INSTALLING` | Writing to the project or global scope | `INSTALLED` · `FAILED` |
| `INSTALLED` | Registered, available to runs | `UPDATING` (later) |
| `UPDATING` | Fetching a newer version, diffing | `AWAITING_CONFIRMATION` · `INSTALLED` |
| `UNINSTALLING` | Removing the files, keeping the log entry | `IDLE` |
| `FAILED` | With an `AppError` | `IDLE` |

**Properties:**

- **No write without `AWAITING_CONFIRMATION`.** The installer returns a plan describing every file and its path. The user sees it. Only a confirmed call writes.
- **Never overwrites silently.** An existing file with different content produces a conflict in the plan, and the user chooses. The choice is recorded in the transparency log.
- **A broken install is visible.** A skill that fails validation is stored and marked `isValid = false` with the reason. It is not hidden, and not silently skipped.
- **Uninstall removes skill files, not history.** The `SkillInstall` row is retained with a removal timestamp, and the log keeps the entry. Consistent with the never-delete rule at the level of the app's own registry.

---

## Transitions common to all four

| Rule | Reason |
|---|---|
| Every failure path carries a code | An error without a code cannot be tracked or fixed |
| Every non-terminal state has a way out | No state is a dead end except the four explicit terminals |
| Every transition is persisted before it is acted on | A crash between the write and the action leaves a recoverable state |
| No machine depends on another's internal states | They communicate through events, never by reading each other |
| Timeouts are explicit | A state that waits has a defined timeout and a defined behaviour when it fires |
