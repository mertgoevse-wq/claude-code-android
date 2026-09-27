# Transparency log

The record of everything the app did. Append-only, complete, exportable, and readable by a person.

## The rule this implements

> Never hide anything.

That is one of the five hard rules, and it is the one that is hardest to honour because it is easy to implement in spirit and hard in practice: logs get truncated, streams get sampled, errors get summarised, and somewhere a decision gets made that the user does not need to see the details of that.

The design consequence: **the transparency record is a consumer of the same event stream that renders the chat**, not a separate logging system. Per `02-architecture/event-protocol.md`, one stream has four consumers. Because the log is one of them, the record is complete by construction rather than by discipline — a thing that appears on screen is in the log, and a thing that happens off screen is also in the log.

## What is recorded

| Category | Content | Level |
|---|---|---|
| `SYSTEM` | App start, version, runtime profile, permission grants, settings changes, feature flags | INFO |
| `STATE` | Every run state transition, with the timestamp, the run, and the elapsed time | INFO |
| `COMMAND` | Every command the app executed: git, the engine, the probes, the install steps, the runner operations. The full command line and the exit code. | INFO |
| `DIFF` | Every file change a run produced, with the attempt number | INFO |
| `PERMISSION` | Every permission request and the answer, with the level that caused it | INFO |
| `POLICY` | Every hard-block refusal, with the rule and the reason | WARN |
| `NETWORK` | Every outbound request's host and purpose. Never a header, never a body, never a key. | INFO |
| `COST` | Every usage event, with tokens and the computed cost | INFO |
| `ERROR` | Every error, with its code, its context, and its cause | ERROR |
| `SECURITY` | Key accesses, biometric authentications, app-lock transitions, a key reveal | WARN |
| `ERASURE` | The one deletion, with what was and was not removed | WARN |

**Commands are recorded in full.** A user who wants to know what the app did to their repository can read every git command, in order, with its exit code. This is the single most load-bearing category: an unattended tool that edits code is only trustworthy if its command log is complete enough to reconstruct what happened.

## What is never recorded

| Never | Enforcement |
|---|---|
| A key or a token, in any form | `Redactor` runs on every write path. A test seeds known key patterns and asserts their absence from every sink. |
| A request or response body to a model | The task text and the transcript are recorded; the provider's raw request and response are not |
| A GitHub token | Stored in the Keystore, referenced by id, never written |
| A file's contents, unless a run changed it | Only paths, sizes, and hashes. The app does not read your files to log them. |
| A password | Never collected. See `04-screens/14-remote-runner-setup.md`. |
| A screenshot | Never taken by the app. The `FLAG_SECURE` option prevents the *system* from taking one in the recents list. |

The redaction is a real pass, not a naming convention. `Redactor` matches key-shaped patterns — `sk-…`, `ghp_…`, `github_pat_…`, `AKIA…`, JWT shapes, `ANTHROPIC_API_KEY=` assignments, bearer headers — and replaces them with a shape-preserving placeholder, so `sk-ant-api03-abc…xyz` becomes `sk-ant-api03-[redacted]…[redacted]` and the reader can still see that a key was there. A test asserts the log never contains a real one.

## The view

A screen, reachable from Settings and from every run's summary.

| Element | Content |
|---|---|
| Filters | Category, severity, project, run, time range |
| Search | Full text across the message and the detail |
| Grouping | By day, then by run |
| Row | Time, category glyph, the message, the severity tint for errors |
| Expand | The full detail, formatted, with a copy action |
| Export | A whole filtered range as JSON, or as Markdown |
| A run's log | The same view, pre-filtered to that run, linked from the run summary |

### Reading it

The view is built for a person, not for a machine.

| Property | Behaviour |
|---|---|
| Grouping | By day and run. A wall of 4.000 timestamped lines is not a log, it is noise. |
| Default filter | Everything, newest first, with errors given a subtle emphasis rather than being hidden at the bottom |
| A `POLICY` entry | Always visible with its reason, because a refusal the user does not know about is a run that appears to have done nothing |
| A `SECURITY` entry | A key access is a `WARN` and stands out. Somebody should be able to see at a glance when their key was used. |
| Search | Full text, including inside the detail JSON, which is often where the specific value is |
| Long values | Truncated in the row with a visible marker, never silently |
| Timestamps | Local time with the zone, and UTC on request. A log without a zone is a log that cannot be correlated with a provider's. |

## The export

The same content, in a shape a person or a bug report can use.

| Format | When |
|---|---|
| JSON | For tooling and for attaching to an issue. The full structured record. |
| Markdown | For a person. Grouped by run, with the commands as a list and the errors as a section. |
| Plain text | For pasting into an email. Commands and errors only, timestamps included. |

**Every export is redacted**, and the export screen says so: "Schlüssel und Token sind entfernt. Der Inhalt ist vollständig, ausser der Geheimnisse." That sentence matters: an export that is complete *except* for secrets is still complete for the purpose, and saying so prevents somebody from not trusting it.

## The retention question, answered honestly

The log grows. It is a database table plus a set of files, and a long-running installation produces a lot of it.

| Kept forever | Pruned by the retention setting |
|---|---|
| Every `POLICY` entry — a refusal is a promise, and promises have a long tail | `NETWORK` entries, which are numerous and low-value in aggregate |
| Every `SECURITY` entry | Verbose `COMMAND` output bodies, which live in files, not rows |
| Every `ERROR` | The `STATE` transitions of runs older than the retention window |
| Every `COMMAND` line, without its output | |
| Every `ERASURE` | |

**Pruning removes the app's own record of an event, never the event.** A pruned `STATE` entry does not un-happen; the commit is still in git, the cost is in the provider's dashboard, and the branch is on GitHub. The log is a convenience for reconstructing, and pruning it is the app forgetting rather than the user losing.

The maintenance notification says exactly what will go and when, the day before, with a count. And the export is available before it happens, because a user who wants a record of a period can take one.

## Where the log is not enough

Two things the log deliberately does not cover, and states so.

**The model's reasoning.** Thinking blocks are summarised in the transcript and the raw text is capped at 64 KB. The log records that thinking happened and how long it took, not the full stream of it. Reasoning is not a record of what the *app* did, and a log that grew without bound with it would stop being readable.

**The contents of files the app did not change.** The app does not read a user's files in order to log them. If a run read a file, the log has the path, the size, and the fact that it was read. If a user wants to know what was in it, it is in git, and that is a better place for it.

Both are stated in the log's own About panel: "Was hier steht: alles, was die App getan hat. Was nicht: die vollständigen Gedankengänge des Modells und der Inhalt von Dateien, die kein Lauf geändert hat."

## Testing

| Test | Type |
|---|---|
| `LogCompleteness` | Integration — for a synthetic run, the log entry count equals the event count, with no drops |
| `LogIsAConcurrentConsumer` | Integration — a log entry exists for every event the chat rendered, proven by comparing the two sequences |
| `LogAppendOnly` | Unit — the repository has no update and no delete method; a static check asserts it |
| `LogRedaction` | Unit — ten known key patterns, ten sinks, zero occurrences in any output |
| `LogCommandsComplete` | E2E — every git command the app ran appears, in order, with its exit code |
| `LogPolicyAlwaysVisible` | UI — a `POLICY` entry is not filtered out by any default filter |
| `LogSecurityEmphasis` | UI — key accesses are visually distinct in the list |
| `LogSearchDetail` | UI — a search finds a value that appears only inside the detail JSON |
| `LogExportRedacted` | E2E — the exported JSON and Markdown contain no key, and the export screen states they are redacted |
| `LogRetention` | E2E — pruning removes the specified categories, keeps `POLICY`, `SECURITY`, `ERROR`, and `ERASURE`, and the notification precedes it by a day |
| `LogEraseSurvives` | E2E — after a project erasure, the `ERASURE` entry is still readable |
| `LogAboutPanel` | Screenshot — the log's own About panel, stating what is and is not recorded |
| `LogTimeZones` | Unit — timestamps round-trip through UTC and render in the local zone with it |
