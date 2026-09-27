# Crash reporting

Crash reports are opt-in, redacted, and self-hostable. The default is off, and a person who does not opt in sends nothing, ever.

## The default is off

`CRASH_REPORTING` is `local` in debug, `none` in benchmark, and `opt-in` in release. `opt-in` means: a user has to turn it on in Settings → Privacy before a single byte leaves the device. There is no "anonymous analytics, we promise", because a promise is not a control.

## Why opt-in at all

Crash reports are the single most useful diagnostic we have. A stack trace from a device we will never see tells us what a real user hit that a test matrix did not. The tension between that and privacy is real, and the resolution is a switch the user controls, with a clear explanation of what is sent.

## What is collected

Only these, and only when the user has opted in:

| Field | Why |
|---|---|
| App version, variant, build type | The most important single fact |
| Android version, API level, device model, manufacturer | Compatibility |
| ABI | Which runtime profile applies |
| The exception type, message, and stack trace | The report |
| Thread name, and all threads for a native crash | The report |
| The last 50 log records from the run log, redacted | Context, often decisive |
| The last 20 transparency-log entries (IDs and types only, no content) | What the app was doing |
| The app state: active screen, active run ID, runtime profile, autonomy level | Context |

## What is never collected

| Never | Why |
|---|---|
| API keys, tokens, cookies, `Authorization` headers | Not negotiable |
| Prompt or assistant text | It is the user's content, not our diagnostic |
| Diff contents, file contents | Same |
| Project names, project paths | A path is identifying. Only a salted hash is included, to correlate crashes with a project without naming it. |
| File names or directory listings | Same |
| Anything from the device's contacts, photos, location, or storage outside the app | Not our business |
| The IP address | The transport will, unless we configure otherwise; see below |

The last row deserves attention. Any report carries an IP address. We do not store it, we strip it at the edge where the report is received, and the retention policy is 30 days.

## Redaction on the way out

The `LogRedactor` from `11-operations/logging.md` is applied to crash reports as well, with one addition: message text is scanned for anything that looks like a key, a path, or an email address, and replaced. A crash message is the most likely place in the whole app for user data to leak, because exception messages are often built from the thing that failed.

```
java.io.FileNotFoundException: /storage/emulated/0/Dev/Acme-Secrets/.env
  → java.io.FileNotFoundException: <path redacted>/.env
```

The path is stripped but the fact that a file was missing is kept, because that is the diagnostic.

## Native crashes

A crash in the native helper or in the engine process is a different animal. The app does not crash with it — the process supervisor sees a signal death and records it. So:

- A signal death (`SIGSEGV`, `SIGABRT`, `SIGILL`) inside the engine process is reported as a **process death**, with the signal, the exit code, and the last output. It does not crash the app, so it is a run failure, not an app crash, and it appears in the run record whether or not reporting is on.
- A crash in *our own* native code does crash the app, and is reported through the normal path with native symbols uploaded from `signing-and-keystores.md`.
- Tombstones are read on next launch and attached to the report. A tombstone is not redacted by our code — it is a system artifact — so it is **not uploaded**; instead the app extracts the signal, the faulting library, and the faulting offset, and reports only that.

## Where reports go

| Backend | Used when | Notes |
|---|---|---|
| Local sink | Debug builds | Written to a file, never uploaded |
| Self-hosted (Sentry-compatible) | Production, if self-hosted | The default recommendation in the README |
| A hosted third party | Only if the user chooses it | Off unless configured |

A self-hosted Sentry is about 200 MB of RAM and runs on a home PC, a small VPS, or a free-tier container. `11-operations/privacy.md` explains the trade-off; the short version is that self-hosting means the crash data never leaves infrastructure the user controls.

The server URL is configurable. Pointing it at a local instance is a supported and documented path, not a hack.

## What the user sees

| Moment | Message | Action |
|---|---|---|
| Crash happens | Nothing. The app restarts. | — |
| Next launch | A single line: "Die App ist unerwartet beendet worden." with the time and the screen | Tap to view, tap to share |
| If reporting is off | "Es werden keine Fehlerberichte gesendet." | Link to Settings |
| If reporting is on | "Fehlerbericht wird gesendet." with a count of pending reports | Tap to opt out, and pending reports are discarded |
| The user opts out | Any queued reports are deleted immediately, and the next report is not sent | Confirmed in the dialog |

No crash dialog with a "send anyway" prompt. Nobody wants to decide that at the moment their app died.

## Symbols and obfuscation

| Build | Symbols uploaded | Report readable |
|---|---|---|
| `debug` | n/a | Yes, fully |
| `nightly` | Yes, to the backend | Yes — the nightly build is how the symbol pipeline gets tested |
| `release` / `dist` | Yes, at publication time | Yes, after de-obfuscation by the backend |

Uploading symbols *before* publishing the build is part of the release checklist (`12-delivery/release-checklist.md`). A crash report that arrives before the symbols does is deferred for 24 hours and then sent un-deobfuscated, with a note.

## Retention

| Data | Retained |
|---|---|
| Crash reports | 30 days |
| De-obfuscation symbols | As long as the version is downloadable, plus 1 year |
| Aggregated counts (crash-free rate) | Forever, with no identifiers |

## Crash-free rate

The only metric we track about users, and it is a rate, not a count. It is computed from opted-in reports only, and the number reported in `00-vision/success-metrics.md` states its sample size honestly rather than implying it describes everyone.

## Sentry is a dependency we do not ship

The reporting client is behind an interface (`CrashReporter`) and the default implementation is a local sink. Adding a hosted client is a one-class change in `:app`, guarded by the `dependency-analysis` check. A user who builds from source with reporting on and no backend configured gets a clear message in Settings, not a silent failure.
