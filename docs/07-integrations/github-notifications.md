# GitHub notifications

Knowing that something happened on GitHub while the phone was in a pocket, without running a server.

## Why polling

| | Webhook | Polling |
|---|---|---|
| Latency | Seconds | 30 seconds to 30 minutes |
| Needs a public endpoint | Yes | No |
| Costs a server | Yes | No |
| Works with the app's promise of no server | No | Yes |
| Works while the device is asleep | No | Partly |

A webhook is the better mechanism and it needs an inbound endpoint, which means a machine that is always on and is not the user's phone. This app's central promise is that it needs no server of anyone's. **The latency is worth the promise.**

The trade, stated honestly: a comment on a pull request is noticed in about a minute while the app is in use, and up to half an hour while the phone is idle. For somebody who is not waiting on the notification, that is fine. For somebody who is, the app's own run notifications are seconds, because those are local.

## What is polled

| Source | Endpoint | Interval |
|---|---|---|
| Pull request state | `GET /pulls?state=open` for the tracked repositories | 60 s foreground, 5 min with an active run, 30 min idle |
| New comments | `GET /issues/{n}/comments?since={last}` | With the pull request poll |
| Reviews | `GET /pulls/{n}/reviews` | With the pull request poll |
| Checks | `GET /commits/{ref}/check-runs` | Only for pull requests with runs the app created, and only while a run is unresolved |
| Mention notifications | `GET /notifications` | 5 min, and only if the user has not disabled it |

**Only what the app created is tracked deeply.** A pull request the app opened is its business; a repository the user happens to have open elsewhere is not.

## Deduplication

Polling means the same state is returned many times. Deduplication is a local high-water mark, not a server subscription.

| Per pull request | Stored |
|---|---|
| Last seen comment id | So only ids above it are new |
| Last seen review id | |
| Last seen check run id | |
| Last seen state | So a state that has not changed produces nothing |
| Last checked at | For the UI's "zuletzt geprüft" |

| Rule | Detail |
|---|---|
| Ids, not timestamps | Two comments in the same second must both be seen. Ids are monotonic; timestamps are not. |
| State changes | Only a transition produces a notification. A pull request that stays open is not news. |
| A state that flaps | A check that goes pending → running → pending → success produces one notification for the success, not four. A run is considered resolved when it reaches a terminal state. |
| A comment by the user | Not notified. Somebody should not be told about their own comment. |
| A comment by a bot | Notified, and marked as a bot. CI chatter is filtered separately. |
| A re-poll after being offline | The high-water mark is updated, and only genuinely new items are announced. A phone that was off for a day does not announce forty items. |

## What produces a notification

| Event | Channel | Title | Priority |
|---|---|---|---|
| A pull request the app opened was merged | `github` | "Zweig zusammengeführt · {repo}" | Default |
| A review was requested | `github` | "Review angefordert · {repo}" | Default |
| A review was submitted | `github` | "{reviewer} hat geprüft · {repo}" | Default |
| A new comment | `github` | "{author} kommentiert · {repo}" | Default |
| A mention of the user | `github` | "Erwähnung in {repo}" | High |
| A check failed on a pull request the app created | `github` | "Prüfung fehlgeschlagen · {repo}" | High |
| A check succeeded on a pull request the app created | `github` | "Prüfung bestanden · {repo}" | Low |
| The branch was deleted remotely | `github` | "Zweig entfernt · {repo}" | Default |
| The repository is no longer accessible | `github` | "Zugriff auf {repo} verloren" | High |
| The token expired | `security` | "GitHub-Verbindung abgelaufen" | High |

**A failed check is high priority and a successful one is low.** Somebody whose run failed cares immediately. Somebody whose run passed will see it when they open the app, and a notification for it is noise.

## What does not

| Not notified | Reason |
|---|---|
| Stars, forks, watchers | Irrelevant to the work |
| Issue events | The app does not create or manage issues |
| Releases | Not in scope |
| Repository renames | Shown in the app when it is next opened, not as a notification |
| Every CI job on every push | Only on pull requests the app created, and only the aggregate state |
| A pull request the user opened themselves | Not the app's run. It still shows in the app. |

## Intervals, and the battery

| State | Interval |
|---|---|
| App in the foreground | 60 s |
| App backgrounded, an active run | 5 min |
| App backgrounded, no run | 30 min |
| Screen off, no run | 30 min |
| Below the battery threshold | **No polling** |
| Below the battery threshold *and* a run is active | 5 min, and a warning that GitHub checks are reduced |
| Doze mode | Whatever the platform allows. The work manager is used so the poll survives it. |

The battery threshold is a setting, default 20 %, and it applies only to the GitHub channel. Local run notifications are unaffected: a run that finishes notifies whatever the battery is doing, because that notification is the whole reason somebody left the phone alone.

## Rate limit cost

| Poll | Requests |
|---|---|
| Foreground, 1 repository, 1 open pull request | ~60/h |
| Foreground, 5 repositories | ~180/h |
| Idle, 5 repositories | ~10/h |
| The authenticated limit | 5.000/h |

Comfortably inside. With 20 repositories at 30 minutes it is still under 100/h. Above roughly 100 tracked repositories the poll becomes noticeable, and the app says so: "Bei {n} Repositories ist die Abfrage seltener." and lengthens the interval. Never a silent degradation of somebody's notifications.

## Deep links

A notification opens the exact place.

| From | To |
|---|---|
| A comment notification | The chat, scrolled to the run that created the pull request, with the comment quoted in a sheet |
| A review request | The project detail, with the pull request's review state |
| A check failure | The verification panel of that run, scrolled to the failing command |
| A merge | The project detail, with the commit marked as merged |
| Access lost | The GitHub settings screen |

A notification that opens the app's home screen is a notification that will be dismissed without being read.

## Settings

| Setting | Default | Notes |
|---|---|---|
| GitHub notifications | On when connected | Off until then, with the reason on the row |
| Comment notifications | On | |
| Review notifications | On | |
| Check notifications | On | Failure at high, success at low |
| Mention notifications | On | |
| Successful check notifications | Off | On by default is too noisy. Off, with a one-tap enable. |
| Poll interval, foreground | 60 s | Not user-configurable below 30 s |
| Battery threshold | 20 % | A slider, with a note about what it affects |
| Quiet hours | None | The OS's own notification settings are respected, and the app does not add a second schedule |

**Quiet hours are not reimplemented.** A user who has told Android not to disturb them at night has said so, and a second schedule inside the app would be one more thing to configure and one more way for it to be wrong.

## Implementation

| Concern | Approach |
|---|---|
| Scheduling | A work manager periodic task, so the poll survives Doze |
| In the foreground | An in-process ticker, because a work manager task is too slow for a 60 s interval |
| A slow network | A 15 s timeout. A poll that hangs must not hold the next one. |
| One at a time | A mutex. Two concurrent polls double the request count for no benefit. |
| A failed poll | Silently retried at the next interval, with a consecutive-failure counter. After 10 failures a `warning` appears in Settings: "GitHub wird seit {n} Minuten nicht erreicht." |
| A revoked token | Detected on a 401, and it stops the polling and prompts the sign-in, rather than failing every minute |
| Cached state | The last poll's result in memory, so the project detail is instant |
| No network | Polling is skipped entirely, and the UI shows the last known state with its timestamp |

## Testing

| Test | Type |
| --- | --- |
| `PollIntervals` | Integration — foreground 60 s, background 5 min with a run, 30 min idle, asserted against a fake clock |
| `BatteryThresholdStopsPolling` | E2E — below the threshold, GitHub polling stops and run notifications continue |
| `DedupeById` | Unit — two comments with the same timestamp and different ids both notify |
| `DedupeState` | Unit — an unchanged pull request state produces nothing |
| `CheckFlapSuppressed` | E2E — pending → running → pending → success produces one notification |
| `OwnCommentSuppressed` | E2E — a comment by the connected user produces nothing |
| `BotMarked` | E2E — a bot comment notifies and is marked |
| `SeveritySplit` | UI — a failed check is high, a passed one is low, and the success one is off by default |
| `OfflineCatchup` | E2E — after 24 h offline, the high-water mark advances and only genuinely new items are announced |
| `DeepLinks` | E2E — every notification type opens the correct place with the correct scroll position |
| `RateLimitRespected` | Integration — 20 repositories at 30 minutes stays under 100 requests per hour |
| `LargeRepoNotice` | UI — above 100 tracked repositories the app says the interval lengthened |
| `OneAtATime` | Integration — the mutex prevents concurrent polls |
| `TokenRevokedStopsPolling` | E2E — a 401 stops the polling and prompts, rather than failing every minute |
| `ConsecutiveFailures` | E2E — after 10 failures a warning appears in Settings with the duration |
| `NoQuietHoursOverride` | UI — asserts the app has no quiet-hours setting of its own |
| `OfflineNoPoll` | Integration — with no network, zero poll requests are made |
| `LocalNotificationsUnaffected` | E2E — the battery threshold suppresses GitHub polling and never suppresses a run-finished notification |
