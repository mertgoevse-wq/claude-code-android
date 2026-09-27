# Screen 11 — Skill editor and creator

Two routes into the same editor: edit an existing skill, or have Claude write one from a description.

## Purpose

Let someone create a reusable procedure for Claude Code without learning a file format, and let someone who already knows the format edit it precisely. Both end at the same place: a validated skill, previewed before it is saved.

## Layout

```
┌─────────────────────────────────────┐
│ ‹  Skill bearbeiten           ✓    │  save, disabled while invalid
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐  │
│  │ Baue einen Android-Dialog …   │  │  prompt field (creator)
│  └───────────────────────────────┘  │
│  ⊕ Claude erstellen                 │
├─────────────────────────────────────┤
│  Name            [android-dialog  ] │
│  Beschreibung   [Dialog mit …     ] │
│  Gilt für        [Dieses Projekt ▾] │
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐  │
│  │ ---                           │  │  content editor, mono
│  │ name: android-dialog          │  │
│  │ description: …                │  │
│  │ ---                           │  │
│  │                              │  │
│  │ Wenn du einen Dialog …        │  │
│  └───────────────────────────────┘  │
├─────────────────────────────────────┤
│  ✓ Gültig · 214 Wörter · ~380 Tokens │  validation bar
└─────────────────────────────────────┘
```

## The two routes

### Route A — Edit an existing skill

Opened from a skill's detail sheet. The name field is editable but shows a warning if it no longer matches the directory name, because the engine uses both and a mismatch is a confusing failure:

> "Der Name im Inhalt ({name}) weicht vom Ordnernamen ({dir}) ab. Beide sollten gleich sein."

The save button writes only after a successful validation. Save is disabled while the content is invalid, and the validation bar names every problem.

### Route B — Create with Claude

The prompt field at the top is the point of this route. A person types what they want the skill to do, in ordinary language:

| Example prompt | What gets produced |
|---|---|
| "Bau immer einen Compose-Dialog mit Bestätigung" | A skill that mandates a confirmation dialog pattern |
| "Wenn ich 'test' sage, schreib Unit-Tests" | A skill triggered by a keyword |
| "Prüfe bei jeder Änderung, ob die Typen stimmen" | A skill adding a verification step |

Tapping "Mit Claude erstellen" opens a sheet with the three inputs that shape a skill well, each optional:

| Input | Placeholder | Why |
|---|---|---|
| Wann verwenden? | "z.B. wenn ich einen neuen Bildschirm baue" | The description drives when Claude reaches for the skill |
| Welche Schritte? | "z.B. erst planen, dann bauen, dann testen" | The body needs a procedure, not a wish |
| Was vermeiden? | "z.B. keine hardcodierten Farben" | Constraints are what make a skill worth keeping |

The generated skill arrives in the editor as a normal editable draft. **Nothing is saved automatically.** The user reads it, changes what is wrong, and saves. A generated skill the user has not read is a skill they cannot trust, and a skill nobody reads is one that quietly misbehaves.

### Generation states

| State | Behaviour |
|---|---|
| Generating | A streaming preview in the content editor, the save button disabled |
| Generated | The content is filled, the validation bar runs, the save button enables if valid |
| Generated but invalid | The content is filled and the problems are named. The user can edit and save once valid. |
| Failed | The specific error, and the editor is unchanged. A failed generation never destroys an existing draft. |
| Cancelled | The draft is kept. Cancelling mid-generation leaves the partial text, editable. |

Generation uses the same provider and key as a run, and its cost is shown afterwards. Generating a skill is a model call like any other, and it is not free.

## The content editor

A monospace text field, because a skill's content is markdown and someone editing it needs to see the frontmatter.

| Feature | Behaviour |
|---|---|
| Validation | Live, on a 400 ms debounce. Every problem at once, never one per attempt. |
| Frontmatter | Edited as text, not as a form. A form would fight the format and produce output people cannot paste elsewhere. |
| Word and token count | Live, at the bottom. A skill over about 2000 tokens is a skill that will not fit comfortably in a context; the count makes that visible before it matters. |
| Save | Disabled while invalid. Enabled the moment it is valid. |
| Save with a conflict | If the file changed on disk since it was opened, a sheet: "Die Datei wurde zwischenzeitlich geändert. Deine Version überschreiben oder neu laden?" Neither is silent. |
| Undo | The editor keeps an undo stack for the session. After saving, the previous content is in the activity log. |
| Export | Copies the full skill, frontmatter included, to the clipboard, so it can be pasted into a repository. |

## Validation rules

Shown inline at the exact line where the problem is, not only in a summary at the bottom.

| Rule | Message |
|---|---|
| Frontmatter present | "Es fehlt ein --- Block am Anfang mit name und description." |
| Name present | "name fehlt." |
| Name format | "name darf nur Kleinbuchstaben, Zahlen und Bindestriche enthalten, maximal 64 Zeichen." |
| Name matches the directory | "Der Name ({n}) weicht vom Ordnernamen ({d}) ab." |
| Description present | "description fehlt." |
| Description length | "description ist {n} Zeichen. 40 bis 300 sind sinnvoll, damit Claude den Skill richtig zuordnet." |
| Description not vague | "description beschreibt nur, dass der Skill existiert. Sie muss sagen, wann er verwendet wird." |
| Body present | "Der Inhalt ist leer. Was soll der Skill tun?" |
| Body has steps | "Keine Schritte gefunden. Eine nummerierte oder eine Aufzählung liest sich als Ablauf." |
| Line count | "Der Skill ist sehr lang ({n} Zeilen). Längere Skills werden seltener geladen." A warning, not an error. |
| Unknown frontmatter keys | "Unbekanntes Feld: {key}." A warning; the engine may add fields. |

## Saving

| Property | Rule |
|---|---|
| Location | `.claude/skills/{name}/SKILL.md` for project scope, `~/.claude/skills/{name}/SKILL.md` for global |
| Validation before writing | The save is refused if any error-level rule fails |
| Atomic | Written to a temporary file and renamed, so an interrupted save cannot leave a half-written skill |
| Log | The log records the change with a diff, because a skill is behaviour |
| After saving | A snackbar: "{name} gespeichert. Gilt ab dem nächsten Lauf." |

The "gilt ab dem nächsten Lauf" is not hedging. A run's skills are resolved when the run starts; changing them mid-run would make the run's behaviour unexplainable afterwards. See `10-skills-browser.md`.

## States

| State | Behaviour |
|---|---|
| New, nothing typed | The content editor shows a template with a comment explaining the format. Not "Lorem ipsum" — a real, minimal, working example. |
| Dirty, navigating back | A confirm: "Änderungen verwerfen?" The three buttons: "Speichern", "Verwerfen", "Abbrechen". "Verwerfen" here discards the user's own unsaved edits, which is why it is behind a confirm and never a default. |
| Disk changed underneath | The conflict sheet |
| Read-only skill | A bundled skill is editable but shows "Übernimm Änderungen beim nächsten Update zurück." So the user knows their edit is temporary. |
| Validation error | Inline at the line, plus in the bar, plus a count: "2 Probleme" |
| Saving | The save button shows an indicator and is disabled. Double-tap cannot save twice. |
| Save failed | The draft is kept in full, the error is specific, and the save can be retried |

## Accessibility

| Requirement | Implementation |
|---|---|
| Fields | Every field has a persistent label above it and is programmatically associated |
| The editor | A labelled multi-line text field. Announced as "Skill-Inhalt, mehrzeiliges Textfeld". |
| Validation | Each error is announced as it appears, with its line number. Errors are not a silent red border. |
| The validation bar | A polite live region, announcing the count and the first problem, so a screen reader user is not read every error at once |
| Save, disabled | "Speichern ist nicht möglich: {first problem}" |
| Generation | Announced on completion, not per token |
| The dirty-state confirm | Focus moves in, the three options are read, and the safest one is the last |
| Font scale 1.3 | The editor grows; the bar stays pinned |
| Target | Every control 48 dp; the tab-style scope selector 48 dp tall |

## Testing

| Test | Type |
|---|---|
| `SkillEditorEmpty` | Screenshot — the working template, not placeholder text |
| `SkillEditorValid` | Screenshot with a valid skill and a green bar |
| `SkillEditorErrors` | Screenshot with every validation rule violated, each inline at its line, all listed at once |
| `SkillEditorSaveDisabled` | UI — save is disabled while invalid, enabled the moment it is valid |
| `SkillEditorCreateWithClaude` | E2E — a prompt produces a valid, editable draft, and nothing is saved without confirmation |
| `SkillEditorGenerationCancelled` | E2E — cancelling mid-generation keeps the partial draft and does not destroy an existing one |
| `SkillEditorGenerationFailed` | UI — the editor is unchanged and the error is specific |
| `SkillEditorDiskConflict` | E2E — the file changes underneath, the conflict sheet appears, neither version is silently lost |
| `SkillEditorNameMismatch` | UI — the warning names both values |
| `SkillEditorDirtyBack` | UI — three options, and "Verwerfen" is not the default focus |
| `SkillEditorAtomicWrite` | E2E — a save interrupted mid-write leaves either the old or the new file, never a partial one |
| `SkillEditorExport` | UI — the clipboard receives the full skill including frontmatter |
| `SkillEditorFontScale` | Screenshot at 1.3 |
