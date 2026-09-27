# Import and export

Getting work in and out, in formats that are useful to a person and not only to the app.

## Purpose

An app that holds somebody's conversations, their costs, and their run history is holding something they will eventually want to move, back up, or read outside it. The formats have to survive that.

## Export targets

| Target | What | Format |
|---|---|---|
| A single run | The complete record of a task | Markdown |
| A conversation | The full transcript, with tool calls and results | Markdown |
| A project's history | Every run for a project | Markdown, one file per run, in a folder |
| The activity log | A filtered range | JSON, or Markdown, or plain text |
| A skill | The full skill, frontmatter included | Markdown, to the clipboard |
| A plan | The plan with its final states | Markdown |
| A verification run | Every command, its output, and the judgement | Markdown, or plain text |
| A diff | The change set | The standard unified diff, or a patch file |
| The terminal scrollback | A session's output | Plain text, or a file in the project |
| A project | The working copy | A git bundle, so history travels with it |
| The app's configuration | Providers, settings, per-project configuration — **never keys** | JSON |

## The Markdown export

The one that matters. It is written for somebody who was not there.

### Structure

```markdown
---
task: Unterstützung für benutzerdefinierte Prüfbefehle
project: claude-code-android
run: 01HQ8X2M4K7P...
branch: task/verify-commands
commit: a3f19c2
pull_request: https://github.com/…/pull/12
started: 2026-09-27T14:31:02Z
ended: 2026-09-27T14:41:33Z
duration: 10m 31s
verification: 2 of 3 passed
cost_usd: 0.31
backend: this-device (native profile)
autonomy: ASK_RISKY
skills: gradle-test-helper, android-conventions
claude_code: 2.1.283
---

# Unterstützung für benutzerdefinierte Prüfbefehle

## Plan

- [x] Prüfbefehle aus dem Projekt erkennen
      _erledigt in 4 s · automatisch geprüft_
- [x] Prüfbefehle im Code konfigurierbar machen
      _erledigt in 1 Min · automatisch geprüft_
- [x] Tests schreiben
      _erledigt in 2 Min 14 s · **nicht automatisch prüfbar**_
- [ ] Alles bauen und prüfen
      _fehlgeschlagen: ./gradlew test_

## Unterhaltung

**Du** · 14:31:02

> Unterstützung für benutzerdefinierte Prüfbefehle einbauen.

**Claude** · 14:31:09

Ich schaue mir zuerst an, wie die Prüfung aktuell läuft.

<details>
<summary>🔧 Datei gelesen · build.gradle.kts · 0.4s</summary>
…
</details>
```

### The rules

| Rule | Detail |
|---|---|
| The frontmatter is machine-readable | A run's export can be processed by a script without parsing prose |
| The verification state is in the frontmatter | `2 of 3 passed`, or `not verified`. Never ambiguous. |
| The plan's checkboxes reflect verification | Not the agent's belief. A step that could not be checked is `[x]` with a note saying so, and a step that failed is `[ ]`. |
| Tool calls are collapsed | Behind a `<details>` element with a one-line summary. A transcript with 200 full tool outputs is unreadable. |
| Unverifiable steps say so | `_nicht automatisch prüfbar_` under the step, in plain text, so it survives a Markdown renderer that drops styling |
| Code is fenced correctly | Fence lengths are chosen to contain any backticks in the content |
| Timestamps are ISO 8601 with a zone | And a human-readable form in the body |
| Costs are in the frontmatter | As a number, not a formatted string |
| The privacy statement is in every export | At the top, in italics: "Enthält keine Schlüssel. Redigiert am {timestamp}." |

## The project export

A `git bundle`, which is a single file containing the repository with its full history.

| Aspect | Behaviour |
|---|---|
| What | `git bundle create`, containing the current branch and its history |
| Why a bundle and not a zip | A zip of the working directory loses the history, the branches, and the tags. A bundle is one file and it is complete. |
| Size | Shown before it is created, with a warning above 100 MB |
| Writing | To a user-chosen location via the system picker, or to the app's share sheet |
| Progress | With a cancel. A cancelled bundle is removed — it is our incomplete output, and removing it is not a rule violation. |
| Never | It does not include anything outside the project |
| Note | It does not push anywhere. Export is not publication. |

## The configuration export

Everything about how the app is set up, so it can be moved to another device.

| Included | Excluded |
|---|---|
| Providers: name, kind, base URL, model list, enabled state | Keys and tokens, always, without exception |
| Per-project settings: autonomy, model, verification commands, retry budget, offload policy | Conversation content |
| App settings: language, theme, text size, retention, notification preferences | The activity log |
| Installed skills: their sources and versions, not their content | Project working copies |
| Runtime profile and its version | The runtime binary — re-downloaded, not exported |
| Remote runners: names and kinds | Their keys |

The export screen says, in the list: "Schlüssel sind nicht enthalten und werden es nie sein." The export runs `Redactor` over the result as a second layer, and a test asserts that a configuration containing a key produces an export containing none.

## Import

| Import | From | Behaviour |
|---|---|---|
| A skill | A `SKILL.md` file, a folder, a zip, or a GitHub URL | Per `04-screens/10-skills-browser.md`, with a preview and validation |
| A conversation | A Markdown export | Reconstructed as a read-only historical conversation, with the original run's metadata in the frontmatter. It is not attached to a session and cannot be resumed. |
| A run record | A Markdown export | The same, shown in the history, marked as imported |
| A project | A git bundle | Cloned like any other project, with the same inspection and the same private-only rules |
| Configuration | A JSON export | Reviewed field by field, with nothing applied until confirmed, and keys never present |
| A diff | A patch file | Shown as a proposed change set, reviewable, with per-hunk accept and revert. Applying it is an edit, not a merge, and the same never-weaken-a-check rules apply. |

**Nothing is imported silently.** Every import is a preview, a confirmation, and a log entry. A configuration import in particular shows every field that differs and asks once.

## An imported conversation, specifically

Worth spelling out, because it is the least obvious case.

| Property | Behaviour |
|---|---|
| Where it appears | In the chat list, marked with an `info` badge "Importiert" |
| Its content | The full transcript, exactly as exported, including the collapsed tool calls |
| Its run | Present in the history, marked as imported, with the original metadata |
| Resumable | **No.** There is no session to resume, because the session file is on the device that produced it. |
| The project | Attached to the project in the export, if it exists; otherwise unattached and shown in an "Importiert" group |
| Cost | The original figure, from the frontmatter, labelled as the original device's figure rather than this device's |
| Deletable | The app's local copy can be removed through the same audited path as a project's local data. The export file the user has on their own storage is not the app's to touch. |

## Sharing

| Method | Detail |
|---|---|
| The system share sheet | A Markdown file, via the standard Android mechanism, to wherever the user wants |
| A repository | A run export committed to a branch, so a conversation becomes part of a project's documentation. The commit is on a branch, never the default, and is announced. |
| Clipboard | A skill, a plan, a summary. Never a transcript, which is too long for a clipboard to be useful. |
| QR | Not implemented. A run export is too large for a QR code to be useful, and a link would need a server. |

## Formats, and why

| Format | When | Reasoning |
|---|---|---|
| Markdown | Anything a person reads | Renders everywhere, diffs in git, readable in twenty years, editable in any text editor |
| JSON | The log, the configuration | Machine-readable, complete, no information loss |
| Plain text | Terminal output | It was text. Keep it text. |
| Unified diff / patch | Changes | The universal format, and it is what `git apply` expects |
| `git bundle` | A project | Complete, one file, standard |

**No proprietary format.** Every export is readable without this app. A format only this app understands is a lock-in, and an app that holds a user's work history has no business building one.

## Testing

| Test | Type |
|---|---|
| `ExportRunMarkdown` | E2E — the frontmatter contains every field, the plan's checkboxes reflect verification, and a failed step is unchecked |
| `ExportUnverifiableStepNoted` | E2E — an unverifiable step carries the plain-text note |
| `ExportToolCollapsed` | E2E — 200 tool calls are inside `<details>` elements with one-line summaries |
| `ExportFenceEscaping` | Unit — content containing ``` does not break the Markdown |
| `ExportRedaction` | E2E — a run whose transcript contains a key-shaped string exports with it redacted, and the privacy line is present |
| `ExportProjectBundle` | E2E — the bundle contains the history, and the file is written where the user chose |
| `ExportConfigNoKeys` | E2E — a configuration with 12 keys installed exports none of them, and the screen says so |
| `ExportProgress` | UI — a bundle over 100 MB warns with a size, and a cancel removes the partial file |
| `ImportSkill` | E2E — a `SKILL.md` file imports with a preview, validation, and a log entry |
| `ImportConversation` | E2E — a Markdown export appears as an imported conversation, is not resumable, and carries the original cost labelled as the original's |
| `ImportProjectBundle` | E2E — a bundle clones, passes inspection, and lands on the project with the same private-only rules |
| `ImportConfigFieldByField` | UI — every differing field is shown, and nothing is applied without confirmation |
| `ImportDiffPatch` | E2E — a patch file is shown as a change set, applied selectively, and the never-weaken rules apply |
| `ImportNothingSilent` | Integration — every import path produces a log entry and required a confirmation |
| `ShareToRepository` | E2E — the commit lands on a branch, is announced, and does not touch the default branch |
| `ExportFormatsOpen` | E2E — each exported file opens in a standard tool with no app-specific handling, asserted by a check that no format has a custom magic header |
