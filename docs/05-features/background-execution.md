# Background execution

A run must survive the screen turning off, the app being swiped away, and the device rebooting. On Android that is not a default; it is a fight, and this document is the plan for winning it.

## The problem, stated honestly

Android is built to stop background work. A process with no visible UI, no notification, and no foreground-service declaration gets killed when convenient, and "when convenient" is not something the app can predict. A twenty-minute build on a phone will be killed several times if nothing is done about it.

The tools available, in order of strength:

| Mechanism | Strength | Cost |
|---|---|---|
| Foreground service with an ongoing notification | The system will not kill it | A permanent, visible notification. Required to be honest about what the app is doing. |
| A wake lock | The CPU stays on | Battery. Only legitimate while a run is active. |
| A boot receiver | Restores state after a reboot | Android restricts background receivers; the user can be asked to exempt the app |
| WorkManager | Deferred, guaranteed, not immediate | Wrong tool: it is for work that should happen, not work that is happening now |
| The default behaviour | Nothing survives | Unacceptable here |

## The foreground service

One service, `AgentForegroundService`, running whenever at least one run is active.

| Aspect | Behaviour |
|---|---|
| Type | `dataSync`, on a modern API level; `specialUse` where the platform requires it, with the reason declared in the manifest |
| Notification | Ongoing, `low` importance, not dismissible while a run is active |
| Content | The project, the current step, the elapsed time, and the cost |
| Actions | Pause, Stop, Open |
| Multiple runs | The notification shows the most recent, and the action opens the list of the others |
| Channels | Two: one for active runs at `low`, one for completion and errors at `default` |
| Foreground | Entered immediately, before the process is considered idle |
| `onTaskRemoved` | The run continues. A boot receiver restores the session on the next launch. |
| `stopSelf` | Only when no run is active and no session is attached. |
| User stopping from the shade | The stop button in the notification, with a confirm if the run is mid-edit |

**The notification is not a cost of the design. It is the design.** Somebody whose phone is running a tool against their repository for two hours is entitled to see that, permanently, in the shade, with a stop button. A silent background service would be the dishonest choice.

## The wake lock

| Rule | Detail |
|---|---|
| Acquired | Only while a process is genuinely running and there is no active terminal session. |
| Released | On completion, on cancel, on pause, and in a `finally`. A leaked wake lock is a bug with a test. |
| Type | `PARTIAL_WAKE_LOCK`, so the screen can still be off |
| Timeout | A maximum of 2 hours, after which the lock is released and the run continues on a partial one. A phone locked awake for six hours is a phone at 4 %. |
| Never with the screen on | If the screen is on, the CPU is already awake. Acquiring a wake lock then is pure waste. |
| Battery warning | Below 15 %, and not charging, a snackbar and a notification: "Der Akku ist fast leer. Für lange Läufe ist ein Ladegerät nötig." with an offer to offload. |

## Surviving process death

Android will kill the process eventually, and the app must treat that as normal rather than exceptional.

| What survives | How |
|---|---|
| The run state | Written to the database on every event, not at the end |
| The session id | Stored, so a resume continues rather than restarts |
| The transcript | Every event persisted as it arrives |
| The log | Same |
| The cost | Same |
| The plan and the steps | Same |
| The working tree | On disk, untouched |
| The commit | Once made |

| What does not survive | How it is recovered |
|---|---|
| The process | A new one is spawned, and the session is resumed |
| The PTY | A new one is allocated; the old scrollback is on disk |
| The in-memory terminal selection | Lost. Acceptable. |
| A partially written file by the engine | The engine's business; a re-run is offered |
| The environment of a running shell | Lost, and the shell is marked as needing a restart |

### On next launch

1. Find every run in a non-terminal state.
2. Check whether its backend is alive. On a local backend, a process scan; on a runner, a probe.
3. If it is alive, offer a resume with the current state intact.
4. If it is not, mark the run `INTERRUPTED` with the reason: `APP_KILLED`, `DEVICE_REBOOT`, or `APP_UPDATED`.
5. Commit any uncommitted work to the `wip/` branch, so nothing is lost.
6. Tell the user, in plain language, what happened and what they can do.

**It never restarts a run without asking.** A silent restart risks doing the work twice, and "twice" on a code change means a duplicate commit, a duplicate push, and a pull request with twice the diff. Asking costs one tap.

## Surviving a reboot

| Step | Behaviour |
|---|---|
| 1 | A boot receiver runs on `BOOT_COMPLETED` |
| 2 | It checks whether any run was active at shutdown |
| 3 | If so, it restarts the foreground service, which shows the notification and marks the runs interrupted with `DEVICE_REBOOT` |
| 4 | It notifies: "Nach dem Neustart: {n} Läufe wurden unterbrochen." |
| 5 | It does not auto-resume |

Android increasingly restricts what a boot receiver may do, and the user may have to allow the app to run in the background. The app detects that restriction and shows a one-time prompt: "Damit Läufe einen Neustart überleben, erlaube der App den Hintergrundbetrieb." with a link to the system settings. If the user declines, the app works; it simply cannot survive a reboot, and the notification says so honestly.

## Screen off

The common case, and the one that must be invisible.

| Aspect | Behaviour |
|---|---|
| The screen turns off | Nothing changes. The service and the wake lock keep the process alive. |
| The app is swiped from recents | The service continues. Android may restart the process later. |
| The app is backgrounded for hours | The run continues, the notification updates every 30 s, and nothing is lost |
| A long idle gap | The wake-lock timeout applies. The run continues, slower, on a partial lock. |
| Thermal throttling | The system slows the CPU. The run continues and gets slower. The elapsed time shows it, and the app does not pretend otherwise. |
| The battery dies | The run stops. On next launch it is `INTERRUPTED` with `POWER_LOST`, the working tree is intact, and the resume is offered. |

## The notification, in detail

While a run is active:

```
⟐  claude-code-android
    liest build.gradle.kts · 4:12 · ~0,08 $
    [Pause]  [Stop]  [Öffnen]
```

| Element | Update rate | Notes |
|---|---|---|
| The state line | On change | The current tool or step, in plain language |
| The elapsed time | Every 30 s | Tabular figures, so it does not jitter |
| The cost | On change | With a `~` while estimated |
| Pause | Immediate | Suspends, keeping the process. The notification becomes "Angehalten" |
| Stop | Immediate | With a confirm if the run is mid-edit |
| Open | — | The chat |

On completion:

| Outcome | Title | Body | Actions |
|---|---|---|---|
| Verified | "{project} ist fertig" | "{n} Dateien · geprüft · {cost}" | Ansehen, Diff |
| Unverified | "{project} ist fertig, aber ungeprüft" | "{n} Dateien · nicht geprüft · {cost}" | Ansehen, Prüfbefehle einrichten |
| Failed | "{project} ist fehlgeschlagen" | The first line of the report, not a code | Ansehen, Bericht |
| Retrying | "{project} versucht es erneut" | "Versuch {n} von {m}" | Ansehen |
| Refused | "{project} wurde blockiert" | The rule, in plain language | Ansehen |
| Interrupted | "{project} wurde unterbrochen" | The reason and "Fortsetzen" | Fortsetzen, Ansehen |
| Offloaded | "{project} läuft auf {runner}" | The reason it was offloaded | Ansehen |

**The unverified completion notification says "ungeprüft" in its title.** Not in the body where it can be missed. Somebody glancing at a lock screen sees the difference between a checked result and an unchecked one, which is the entire point of having the distinction.

## Channels and permissions

| Channel | Importance | Used for | Requested |
|---|---|---|---|
| `runs` | `LOW` | The ongoing active-run notification | At first run |
| `results` | `DEFAULT` | Completion, failure, permission requests | At first run |
| `github` | `DEFAULT` | PR comments, CI results | When GitHub is connected |
| `security` | `HIGH` | A key was used, a biometric check failed | When the app lock is on |

`POST_NOTIFICATIONS` is requested during onboarding, with a real explanation of what each channel is for. If it is denied:

- The foreground-service notification still appears — the system requires it and it is exempt from the permission. This is the one notification that cannot be suppressed.
- Completion and error notifications do not appear. The chat screen's own status is unaffected.
- The app says what is degraded and links to the system settings. It does not nag on every launch; once at onboarding, and once more when a user turns it on in Settings.

## The terminal as a background process

A persistent terminal session follows the same rules, with one difference: it produces no output when idle, so there is no "active run" to attach to.

| Rule | Behaviour |
|---|---|
| An idle shell | No foreground service, no wake lock. It survives as long as the process does, and no longer. |
| A shell running a command | The same rules as a run: a foreground service and a wake lock |
| A long build typed by hand | Treated exactly like a run's build. It gets the same notification, the same survival, the same interruption handling. |
| Closing the terminal screen | The session continues, and the session list shows it as running |

Somebody typing `./gradlew assembleDebug` into the terminal and locking the phone is doing the same thing as a run, and gets the same treatment. Treating it as second-class would be a bug.

## What is measured

| Metric | Where | Target |
|---|---|---|
| Runs killed despite the service | The About screen, 30 days | Zero, on devices where the service is active |
| Runs surviving a reboot | The About screen | The count, and whether the exemption is granted |
| Wake-lock duration per run | The About screen | Under the run's wall-clock time |
| Notification action usage | Nowhere | Not measured. Whether somebody taps "Stop" from the shade is their business. |

## Testing

| Test | Type |
|---|---|
| `ServiceLifecycle` | Integration — the service starts with a run and stops when the last one ends, with no leaks |
| `ServiceNotificationContent` | UI — the active notification shows the project, the state, the elapsed time, and the cost |
| `NotificationUnverifiedTitle` | UI — asserts the unverified title contains "ungeprüft", not only the body |
| `WakeLockAcquireRelease` | Integration — acquired on start, released in a `finally`, and a test asserts no leak after every failure path |
| `WakeLockOnlyWhenNeeded` | Integration — not acquired when the screen is on |
| `WakeLockTimeout` | E2E — released at 2 hours, the run continues |
| `ProcessDeathRecovery` | E2E — kill the process mid-run, relaunch, the state and session are intact, and a resume is offered rather than performed |
| `ProcessDeathNoAutoRestart` | E2E — the run is not silently restarted, because a silent restart can duplicate a commit |
| `RebootRecovery` | E2E — a simulated reboot restores the state, marks the runs interrupted with `DEVICE_REBOOT`, commits to `wip/`, and notifies |
| `RebootExemptionPrompt` | Screenshot — the one-time background permission prompt, and the honest statement when it is declined |
| `ScreenOff` | E2E — with the screen off for 10 minutes, the run continues and every event is persisted |
| `SwipeFromRecents` | E2E — the run survives |
| `TerminalBackgroundCommand` | E2E — a long build typed into the terminal gets the same service and notification as a run |
| `NotificationDenied` | E2E — with the permission denied, the foreground notification still appears and the others do not, and the degradation is stated |
| `BatteryWarning` | Screenshot below 15 %, not charging, with the offload offer |
