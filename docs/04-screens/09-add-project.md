# Screen 09 — Add project

Three routes into one destination: a project on the device, a project on GitHub, a project on a server.

## Purpose

Add a project with the least effort that still leaves the user in control of what happens to their code, and never offers a choice the app will not honour.

## Route A — Von GitHub

### Step 1 — Choose the account

If GitHub is connected, the account is shown with an avatar, the login name, and a "Konto wechseln" action. If not, this step is a connect screen.

### Step 2 — Choose the repository

| Element | Behaviour |
|---|---|
| Search | Filters as you type, by name, after a 400 ms debounce |
| List | Private repositories only. Public ones are hidden, because the app does not work with them and offering them would be a dead end. |
| Sorted | Recently pushed first. Somebody's active repository is almost always what they want. |
| Row | Owner/name, the primary language with its colour dot, the default branch, the last push time |
| Already cloned | A check glyph, and tapping opens the existing project |
| Row action | A clone button, plus a three-dot menu: "Mit anderem Zweig klonen", "In Unterordner klonen" |

| State | Behaviour |
|---|---|
| Not connected | "Mit GitHub anmelden" as the primary, with the token route as a secondary, and the scope list shown before leaving the app |
| Token route | A field, a link to GitHub's token settings, a scope checklist, and a "Token prüfen" button that lists the repositories it can see |
| No repositories | "Für dieses Konto sind keine privaten Repositories sichtbar." with the two causes named: no private repositories, or the token lacks repository access |
| Rate limited | "GitHub-Limit erreicht · wieder verfügbar um 14:32" |
| Offline | "GitHub ist nicht erreichbar. Lokale Ordner funktionieren auch offline." with the local route as a primary action |
| 200+ repositories | Paginated at 50, with "weitere laden". The list is virtualised. |

### Step 3 — Clone

A progress screen, not a spinner.

```
Fortschritt · 14.203 Objekte
▸ Lese Packdatei     erledigt
▸ Prüfe Objekte      läuft · 68 %
▸ Schreibe Arbeitsverzeichnis  wartet
```

| Property | Rule |
|---|---|
| Steps | Clone, checkout, install if the project needs it, verify |
| Per-step state | Erledigt / Läuft mit Prozent / Wartet / Fehlgeschlagen |
| Elapsed | Live |
| Cancel | Available, with a confirm. Cancelling a clone removes the partial clone; nothing else is touched. |
| Failure | The specific error, the full log behind it, and "Erneut versuchen" |
| On success | The project detail screen, with a one-line note: "Bereit. Für Builds wird eventuell eine Ubuntu-Laufzeit gebraucht." |

**Cancelling a partial clone is not a rule violation.** Removing a half-downloaded clone the user just cancelled is cleaning up our own incomplete work, not deleting the user's. The transparency log records it as `CLONE_CANCELLED`, not as a deletion. This distinction matters and is written down so a future reader does not "fix" it into a hard block.

## Route B — Mit URL

| Field | Behaviour |
|---|---|
| URL | Pasted or typed. Accepts `git@github.com:owner/repo.git` and `https://github.com/owner/repo.git` |
| Detection | On paste, the owner and name are extracted and shown as a chip, so the user can see what was understood before cloning |
| Validation | Runs on blur: the URL parses, the repository exists, it is reachable with the current credentials |
| Credentials | A segmented control: "Mit meinem GitHub-Konto" (default) or "Öffentlich lesen" — the latter only for a public repository, which this app does not work with, so it is **absent** |
| Destination | An optional subfolder field, so several repositories can live under one project. Empty means the repository's own name. |

There is no visibility field, no "create new repository" option, and no way to end up with a public repository through this screen. The sheet states: "Das Repository wird unverändert geklont. Sichtbarkeit kann nur auf GitHub selbst geändert werden."

## Route C — Lokaler Ordner

| Option | Behaviour |
|---|---|
| Choose a folder | The system document picker, scoped to the user's chosen directories and the app's own storage |
| Create a new folder | The picker with a name field. Creates an empty directory, which can then be initialised as a repository. |
| App storage | A shortcut into the app's own `filesDir/cca/projects/`, for somebody who wants a self-contained project on the device. |
| Recent folders | The last five, as chips |

After choosing, a **project inspection** runs and its result is shown before anything is saved:

```
Projekt erkannt
✓ Git-Repository        ja
✓ Hauptzweig           main
✓ Prüfbefehle          ./gradlew test
✓ Werkzeuge            Gradle 8.7, Java 21
⚠ Großes Repository    1.2 GB, das Klonen dauert evtl. 20 Minuten
⚠ Kein Laufzeitprofil  Builds brauchen Ubuntu (proot)
```

Two things here are worth more than they look. **The app says what it found before it starts**, so a wrong folder is caught in the first second rather than the twentieth minute. And **it says what is missing**, so a project that needs a toolchain does not fail 15 minutes in.

The primary action is "Als Projekt hinzufügen". For a detected Git repository, it is "Klonen"; for a plain folder, it is "Ordner verwenden". For a plain folder, a secondary sheet offers "Als Git-Repository einrichten", with:

> "Dadurch wird ein lokales Git-Repository erstellt und der erste Commit angelegt. Es wird nichts hochgeladen. Der Befehl lautet: `git init && git add -A && git commit -m "Projekt von claude-code-android"`. Es wird nichts gelöscht."

The exact command is shown, because a person who understands it can verify what will happen, and one who does not can at least see that nothing is being uploaded and nothing is being removed.

## States

| State | Behaviour |
|---|---|
| Inspected, all good | The green rows, primary action enabled |
| Inspected, missing toolchain | A `warning` row, with two actions: "Trotzdem hinzufügen" (secondary) and "Ubuntu-Laufzeit einrichten" (primary, because it is almost always the right answer) |
| Not a repository | A `warning` row and the "Git-Repository einrichten" action, never automatic |
| Permission denied to read | "Der Ordner kann nicht gelesen werden." with a link to the app's storage permission |
| A folder with no write access | Refused up front with the reason. The app must never create a project it cannot write to. |
| Already a project | Routes to the existing project |
| The name is taken | "Ein Projekt namens {name} gibt es schon." with "Trotzdem hinzufügen" and the existing project as a link. Two projects with the same name is confusing but not harmful, so it is allowed with a warning. |

## Accessibility

| Requirement | Implementation |
|---|---|
| Route selection | Three list items, each naming its route and what it does |
| The repository list | A list; each row announces name, language, branch, and last push |
| The inspection panel | A list of checks, each announcing pass, warning, or fail with the detail |
| The progress steps | A live region, updated per step, not per percent. A screen reader announcing "68 percent" thirty times is unusable. |
| The confirm for `git init` | Focus moves to it, and the exact command is read before the action |
| Font scale 1.3 | The inspection panel wraps; the command block scrolls horizontally |
| Target | Every row 48 dp minimum; the clone button 48 dp even though the row is 64 dp |

## Testing

| Test | Type |
|---|---|
| `AddProjectRoutes` | Screenshot — the three routes |
| `AddFromGitHubList` | E2E — connect, list, clone, land on the project |
| `AddFromGitHubHidesPublic` | UI — asserts a public repository is not offered |
| `AddFromGitHubRateLimit` | Screenshot with the reset time |
| `AddCloneProgress` | Screenshot mid-clone with per-step states and a cancel button |
| `AddCloneCancel` | E2E — the partial clone is removed, the log records `CLONE_CANCELLED`, and nothing else is touched |
| `AddByURL` | E2E — both URL forms parse, the extracted chip is correct, the clone succeeds |
| `AddLocalInspection` | E2E — a project with a missing toolchain shows the warning with both actions before saving |
| `AddLocalNotARepo` | UI — the `git init` confirm shows the exact command and states nothing is uploaded or deleted |
| `AddLocalPermissionDenied` | Screenshot with the reason and the settings link |
| `AddDuplicateName` | Screenshot with the warning and both actions |
| `AddNoVisibilityControl` | UI — asserts no visibility field exists on any route |
| `AddFontScale` | Screenshot at 1.3 |
