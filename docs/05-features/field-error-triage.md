# Field error triage

Turning "it crashed" into "here is a fix, on a branch, for you to look at" — without the app changing itself.

## The problem

An unattended tool on a phone develops failure modes its author never saw. The user hits one, the app fails in a way they cannot describe, and the natural outcome is that they abandon it.

The alternative is a mechanism that takes what the app already knows, turns it into a concrete task, and produces a reviewable change to the app's own repository. The user gets a pull request. A human decides. The app never modifies itself.

## What is collected, and what is not

| Collected | Not collected |
|---|---|
| The crash stack, redacted | Any file content |
| The app version, the build type, the device model, the OS version | The device identifier, the advertising id, any serial |
| The runtime profile and the Claude Code version | The user's key, ever |
| The last N log entries before the crash, redacted | The transcript of unrelated conversations |
| The state machine's state at the crash | Any project outside the crashing one |
| The run id, the project id, the backend | The project name. Only the id, which is meaningless outside the app. |

The collection is local. Nothing leaves the device until the user exports a bundle or chooses to report. See `11-operations/crash-reporting.md`.

## The error signature

A crash is reduced to a **signature**: a stable fingerprint that can be counted.

```
RuntimeException: Cannot invoke "java.io.File.delete()" because "f" is null
  at dev.claudecode.android.runtime.install.GlibcRuntimeInstaller.install(…:184)
  at dev.claudecode.android.runtime.state.BootstrapStateMachine$on…
```

| Part of the signature | Example |
|---|---|
| The exception type | `RuntimeException` |
| The first app frame | `GlibcRuntimeInstaller.install:184` |
| The machine state | `DOWNLOADING_RUNTIME` |

Everything else is excluded: line numbers inside framework code, lambda suffixes, hash codes, memory addresses, and the specific paths involved. A signature that changes on every run cannot be counted, and something that cannot be counted cannot be prioritised.

**The count is the whole value.** One crash report is an anecdote. Eleven identical signatures is a bug, and a bug with a count is worth fixing before the twelfth.

## The crash list

A screen, reachable from Settings and from a crash banner.

| Row | Content |
|---|---|
| Signature | A shortened, human-readable form of the fingerprint |
| What happened | The first frame's meaning in plain language: "Fehler beim Installieren der Laufzeitumgebung" |
| Count | How many times, on this device |
| First and last seen | Dates |
| Versions | The app versions it occurred in |
| A reproduction hint | Derived where possible: "Aufgetreten beim Einrichten der Laufzeit, Android 15, Profil Native" |
| Actions | Details, Exportieren, Als Aufgabe öffnen, Ignorieren |

### Ignored signatures

A user can ignore a signature they have decided is not worth reporting. It stops appearing in the banner and stays in the list, marked, with a restore action. Ignoring is a local, reversible act, and it is recorded in the log.

## The banner

A crash does not interrupt. A dismissible banner appears on the chat screen and in Settings.

```
⚠ Ein Fehler ist aufgetreten (3×) — Details
```

| Rule | Detail |
|---|---|
| Dismissible | Yes, and the dismissal lasts for the session |
| Never blocking | A crash is reported, not gated behind. A user must always be able to work. |
| Count shown | Because "it happened once" and "it happened eleven times" deserve different reactions |
| Only in the app | Never a modal, never a dialog, never on the launch screen |
| Tapping | The crash list, scrolled to that signature |

## Turning a crash into a task

This is the feature. Tapping "Als Aufgabe öffnen" on a signature.

### What is assembled

| Input | Content |
|---|---|
| The prompt | A generated task description, in the app's own repository, written for a model to act on |
| The signature | The full fingerprint |
| The occurrences | The count, the versions, the device and OS, the runtime profile, the machine state |
| The log excerpt | The 50 lines before the crash, redacted |
| The stack | The app frames, the framework frames elided |
| A reproduction hint | Derived where possible |
| Constraints | Stated explicitly, and they are not negotiable |

### The generated prompt

```
Die App stürzt beim Installieren der Laufzeitumgebung ab. 3 Fälle,
zuletzt in Version 1.0.0 (42) auf Android 15, Profil Native.

Zustand beim Absturz: DOWNLOADING_RUNTIME
Zeitstempel: 2026-09-27T14:12:03Z

Stack (App-Frames):
  dev.claudecode.android.runtime.install.GlibcRuntimeInstaller.install:184
  dev.claudecode.android.runtime.state.BootstrapStateMachine$onDownloadComplete:96
  dev.claudecode.android.runtime.BootstrapManager.start:41

Log (letzte 50 Zeilen, redigiert):
  14:12:01 [INFO ] Laufzeit wird entpackt: glibc-runner
  14:12:02 [WARN ] Checksum ok, Interpreter wird gepatcht
  14:12:03 [ERROR] patchelf exit 1
  …

Vermuteter Ablauf: Die Archivierung meldet Erfolg, bevor alle Dateien
geschrieben sind, danach schlägt das Patchen fehl.

Aufgabe:
1. Lies den Code um GlibcRuntimeInstaller.kt:184 und finde die Ursache.
2. Prüfe, ob der Fehler reproduzierbar ist, und wie.
3. Behebe die Ursache. Nicht das Symptom.
4. Ergänze einen Test, der mit dem beschriebenen Szenario bricht und
   mit deiner Behebung nicht mehr bricht.
5. Führe die Prüfbefehle aus. Sie müssen bestehen.
6. Erkläre die Ursache in 3 Sätzen im Commit.

Randbedingungen:
- Nur Dateien in diesem Repository ändern.
- Nichts löschen. Nichts hochladen ausser auf einen eigenen Zweig.
- Keine bestehenden Prüfungen abschwächen.
- Keine neue Abhängigkeit ohne Begründung.
```

**The prompt names the suspected flow, not the fix.** "Die Archivierung meldet Erfolg, bevor alle Dateien geschrieben sind" is a hypothesis derived from the log, stated as a hypothesis. Writing the fix into the prompt would produce a change that reproduces the author's guess rather than solving the problem.

### The constraints are load-bearing

| Constraint | Why it is in the prompt and not just in the project's settings |
|---|---|
| Only this repository | The app's own repo is a different project from the user's, and a self-repair task must not be able to reach a user's repository |
| Nothing deleted | The hard rule, restated where a model is about to edit code |
| Nothing uploaded except to a branch | The change must be reviewable, not published |
| No weakened checks | The rule from `05-features/retry-and-self-healing.md`, which is the most likely thing to go wrong when a model is fixing a failing test |
| No new dependency without justification | A crash fix that adds a library is usually a different bug |

### What happens

1. A new project is created from the app's own repository, cloned locally.
2. A branch is created: `fix/runtime-installer-null-file`.
3. The assembled prompt is the first message.
4. The plan appears, and the user can edit it before it is approved.
5. The run proceeds with the project's normal autonomy level, which for a self-repair defaults to `ASK_RISKY` regardless of the user's global default.
6. Verification runs, the diff is produced, a commit is made, and the run ends.
7. **Nothing is pushed. No pull request is opened.** The change sits on a local branch, and the screen offers to push and open a PR as a deliberate second step.

That last point is the whole design. The app produces a change; a person decides whether to publish it. An app that opened pull requests against its own repository as a matter of course would be an app that ships its own fixes without review, which is a worse outcome than not fixing them.

## What the app can and cannot do

| | Detail |
|---|---|
| Detect a crash | Locally, on the next launch or immediately for a non-fatal one |
| Count occurrences | By signature, on the device |
| Assemble a report | Locally, redacted |
| Export a report | The user chooses where it goes |
| Turn a report into a task | Locally, against the app's own repository |
| Produce a change | On a local branch, with verification |
| **Publish the change** | **No.** The user pushes and opens the PR. |
| **Modify the installed app** | **No.** There is no code path from any task to the app's own installed code. |
| Read a user's project during a self-repair task | **No.** The task's project is the app's repository, and the prompt says so. |

The last two are absolute, and they are tested. There is a static check that no orchestration path can reach the app's own installation directory, and a unit test that a self-repair task's project is always the app's repository and never a user's.

## Repository access

The app's repository is public, so cloning it needs nothing. Pushing a branch needs a token, and that is the one place the app asks a user for one outside the GitHub settings screen.

| Aspect | Behaviour |
|---|---|
| Which repository | A constant in the build configuration, so a fork self-repairs against its own repository. The About screen names it. |
| Auth | A dedicated key profile, created for this purpose, named "Fehlerberichte". It is scoped to that one repository, and it is stored in the Keystore like any other. |
| Permissions | Contents: read and write. Nothing else. The token cannot open an issue, cannot read any other repository, and cannot administer anything. |
| Not set up | The feature says so: "Ohne Token kannst du Fehler nur exportieren, nicht beheben." and offers the setup. |
| Shown clearly | The About screen lists the repository and the token's scope, in plain words, at all times. |

**A token that exists only to push a branch to one public repository is a small, specific, defensible thing.** The alternative — no self-repair at all — leaves a user with a bug report they have to write by hand, which is the outcome this feature exists to prevent.

## A note on frequency

Crash signatures are counted **on the device only**. There is no aggregate, no fleet view, and no download of anybody else's crash data. That means the app cannot tell how many users hit a given crash, and this document does not pretend otherwise.

What it can do is help the one user who is looking at it, right now, with real data from their own device. The trade is deliberate: no telemetry means no aggregate insight, and the project accepts that, per `11-operations/telemetry.md`.

## Testing

| Test | Type |
|---|---|
| `SignatureStability` | Unit — the same crash produces the same signature across 100 runs, and a differing line number or hash does not change it |
| `SignatureDistinguishes` | Unit — two different crashes produce different signatures |
| `CrashCounting` | E2E — 11 identical crashes count as 11, and the banner shows the count |
| `CrashBannerDismissible` | UI — dismissible, non-blocking, and the work is never gated |
| `CrashIgnore` | E2E — ignoring hides the banner, keeps the entry, is reversible, and is logged |
| `ReportRedaction` | E2E — a crash with a key in the log exports with it redacted, asserted across every sink |
| `ReportNoProjectName` | E2E — the report contains project ids, not names, and no unrelated conversation content |
| `TaskPromptAssembly` | Unit — the generated prompt contains the signature, the state, the log excerpt, the stack, and every constraint |
| `TaskPromptNamesHypothesis` | Unit — the prompt states a hypothesis and contains no prescribed fix |
| `TaskUsesOwnRepo` | E2E — a self-repair task's project is the app's repository |
| `TaskNeverTouchesUserProject` | Integration — a static check that the self-repair path cannot resolve a user's project id |
| `TaskDefaultAutonomy` | E2E — a self-repair task runs at `ASK_RISKY` even when the global default is `FULL_AUTO` |
| `TaskNoPushNoPr` | E2E — after a successful self-repair, no branch is pushed and no pull request exists; both are offered as a separate action |
| `TaskCannotModifyApp` | Unit — a static check that no code path writes to the app's own installation directory from a task |
| `TokenScope` | UI — the token setup names the repository and the permissions, and the About screen keeps showing them |
| `NoTokenGraceful` | E2E — without a token, the feature explains itself and offers export only |
| `NoFleetView` | UI — asserts no screen displays an aggregate crash count from any source but the local device |
