# Screen 07 — Project list

Every project, with its state at a glance, and one place to add one.

## Purpose

Answer "what am I working on, and is any of it running?" in one screen, and be the entry point for adding a project.

## Structure

### Top bar

Left: title "Projekte". Right: a search field toggle and a `+` button.

### The active-run banner

Identical to the chat list's, and shown here too. A run belongs to a project, and this is where a user lands looking for it.

### The list

Cards, 1 column compact, 2 medium, 3 expanded. Sorted: active runs first, then by `lastRunAt` descending.

#### Project card

| Row | Content |
|---|---|
| Leading (40 dp) | A repository glyph for a GitHub project, a folder glyph for a local one, a server glyph for a remote one |
| Title | `titleLarge`, the project name, one line |
| Subtitle | `bodySmall`, `textSecondary`: "privat · {owner}/{name} · main" or the absolute path for a local project |
| Privacy | A `lock` glyph, always. A private repository says so, permanently, on the card. |
| Autonomy | The level as a chip, always visible. This is the project's most important setting and it belongs on the card. |
| Status | The last run's `StatusBadge` |
| Last run | "vor 2 Stunden" in `labelSmall` |
| Cost | "12,40 $ gesamt" in `labelSmall`, tappable to the cost sheet |
| Open PRs | "{n} Pull Requests" when non-zero, tappable, with the GitHub glyph |
| Active run | A running indicator bar at the card's bottom edge, in `accentText` at 30 % |

Tapping opens the project detail. Long press opens a sheet: Öffnen, Einstellungen, Verlauf, Archivieren, Exportieren. **No delete**, consistent with every other list in the app.

### The add flow

Tapping `+` opens a sheet with three routes:

| Route | Description |
|---|---|
| Von GitHub | The connected account's private repositories, searchable, each with a clone button. Already-cloned ones are marked and link to the project. |
| Mit URL | A text field for a repository URL, plus a visibility indicator fixed on "privat" and not editable. |
| Lokaler Ordner | The system folder picker, scoped to app-private storage and the user's chosen directories. |

**The visibility control does not exist.** A public option that cannot be selected is a lie in the interface; the app simply has no such option, and the sheet says "Das Repository wird privat angelegt". See `05-features/project-lifecycle.md`.

## States

| State | Behaviour |
|---|---|
| No projects | `empty-projects.svg`, "Noch keine Projekte", one sentence, and a primary "Projekt hinzufügen" |
| Loading | Nothing. A local query. |
| Searching | Filters on name, path, and owner |
| Search, no results | "Kein Projekt passt zu „{query}“" |
| GitHub not connected | The "Von GitHub" route shows a connect prompt instead of a repository list, and local routes stay fully available |
| GitHub rate limited | "GitHub-Limit erreicht, wieder da um 14:32" with the time, not a spinner |
| A project with a failed run | The card's badge is `danger`, and a one-line summary of the failure. The failure is visible from the list, because a failure the user has to open a project to discover is a failure they will not discover. |
| A project with an unverified last run | `warning` badge. Visible here too, for the same reason. |
| Offline | Local and cloned projects list normally. GitHub routes say "GitHub ist offline". |
| Storage pressure | If free space is under 500 MB, a notice at the top with the exact figure. The app never deletes to make room. |

## The list versus the chat list

Both list work, and they overlap deliberately. The difference is the axis:

| | Chat list | Project list |
|---|---|---|
| One row is | A conversation | A codebase |
| Sorted by | Recency | Active runs, then recency |
| The primary question | "What was I doing?" | "What is this project, and is it OK?" |
| Shows cost | Per conversation | Cumulative per project |
| Shows autonomy | Only if non-default | Always |

## Accessibility

| Requirement | Implementation |
|---|---|
| A card | One merged node: "{name}, privates Repository, {owner}/{name}, Hauptzweig main, Rechte ASK_RISKY, letzter Lauf vor 2 Stunden, bestanden" |
| Autonomy | In the accessible name, not a separate node, because it is the consequence of choosing the project |
| The active banner | A live region |
| The grid | At medium and expanded, a real grid with correct row and column semantics, so TalkBack announces "Element 2 von 6" rather than "Element 2" |
| The add sheet | Focus moves in; the background is marked; the three routes are a list |
| Font scale 1.3 | The card's rows wrap; the metadata row moves below the title rather than truncating |
| Target | The whole card, 48 dp minimum, with a visible press scale so an accidental scroll-release does not open a project |

## Testing

| Test | Type |
|---|---|
| `ProjectListEmpty` | Screenshot, both themes |
| `ProjectListMixed` | Screenshot with a GitHub project, a local project, a remote project, a running one, a failed one, an unverified one |
| `ProjectListActiveFirst` | UI — a running project sorts first, and the order updates when the run finishes |
| `ProjectListGrid` | Screenshot at 1, 2, and 3 columns |
| `ProjectListAddGitHub` | E2E — connect, list private repositories, clone, land on the project |
| `ProjectListAddNoVisibilityToggle` | UI — asserts no visibility control exists anywhere in the add flow |
| `ProjectListLongPress` | UI — the sheet has five actions and no delete |
| `ProjectListRateLimited` | Screenshot with the reset time |
| `ProjectListStorageWarning` | Screenshot under 500 MB free |
| `ProjectListFontScale` | Screenshot at 1.3 |
| `ProjectListAccessibilityMerge` | UI — the card is a single merged node with the full description |
