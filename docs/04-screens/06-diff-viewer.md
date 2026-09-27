# Screen 06 — Diff viewer

Every change, reviewable, with a per-hunk decision and an honest account of what reverting does.

## Purpose

Let a user judge the work without reading code line by line, while making a proper review possible for someone who wants to do exactly that. And never present a revert as a deletion.

## Structure

```
┌─────────────────────────────────────┐
│ ‹  3 Dateien          ⊕ Alle        │  top bar
├─────────────────────────────────────┤
│  build.gradle.kts        +12 −4     │  file header, sticky
│  ▸ src/Main.kt            +48 −31   │
│  ▾ README.md              +6 −0     │  expanded
├─────────────────────────────────────┤
│  @@ −18,7 +18,9 @@                  │  hunk header
│   dependencies {                    │
│ −  implementation "…:1.1.0"          │  removed
│ +  implementation "…:1.2.0"          │  added
│ +  testImplementation(…) { … }       │  added
│  …                                   │
│  [ Rückgängig ]  [ Übernehmen ]      │  per-hunk actions
├─────────────────────────────────────┤
│  [ 1 von 6 hunks · 2 offen ]  [Diff] │  sticky bottom bar
└─────────────────────────────────────┘
```

## The file list

A sticky header strip below the top bar, one row per changed file, horizontally scrollable when there are many.

| Element | Content |
|---|---|
| Name | Path in `monoSmall`, truncated from the left so the filename stays visible: `…/data/repo/ProjectRepository.kt` |
| Counts | `+12 −4` in `labelSmall`; green and red, plus the `+` and `−` signs so colour is never alone |
| State glyph | A `warning` marker while a hunk is pending, `success` once all hunks are accepted, `danger` once a hunk is reverted |
| Order | Alphabetical by path. Grouping by directory is a second mode, in the overflow menu, for projects with hundreds of changed files. |

Tapping a file expands it and scrolls to its first pending hunk.

## Hunk display

| Property | Rule |
|---|---|
| Default | Unified. Split is available at medium and expanded widths, on by default at expanded. |
| Context lines | 3, expandable per hunk to the full file |
| Hunk header | `@@ −a,b +c,d @@` in mono, on `diffHunkHeader` |
| Gutter | Fixed 32 dp, holding the `+` / `−` / space sign, the old line number, and the new line number |
| Added | `diffAddBackground`, `+` in `diffAddMarker` |
| Removed | `diffDelBackground`, `−` in `diffDelMarker` |
| Unchanged | `background`, numbers in `textTertiary` |
| Syntax | Highlighted with the token set from `color-and-contrast.md`. A language with no highlighter renders plain mono, which is honest. |
| Very long lines | Truncated with a `…` in the gutter and a "Zeige ganze Zeile" affordance. **Never silently truncated.** |
| Tabs in code | Rendered as 4 spaces, visibly, with a warning glyph on the file header, so a whitespace-only change is not invisible. |
| Binary | One line: "Binäre Datei · {size}" with a button to open it externally. Never an empty box. |
| No newline at end of file | The `\ No newline at end of file` marker is shown, because a diff that hides it makes a real change invisible. |

## The decision model

Every hunk has a state: `PENDING`, `ACCEPTED`, `REVERTED`.

| Action | Effect |
|---|---|
| Per hunk: Übernehmen | Marks the hunk accepted. Does **not** write anything. The decision is recorded and applied at commit time. |
| Per hunk: Rückgängig | Marks the hunk reverted, in the inverse direction |
| Per file: Alle übernehmen / Alle rückgängig | Applies to every pending hunk in the file, after a confirm showing the count |
| Top bar: Alle | Same for the whole run, after a confirm with the total count and the file count |

**Decisions are not applied immediately.** They are recorded, and the diff shown is a preview. Nothing touches the working tree until the user confirms the whole set with "Änderungen anwenden", which is the sticky bottom bar's primary action. This matters: a per-hunk button that writes on tap is a per-hunk button that can be mis-tapped.

### What reverting actually does

The wording is deliberate and is not negotiable:

| Label | Never |
|---|---|
| "Rückgängig" (undo) | "Verwerfen" (discard) |
| "Hunk wird beim Anwenden zurückgenommen" | "Änderung gelöscht" |
| The confirm sheet says: "Der Originalzustand bleibt in der Git-Historie erhalten." | Any wording implying a file is removed |

Reverting a hunk produces an inverse patch. A reverted `Edit` restores the previous content and records a `revertCommitId` in the database, so a revert is itself traceable. **A revert never deletes a file, and never removes history.** This is the never-delete rule expressed as a UI affordance, and the wording carries it.

For an `ADDED` file, reverting is a decision not to include it. The file is not deleted from the working tree by the app; the commit simply does not add it, and the file remains as an untracked file that the user can see in the file tree. The confirm sheet says exactly that: "Die Datei bleibt im Ordner, wird aber nicht committet."

## The bottom bar

Sticky, always present when at least one hunk is pending.

| Element | Content |
|---|---|
| Left | "{n} von {m} Hunks offen" |
| Primary | "Änderungen anwenden" |
| Secondary | "Diff / Seite an Seite" toggle, at medium and up |
| Tertiary | Overflow: Springe zum nächsten offenen Hunk |

After applying, the bar becomes a summary: "3 Dateien · +66 −35 · 2 Hunks zurückgenommen" with a link to the diff and an "Rückgängig machen" action that reverts the *application* of the decisions — again without deleting anything, by producing the inverse patch.

## States

| State | Behaviour |
|---|---|
| Loading | Nothing. Diffs come from a local git call and appear in under 100 ms. |
| No changes | `empty-projects.svg`, "Keine Änderungen", and the git command output that proves it. A run that changed nothing shows the log, not a blank screen. |
| One hunk | Shown expanded by default. There is nothing to fold. |
| Many hunks | All collapsed except the first pending one, which is expanded. |
| Binary file only | A single line per file, as above. |
| Very large diff, over 5000 lines | Virtualised. A note: "Große Änderung · 8.412 Zeilen". No truncation; the whole thing is browsable. |
| File was deleted outside the app | Shown as a deletion, with a note that the app did not do it. The hard block applies to the app's own actions, and the transparency log says so. |
| File renamed | The header shows `oldPath → newPath` |
| Conflict markers in a file | A banner: "Diese Datei enthält Konfliktmarkierungen", with the line numbers, and a link to the terminal |
| A hunk cannot be reverted cleanly | The button is disabled, and the reason names the line: "Hunk 4 überschneidet sich mit einem anderen. Bitte im Terminal lösen." |
| File too large to highlight | Highlighted with a line cap and a note. Rendering 200.000 lines of syntax highlighting on a phone is not a feature. |

## The verification connection

The diff viewer's top bar carries a link to the verification panel, and the relationship is stated:

> Geprüft mit 3 Prüfbefehlen. Diese Änderungen sind seit dem letzten erfolgreichen Lauf entstanden.

Or, when verification has not run:

> Noch nicht geprüft. Diese Änderungen sind ungeprüft.

A user looking at a diff needs to know whether what they are looking at has been checked. Putting that in the diff view, not only in the run summary, is the difference between a review and a reading.

## Accessibility

| Requirement | Implementation |
|---|---|
| The file list | A list of controls. Each announces path, added count, removed count, and decision state. |
| Hunks | Announced as a group: "Hunk 3 von 6, Zeilen 18 bis 26, 2 hinzugefügt, 0 entfernt, offen" |
| A hunk's content | Available on demand, not announced automatically. A screen reader user gets the counts, then asks for the content. Announcing a 60-line diff unprompted is unusable. |
| Decisions | "Hunk 3 übernehmen", with the resulting state announced on confirmation |
| The bottom bar | "3 von 6 Hunks offen. Aktion: Änderungen anwenden" |
| The "no newline" marker | Announced, because it is a real change |
| Font scale | Line numbers and gutter stay fixed; the code area shrinks. A diff never reflows, because reflowing code misrepresents it. |
| The verify link | Announced with its state, not only its label |
| Target | Every hunk action is 48 dp, even though the button looks like 36 |

## Testing

| Test | Type |
|---|---|
| `DiffUnified` | Screenshot with additions, removals, context, multiple hunks, multiple files, both themes |
| `DiffSplit` | Screenshot at medium and expanded, and a UI test asserting the toggle |
| `DiffNoChanges` | Screenshot with the empty state and the git output |
| `DiffBinary` | Screenshot — the binary line, never an empty box |
| `DiffLongLine` | Screenshot with a 400-character line, asserting the `…` and the expand affordance |
| `DiffWhitespace` | Screenshot with a tab-indentation-only change, asserting the warning glyph is visible |
| `DiffDecideWithoutWriting` | E2E — tapping per-hunk decisions changes the UI and the database and **not** the working tree |
| `DiffApply` | E2E — applying produces the expected file content, verified against `git diff` |
| `DiffRevertWording` | UI — asserts the labels are "Rückgängig", that "Verwerfen" and "gelöscht" appear nowhere, and that the confirm sheet states history is preserved |
| `DiffRevertAddedFile` | E2E — reverting an added file leaves it on disk as untracked and out of the commit |
| `DiffVerifyLink` | UI — the verified and unverified states are distinguished and announced |
| `DiffLongPressSelect` | UI — copy a range of lines |
| `DiffFontScale` | Screenshot at 1.3 |
| `DiffHuge` | Performance — 50.000 lines remain scrollable |
