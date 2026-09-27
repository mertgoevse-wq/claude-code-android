# Run history

Every run, ever, queryable, with its outcome, its cost, its verification, and its result.

## Purpose

Answer three questions a person actually asks: what happened, what did it cost, and can I look at the result of that one from last Tuesday.

## The list

| Sort | Default |
|---|---|
| Newest first | Yes |
| Project | Filterable |
| Outcome | Filterable: all, finished, unverified, failed, interrupted, cancelled |
| Date range | Filterable |
| Search | Across the task text, the project name, the branch, and the commit hash |

At medium and expanded widths the list becomes two columns: the run summary on the left, the outcome panel on the right. At compact it is a single column of cards, with the outcome in the card.

## The run row

| Element | Content |
|---|---|
| Status badge | The outcome. Never a colour alone. |
| Task | The original order, 2 lines, `bodyMedium` |
| Project and branch | `bodySmall`, the branch in `mono` |
| When | Relative for the last week, absolute beyond that |
| Duration | Wall-clock, from `PLANNING` to the terminal state. Excludes time spent in `AWAITING_PERMISSION`, which is the user's time and not the agent's. |
| Changes | "{n} Dateien · +{a} −{r}" |
| Verification | "3 von 3 bestanden", "1 von 3 fehlgeschlagen", or "Nicht geprüft" |
| Attempts | Only when greater than 1: "3 Versuche" |
| Cost | With a `~` when estimated |
| Commit | The 7-character hash, tappable to copy |
| PR | A number and a link, when one exists |
| Backend | "Dieses Gerät" or the runner's name. A run's location is part of its identity, and a slow runner run is a different fact from a slow phone run. |

## Opening a run

Tapping opens a read-only view of the run, which is not the same screen as the live chat.

| Section | Content |
|---|---|
| Outcome | The badge, first, and for an unverified run the "Nicht geprüft" line above everything else |
| The task | Verbatim |
| The plan | Final states, each step's criterion, and whether it was checked |
| The transcript | The full read-only transcript, with the tool cards, exactly as they were |
| The verification | The panel, with every command, its output, and its parsed summary |
| The changes | The diff, at that run's state, with the decisions that were made |
| The commit and the PR | With links |
| The cost | The token breakdown |
| The log | Every entry for this run, in order, filterable by category |
| The terminal | The session's scrollback for that window of time |

**Read-only means read-only.** No action is available on a finished run except: open the diff, open the log, open the PR, copy the commit hash, re-run the task, and export. There is no "revert this run" in the history view, because reverting is a git operation and it belongs in the diff viewer, where the user can see what they are reverting.

## Archiving and retention

A run's history is the user's. Retention is configurable and defaults to keeping everything.

| Setting | Behaviour |
|---|---|
| All runs | The default. Nothing is pruned. |
| 30 / 90 / 365 days | Prunes the *index* of runs older than the period. |
| What pruning does | Removes the app's own rows: the transcript, the tool invocations, the cost records, the log entries. |
| What pruning never does | Touch the working tree, the commits, the branches, the remote, the PR. |
| When it happens | During a maintenance window, with a count and a notification the day before: "{n} ältere Läufe werden am {date} aus dem Verlauf entfernt. Deine Commits und Zweige bleiben." |
| Reversible | No. It is the app's own index. The user was told in advance, and the commits and branches are all still there. |

Pruning the app's index is not the same as deleting work, and the distinction is stated in the setting's own description, in the notification, and here. The rule is that the app may forget; the user's code may not be lost by the app forgetting.

## The run's own timeline

A vertical timeline of the run's state transitions, with timestamps.

```
14:31:02  Start                    Gerät, Profil Native
14:31:09  Plan                     4 Schritte
14:31:40  Prüfung erkannt          ./gradlew test
14:33:12  Datei geändert           build.gradle.kts
14:38:55  Prüfung gestartet        3 Befehle
14:41:20  Prüfung bestanden        3 von 3
14:41:21  Gesichert                 task/verify-commands · a3f19c2
14:41:33  Hochgeladen               +  Pull Request #12
14:41:33  Fertig                   geprüft · 0,31 $
```

| Rule | Detail |
|---|---|
| Every transition is here | Including the ones nobody wants to see: retries, refusals, permission waits, offloads |
| A permission wait shows its duration | "Warte auf Erlaubnis · 4 Min. 12 s" — this is the honest measure of how much of the run was the user's, and it is why `duration` excludes it |
| A refusal appears | "Blockiert · Löschen · Regel: niemals löschen" |
| An offload appears | "Ausgelagert auf Oracle Free · Grund: Gradle fehlt auf dem Gerät" |
| Tapping an entry | Jumps to the corresponding place in the transcript or the log |

This timeline is the raw material for `05-features/transparency-log.md`, restricted to one run.

## Grouping

| Group | Rule |
|---|---|
| By project | Toggle. Default off; the list is chronological because that is what people scan. |
| By day | Available as a group header, same as the chat list. |
| By outcome | A filter, not a group. Grouping by outcome produces a negative-feeling interface, and this app's tone does not have a place for that. |
| By week, for the older runs | Month headers beyond 30 days |

## States

| State | Behaviour |
|---|---|
| No runs | `empty-chat.svg`, "Noch keine Läufe", one sentence, and a link to the projects |
| No runs for a filter | "Keine Läufe entsprechen dem Filter." with a "Filter zurücksetzen" action |
| Loading | Nothing. A local query. |
| A run's log is missing | "Die Protokolldatei wurde entfernt. Die Zusammenfassung ist noch da." Never a blank section. |
| A run's diff is not available | "Keine Änderungen aufgezeichnet." — the run recorded no file changes, or the working tree moved. Said plainly, not hidden. |
| A very large run, over 10.000 events | Virtualised, with a search within the run |
| An archived run | Shown with an `info` badge and the archive date |
| Retention has pruned a run | The run is absent, and the count in Settings reflects it |

## The export

Every run exports as a Markdown file, written to the project directory or shared via the system sheet.

The export contains the task, the plan with its final states, the full transcript including tool calls and outputs, the verification results with their raw output, the file changes with the diff, the commit and PR references, the cost, the timeline, the backend and runtime profile, the active skills, and the autonomy level.

It is written to be useful to somebody who was not there: a colleague, a future version of the user, or an issue report. The transparency log is the machine-readable version; this is the human-readable one.

## Retention of the log versus the run

| Kept | Pruned by retention |
|---|---|
| The run summary, forever | The tool invocation rows |
| The commit hash and the PR link | The full tool output |
| The cost total | The raw stdout, which is large |
| The verification result | The per-token streaming deltas |
| The timeline | |

The summary is small and is the thing a person looks at. The bulk is large and is what a person reads when something went wrong and wants the raw output — which is why the bulk is pruned on a schedule and the summary never is. Someone who needs the bulk can export the run before it is pruned, and the maintenance notification tells them when.

## Testing

| Test | Type |
|---|---|
| `HistoryList` | Screenshot — every outcome, filters, both themes |
| `HistoryFilters` | UI — project, outcome, date, and search compose correctly |
| `HistoryDurationExcludesPermission` | Unit — a run with a 10-minute permission wait does not include it in the duration |
| `HistoryTimeline` | E2E — every state transition appears, including retries, refusals, and offloads |
| `HistoryTimelinePermissionWait` | E2E — the wait duration is recorded and excluded from the run duration |
| `HistoryReadOnly` | UI — asserts no mutating action exists on a finished run |
| `HistoryUnverifiedLeads` | Screenshot — the unverified line is above the changes and the cost |
| `HistoryRetention` | E2E — runs older than the retention window are pruned from the index, with the notification the day before |
| `HistoryRetentionPreservesGit` | E2E — after pruning, the commits, branches, and PR still exist and the working tree is untouched |
| `HistoryMissingLog` | Screenshot with the specific message, not a blank |
| `HistoryExport` | E2E — the exported Markdown contains every required section |
| `HistoryLarge` | Performance — a 10.000-event run remains scrollable |
| `HistoryNoPruningByDefault` | Unit — the default retention is "keep everything", asserted so a default change is a deliberate act |
