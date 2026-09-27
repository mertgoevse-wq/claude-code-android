# The native runtime profile

The default profile. A patched official binary, a glibc shim, and a checksum. The hardest part of this project, and the part most likely to break.

## What it is

| Component | What | Source |
|---|---|---|
| glibc-runner | A shim that loads glibc-linked binaries on Android's Bionic libc | Termux's release artifacts |
| patchelf-glibc | Rewrites a binary's ELF interpreter field | Termux's release artifacts |
| The Claude Code binary | The official `linux-arm64` build | Anthropic's CDN |
| A wrapper | A shell script that checks for a new version daily and swaps binaries | Written for this project |

Total: roughly 300–500 MB depending on the release.

## Why it is necessary

Anthropic ships a glibc-linked Linux binary. Android uses Bionic. The binary requests `/lib64/ld-linux-aarch64.so.1` at load time, which does not exist, so it fails immediately:

```
ENOENT /lib64/ld-linux-aarch64.so.1
```

There is no official Android build. Upstream tracking: `anthropics/claude-code#50270`. The real fix would be a `bun android-arm64` target or a static musl build.

**Everything in this document is a workaround, and the app says so to the user.** The Settings screen names the profile "Native (Workaround)" with a one-line explanation, because a person whose entire project depends on this deserves to know that it is a workaround.

## Installation, step by step

Every step is idempotent, resumable, verifiable, and reported with progress.

### 1. Device check

| Check | Failure |
|---|---|
| `uname -m` is `aarch64` or `arm64` | 32-bit devices are refused before anything is downloaded. Some budget phones ship a 32-bit OS on 64-bit hardware and cannot work. |
| Android 8 or later | Older versions lack the required syscall behaviour |
| Free space ≥ 600 MB | Reported with the exact shortfall. The app never deletes anything to make room. |
| A supported ABI | `arm64-v8a` |

### 2. Install the shim

Downloads `glibc-runner` and `patchelf-glibc`, verifies their checksums against the Termux release's published values, and extracts them into the app's private prefix at `filesDir/cca/prefix/`.

```
filesDir/cca/prefix/
├── lib/
│   └── ld-linux-aarch64.so.1        the shim
├── glibc/
│   └── lib*.so*                     the glibc the shim provides
└── bin/
    ├── patchelf                     for step 5
    └── glibc-runner                 the wrapper that sets the environment
```

The prefix is inside app-private storage. Not `/sdcard`, not a shared location, not a system path. Uninstalling the app removes all of it.

### 3. Download the engine

| Property | Behaviour |
|---|---|
| Source | Anthropic's own CDN. Not a mirror, not a package repository, not another project's copy. |
| URL | Resolved from the release manifest, with a pin to a known-good version as a fallback |
| Progress | Resumable, with a cancel, and a notification when backgrounded |
| Storage | A staging file, so a partial download never occupies the final path |
| Size | Shown before starting, with a warning on a metered connection |

### 4. Verify the checksum

**This step is a hard gate. There is no path from here to step 5 that does not pass it.**

| Property | Behaviour |
|---|---|
| The list | Anthropic's published checksums for the release |
| The comparison | Constant-time, against the downloaded file's actual digest |
| A mismatch | Everything is deleted. Nothing is installed. The user is told, in plain language, that the download was damaged. |
| The list is unreachable | Retried a few times, then the install is refused. **A binary we cannot verify is a binary we will not run.** Not even "run it once to see". |

Both the binary and the checksum come from the same host, so this catches corruption and truncation. It is **not** code signing and does not prove the host has not been compromised. The security screen states that limitation in those words, because a user reading "verified" would otherwise assume more than is true.

### 5. Patch

`patchelf --set-interpreter <prefix>/lib/ld-linux-aarch64.so.1 claude`

| Property | Behaviour |
|---|---|
| The original | Kept as `claude.orig` for the rollback, until the probe succeeds |
| Idempotency | A file already patched with our interpreter is detected and skipped |
| Verification | After patching, the interpreter field is read back and compared. A patch that silently did nothing is caught here rather than at launch. |
| Failure | The original is restored, the state machine goes to `FAILED`, and the log records the patchelf output verbatim |

### 6. Probe

**The step that makes the whole thing safe.**

```
claude --version
```

run once, in isolation, with a 30-second timeout and a minimal environment.

| Outcome | Action |
|---|---|
| A version banner within 30 s | The binary is kept, the wrapper is installed, the state is `READY` |
| A crash, a non-zero exit, or a timeout | The patched binary is deleted, `claude.orig` is restored and re-patched for the previous version, the state is `ROLLING_BACK` |
| A hang | Killed, treated as a failure |

The probe output is kept verbatim in the log. When somebody reports "it does not work", this line answers it in one place.

### 7. Install the wrapper

```sh
#!/data/data/dev.claudecode.android/files/cca/prefix/bin/sh
# once-daily version check, then exec the real binary
```

| Property | Behaviour |
|---|---|
| Check interval | Once a day, tracked by a timestamp file |
| The check | A single small HTTPS request. Nothing is downloaded until the user accepts an update, or the update rules below apply. |
| Transparency | The check happens with the app in the foreground, or with the background service active. It never runs in a hidden long-lived process. |
| The swap | Download, verify, patch, probe, then swap. Exactly the steps above, in the wrapper rather than in the app. |
| Rollback | On a failed probe, restore the previous version and report. |
| `--update-now` | Forces a check. A wrapper feature, not a Claude Code flag, and documented as such. |
| Offline | The check fails silently and the binary runs. |

## Verification commands

The two checks that prove the profile works, and the exact expected output.

```
$ filesDir/cca/prefix/bin/claude --version
2.1.283 (Claude Code)

$ echo '{"type":"user","message":{"role":"user","content":"reply with OK"}}' \
    | filesDir/cca/prefix/bin/claude -p --output-format stream-json --verbose
{"type":"system","subtype":"init",...}
{"type":"stream_event",...,"text_delta","OK"}
{"type":"result","subtype":"success",...}
```

These are the fixtures the E2E test runs. A profile that produces a version banner but not a working streaming response is not a working profile, and the health check requires both.

## The DNS override, opt-in

Some Android networks cause Claude Code's own DNS lookups to hang. The community workaround is to point them at public resolvers. **It is opt-in, with a visible warning**, because it silently overrides a VPN, a split tunnel, or a Pi-hole.

| Aspect | Behaviour |
|---|---|
| Default | Off |
| The warning | "Wenn deine Verbindung nach der Einrichtung hängt, kannst du die DNS-Anfrage auf 8.8.8.8 und 8.8.4.4 umleiten. Das umgeht dabei VPN und Pi-hole." |
| Location | Security settings, in the advanced section |
| Effect | Documented in the same place: what it changes, what it bypasses, and how to turn it off |
| Logged | Every change, both directions |
| Never | Applied without the user turning it on |

## Repair and recovery

| Situation | What the app does |
|---|---|
| The binary is missing | Health check detects it, offers a repair that re-downloads without touching projects |
| The shim is wrong for the device's ABI | Detected by the probe, reported with the device's ABI and what is expected |
| Free space ran out mid-install | Reports the exact remaining need. Resumes after space is freed. |
| The install was interrupted | Resumes at the failed step, because every step is idempotent |
| Everything is broken | A "Runtime reparieren" action in Settings, which re-runs the whole bootstrap. Projects, conversations, and keys are untouched. |
| Two broken versions in a row | The recovery screen, per `05-features/self-update.md`, with a clean reinstall |

## Known limitations

Stated in the app, not only here.

| Limitation | Consequence |
|---|---|
| Not a full Linux | No package manager, an incomplete userland. A build needing Gradle or Node should use profile B or a runner. |
| `ptrace` is unavailable | A few tools that need it will not work. Not many, but some. |
| Some Node tooling is flaky | Aggressive process spawning can misbehave under the shim. |
| Binary patching is fragile | A release that changes its linking breaks it. Hence the probe, the rollback, and profile B as a fallback. |
| No AVX or similar acceleration | Not relevant; the binary is ARM. |
| DNS can hang on some networks | Hence the opt-in override. |
| Storage | 300–500 MB. Real, and stated before the first download. |

## The wrapper's version check, in detail

Because a self-updating binary that can break the app is a genuine risk.

| Property | Behaviour |
|---|---|
| When | Once a day, on the first launch after 24 h |
| Where | Only when the app is in the foreground or the background service is active. Never in a hidden long-lived process. |
| Size | A few hundred bytes |
| On a new version | Downloaded, checksum-verified, patched, and probed. Only then swapped. |
| On a failed probe | The previous version is restored and the user is told, with both version numbers. |
| Never | A swap without a probe. There is no code path that installs an unprobed binary. |
| Disabled | A setting exists, for somebody who wants to pin a version. |
| Logged | Every check, every download, every probe, every swap, every rollback |

## Testing

| Test | Type |
|---|---|
| `DeviceCheck32Bit` | Integration — a 32-bit device is refused before any download |
| `DeviceCheckStorage` | Integration — insufficient space is reported with the exact shortfall, and nothing is deleted |
| `InstallStepsIdempotent` | Integration — running the bootstrap twice performs no duplicate work |
| `InstallResumable` | E2E — interrupted at each of the 7 steps, resumed, and reaches `READY` |
| `ChecksumMismatchAborts` | E2E — a corrupted binary is deleted, nothing is installed, and the state is `FAILED` |
| `ChecksumUnreachableAborts` | E2E — an unreachable checksum list aborts, and no probe is attempted |
| `ChecksumConstantTime` | Unit — the comparison does not short-circuit |
| `PatchVerifiesInterpreter` | E2E — after patching, the interpreter field is read back and matches; a no-op patch is caught |
| `PatchFailureRestores` | E2E — a patchelf failure restores the original and records its output verbatim |
| `ProbeSuccess` | E2E — `claude --version` returns a banner within 30 s |
| `ProbeStreaming` | E2E — a real streaming response is produced, not just a version banner |
| `ProbeFailureRollsBack` | E2E — a binary that cannot start is discarded, the previous version is restored, and both version numbers are reported |
| `ProbeTimeout` | E2E — a hanging binary is killed at 30 s and treated as a failure |
| `WrapperDailyCheck` | E2E — one check per 24 h, resumable, and silent on failure |
| `WrapperSwapWithProbe` | E2E — a wrapper swap probes before swapping, and rolls back on failure |
| `WrapperNeverHidden` | Integration — no long-lived process performs a check outside the foreground or the service |
| `DnsOverrideOptIn` | E2E — off by default, on only after an explicit action, logged both ways |
| `RepairTouchesNothing` | E2E — a repair leaves projects, conversations, and keys byte-identical |
| `HealthCheck` | Integration — a missing shim, a missing binary, a wrong ABI, and no network each produce their specific diagnosis |
| `RealDeviceE2E` | E2E, nightly — the full bootstrap on a real device or emulator, from nothing to a streaming response |
