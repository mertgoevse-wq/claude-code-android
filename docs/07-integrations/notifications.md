# Notifications

Getting a message to a person who is not looking at the app, without becoming the app they swipe away.

## The channels

| Channel | Importance | Used for | DND behaviour |
|---|---|---|---|
| `runs` | `LOW` | The ongoing active-run notification | Not suppressed. The system requires it. |
| `results` | `DEFAULT` | Completion, failure, retries, permission requests | Suppressed by the user's DND |
| `github` | `DEFAULT` | PR comments, reviews, check results | Suppressed by the user's DND |
| `security` | `HIGH` | A key was used, a biometric check failed, an unusual file access | Suppressed by the user's DND |

Four channels, not one, so a user can silence GitHub and still hear about a run that needs them. Somebody who does not care about pull request comments should not have to choose between those and a permission prompt that is blocking their own task.

## What triggers what

### `runs`

| Event | Title | Body | Actions |
|---|---|---|---|
| A run starts | — | — | The ongoing notification appears, with Pause, Stop, Open |
| A run is preparing | Same notification | "Wird vorbereitet" | As above |
| A run is retrying | "{project} versucht es erneut" | "Versuch {n} von {m} · {failure type in plain words}" | Open |
| A run is finished and verified | "{project} ist fertig" | "{n} Dateien · geprüft · {cost}" | Ansehen, Diff |
| A run is finished and unverified | "{project} ist fertig, aber ungeprüft" | "{n} Dateien · **nicht geprüft** · {cost}" | Ansehen, Prüfbefehle |
| A run failed | "{project} ist fehlgeschlagen" | The first line of the report | Ansehen, Bericht |
| A run was blocked | "{project} wurde blockiert" | The rule, in plain words | Ansehen |
| A run was interrupted | "{project} wurde unterbrochen" | The reason and "Fortsetzen" | Fortsetzen, Ansehen |
| A run was offloaded | "{project} läuft auf {host}" | The reason it was offloaded | Ansehen |
| A run needs a permission | "{project} braucht eine Erlaubnis" | The action, named | Erlauben, Ablehnen, Ansehen |
| A permission was pending 30 min | The same, again | "Wartet seit 30 Minuten" | Erlauben, Ablehnen |
| The retry loop is looping at `FULL_AUTO` | "{project} hängt fest" | "Gleicher Fehler {n}-mal. {cost} bisher." | Ansehen, Stoppen |
| A verification passes | — | — | **Not notified.** It is inside the run notification's progress. |
| A verification fails, mid-retry | — | — | **Not notified.** The retry notification covers it. |

**A looping run notifies.** This is the one that matters for unattended work: at `FULL_AUTO` with a 50-attempt budget, a loop is the difference between a $2 run and a $40 one, and the person who needs to know is not watching the screen.

### `github`

Per `07-integrations/github-notifications.md`.

### `security`

| Event | Why it is a notification |
|---|---|
| The app lock was disabled | Somebody changed a security setting, and the next launch is now unprotected |
| A key was revealed | A reveal is a deliberate act, and it is worth confirming that the deliberate act was the user's |
| A biometric check failed 3 times | A pattern, not an accident |
| A diagnostics bundle was exported | The user should know their logs left the app |
| A self-repair task was created | A task that writes code against the app's own repository |
| A runner's host key changed | Refused elsewhere, but worth a notification if it happened |

## Wording

Every notification text is precomputed and stored, per ADR-010, so a background worker can build one without the UI.

| Rule | Example |
|---|---|
| Name the project | "claude-code-android ist fertig" |
| Say what happened | "2 Prüfungen fehlgeschlagen", not "Fehler" |
| Say what to do | The action button, or the second line |
| The cost, when there is one | "0,31 $" — somebody is paying this and should see it at a glance |
| The unverified state in the title | Not only in the body. See below. |
| Plain language | "Der Server ist überlastet", not "503 upstream" |
| No exclamation marks | None, anywhere |
| No emoji | None |
| No urgency | No "Achtung!" or "Jetzt handeln!" |
| The retry count | Always with both numbers: "Versuch 2 von 10" |
| Never blame the user | Not "Deine Verbindung ist schwach", but "Zeitüberschreitung bei {host}" |

### Why "ungeprüft" is in the title

```
✓ claude-code-android ist fertig
  3 Dateien · geprüft · 0,31 $

! claude-code-android ist fertig, aber ungeprüft
  3 Dateien · nicht geprüft · 0,31 $
```

A person glancing at a lock screen sees one line. If the difference between "checked" and "not checked" is in the body, the person who never opens the notification cannot tell the two apart — and that person is exactly the one who chose not to watch. This is ADR-011 rendered as 34 characters.

## Actions

| Action | Where | Behaviour |
|---|---|---|
| Erlauben | A permission notification | Answers the permission, and the run continues without the app being opened |
| Ablehnen | A permission notification | Same |
| Fortsetzen | An interrupted run | Resumes, with a confirmation if the project is busy |
| Stoppen | A looping or active run | Stops, with a confirmation if the run is mid-edit |
| Diff ansehen | A finished run | Opens the diff directly |
| Prüfbefehle | An unverified run | Opens the verification command editor directly |
| Ansehen | Everything else | The exact place, per the deep links in `07-integrations/github-notifications.md` |
| Terminal | An interrupted run | The terminal at the interruption point |

**Every notification has at least one action that does something.** A notification that only opens a screen is a worse version of opening the screen yourself.

## Grouping

Android groups notifications by channel automatically. Beyond that:

| Rule | Detail |
|---|---|
| One notification per run | Not per event. A run updating one notification avoids a wall of five notifications for one task. |
| The ongoing notification | Never grouped. It is a foreground-service notification. |
| Identical notifications within 5 min | Collapsed by Android, and the app does not post duplicates anyway |
| A burst of GitHub notifications | Up to 5, then a summary: "5 Benachrichtigungen zu {repo}" |
| The summary | Tappable to the list. A summary is never the only notification; the first one is still individual. |

## Timing

| Situation | Behaviour |
|---|---|
| A run finishes while the app is in the foreground | The chat screen updates. **No notification.** Notifying somebody about something they are looking at is noise. |
| A run finishes while backgrounded | Immediately |
| A run finishes after the screen was off | Immediately, on the lock screen if allowed |
| Several runs finish at once | One per run, at most 5, then a summary |
| A permission request while the app is foreground | The sheet. **No notification.** The user is right there. |
| A permission request while backgrounded | Immediately, and again after 30 minutes |
| An error while foregrounded | An inline card. **No notification.** |
| An error while backgrounded | Immediately |
| A maintenance notification | The day before, at a sensible local hour, not at 3 a.m. |

## Permission handling

| State | Behaviour |
|---|---|
| Not yet asked | Asked during onboarding, with a real explanation of what each channel is for |
| Granted | Everything works |
| Denied | The foreground-service notification **still appears** — the system requires it and it is exempt. Everything else does not. The app says what is degraded, links to the settings, and asks once. |
| Asked again | Never, unprompted. The Settings row has a link. |
| Channels individually disabled | Respected. The app does not try to work around a channel the user turned off. |

## Quiet hours

**Not implemented, deliberately.** Android's own notification settings are respected, and the app adds no schedule. A second source of truth for "when should I be quiet" produces a configuration where the user has set the wrong one. If somebody genuinely wants run notifications during a DND window — a long build they are waiting on — Android offers a per-category exception and the app's `results` channel is a normal notification category.

## Delivery guarantees

| Property | Behaviour |
|---|---|
| Delivery | Best effort. Android may delay or drop a notification under pressure, and the app cannot override that. |
| A dropped notification | The run's state is in the database, so the chat screen is correct regardless. A notification is a convenience over a correct state, never the source of truth. |
| Ordering | A run's notifications are posted in state order. A completion cannot arrive before its start on the same run. |
| A permission action after the run ended | The action is ignored with a message: "Dieser Lauf ist nicht mehr aktiv." The run's own state is checked before an action is applied. |
| A stale deep link | Opens the closest valid screen, with a message if the object is gone. |
| Idempotency | Posting the same notification id twice updates it rather than duplicating it. |

## Access and content

| Concern | Behaviour |
|---|---|
| On the lock screen | Configurable. The default shows the project name and what happened, not the content of a message. A private project name is itself information, so the setting is offered and the default is the middle one: the project name, no task text. |
| A secret in a notification | Never. Provider names and a model name are fine; a key never reaches a notification because a key never reaches the string in the first place. |
| A file path in a notification | A project name, not a path. A path on a lock screen is more information than it needs to be. |
| Notification content in the log | The exact posted text, so somebody reviewing notifications can see what was said |
| Truncation | Titles are short enough not to truncate. Bodies may truncate, and the title carries the essential fact. |

## Testing

| Test | Type |
|---|---|
| `EveryNotificationActionWorks` | E2E — every action on every notification is tapped and its effect asserted. A dead action fails this. |
| `NoNotificationWhenForeground` | E2E — a run finishing with the app in the foreground posts nothing |
| `NoPermissionSheetNotificationWhenForeground` | E2E — a permission request while foregrounded shows the sheet and no notification |
| `UnverifiedInTitle` | UI — asserts the unverified notification's title contains "ungeprüft" |
| `LoopNotifies` | E2E — a looping run at `FULL_AUTO` notifies, with both the count and the cost so far |
| `Channels` | UI — four channels with the documented importance, and each is separately configurable |
| `GitHubSilencedIndependently` | E2E — with `github` disabled, a run finishing still notifies |
| `PermissionReminder` | E2E — a permission pending 30 minutes notifies again |
| `ActionAfterEnd` | E2E — an action on a notification for a finished run is refused with a message |
| `Grouping` | E2E — 7 runs finishing produce 5 notifications and a summary, never 7 |
| `DeepLinks` | E2E — every notification opens the correct place |
| `LockScreenContent` | UI — the three content settings produce the documented text, and a key never appears |
| `DndRespected` | E2E — with the channel suppressed by DND, the app posts no workaround |
| `PermissionDeniedDegradation` | E2E — with the permission denied, the foreground notification still appears and the degradation is stated |
| `MaintenanceNotAtNight` | E2E — a maintenance notification is scheduled at a local daytime hour |
| `Wording` | Unit — no notification string contains an exclamation mark, an emoji, or a banned phrase |
| `NoUrgency` | Unit — no notification string contains urgency language |
| `PostedTextInLog` | Integration — every notification's exact text is in the log, correlated |
| `NotificationIdIdempotent` | Integration — posting the same id twice updates rather than duplicating |
| `DeliveredRegardlessOfNotification` | E2E — with notifications denied entirely, a run still completes correctly and the chat shows the true state |
