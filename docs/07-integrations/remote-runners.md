# Remote runners

A machine somewhere else that does the work, so a phone does not have to.

## The interface it implements

`ExecutionBackend`, per `06-runtime/execution-backends.md`. Everything about how a runner behaves as a *destination* is in `04-screens/14-remote-runner-setup.md`. This document is about the parts that are not the setup screen: the probe, the file transfer, the session, and the failure modes.

## Kinds

| Kind | Transport | Best for |
|---|---|---|
| `SSH` | SSH over TCP, or a VPN | A home PC, a NAS, a Raspberry Pi. Anything already switched on. |
| `ORACLE` | SSH, with a provisioning guide | A free always-on cloud VM |
| `GITHUB_ACTIONS` | The REST API and the job log | Short jobs: builds, test suites, a single verification run |

`Oracle` is an `SshBackend` with a different `prepare()` and a quota probe. Not a fourth implementation.

## The probe

Run on add, on demand, on a schedule, and before a dispatch. Never assumed.

```bash
# identity
uname -srm
# cpu and memory
nproc; free -m | awk 'NR==2{print $2}'
# disk
df -BG --output=avail "$PROJECT_ROOT" | tail -1 | tr -dc '0-9'
# toolchains
for t in git node npm python3 go cargo java gradle docker; do
  command -v "$t" >/dev/null && echo "$t: $(command -v $t)"
done
# the engine
claude --version 2>/dev/null || echo "claude: missing"
# quota, where applicable
```

| Probed | Used for |
|---|---|
| `cpuCount` | The offload decision, the "how long will this take" estimate |
| `availableMemoryMb` | The same, and refusing a task that cannot fit |
| `availableDiskMb` | Refusing a clone that will not fit, before starting it |
| `toolchains` | **The offload decision.** A project needing Gradle goes to a host with Gradle. |
| `hasClaudeCode`, `claudeCodeVersion` | Whether a setup step is needed |
| The quota | Refusing a dispatch into an exhausted allowance |
| `probedAt` | A capability from last week is not today's capability |

**A probe result is a cache with a timestamp, and a stale one is re-probed.** A host that had a toolchain last week and does not now would otherwise produce a run that fails twenty minutes in.

## The bootstrap

One idempotent script, run over SSH, that brings a fresh host up to a working state.

```bash
#!/bin/sh
set -eu
PREFIX="${CCA_PREFIX:-$HOME/.cca}"
mkdir -p "$PREFIX/bin"

# 1. node, if missing
command -v node >/dev/null || install_node "$PREFIX"

# 2. claude code, if missing or outdated
if ! command -v claude >/dev/null; then
  curl -fsSL https://claude.ai/install.sh | sh
fi
export PATH="$HOME/.local/bin:$PREFIX/bin:$PATH"

# 3. verify
claude --version

# 4. a bridge script so the app can start sessions
install -m 0755 "$PREFIX/ccabridge" "$PREFIX/bin/ccabridge"

# 5. record
printf '{"version":"1","installedAt":%s}\n' "$(date +%s)" > "$PREFIX/state.json"
```

| Rule | Detail |
|---|---|
| Shown before it runs | The full script, in the UI, before it is offered. Not a hidden pipe. |
| Requires confirmation | Always. It installs software on a machine the user owns. |
| Idempotent | Every step checks. Running it twice is a no-op. |
| No secrets | The key is already in place. The script never handles a credential. |
| No `sudo` | It installs into `$HOME`. A runner that needs root is not a runner we set up. |
| Unattended | Yes, after the first confirmation, because a re-provision after a host restart should not need a human |
| Logs | Everything, redacted, retained locally |

## File transfer

| Case | Approach |
|---|---|
| A cloned project | The clone happens on the runner, from its own git credentials or the app's token. Nothing crosses the network from the device. |
| A local folder | A `git bundle` transfer, per `05-features/import-export.md`. History travels; a zip of a directory does not. |
| A file the user attached | A base64 or binary transfer over the SSH channel, for small files. Refused above 5 MB, with the "use a project file" alternative. |
| Result files | **Not transferred back.** The work is committed and pushed, which is the deliverable. A diff can be fetched on demand. |
| A large result | Never. Pushing to a branch is the mechanism, and a megabyte-scale download onto a phone is not something to design for. |

**A local folder's history is preserved by the bundle.** A zip would deliver a working copy with no commits, and a task on a repository with no history produces commits with no context, which is a bad thing to hand somebody.

## The session

| Aspect | Behaviour |
|---|---|
| A persistent shell | One per project, reused. `SshBackend` opens a PTY and keeps it. |
| The engine's session file | Lives on the host. A resume works, and it resumes against the host's copy. |
| The app's run state | Lives on the device. A device death does not lose the host's work. |
| Reconnection | A dropped SSH connection retries with backoff, and the run is marked interrupted if it cannot be restored within 2 minutes. |
| The connection's lifetime | Held for the run's duration and for as long as a terminal session is open. An idle shell times out after 24 hours, with a reconnection on next use. |
| Multiple connections | One per project, bounded by `maxConcurrentRuns`. |

## The offload decision

Per `04-screens/14-remote-runner-setup.md`, the decision is made by a set of rules, and **the reason is always displayed**.

| Trigger | Priority | Reason shown |
|---|---|---|
| The project is pinned to a runner | Highest | "Auf {host} festgelegt" |
| The user chose it for this run | Highest | "Von dir gewählt" |
| The offload policy is `ALWAYS` | High | "Einstellung: Immer auslagern" |
| A required toolchain is missing on the device | High | "{tool} fehlt auf diesem Gerät" |
| The estimated duration exceeds the threshold | Medium | "Erwartet über {n} Minuten" |
| The battery is below 15 % and not charging | Medium | "Akku bei {n} %" |
| The device is thermally throttled | Medium | "Gerät ist heiss" |
| The offload policy is `WHEN_HEAVY` and none of the above | — | Not offloaded |

| Rule | Detail |
|---|---|
| The reason is never hidden | In the run header, in the log, and in the notification |
| A pinned runner that is offline refuses the run | It does not fall back to the phone. A silent fallback would surprise a user who chose the runner for the battery. |
| The estimate | From the project's historical verification times, not a guess. With no history, the threshold is not used. |
| A project with no history | Only an explicit choice or a missing toolchain triggers an offload. There is nothing to estimate from. |

## Security

| Concern | Control |
|---|---|
| The key | Generated on the device, ed25519, stored in the Keystore, never displayed after creation |
| The password | Never collected. A password typed on a phone is a password in a keyboard's history and in screenshots. |
| Host key verification | TOFU with a pinned key. The first connection's fingerprint is shown to the user to confirm; a change is refused, loudly, because a changed host key means a changed machine. |
| Transport | SSH. There is no unencrypted mode. |
| What crosses the network | A git bundle for a local folder, small attachments, and the conversation. Never a key, never an unrelated project. |
| What the runner stores | The working copy, the session file, the bridge. All under `$HOME/.cca` or the project path, and named as such. |
| Cleanup after a run | The working copy of a *transferred* project is removed and the removal is logged. A *cloned* project's working copy stays, because the user owns it. |
| The privacy confirm | Once per host, naming the host, per `04-screens/14-remote-runner-setup.md` |
| A runner that is compromised | The app cannot detect it. The privacy confirm and the naming of the host are the honest response, and the screen says so. |

## Quota handling

Free tiers are withdrawn, and quotas are exhausted. Both happen.

| Provider | Handling |
|---|---|
| Oracle | The account's free-tier usage is queried before a dispatch. Above 90 %, a warning. Exhausted, a refusal with the reset date. |
| GitHub Actions | The account's included minutes, if the endpoint is available. Above 80 %, a warning before dispatch. |
| A home PC | No quota. Disk and memory are the limits, both probed. |

| Rule | Detail |
|---|---|
| A quota check before a dispatch | Every time for a free tier, because a quota can be exhausted between checks |
| A quota refusal | With the reset date and the alternative. Never a dispatch into an exhausted allowance, because the job would fail halfway. |
| The terms' date | Shown on the runner card, per `01-research/free-remote-runner-options.md`. A free tier whose terms changed is a fact the user deserves. |
| A provider withdrawing a tier | The probe fails, the runner is marked unreachable, and the app proposes another. No feature depends on a provider. |

## GitHub Actions, specifically

The odd one out, and worth being explicit about.

| Property | Behaviour |
|---|---|
| What it is good for | Builds, test suites, one verification run, a bounded task |
| What it is not good for | An open-ended session, a long conversation, anything needing an interactive shell |
| The setup screen says so | In the comparison table, before anybody configures it: "Nicht geeignet für lange Läufe." |
| The workflow file | Shown in full, with a commit to a branch, before it is written. Never silently added to a repository. |
| Streaming | The job log, polled and parsed into the same `AgentEvent` shapes |
| The session across jobs | The session id in a repository variable, so a follow-up job resumes it |
| Between jobs | Nothing persists except the repository. A local project on the device is cloned into the job. |
| Cancellation | Best-effort. The app cannot reach the process tree, and the UI says: "Der Job kann noch einen Moment weiterlaufen." |
| The terminal tab | Hidden, because there is no PTY. With a reason, not a dead screen. |
| Concurrency | One job per repository |
| Never | Auto-selected. It is unsuitable for long runs, and choosing it automatically would manufacture that failure. |

## Failure modes

| Failure | Behaviour |
|---|---|
| The host is unreachable | The run is refused with a reason. A retry with backoff, then the run stays unstarted. |
| The host is off | The same, with "ausgeschaltet" and a notification when it returns. |
| The key is rejected | Re-run the key bootstrap, with the command shown. |
| The host key changed | **Refused, loudly.** "Der Schlüssel des Servers hat sich geändert. Das kann ein neuer Server sein, oder ein Angriff." With the old and new fingerprints, and a confirm that requires typing the host name. |
| The engine is missing | Offered, with the install script shown, after a confirm |
| The disk is full | Refused before the run, with the shortfall |
| A transfer fails mid-way | The partial file is removed. It is our incomplete output. |
| The connection drops mid-run | Retried within 2 minutes, then the run is interrupted with the work committed |
| A job fails on Actions | The job log, the failing step, and a link |
| A quota is exhausted | Refused, with the date and the alternative |
| A provider is withdrawn | The probe fails, the runner is marked unreachable, another is proposed |
| The runner is removed during a run | The run is not killed, per `04-screens/14-remote-runner-setup.md` |

## Testing

| Test | Type |
|---|---|
| `ProbeReal` | E2E, nightly — against a container, the probe returns real values and the app displays them |
| `ProbeParse` | Unit — every probe output format, including a missing tool and a full disk |
| `ProbeStale` | Unit — a probe older than 24 h triggers a re-probe |
| `BootstrapIdempotent` | E2E — the bootstrap run twice performs no duplicate work |
| `BootstrapShownFirst` | UI — the full script is visible before it is offered, and never a hidden pipe |
| `BootstrapNoSudo` | Static — the script contains no `sudo` and no root requirement |
| `BundlePreservesHistory` | E2E — a transferred local folder has its commits and branches on the runner |
| `AttachmentSizeLimit` | E2E — a 6 MB attachment is refused with the alternative offered |
| `SessionReuse` | E2E — two runs against the same project reuse the host's shell |
| `Reconnection` | E2E — a dropped connection reconnects within 2 minutes, or marks the run interrupted |
| `OffloadReasons` | Unit — every trigger, exhaustively, with the reason string it produces |
| `OffloadNoSilentFallback` | E2E — a pinned, offline runner refuses the run rather than using the phone |
| `OffloadReasonDisplayed` | UI — the reason appears in the run header, the log, and the notification |
| `QuotaCheckedBeforeDispatch` | E2E — an exhausted quota refuses before the job starts |
| `QuotaTermsDate` | UI — the runner card shows the date the terms were checked |
| `HostKeyPinned` | E2E — a changed host key is refused, with both fingerprints and a typed confirmation |
| `NoPasswordCollected` | Static — the SSH implementation has no password field |
| `TransferCleanup` | E2E — a failed transfer removes the partial file, and a transferred project's working copy is removed after the run while a cloned one stays |
| `ActionsWorkflowShown` | UI — the workflow file is shown in full before being written, on a branch |
| `ActionsNoPty` | UI — the terminal tab is hidden with a reason |
| `ActionsCancellationHonest` | UI — the message says the job may continue briefly, because it might |
| `ActionsNeverAutoSelected` | E2E — the offload decision never chooses Actions on its own |
| `RunnerNeverAutoSelected` | E2E — with no runner configured, every run uses the device |
