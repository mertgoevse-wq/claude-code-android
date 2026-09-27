# Environment diagnostics

Figuring out what is wrong, and telling the user in a sentence they can act on.

## The principle

An error message exists to be acted on. "Something went wrong" is not an error message; it is a failure to write one. Every diagnostic in this app answers three questions in order:

1. **What is wrong?** In one sentence, in plain language, naming the thing.
2. **Why?** The specific cause, with the actual value where there is one.
3. **What do I do?** A named action, or a specific instruction.

```
✕  Die Verbindung zu Anthropic wird nicht aufgebaut.
   Zeitüberschreitung nach 30 Sekunden. VPN aktiv?
   [ Verbindung erneut versuchen ]  [ Andere Adresse ]
```

Never: a stack trace without a sentence, a code without an explanation, or a retry button with no indication of what would change.

## The health check

A sequence of checks, run before the first run, on demand, and after any environment change.

| # | Check | How | Failure |
|---|---|---|---|
| 1 | Device supported | `uname -m`, the API level, the ABI | "Dieses Gerät wird nicht unterstützt (32-bit)." |
| 2 | Free storage | The filesystem | "1,2 GB nötig, 340 MB frei." |
| 3 | Runtime profile installed | The manifest | "Keine Laufzeitumgebung. Einrichtung starten?" |
| 4 | Runtime integrity | The manifest's digests | "Die Laufzeitumgebung ist beschädigt. Reparieren?" |
| 5 | Engine present | The manifest | "Claude Code fehlt. Neu laden?" |
| 6 | Engine version | Read from the manifest, without launching | "Update verfügbar: 1.2.290." |
| 7 | Engine starts | `claude --version`, 15 s | "Claude Code startet nicht: {reason}." |
| 8 | Provider configured | The database | "Kein Anbieter eingerichtet." |
| 9 | Key present | The Keystore | "Kein Schlüssel für {provider}." |
| 10 | Provider reachable | A minimal HTTPS request, 10 s | The specific network diagnosis |
| 11 | Provider authenticates | A cheap authenticated request | "Der Schlüssel wurde abgelehnt." |
| 12 | Model available | A minimal completion | "Das Modell {model} gibt es bei {provider} nicht." |
| 13 | Git available | `git --version` | "Git fehlt in dieser Laufzeitumgebung." |
| 14 | Git configured | `git config user.name`, `user.email` | "Git kennt keine E-Mail-Adresse. Commits wären nicht zuordenbar." |
| 15 | Project reachable | The path is readable and writable | The specific filesystem error |
| 16 | Project is a repository | `git rev-parse` | "Kein Git-Repository." |
| 17 | Working tree state | `git status --porcelain` | The specific state |
| 18 | Remote reachable | `git ls-remote` | The specific network or auth diagnosis |
| 19 | GitHub authenticated | The token, if a remote exists | "GitHub ist nicht verbunden." |
| 20 | Runner reachable, if assigned | A probe | The runner's state |

### The result

A list, with a single overall verdict and a list of findings.

| Verdict | Meaning | Action |
|---|---|---|
| `READY` | Everything needed for a run is present | Start |
| `DEGRADED` | A run is possible with reduced capability | Start, with the limitation stated |
| `BLOCKED` | A run cannot start | The specific fix, as the primary action |

```
Bereit — 2 Hinweise

✓ Gerät unterstützt          arm64-v8a, Android 15
✓ Speicher                  4,2 GB frei
✓ Laufzeit                  Native 1.2.283, geprüft vor 2 Tagen
✓ Claude Code               1.2.283, Starttest bestanden
✓ Anbieter                  Anthropic, Modell erreichbar
! Git                      Keine globale E-Mail-Adresse gesetzt
                              Commits haben keinen Autor.
                              [ Git-E-Mail setzen ]
! Update                    Claude Code 1.2.290 verfügbar
                              [ Hinweise ansehen ]  [ Jetzt laden ]
```

**A warning never blocks a run.** Git's missing email is a warning, because a commit with no author is bad but not fatal, and the run can proceed. The list distinguishes `!` from `✕` visually, with a glyph, and in the accessibility tree.

## Diagnoses

The specific ones. A generic message is a bug.

### Network

| Symptom | Message | Action |
|---|---|---|
| DNS fails | "Der Name {host} lässt sich nicht auflösen." | "Funk prüfen", or the metered-network check |
| Connection refused | "Die Verbindung wurde abgelehnt. Läuft der Dienst?" | Retry |
| Timeout | "Zeitüberschreitung nach {n} s. VPN aktiv?" | Retry, or another address |
| TLS failure | "Das Zertifikat wird nicht akzeptiert." | The date and time, and the system CA store. Never "trust anyway". |
| No network at all | "Keine Verbindung." | "Offline", with the offline explanation |
| Metered | "Mobiles Netz." | Not an error. Offered as information, and it does not block. |
| IPv6 only | "Nur IPv6 erreichbar." | A specific hint about the local network |

### The runtime

| Symptom | Message | Action |
|---|---|---|
| No profile | "Keine Laufzeitumgebung eingerichtet." | Setup |
| Corrupt prefix | "Die Laufzeitumgebung ist beschädigt (3 Dateien)." | Repair, naming the count |
| Wrong ABI | "Diese Laufzeitumgebung passt nicht zu diesem Gerät." | Repair |
| Engine missing | "Claude Code fehlt." | Reload |
| Engine will not start | "Claude Code startet nicht: {stderr, first line}." | The full log, and a report action |
| PTY unavailable | "Kein echtes Terminal verfügbar." | Continue with pipes, with the notice |
| Out of memory | "Zu wenig Arbeitsspeicher." | Close other apps, or offload |

### The provider

| Symptom | Message | Action |
|---|---|---|
| No provider | "Kein Anbieter eingerichtet." | Setup |
| No key | "Kein Schlüssel für {provider}." | Add a key |
| Key rejected | "Der Schlüssel wurde abgelehnt." | Check the key, and the profile it came from |
| Base URL wrong | "Die Adresse {url} antwortet nicht." | Edit the address |
| Model missing | "Das Modell {model} gibt es bei {provider} nicht." | A list of the models that do exist |
| Rate limited | "Zu viele Anfragen. Wieder da um {time}." | Wait, with the time |
| Overloaded | "Der Anbieter ist überlastet. Versuch {n} von {m}." | Retry, automatically |
| No credit | "Das Guthaben ist aufgebraucht." | Nothing. It is a fact. |
| Wrong format | "Die Antwort von {provider} hat ein unbekanntes Format." | The raw response, and the fallback to a different provider |
| Vision unsupported | "{model} kann keine Bilder." | Remove the image, or change the model |

### Git and GitHub

| Symptom | Message | Action |
|---|---|---|
| Git missing | "Git fehlt in dieser Laufzeitumgebung." | Switch to profile B, or a runner |
| No author | "Git kennt keine E-Mail-Adresse." | Set it, with the app's address prefilled and editable |
| Not a repository | "Kein Git-Repository." | Initialise, with the exact command shown |
| Dirty tree | "Es gibt {n} ungespeicherte Änderungen." | Not an error. A notice, with the safe behaviour stated. |
| Detached head | "Losgelöst bei {hash}." | Create a branch |
| Remote unreachable | "Das Repository ist nicht erreichbar." | Check the URL, then the token |
| Token rejected | "GitHub-Zugriff nicht erlaubt." | The scope list, and a link to the token settings |
| Repository not visible | "Das Repository ist für dieses Konto nicht sichtbar." | The most likely cause first: a fine-grained token without repository access |
| No private access | "Kein Zugriff auf private Repositories." | The scopes, named |
| Rate limited | "GitHub-Limit erreicht, wieder da um {time}." | The time |
| Merge in progress | "Im Projekt läuft ein Merge oder Rebase." | The terminal, to finish it |

### Storage

| Symptom | Message | Action |
|---|---|---|
| Low | "{n} GB frei." | A breakdown by category, and a cache cleanup offer |
| Critical | "Der Speicher ist voll." | The breakdown, the cleanup, and the offload offer. Never a deletion. |
| Download impossible | "{n} MB fehlen." | The exact shortfall, and what to free |

## The diagnostics bundle

The thing a person exports when they are reporting a problem.

| Contents | Excluded |
|---|---|
| The app version, build type, install source | Any key or token |
| The device model, the OS version, the ABI, the screen size | The advertising id, any identifier |
| The runtime profile, the manifest, the engine version | The runtime's file contents |
| The health check's full result | The user's projects beyond a project id |
| The last 500 log entries, redacted | The transcripts of unrelated conversations |
| The last 100 crash signatures with counts | The user's file contents |
| The stored settings, redacted | The key profiles beyond their names and hints |
| The process list, the running services, the memory usage | Anything on shared storage |
| A build fingerprint, so two bundles can be correlated | The git remotes of the user's projects |

| Format | Detail |
|---|---|
| A zip | One file, with a `README.txt` in it explaining every file inside |
| Size | Capped at 5 MB, with the log trimmed to fit, and the trimming stated |
| Naming | `claude-code-android-diagnostics-{version}-{date}.zip` |
| Sharing | The system share sheet. The user chooses the destination; the app has no endpoint. |

**A diagnostics bundle is a document a person can read without the app.** The `README.txt` lists every file and says in one line what it is. Somebody reading it in a bug report should not have to ask what `manifest.json` is.

## Where diagnostics run

| When | How long | Blocking |
|---|---|---|
| Before the first run | Up to 8 s, with a progress line | Yes, with a skip |
| On demand, from Settings | The same | No |
| After a failed run | The checks relevant to the failure, only | No |
| After an environment change | The affected checks | No |
| On a schedule | Once a day, in the background, results cached | No |

**Diagnostics never block a run for long.** The full check is skipped if the quick one passes, and a run is never refused because a diagnostic was slow. A `DEGRADED` verdict starts the run and states the limitation.

## The error message rules

From `02-architecture/error-taxonomy.md`, repeated here because it is the thing that matters most:

1. Two lines. What happened, then what to do.
2. Plain language, no jargon in the first line.
3. The user is never blamed. "Der Server ist überlastet", not "Du hast zu viele Anfragen gestellt".
4. A specific value where there is one: the host, the model, the exit code, the count, the time.
5. A named action, not "OK".
6. The technical detail is one tap away and never in the way.
7. Never a stack trace unless asked for.
8. Never a retry button on a non-retryable error.
9. Every error has a code, and the code is in the log.
10. An error that cannot be given a good message is a bug in the error handling, and it is filed like one.

## Testing

| Test | Type |
|---|---|
| `HealthCheckMatrix` | Integration — every check, every failure mode, producing the documented verdict and message |
| `VerdictRules` | Unit — which findings are warnings and which block, exhaustively |
| `WarningsNeverBlock` | E2E — a `DEGRADED` verdict starts the run and states the limitation |
| `QuickSkipsFull` | Unit — the quick path does not run the full check when the first one passes |
| `RunNeverBlockedBySlowDiagnostics` | E2E — a diagnostics step taking 30 s does not stop a run starting |
| `NetworkDiagnoses` | Unit — DNS, refused, timeout, TLS, offline, metered, IPv6, each with its own message and no generic fallback |
| `ProviderDiagnoses` | Unit — every provider failure mode with its specific message |
| `GitDiagnoses` | Unit — every git and GitHub failure mode with its specific message |
| `NeverBlamesUser` | Unit — a corpus of error strings, asserting no message contains a second-person accusative construction |
| `TwoLines` | Unit — every error message is at most two lines before the technical disclosure |
| `ActionNamed` | Unit — every error has a named action, not "OK" |
| `NoStackUnlessAsked` | UI — the technical detail is collapsed and requires a tap |
| `BundleContents` | E2E — the bundle contains every listed item and none of the excluded ones |
| `BundleRedaction` | E2E — a key seeded in every possible place appears nowhere in the bundle |
| `BundleReadable` | UI — the README lists every file with a one-line description |
| `BundleSize` | E2E — a capped bundle is under 5 MB and states the trimming |
| `SpecificNotGeneric` | Unit — a corpus of induced failures, asserting no message is "something went wrong" in any language |
| `CodesInLog` | Integration — every displayed error has its code in the log, correlated by run and timestamp |
| `RecoveryScreenPath` | E2E — a broken engine with no rollback leads to the recovery screen, not a dead end |
