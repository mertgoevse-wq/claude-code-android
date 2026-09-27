# Error taxonomy

Every error the app can produce, its code, whether it is retryable, what the user sees, and what the app does about it. An error the user cannot act on is a failed message; an error with no code cannot be tracked.

## Shape

```kotlin
sealed interface AppError {
    val code: ErrorCode        // stable, never renumbered
    val messageDe: String      // one line, plain language, says what to do
    val messageEn: String
    val retryable: Boolean
    val cause: Throwable?
}
```

The `AppError` hierarchy is a closed set. A `Throwable` never crosses a module boundary; it becomes an `AppError` at the boundary, keeping its cause for the log.

## Codes

Codes are stable strings, not numbers, so they survive reordering. They appear in the log, in the diagnostics bundle, and in bug reports. **A removed code is retired, never reused.**

### Provider and network

| Code | Retryable | User sees (de) | App does |
|---|---|---|---|
| `PROVIDER_AUTH_FAILED` | No | "Der Schlüssel wurde abgelehnt. Prüfe ihn in den Einstellungen." | Ends the run. Does not retry. |
| `PROVIDER_RATE_LIMITED` | Yes | "Zu viele Anfragen. Versuch {n} von {m}." | Waits the server's delay, shows the attempt. |
| `PROVIDER_OVERLOADED` | Yes | "Der Anbieter ist gerade überlastet. Versuch {n} von {m}." | Same, with a longer backoff. |
| `PROVIDER_BILLING` | No | "Das Guthaben ist aufgebraucht." | Ends the run. Names the provider. |
| `PROVIDER_MODEL_NOT_FOUND` | No | "Das Modell {model} gibt es bei diesem Anbieter nicht." | Offers the known model list. |
| `PROVIDER_BAD_REQUEST` | No | "Die Anfrage wurde abgelehnt: {reason}" | Ends the run. The raw body is in the log, collapsed in the UI. |
| `PROVIDER_TIMEOUT` | Yes | "Zeitüberschreitung beim Anbieter." | Retries within the budget. |
| `PROVIDER_UNREACHABLE` | Yes | "Der Anbieter ist nicht erreichbar. Läuft das VPN?" | Retries; after the budget, suggests another provider. |
| `PROVIDER_MALFORMED_RESPONSE` | Yes | "Die Antwort war unvollständig." | Logs the raw line, retries once, then continues without the fragment. |
| `NO_PROVIDER_CONFIGURED` | No | "Noch kein Anbieter eingerichtet." | Deep-links to the provider setup screen. |

### Engine

| Code | Retryable | User sees (de) | App does |
|---|---|---|---|
| `ENGINE_NOT_INSTALLED` | No | "Claude Code ist noch nicht eingerichtet." | Deep-links to the runtime setup screen. |
| `ENGINE_LAUNCH_FAILED` | No | "Claude Code startet nicht: {reason}" | Offers the repair path, keeps the log. |
| `ENGINE_UNEXPECTED_VERSION` | No | "Diese Version von Claude Code kennen wir nicht." | Refuses to run an unrecognised build. Runs the version probe. |
| `ENGINE_EXIT_NONZERO` | Depends | "Claude Code wurde mit Code {code} beendet." | Classifies from stderr where possible. |
| `ENGINE_KILLED` | No | "Der Prozess wurde beendet." | Marks the run interrupted, offers a resume. |
| `ENGINE_MALFORMED_OUTPUT` | Yes | — | Recorded in the log, run continues. Not shown unless asked for. |
| `ENGINE_SESSION_NOT_FOUND` | No | "Die Unterhaltung ist nicht mehr auf dem Gerät." | Offers a new conversation carrying the text forward. |
| `ENGINE_OUTPUT_TOO_LARGE` | No | "Die Ausgabe war zu groß für den Speicher." | Suggests offloading to a runner. |

### Runtime and environment

| Code | Retryable | User sees (de) | App does |
|---|---|---|---|
| `RUNTIME_PROFILE_MISSING` | No | "Es ist keine Laufzeitumgebung eingerichtet." | Deep-links to setup. |
| `RUNTIME_CHECKSUM_MISMATCH` | No | "Die heruntergeladene Datei ist beschädigt. Es wird nichts installiert." | Aborts. Never installs an unverified binary. |
| `RUNTIME_CHECKSUM_UNREACHABLE` | Yes | "Die Prüfsummen sind nicht erreichbar." | Retries; then aborts rather than proceeding unverified. |
| `RUNTIME_INCOMPATIBLE` | No | "Dieses Gerät unterstützt das nicht (32-bit)." | Stops before downloading anything. |
| `RUNTIME_INSUFFICIENT_STORAGE` | No | "Es fehlen {mb} MB Speicher." | Reports the exact shortfall. Deletes nothing. |
| `RUNTIME_PTY_FAILED` | Yes | "Das Terminal konnte nicht gestartet werden." | Retries, then reports. |
| `RUNTIME_BINARY_PROBE_FAILED` | No | "Die neue Version startet nicht. Es wurde zurückgerollt." | Rolls back to the last known-good version. |

### Project and version control

| Code | Retryable | User sees (de) | App does |
|---|---|---|---|
| `PROJECT_PATH_MISSING` | No | "Der Projektordner ist nicht mehr da." | Offers to re-link or archive. |
| `PROJECT_NOT_A_REPOSITORY` | No | "Dieser Ordner ist kein Git-Repository." | Offers `git init` in a confirmation sheet. Never silently. |
| `GIT_COMMAND_FAILED` | Depends | "Git ist fehlgeschlagen: {stderr}" | Records the full stderr. |
| `GIT_DIRTY_WORKTREE` | No | "Es gibt ungespeicherte Änderungen im Ordner." | Offers a stash-by-branch, never a discard. |
| `GIT_DETACHED_HEAD` | No | "Der Ordner ist auf einem losgelösten Stand." | Names the commit, offers a new branch. |
| `GIT_BRANCH_EXISTS` | No | "Der Zweig {name} gibt es schon." | Offers a different name. Never force-replaces. |
| `GIT_PUSH_REJECTED` | Yes | "Hochladen abgelehnt: {reason}" | Retries on transient rejections; reports on a permission problem. |
| `GIT_REMOTE_NOT_FOUND` | No | "Das Repository ist nicht erreichbar oder wurde umbenannt." | Checks the token, then the URL. |
| `GIT_PUSH_TO_DEFAULT_BLOCKED` | — | "Auf den Hauptzweig wird nie hochgeladen." | The hard block. Not an error state; a refusal. |

### GitHub API

| Code | Retryable | User sees (de) | App does |
|---|---|---|---|
| `GITHUB_NOT_AUTHENTICATED` | No | "GitHub ist nicht verbunden." | Deep-links to the auth screen. |
| `GITHUB_RATE_LIMITED` | Yes | "GitHub-Limit erreicht, wieder da um {time}." | Waits; shows the reset time, not a spinner. |
| `GITHUB_PR_EXISTS` | No | "Für diesen Zweig gibt es schon einen Pull Request." | Links to the existing one. |
| `GITHUB_REPO_NOT_FOUND` | No | "Das Repository ist für dieses Konto nicht sichtbar." | Names the likely cause: missing repository access on a fine-grained token. |
| `GITHUB_SECONDARY_LIMIT` | Yes | "GitHub drosselt vorübergehend." | Backs off aggressively. |

### Verification

| Code | Retryable | User sees (de) | App does |
|---|---|---|---|
| `VERIFY_NO_COMMANDS` | — | "Für dieses Projekt sind keine Prüfbefehle hinterlegt." | The **unverified** state, not a failure. |
| `VERIFY_COMMAND_NOT_FOUND` | No | "Der Prüfbefehl {cmd} wurde nicht gefunden." | Asks for a correction, suggests detection. |
| `VERIFY_FAILED` | Yes | "{n} von {m} Prüfungen fehlgeschlagen." | Diagnose → fix → re-verify, within the budget. |
| `VERIFY_TIMEOUT` | Yes | "Die Prüfung hat zu lange gedauert." | Counts against the budget. |
| `VERIFY_UNPARSABLE` | — | — | Falls back to generic output. Never guesses a result. |

### Permissions and policy

These are refusals, not failures. They are presented differently: calm, and with the reason.

| Code | User sees (de) | App does |
|---|---|---|
| `POLICY_BLOCK_DELETE` | "Diese Aktion würde etwas löschen. Das ist in dieser App nicht möglich." | Refuses. No override, at any level. |
| `POLICY_BLOCK_SPEND` | "Diese Aktion würde Geld kosten. Das ist in dieser App nicht möglich." | Refuses. |
| `POLICY_BLOCK_PUBLISH` | "Dieses Repository bleibt privat." | Refuses. |
| `POLICY_BLOCK_DEFAULT_BRANCH` | "Auf den Hauptzweig wird nie hochgeladen." | Refuses. |
| `POLICY_NEEDS_APPROVAL` | A permission sheet naming the action and its consequences | Waits for an answer. Timeout is configurable; the default never expires silently. |
| `POLICY_RATE_LIMITED_ATTEMPTS` | "Zu viele Versuche in kurzer Zeit." | Slows down. |

### Skills

| Code | Retryable | User sees (de) | App does |
|---|---|---|---|
| `SKILL_PARSE_FAILED` | No | "Die Skill-Datei ist nicht lesbar." | Shows the line and the reason. |
| `SKILL_VALIDATION_FAILED` | No | "Der Skill ist ungültig: {reason}" | Lists every validation problem at once, not one per attempt. |
| `SKILL_ALREADY_INSTALLED` | No | "Dieser Skill ist schon installiert." | Offers update. |
| `SKILL_SOURCE_UNREACHABLE` | Yes | "Die Quelle ist nicht erreichbar." | Retries, then reports. |
| `SKILL_NAME_CONFLICT` | No | "Ein anderer Skill heißt genauso." | Refuses. Refuses to overwrite silently. |

### Remote runners

| Code | Retryable | User sees (de) | App does |
|---|---|---|---|
| `RUNNER_UNREACHABLE` | Yes | "{name} ist nicht erreichbar." | Retries, then proposes a fallback runner. |
| `RUNNER_OFFLINE` | Yes | "{name} ist ausgeschaltet." | Waits, notifies when it returns. |
| `RUNNER_QUOTA_EXCEEDED` | No | "Das kostenlose Kontingent von {name} ist aufgebraucht." | Proposes another runner. States the date the quota was checked. |
| `RUNNER_MISSING_CAPABILITY` | No | "{name} kann {tool} nicht." | Names what is missing and what it was probed for. |
| `RUNNER_AUTH_FAILED` | No | "Der SSH-Schlüssel für {name} wurde abgelehnt." | Re-runs the key bootstrap. |

### Local and system

| Code | Retryable | User sees (de) | App does |
|---|---|---|---|
| `STORAGE_FULL` | No | "Der Speicher ist voll: {mb} MB fehlen." | Names what is using space. Deletes nothing. |
| `NETWORK_OFFLINE` | Yes | "Keine Verbindung." | Marks the run interrupted, not failed. |
| `NETWORK_METERED` | — | "Das ist ein mobiles Netz. Läuft trotzdem." | No block. Cost is the provider's, not the network's. |
| `BATTERY_LOW` | — | "Der Akku ist fast leer. Für lange Läufe ist ein Ladegerät nötig." | Informs. Offers to offload. |
| `PERMISSION_DENIED_ANDROID` | No | "Android hat die Berechtigung verweigert." | Names the permission and links to the system settings. |
| `BIOMETRIC_UNAVAILABLE` | No | "Es ist keine Bildschirmsperre eingerichtet." | Falls back to the device credential, then to no lock, and says which. |

## Presentation rules

1. **Two lines, always.** Line one: what happened, in plain language, no jargon. Line two: what to do. Raw technical text is available beneath, collapsed.
2. **Never blame the user.** "Der Server ist überlastet", not "Du hast zu viele Anfragen gestellt".
3. **Never show a stack trace unless asked.** The log is one tap away, and the log is complete.
4. **Retryable errors show progress.** "Versuch 2 von 5" beats a spinner, because a spinner is indistinguishable from a hang.
5. **Non-retryable errors never look retryable.** No "Erneut versuchen" on a wrong key.
6. **One error at a time in the primary surface.** A run with three failures shows the first as primary and lists the rest.

## What the app does with an error, in order

```
1. Classify it into the taxonomy.
2. Decide retryability from the class, not from the exception type.
3. Log it: code, run, context, cause. Redacted.
4. Notify the user if the run ends or needs attention.
5. If retryable and the budget allows: wait, increment, show the attempt.
6. If the budget is exhausted: write a report, mark FAILED, offer a resume.
7. If it is a hard block: refuse, explain, continue with the rest of the run.
```

Step 7 matters: one refused destructive command must not end a run. The agent is told it was refused and continues. Only a run that cannot make progress ends.
