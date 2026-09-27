# Execution backends

The one place the app knows where it is running. Everything above this interface is the same on a phone, a home server, and a free cloud instance.

## The interface

```kotlin
interface ExecutionBackend {
    val id: BackendId
    val capabilities: BackendCapabilities

    suspend fun prepare(): PrepareResult
    suspend fun stream(request: AgentRequest): Flow<AgentEvent>
    suspend fun send(message: OutboundMessage)
    suspend fun state(): BackendState
    fun events(): Flow<AgentEvent>
    suspend fun close()
}
```

Eight members. The design pressure is to keep it that small: every method added here is a method every implementation must provide, including the ones that cannot possibly support it.

## The members

| Member | Contract |
|---|---|
| `prepare()` | Brings the backend to a state where a run can start. Idempotent. Returns what is missing if it cannot. |
| `stream(request)` | Starts a run and returns its event flow. Completes when the run's last event has been emitted. |
| `send(message)` | Delivers control input: a permission answer, an interrupt. Non-blocking. |
| `state()` | The current state, for the UI and for recovery after a process death. |
| `events()` | A second flow of the same events, for consumers that attach late — the log writer, the notifier. |
| `close()` | Releases the session. Never kills a run that is still wanted. |
| `capabilities` | What this backend can do, probed rather than assumed. |
| `id` | A stable identifier, used in the log and in the run record. |

## Capabilities

Not all backends are equal, and pretending they are produces bugs that only appear on a user's phone.

```kotlin
data class BackendCapabilities(
    val supportsPty: Boolean,
    val supportsPersistentShell: Boolean,
    val supportsStreaming: Boolean,
    val supportsInterruption: Boolean,
    val maxConcurrentRuns: Int,
    val availableDiskMb: Long,
    val availableMemoryMb: Long,
    val cpuCount: Int,
    val toolchains: Set<String>,          // "gradle", "node", "python", "go", …
    val hasClaudeCode: Boolean,
    val claudeCodeVersion: String?,
    val probedAt: Long,
)
```

| Capability | Why it exists |
|---|---|
| `supportsPty` | A GitHub Actions backend has no interactive shell. Feature code that assumes a PTY must check. |
| `supportsPersistentShell` | An Actions job has no shell between turns. The terminal tab is hidden for such a backend. |
| `supportsInterruption` | A job can be cancelled, but the process tree is not ours. Interruption is best-effort there, and the UI says so. |
| `maxConcurrentRuns` | A home PC can do four. The app refuses the fifth with a reason. |
| `toolchains` | Probed, never assumed. This is what drives the offload decision, per `04-screens/14-remote-runner-setup.md`. |
| `hasClaudeCode` | A runner without the engine is not a runner. It must be installed, with the user confirming. |
| `probedAt` | A capability from last week is not a fact about today. Stale probes are re-probed. |

## Implementations

### `AndroidLocalBackend`

The phone. The default.

| Aspect | Behaviour |
|---|---|
| Engine | The patched native binary, or the proot Ubuntu one |
| Launch | A process in the app's own prefix, with the environment assembled by `ProcessEnvironmentBuilder` |
| Streaming | stdout read line by line, mapped by `AgentEventMapper` |
| Interruption | SIGINT, a two-second wait, then SIGTERM to the process tree |
| PTY | A real PTY where the profile provides one; a pipe where it does not, with a note in the terminal tab |
| Persistence | Survives process death, because the state is in the database and the session file is on disk |
| Concurrency | One run per project directory, with a per-project mutex |
| The hard blocks | Enforced in `HardBlockPolicy` in the app's own layer, not in the engine, so `bypassPermissions` cannot skip them |

### `SshBackend`

A home PC or server over SSH.

| Aspect | Behaviour |
|---|---|
| Engine | Whatever the host has, or one the app installs with a confirm |
| Launch | An SSH command, with the process started in a PTY so the engine sees a terminal |
| Streaming | Over the SSH channel, line by line |
| Interruption | Close the channel, which sends SIGHUP to the remote process group |
| Persistence | The session file lives on the host, so a resume works |
| Concurrency | Whatever the host allows, bounded by its `maxConcurrentRuns` |
| Files | Not copied. The run works on the host's own files, which is the point. |
| The privacy confirm | Once per host, per `04-screens/14-remote-runner-setup.md` |

### `OracleBackend`

An always-free cloud VM. The same shape as `SshBackend` with a different bootstrap, because the provisioning story is different and everything after it is identical.

Rather than a fourth class with mostly duplicated code, `OracleBackend` is a `SshBackend` with a different `prepare()` and a probe that also checks the free quota.

### `ActionsBackend`

GitHub Actions. The odd one out.

| Aspect | Behaviour |
|---|---|
| Engine | Installed in the job's container by the workflow |
| Launch | A workflow dispatch, with the task encoded into the workflow inputs |
| Streaming | The workflow's log, polled over the API, parsed for the same event shapes |
| Interruption | A workflow cancellation. The process tree is not ours, so the app cannot kill a grandchild. The UI says "Der Job kann noch einen Moment weiterlaufen". |
| PTY | **No.** There is no interactive shell between turns. The terminal tab is hidden, and the app says why rather than showing a dead screen. |
| Persistence | Between jobs, nothing. The session id is stored in a repository variable, so a follow-up job resumes it. |
| Concurrency | One job at a time per repository |
| Quota | Checked before dispatch, and a warning above 80 % of the monthly allowance |
| Not suitable for | Long open-ended sessions. The setup screen says so, in the comparison table, before anybody configures it. |

## Selecting a backend

```
Project has an assigned runner and it probes ONLINE?
  → that runner
Project's offload policy is WHEN_HEAVY and the phone lacks a toolchain the project needs?
  → the runner
Project's offload policy is ALWAYS?
  → the runner
otherwise
  → AndroidLocalBackend
```

| Rule | Detail |
|---|---|
| An explicit assignment wins | A project pinned to a runner does not silently fall back to the phone. A fallback is a decision with consequences and is never silent. |
| An assigned runner that is offline | The run is refused with a reason, not moved. "The server is off, so it runs on your phone" would surprise a user who chose the server for the battery. |
| A missing toolchain on the phone | The only automatic trigger, and the reason is shown in the run header and the log |
| An explicit user choice | Always honoured, over every rule above |
| A GitHub Actions backend | Never auto-selected. It is unsuitable for long runs, and an automatic choice would produce that failure. |

## The event contract

Every backend emits the same `AgentEvent` set. This is the property that makes the abstraction worth its cost.

| Backend | The hard part | How it is solved |
|---|---|---|
| Local | A malformed line | Recorded, skipped, the run continues |
| SSH | A partial line across a chunk boundary | The line assembler holds a buffer; a line is only emitted at a newline or at stream close |
| SSH | A dropped connection mid-run | A reconnect with a bounded retry; the run is marked interrupted if it cannot be restored |
| Actions | Log output has timestamps and prefixes | Stripped before mapping, and a documented test fixture covers the format |
| Actions | A slow poll | A long poll interval with an immediate first fetch, so the first events are not delayed |
| All | A late-attaching consumer | The replay buffer, per `02-architecture/event-protocol.md` |

## A test backend

`FakeBackend` ships in the test sources, not behind a flag.

| Feature | Why |
|---|---|
| Replays a recorded fixture of real CLI output | The UI, the log, the cost meter, and the judge can all be tested without an engine |
| Scriptable event injection | Error paths, malformed lines, api_retry storms, mid-tool interruptions |
| A controllable clock | Retry and backoff tests that finish in milliseconds |
| A controllable capability set | Every offload decision, without a real server |
| Records every `AgentRequest` | So a test can assert exactly what was asked for |

The E2E journey tests run against `AndroidLocalBackend` with a real engine, because the whole point of the app is that the real thing works. The unit and component tests run against `FakeBackend`, because a test that needs a network and a 400 MB binary to check a rendering is a test that will be skipped.

## Adding a backend

| Step | Detail |
|---|---|
| 1 | Implement the eight members |
| 2 | Provide a `BackendCapabilities` that is probed, not declared |
| 3 | Map its output to `AgentEvent` in the mapper, and add a fixture for it |
| 4 | Add a setup screen, or a configuration route to an existing one |
| 5 | Add the backend id to the persisted `BackendId` enum, with a migration |
| 6 | Add contract tests that run the same suite of event fixtures through it as through the local one |
| 7 | Add it to the runner comparison table, honestly, including what it is bad at |

Step 6 is the one that gets skipped, and it is the one that matters. A backend that passes no contract test is a backend nobody can trust, because every UI behaviour downstream was only ever verified against the local one.

## Testing

| Test | Type |
|---|---|
| `ContractSuite` | Integration — the same fixture of real CLI output through every backend, asserting an identical `AgentEvent` sequence |
| `CapabilitiesProbed` | Integration — a backend that has not been probed reports unknown, not an optimistic default |
| `CapabilitiesStale` | Unit — a probe older than 24 h triggers a re-probe |
| `BackendSelection` | Unit — the selection rules, exhaustively, including every offline-assigned-runner case |
| `NoSilentFallback` | Unit — a pinned runner that is offline refuses the run rather than using the phone |
| `SshLineAssembly` | Unit — a chunk boundary mid-line produces one correct line |
| `SshReconnect` | E2E — a dropped connection reconnects within the retry bound, or marks the run interrupted |
| `ActionsPrefixStripping` | Unit — the Actions log format is mapped to the same events |
| `ActionsNoPty` | UI — the terminal tab is hidden for a backend without PTY, with a reason |
| `ActionsQuotaWarning` | UI — above 80 % of the allowance, before dispatch |
| `InterruptionTree` | E2E — a local interrupt kills the whole process tree, including a running Bash child |
| `HardBlocksRegardless` | E2E — a hard block is refused on every backend, at every autonomy level |
| `LateConsumer` | Integration — a consumer attaching mid-run receives the events from the replay buffer |
| `BackendCloseDoesNotKill` | E2E — closing a backend with a live session does not kill the run |
| `RemoteWorkOnHostFiles` | E2E — an SSH run edits the host's files directly, with nothing copied to the device |
