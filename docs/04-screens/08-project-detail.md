# Screen 08 — Project detail

One project: what it is, how it is configured, what has happened to it, and what is happening now.

## Purpose

Everything about a project in one scrollable screen, with the settings that matter most — autonomy, model, verification — one tap away and never more than one screen deep.

## Structure

### Top bar

Back, the project name in `titleMedium`, and an overflow menu: Einstellungen, Verlauf, Terminal, Exportieren, Archivieren.

### Section 1 — Status

The most important part, at the top, always present.

| Row | Content |
|---|---|
| Status badge | The last run's outcome, or "Noch kein Lauf" if there has been none |
| Last run | "vor 2 Stunden · 3 Dateien · +66 −35" |
| Verification | "3 Prüfbefehle · zuletzt bestanden vor 2 Stunden", or a `warning` "Keine Prüfbefehle hinterlegt" with a setup action |
| Cost | "12,40 $ gesamt · 0,31 $ heute" |
| Branch | The current branch in mono, with a copy action |
| Last commit | The 7-character hash and the subject, both mono, tapping copies the hash |
| Open PRs | List of PRs, each with number, title, state, and checks passing or failing |

When a run is active, this section is replaced at the top by a live status row: the mark's state, the current tool, the elapsed time, and a "Im Chat ansehen" link.

### Section 2 — Schnellaktionen

Four actions, `radiusMedium`, `surface` fill, in a 2 × 2 grid on compact and 4 across on expanded:

| Action | Behaviour |
|---|---|
| Neuer Chat | Opens a new chat for this project, with the project's settings applied |
| Verlauf | The run history for this project |
| Terminal | Opens the project's shell |
| Verifizieren | Runs the verification commands now, without an agent. A run of the project's own checks with no model call. |

`Verifizieren` exists because a user who just changed something by hand wants to know whether it still builds, and should not have to ask a model to find out.

### Section 3 — Einstellungen

Each row is a tappable setting, showing its current value.

| Row | Value shown | Behaviour |
|---|---|---|
| Rechte | The level, with a one-line consequence | Opens the four-level sheet |
| Modell | The model name and the provider | Opens the model sheet, with the provider's models |
| Prüfbefehle | "{n} hinterlegt" or "Keine" with a `warning` tint | Opens the verification command editor |
| Wiederholungen | "3 Versuche", "10", "50", "Unbegrenzt" | A segmented control |
| Werkzeuge | "{n} erlaubt, {m} gesperrt" | The allow and deny lists, with the hard blocks shown as permanently locked and non-editable |
| Auslagerung | "Nie", "Wenn schwer", "Immer" plus the chosen runner | Opens the offload policy sheet |
| Verzweigungspräfix | "task/" | Editable, with a live preview of the resulting branch name |
| Kostenwarnung | "Aus" or "{n} $" | A soft advisory threshold. The sheet says: "Eine Warnung, kein Stopp." |

**The hard blocks appear in the Werkzeuge row as locked entries with a lock glyph**, and the sheet explains once: "Löschen, Bezahlen, Veröffentlichen und Hochladen auf den Hauptzweig sind immer gesperrt. Das lässt sich nicht ändern." Showing them as settings that cannot be changed is honest; hiding them would suggest they are merely off.

### Section 4 — Fähigkeiten

What this project can do on this device. Probed, not assumed.

| Row | Source |
|---|---|
| Laufzeitprofil | "Native", "Ubuntu (proot)", or "Nicht eingerichtet" |
| Nachschubserver | The assigned runner, or "Dieses Gerät" |
| Verfügbare Werkzeuge | A list of detected toolchains: "Gradle 8.7 ✓, Node 20 ✓, Python — fehlt" |
| Werkzeuge installieren | Links to the terminal to install what is missing, on a project that allows it |

**A capability row that says "fehlt" is a feature.** The alternative is a run that fails twenty minutes in because a toolchain was absent. Failing at minute one with "Python fehlt, hier ist der Befehl" is a better experience by a wide margin.

### Section 5 — Skills

The skills scoped to this project, plus the global ones that apply, each with a toggle. A link to install one for this project.

### Section 6 — Danger zone

One row: "Lokale Daten dieses Projekts löschen".

The only delete in the app. Three confirmations, each stating exactly what happens and what does not:

| Confirmation | Text |
|---|---|
| 1 | "Lokale Daten löschen? Der Ordner auf diesem Gerät und alle zugehörigen Unterhaltungen werden entfernt." |
| 2 | "Nicht gelöscht werden: das GitHub-Repository, alle Zweige, die Git-Historie, alle hochgeladenen Commits." |
| 3 | "Der Vorgang wird im Aktivitätsprotokoll vermerkt. Er kann nicht rückgängig gemacht werden." |

Then a typed confirmation: the project name. This is the one irreversible action in the app, and it deserves three steps.

## States

| State | Behaviour |
|---|---|
| No run yet | Section 1 says "Noch kein Lauf" and offers "Ersten Auftrag starten" |
| A run is active | The live status row replaces the last-run summary. Everything else stays usable. |
| A run failed | The failure's first line is in Section 1, with "Verlauf" and "Erneut versuchen" |
| No verification commands | A `warning` row in Section 1 and in Section 3, with "Einrichten" leading to a sheet that detects the project's toolchain and proposes commands |
| Local project, not a repository | Section 1 says so, and offers "Git-Repository einrichten" in a confirm sheet. Never done silently. |
| Detached head | The branch row says "Losgelöst bei {hash}" with "Neuen Zweig erstellen" |
| Dirty worktree before a run | A notice: "Es gibt ungespeicherte Änderungen. Sie werden auf einem eigenen Zweig gesichert, nicht verworfen." |
| A runner is assigned but offline | Section 4 names it and its state, with "Erneut prüfen" |
| The project was archived | A banner: "Dieses Projekt ist archiviert." with "Wiederherstellen" |

## The dirty-worktree rule

Worth stating separately because it is a hard rule meeting a common situation.

The app never stashes, never discards, and never resolves a conflict by throwing work away. When a run starts on a project with uncommitted changes:

1. A branch is created from the current state, so the uncommitted work comes along.
2. The run proceeds on that branch.
3. The uncommitted changes remain uncommitted, and appear in the diff viewer's "nicht committet" section.
4. The user decides what to do with them, and the app offers: commit them, keep them uncommitted, or look at the diff.

"Verwerfen" does not appear in that sheet. Discarding a person's unsaved work is a deletion, and there is no path to it.

## Accessibility

| Requirement | Implementation |
|---|---|
| Section 1 | A live region for the status. Updated on state change only. |
| Settings rows | "Rechte: Auf riskante Schritte fragen. Ändern." — the value first, then the action |
| Hard blocks | "Löschen: immer gesperrt. Ändern nicht möglich." Announced with the locked state |
| Danger zone | Not a live region. Confirmed three times, so a screen reader user has three chances to cancel. |
| Capability rows | "Gradle 8.7, verfügbar" / "Python, fehlt" |
| The active row | "Läuft, liest build.gradle.kts, seit 2 Minuten. Im Chat ansehen." |
| Font scale 1.3 | The 2 × 2 grid becomes a single column of full-width rows |
| Target | Every settings row is at least 56 dp, larger than the minimum, because they are the most-tapped controls in the app |

## Testing

| Test | Type |
|---|---|
| `ProjectDetailFresh` | Screenshot — no run, no verification, all states visible |
| `ProjectDetailActive` | Screenshot during a run, with the live row |
| `ProjectDetailUnverified` | Screenshot — the `warning` appears in both Section 1 and Section 3 |
| `ProjectDetailSettings` | UI — every row opens the correct sheet with the current value |
| `ProjectDetailHardBlocksLocked` | UI — asserts the hard blocks are present, locked, and not editable |
| `ProjectDetailCapabilities` | UI — probed values shown; a missing tool yields "fehlt" with an install link, not a failure later |
| `ProjectDetailVerifyDirect` | E2E — "Verifizieren" runs the commands with no model call and reports per command |
| `ProjectDetailDirtyWorktree` | E2E — uncommitted changes survive a run, appear in the diff, and no "Verwerfen" option exists |
| `ProjectDetailDeleteFlow` | E2E — three confirmations, a typed project name, and after it: local data gone, the remote repository, branches, and history intact |
| `ProjectDetailDeleteLogged` | UI — the erase appears in the activity log |
| `ProjectDetailFontScale` | Screenshot at 1.3 |
| `ProjectDetailNoRunYet` | Screenshot with the first-run prompt |
