# Screen 10 — Skills browser

Everything installed, plus everything available, in one searchable list.

## Purpose

Make skills a first-class, manageable thing rather than a folder of files the user has to know about. Answer "what is shaping my work, and can I turn it off" in one screen.

## Structure

### Top bar

Title "Skills", a search toggle, and a `+` button whose menu offers: Aus URL installieren, Mit Claude erstellen, Aus Marktplatz durchsuchen.

### Tabs

Two tabs, a 2 dp accent indicator.

| Tab | Content |
|---|---|
| Installiert | Everything installed, grouped by scope |
| Marktplatz | The bundled index plus the optional remote index |

### Tab 1 — Installiert

A list, not a grid. A skill is a name, a description, a scope, and a state; a grid would give the description no room to be read, and a description is the whole point.

| Row | Content |
|---|---|
| Leading (40 dp) | The puzzle glyph, or a source glyph for AI-generated and bundled |
| Title | `titleMedium`, the skill name |
| Description | `bodySmall`, `textSecondary`, two lines. A skill's description is what tells a user whether they want it, so it is never truncated to one line. |
| Scope chip | "Global" or a project name, `labelSmall` |
| State | A switch, if enabled. `disabled` skills are dimmed to `textTertiary` but stay in the list — a disabled skill is still installed, and hiding it would make it impossible to find. |
| Validation | A `danger` glyph and the reason, for a skill that failed validation |
| Update | A `success` chip "Aktualisierung verfügbar" when the source has a newer version |
| Version | `labelSmall`, `textTertiary`, from the source when available |

| Group | Contents |
|---|---|
| Global | Skills installed for every project |
| Projekte | One group per project, showing the project name and count |
| Beschädigt | Invalid skills, at the top of the list, with a repair action each |

**Invalid skills are grouped at the top, not hidden.** A skill that silently does not work is worse than one that says it is broken, and someone who installed six skills needs to know which one is failing.

| Interaction | Behaviour |
|---|---|
| Tap | The skill detail sheet |
| Toggle the switch | Enable or disable, immediately, with a haptic. Nothing is written to the project; the registry entry flips. |
| Long press | Skill detail, Bearbeiten, Duplizieren, Deinstallieren. **No delete of the source** — deinstalling removes our installed copy, and the sheet says so. |

### Tab 2 — Marktplatz

A searchable list from a bundled JSON index, plus a remote index if one is configured.

| Row | Content |
|---|---|
| Title and one-line description | |
| Source | Author or organisation, `labelSmall` |
| Badge | "In App" for bundled entries, otherwise a network glyph |
| Action | "Installieren" |

| State | Behaviour |
|---|---|
| No remote index configured | The bundled entries only, with a note: "Du kannst eigene Quellen hinzufügen." |
| A remote index is configured | Merged with the bundled entries, marked with their source, with the remote index's fetch date shown |
| The index is unreachable | The bundled entries, plus a `warning` line: "Marktplatz-Index nicht erreichbar · vom {date}" |
| A skill is already installed | "Installiert" and a check, tappable to the detail |
| Installation in progress | The button becomes an indicator; the row is not re-laid-out |

## The install flow

Tapping "Installieren" — from the marketplace or from the URL route — **never writes immediately.**

### Step 1 — Fetch and parse

| State | Behaviour |
|---|---|
| Fetching | A progress row with the repository name and a cancel |
| Not a skill | "In dieser Quelle wurde kein Skill gefunden." with what was looked for: `SKILL.md`, `.claude/skills/*`, a plugin manifest |
| Broken | Every validation problem listed at once, not one per attempt. "Fehlender Frontmatter", "name ist ungültig: darf nur Kleinbuchstaben, Zahlen und Bindestriche enthalten", "description fehlt" |
| Name conflict | "Ein anderer Skill heißt {name}." Refusing is correct: a silent overwrite would change the behaviour of existing work with no record. The user can install it under a different name or uninstall the old one. |

### Step 2 — Preview

The **whole** skill, scrollable, rendered as it will be used. The frontmatter is shown as metadata, not as raw YAML at the top of the prose.

| Element | Content |
|---|---|
| Name and description | From the frontmatter |
| Version and source | With the commit or tag, so a specific version is identifiable |
| Files | A list of every file to be written, with its path and size |
| Content | The full `SKILL.md` text, formatted |
| Scope choice | Global or a project, as a segmented control, defaulting to the current project |
| Conflict warnings | Any file that already exists with different content, per file, with a choice: "Ersetzen" or "Behalten" |

| Action | Primary: "Installieren" (with the file count: "Installieren · 3 Dateien") |
| Secondary | "Abbrechen" |

**The conflict choice is per file and is recorded in the log.** Overwriting somebody's edited skill silently is the kind of thing that costs an afternoon.

### Step 3 — Installed

The skill appears in the list, the row animates in over `standard`, and a snackbar confirms with an "Rückgängig" action that deinstalls it again — because a newly installed skill that turns out to be unwanted should cost one tap to undo, not a trip through the deinstall flow.

## Deinstalling

| Step | Text |
|---|---|
| Confirm | "Skill deinstallieren? Die Dateien werden aus diesem Projekt entfernt. Die Quelle ist nicht betroffen." |
| In use | "Dieser Skill ist in 3 aktiven Skills enthalten." Nested skills are listed, and deinstalling the parent offers to remove the children. |
| History | "Die Installationshistorie bleibt im Protokoll erhalten." |

The source on GitHub is never touched. The app removes its own installed copy, which is the same distinction as the local-project erase in the project screen: we clean up what we put there, and we never touch the user's work.

## State changes while running

| Situation | Behaviour |
|---|---|
| A skill is disabled during a run | The change applies to the next run. The current run keeps the skills it started with, and the snackbar says so: "Gilt ab dem nächsten Lauf." |
| A skill is installed during a run | Same. A skill added mid-run does not change the run in progress. |
| A skill is removed during a run | Same, and the run's log records which skills were active when it started |

This matters for reproducibility. A run's log lists the skills that were active at its start, so a result can be explained later. See `05-features/transparency-log.md`.

## Accessibility

| Requirement | Implementation |
|---|---|
| Tabs | Real tabs with the state announced |
| A skill row | One merged node: "{name}. {description}. Gilt für alle Projekte. Eingeschaltet. Schalter." |
| Invalid skills | The group header and each reason are announced; the row is focusable and the repair action is reachable |
| The preview | Scrollable text, announced as a document |
| The install progress | A live region, updated on state change only |
| The conflict choice | Per file, each announced with the path and the existing size, so "which file" is unambiguous |
| Toggles | Labelled with the skill name and the scope, not just "switch" |
| Font scale 1.3 | The description goes to three lines; the sheet scrolls |
| Target | Rows 72 dp minimum; the switch 48 dp |

## Testing

| Test | Type |
|---|---|
| `SkillsEmpty` | Screenshot, both tabs, both themes |
| `SkillsInstalled` | Screenshot with global and project skills, one disabled, one needing an update, one invalid |
| `SkillsInvalidGroupedTop` | UI — invalid skills appear in their own group above the valid ones |
| `SkillsToggle` | E2E — toggling changes the registry and the next run's skill list, and the current run is unaffected |
| `SkillsMarketplace` | Screenshot with bundled and remote entries marked by source |
| `SkillsMarketplaceOffline` | Screenshot with the bundled entries and the date of the last successful fetch |
| `SkillsInstallFromURL` | E2E — paste a URL, preview the whole skill and every file, confirm, installed |
| `SkillsInstallInvalid` | Screenshot — all validation problems listed at once |
| `SkillsInstallNameConflict` | Screenshot — refused, with the two ways forward |
| `SkillsInstallFileConflict` | Screenshot — per-file choice, recorded in the log |
| `SkillsUninstall` | E2E — the installed copy is removed, the source is untouched, the log retains the entry, and "Rückgängig" works after install |
| `SkillsFontScale` | Screenshot at 1.3 |
