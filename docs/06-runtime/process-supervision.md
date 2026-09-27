# Process supervision

Starting a process, keeping it alive, knowing what it is doing, and being certain it is dead when the user says stop.

## The model

| Aspect | Rule |
|---|---|
| Everything spawns through `ProcessSupervisor` | No code anywhere else calls `ProcessBuilder`, `Runtime.exec`, or a shell |
| Every process is registered | A process not registered cannot be killed, so it is a bug |
| A process tree, not a process | A run spawns Bash, which spawns Gradle, which spawns a compiler. Stopping means all of them. |
| Output goes to a ring buffer and a file | Never only to memory; never only to a file |
| Exit is recorded | With the code, the signal, and the duration |
| Logs never contain a key | `Redactor` runs on the write path |

## `ProcessSupervisor`

```kotlin
class ProcessSupervisor(scope: CoroutineScope, clock: TimeProvider) {
    fun launch(request: ProcessRequest): ProcessHandle
    fun terminate(handle: ProcessHandle, reason: TerminateReason)
    fun tree(handle: ProcessHandle): List<ProcessNode>
    fun state(handle: ProcessHandle): ProcessState
    val processes: StateFlow<List<ProcessNode>>
}
```

### `ProcessRequest`

| Field | Meaning |
|---|---|
| `command` | argv, typed. Never a string. |
| `workingDirectory` | Absolute, validated to be inside an allowed root |
| `environment` | Built by `ProcessEnvironmentBuilder`, not inherited wholesale |
| `stdin` | A pipe, a file, or closed |
| `redirectStdout` | To a pipe, a PTY, or a file |
| `redirectStderr` | Merged into stdout, or separate. Merged by default, because a tool that writes to stderr and one that writes to stdout are usually the same conversation. |
| `timeout` | Optional, with a specific error on expiry |
| `trackChildren` | Whether to walk the process tree |
| `onExit` | Called exactly once, with the code and the signal |

### Working-directory validation

Every working directory is checked against the allowed roots before a process starts.

| Allowed | Not allowed |
|---|---|
| The app's own `filesDir/cca/` | `/` |
| A registered project's path | Any registered project's parent |
| A path the user explicitly selected for a project | `/sdcard` in general |
| A runner's project path, over SSH | A path containing `..` after normalisation |

A validation failure is an `AppError` with a specific message, not a silent fallback. It is a test that no orchestration path can construct a `ProcessRequest` with an unvalidated directory.

## The process tree

| Platform | How the tree is found |
|---|---|
| Local, API 26+ | The child processes of a pid, read from `/proc` |
| Local, older | A process group, with the pgid tracked from the spawn |
| Over SSH | The remote `ps` tree, correlated by the session's process group |
| GitHub Actions | **Not available.** The job's processes belong to the runner. Cancellation is best-effort, and the UI says so. |

```kotlin
data class ProcessNode(
    val pid: Int,
    val ppid: Int,
    val command: String,
    val startedAt: Long,
    val children: List<ProcessNode>,
)
```

### Termination

The sequence, in order, and why:

| Step | Action | Wait | Why |
|---|---|---|---|
| 1 | SIGINT to the process group | 2 s | The clean way to end a turn. The session stays resumable. |
| 2 | SIGTERM to the group | 2 s | A tool that ignores SIGINT but handles SIGTERM. |
| 3 | SIGKILL to the group | 0.5 s | Nothing survives this. |
| 4 | SIGKILL to any remaining known pid | — | A process that escaped the group, found by a `/proc` walk |
| 5 | Verify with a tree walk | — | Assert nothing is left. A kill that was not verified is a kill that failed. |

**Step 5 is the one that matters for the user.** Tapping stop and watching a spinner is the failure this whole sequence exists to prevent. The UI says "Wird gestoppt" immediately, and "Gestoppt" only after the verification walk finds nothing. If the walk finds something after the kill, the app says so rather than claiming success.

### The process group

Everything spawns into its own process group, so a kill is a group kill rather than a pid kill.

| Problem | Solution |
|---|---|
| On Android, `setpgid` is not available through `ProcessBuilder` | A tiny native helper, `ccaspawn`, is the launcher. It is a hundred lines of C, built from source in this repository, and its only job is `fork`, `setpgid`, `exec`. |
| Why not just kill the pid | A tool that forks a grandchild and exits would leave the grandchild running. A group kill does not have that hole. |
| A detached process that escapes the group | Caught by step 4's `/proc` walk, and reported |

The native helper is the one piece of C in the project, and it exists for one reason that the standard library cannot solve. It is small, it is in the repository, and it is reviewed like anything else.

## Output handling

| Stream | Handling |
|---|---|
| stdout | A reader coroutine, line by line, into the ring buffer and the file |
| stderr | Merged into stdout by default, tagged with its origin in the log so a tool that writes to stderr is still distinguishable |
| A line without a trailing newline | Held in a partial buffer until a newline or stream close. A binary that prints without newlines must not produce one enormous "line". |
| A very long line | Truncated at 64 KB with a visible marker, and the full content in the file |
| Binary output | Detected by a null byte, written to the file, and shown as "Binärausgabe" rather than rendered as mojibake |
| A flood | The ring buffer is bounded, so a `yes` command cannot exhaust memory. The file keeps a bounded tail as well, and says so when it truncates. |

### The ring buffer

| Property | Value |
|---|---|
| Size | 1 MB of recent output, configurable down to 256 KB |
| Purpose | The live view, so scrolling back does not re-read the file |
| Behaviour on overflow | The oldest lines are dropped, and a marker says how many were |
| The file | Separate, on disk, with a size cap of 10 MB per session by default, configurable |
| Never | A key, in either. `Redactor` runs before anything enters the buffer. |

## The foreground service relationship

Per `05-features/background-execution.md`, a process with a live PTY and no output does not hold a wake lock indefinitely; a process actively producing output does.

| Condition | Wake lock |
|---|---|
| A run is executing | Held |
| A run is `AWAITING_PERMISSION` | Released. The user is the bottleneck, and the CPU is not needed. |
| A verification command is running | Held |
| A terminal session is idle | Released |
| A terminal session is running a command | Held |
| Between turns, waiting for the model | Released, for 2 minutes. Then re-acquired. |
| Downloading, unpacking, patching | Held |
| Waiting for a permission request | Released |

Releasing the lock while waiting for the user is the difference between a phone that lasts an hour of unattended work and one that lasts three.

## Lifecycle across process death

| What | Survives | Recovered how |
|---|---|---|
| The process | No | A new one, via a resume |
| The session id | Yes, in the database | The CLI resumes it |
| The run state | Yes, written on every event | Read from the database |
| The transcript | Yes | Read from the database |
| The log | Yes | On disk, plus the database rows |
| The scrollback | Yes, in the file | Read back into the ring buffer |
| The exit code | No | Unknowable after a death. Recorded as `UNKNOWN`, never guessed. |
| The partial output of a killed command | The file's content | Read from the file |

A run whose process died is `INTERRUPTED`, not `FAILED`. The distinction matters: an interrupted run has a session to resume, and a failed one needs a decision.

## Resource limits

| Limit | Value | Enforced by |
|---|---|---|
| Concurrent runs per project | 1 | A per-project mutex, and a refusal with a reason |
| Concurrent runs overall | 3 on the phone, `maxConcurrentRuns` elsewhere | A counter, and a refusal naming what is already running |
| Memory | The OS's, with the app's own ceiling observed | The OS. The app watches for a low-memory signal and marks runs for interruption. |
| Output rate | Backpressure: a reader that cannot keep up stops reading, and the pipe applies it to the writer | The natural behaviour of a bounded channel |
| CPU | Not throttled by the app | The OS's thermal policy. The app shows that a run got slower, and does not pretend otherwise. |
| Open files | Not managed by the app | The OS |

## What is measured

| Metric | Where | Why |
|---|---|---|
| Processes currently running | The About screen and a diagnostic | Somebody reporting a stuck run wants this |
| Stop latency | The About screen, 30 days | The promise is 3 seconds, and this is whether it is kept |
| Verified kills | The log | A kill that left something behind is a bug and is recorded as one |
| Longest run | The About screen | Useful context when something feels wrong |
| Native helper failures | The log | A `ccaspawn` failure is a bug in our own code |

## Testing

| Test | Type |
|---|---|
| `SupervisorIsOnlyLauncher` | Static — a check that no code outside `ProcessSupervisor` spawns a process |
| `WorkingDirectoryValidation` | Unit — every allowed case passes, every disallowed case is refused with the specific error |
| `TreeDiscovery` | Integration — a three-deep process tree is discovered completely |
| `TerminateSequence` | E2E — SIGINT, SIGTERM, SIGKILL, walk, and the verification pass |
| `StopUnderThreeSeconds` | E2E — a run with a sleeping grandchild is fully dead within 3 s, asserted |
| `StopReportsVerified` | UI — "Gestoppt" appears only after the walk finds nothing, and a survivor is reported as such |
| `ProcessGroupIsolation` | E2E — a process that forks and exits leaves a grandchild that is still killed |
| `PartialLineHandling` | Unit — output without newlines does not produce one enormous line |
| `LongLineTruncation` | Unit — a 10 MB single line is truncated with a visible marker |
| `BinaryOutput` | E2E — binary output is written to the file and reported as binary, not rendered |
| `FloodSafety` | E2E — a `yes` command for 10 s does not exhaust memory, and the truncation is marked |
| `RedactionEverywhere` | Unit — a key in stdout, in stderr, and in the environment appears nowhere in the buffer or the file |
| `InterruptionResumable` | E2E — SIGINT leaves the session resumable; SIGTERM does not, and the app offers the right option for each |
| `ProcessDeathRecovery` | E2E — killed mid-run, relaunched, the run is `INTERRUPTED` with a resumable session and `UNKNOWN` as the exit code, never a guess |
| `ConcurrencyLimits` | E2E — a second run for a busy project is refused with a reason; a fourth concurrent run overall is refused naming the three that are running |
| `WakeLockRelease` | Integration — released while awaiting a permission, and after 2 minutes of waiting for the model |
| `NativeHelperBuilds` | CI — the C helper compiles for the target ABIs, or the build fails |
