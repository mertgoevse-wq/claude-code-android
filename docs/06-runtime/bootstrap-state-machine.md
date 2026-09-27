# Bootstrap state machine

Getting Claude Code onto a phone, from nothing, without losing an hour of the user's time to an interrupted download.

## Why a state machine and not a function

Because this is the one operation in the app that is long, interruptible, likely to fail, and impossible to redo cheaply. A user who backgrounds the app during a 400 MB download must not restart it. A user whose storage filled up must not restart it. A user who kills the app must not restart it.

Every state is persisted on entry, so the machine can be reconstructed from the database alone.

## The states

```
IDLE
  └─▶ CHECKING_DEVICE
        ├─▶ UNSUPPORTED          (terminal, with a reason)
        ├─▶ NEEDS_STORAGE        (waits; resumes when space appears)
        └─▶ DOWNLOADING_RUNTIME
              └─▶ VERIFYING_RUNTIME
                    ├─▶ INSTALLING_RUNTIME
                    │     └─▶ CHECKING_NETWORK
                    │           ├─▶ DOWNLOADING_ENGINE
                    │           │     └─▶ VERIFYING_ENGINE
                    │           │           ├─▶ PATCHING_ENGINE
                    │           │           │     └─▶ PROBING_ENGINE
                    │           │           │           ├─▶ READY
                    │           │           │           └─▶ ROLLING_BACK
                    │           │           │                 ├─▶ READY
                    │           │           │                 └─▶ FAILED
                    │           │           └─▶ FAILED   (checksum mismatch / unreachable)
                    │           └─▶ FAILED
                    └─▶ FAILED
```

The state graph is in `02-architecture/state-machines.md`. This document is the behaviour of each state.

## Per-state behaviour

| State | Work | Progress reported | Failure |
|---|---|---|---|
| `IDLE` | Nothing | — | — |
| `CHECKING_DEVICE` | Arch, ABI, OS, free space | 4 quick checks, no progress needed | `UNSUPPORTED` or `NEEDS_STORAGE` |
| `DOWNLOADING_RUNTIME` | The shim, ~20 MB | Bytes and percent, resumable | Network, storage |
| `VERIFYING_RUNTIME` | Checksums of the shim | Indeterminate, a second | **Mismatch or unreachable: hard stop** |
| `INSTALLING_RUNTIME` | Extract into the prefix | Files and percent | Storage, a corrupt archive |
| `CHECKING_NETWORK` | Reachability of the CDN and the checksum host | Indeterminate, a few seconds | Network, with a specific diagnosis |
| `DOWNLOADING_ENGINE` | The binary, ~150 MB | Bytes and percent, resumable | Network, storage, cancelled |
| `VERIFYING_ENGINE` | Checksum against Anthropic's list | Indeterminate | **Mismatch or unreachable: hard stop** |
| `PATCHING_ENGINE` | `patchelf` plus a read-back check | Indeterminate, seconds | patchelf failed; the original is restored |
| `PROBING_ENGINE` | `claude --version`, then a streaming request | Two named steps | Crash, non-zero exit, or a 30 s timeout |
| `ROLLING_BACK` | Restore the previous binary and re-probe it | Indeterminate | Both versions broken: the recovery screen |
| `READY` | — | — | — |
| `FAILED` | — | The specific `AppError`, and what to do | Retryable from the failed step |
| `UNSUPPORTED` | — | The specific reason, and the two working alternatives | Terminal |
| `NEEDS_STORAGE` | — | Exactly how much is missing | Resumes on a storage-changed broadcast |

## The five properties

### 1. Idempotent

Every step checks whether its work is already done and skips it.

| Step | The check |
|---|---|
| `DOWNLOADING_RUNTIME` | The files exist and their digests match |
| `INSTALLING_RUNTIME` | The marker file for the installed version exists |
| `DOWNLOADING_ENGINE` | The staged file exists with the expected size and digest |
| `PATCHING_ENGINE` | The binary's interpreter field already points at our shim |
| `PROBING_ENGINE` | The last successful probe matches the current binary's digest |

A test runs the whole bootstrap twice and asserts that the second run performs no writes.

### 2. Resumable

The current state, the step's own progress, and the resume point are persisted on every transition and every progress tick.

| Interruption | On resume |
|---|---|
| The app is killed | Resumes at the current state, and within the step at the last completed chunk |
| The device reboots | The same, via the boot receiver |
| The network drops | The step fails with a network error; retrying resumes the partial download |
| Storage fills | The step fails; the app waits for space and resumes |
| The user backgrounds the app | Nothing. The download continues in the service, or pauses and resumes. |

A resumed HTTP download uses `Range`. If the server does not support it, the download restarts, and the UI says so rather than appearing to hang.

### 3. Fail-closed

**The verification steps are hard gates.** There is no path from `VERIFYING_ENGINE` to `PATCHING_ENGINE` that does not pass the checksum.

| Failure | Behaviour |
|---|---|
| The digest does not match | Delete everything staged. `FAILED`. Nothing is installed. The message names the expected and the actual digest, so a support conversation can start with real data. |
| The checksum list is unreachable | Retry three times with backoff, then `FAILED`. Not "continue anyway". |
| The list is present but has no entry for this version | `FAILED`. The app does not assume a missing entry means "unverified means fine". |

This is the rule that a future maintainer might be tempted to relax for convenience. It is the one place in the app where the failure mode of relaxing is a binary of unknown provenance running with the user's API key. A test asserts that `PATCHING_ENGINE` has exactly one inbound edge, and that it comes from `VERIFYING_ENGINE`.

### 4. Reported

Every state is visible in the UI, with a plain-language description of what is happening.

```
Laufzeit wird eingerichtet                    Schritt 6 von 8
──────────────────────────────────────────────────────────
✓ Prüfung des Geräts
✓ Laufzeitumgebung heruntergeladen — 21 MB
✓ Laufzeitumgebung geprüft
✓ Laufzeitumgebung installiert
⟐ Verbindung wird geprüft
· Claude Code wird heruntergeladen
· Prüfsumme wird geprüft
· Binärdatei wird angepasst
· Starttest
```

| Rule | Detail |
|---|---|
| Language | Plain. "Prüfsumme wird geprüft", not "Verifying checksum". |
| The current step | Named, not a percentage alone |
| The step count | "Schritt 6 von 8", so the user knows there is an end |
| Each completed step | Its duration |
| A long step | "Lädt seit 2:14" after 30 seconds, so silence is never ambiguous |
| A failure | The specific reason and a named action, never a retry button with no explanation |
| Leaving the screen | Allowed. The bootstrap continues in the service, and the notification reports the progress. |

### 5. Recoverable

| Failure | Recovery |
|---|---|
| The shim is missing or wrong | A repair step that re-downloads it, touching nothing else |
| The binary is missing | A repair step that re-downloads and re-patches it |
| The patch is wrong | Detected by the read-back check; re-patched |
| A new version does not start | Rollback to the previous |
| Everything is broken | The recovery screen with a clean reinstall, per `05-features/self-update.md` |
| A partial prefix is corrupt | The prefix is verified against a manifest; a mismatch triggers a targeted repair of the affected files, not a full re-download |

## Progress reporting

| Detail | Value |
|---|---|
| Granularity | A progress tick at most every 250 ms, and only on a real change |
| Through an unknown length | Bytes, and an indeterminate bar after 3 s without a content length |
| A slow step | After 30 s without progress, the elapsed time appears, and after 90 s the UI says the step might be slow and offers a cancel |
| Overall | Never a single percentage across all steps, because the steps have wildly different durations and a 60 % that means "the download" and "the patch" is worse than no number |
| In the background | The notification shows the current step's name, not a percentage |
| On resume | "Fortgesetzt · Schritt 4 von 8", so the user knows it did not start over |

## The manifest

The prefix is described by a manifest, so a repair knows what should be there.

```json
{
  "profile": "native",
  "version": 1,
  "installedAt": 1756280000000,
  "files": [
    { "path": "lib/ld-linux-aarch64.so.1", "size": 198234, "sha256": "…" },
    { "path": "glibc/libc.so.6", "size": 1874304, "sha256": "…" }
  ],
  "engine": {
    "version": "2.1.283",
    "path": "bin/claude",
    "sha256": "…",
    "interpreter": "…/lib/ld-linux-aarch64.so.1",
    "probedAt": 1756280120000
  }
}
```

| The manifest is used for | It is not used for |
|---|---|
| Verifying the prefix's integrity | Anything that requires a network call |
| Targeted repair of a corrupt file | Deciding what to delete. It never causes a deletion. |
| Knowing the engine version without launching it | — |
| The Settings screen's runtime information | — |

## Concurrent bootstrap

Two bootstraps at once would corrupt the prefix.

| Protection | Detail |
|---|---|
| A mutex | One bootstrap at a time, per app process |
| Across processes | The foreground service and the UI share one bootstrap, via a bound service. A second process cannot start a second bootstrap. |
| A file lock | A lock file in the prefix, so a crashed process does not leave a permanent lock. A lock older than 10 minutes is treated as stale. |
| A test | Two concurrent bootstrap requests produce one bootstrap, and both observe the same state |

## Profile switching

Switching profiles re-runs the bootstrap for the new profile. It is not a second machine.

| Property | Behaviour |
|---|---|
| Confirmation | States what is installed, what will be, and what is untouched |
| Projects | Never moved, never copied, never deleted |
| The native profile | Kept alongside, so switching back is fast |
| A failed switch | The previous profile is restored and is still the active one |
| Storage | Both profiles' footprints are shown in the storage screen |
| During a run | A switch is refused while any run is active |

## Testing

| Test | Type |
|---|---|
| `HappyPath` | E2E — the whole machine to `READY` |
| `Idempotent` | E2E — a second run performs no writes |
| `ResumableAtEachState` | E2E — killed at each of the 14 states, resumed, reaching `READY` |
| `ResumeReportsContinuation` | UI — the resumed run says it continued, with the step number |
| `FailClosedChecksumMismatch` | E2E — a mismatch deletes the staged file and never reaches `PATCHING_ENGINE` |
| `FailClosedChecksumUnreachable` | E2E — an unreachable list aborts after retries and never reaches `PATCHING_ENGINE` |
| `FailClosedMissingEntry` | E2E — a version absent from the list aborts |
| `GraphHasOnePathToPatch` | Unit — a graph assertion that `PATCHING_ENGINE` has exactly one inbound edge, from `VERIFYING_ENGINE` |
| `PatchReadBack` | E2E — a no-op patch is detected by the read-back check |
| `ProbeBothChecks` | E2E — a version banner and a real streaming response |
| `ProbeFailureRollsBack` | E2E — the previous version is restored, both versions reported |
| `BothVersionsBroken` | UI — the recovery screen, with a clean reinstall |
| `StorageWaitsAndResumes` | E2E — storage fills mid-download, the app waits, resumes when freed |
| `NeverDeletes` | E2E — an assertion across the whole bootstrap that no project file, conversation, or setting is ever removed |
| `ManifestIntegrity` | Integration — a corrupted prefix file is detected and repaired individually |
| `ConcurrentBootstrap` | Integration — two requests produce one bootstrap |
| `StaleLock` | Integration — a lock older than 10 minutes is treated as stale |
| `ProgressHonest` | UI — after 30 s without progress the elapsed time appears; there is never a single overall percentage |
| `BackgroundContinues` | E2E — backgrounded mid-download, the service continues and the notification reports the step |
| `ProfileSwitchSafe` | E2E — a switch preserves projects byte-identically, restores the previous on failure, and is refused during a run |
| `DiagnosticAccuracy` | E2E — each induced failure produces exactly the documented `AppError` and message |
