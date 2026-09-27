# Component library

Every reusable component, its variants, its states, and the rule for when to use it. If a screen needs something that is not here, the correct move is to add a component here first, not to build it inline.

Components live in `shared/ui/component/`. `Primitives.kt` holds the low-level set; everything else is a feature component.

---

## Primitives

### `AppScaffold`

The one screen skeleton from `spacing-and-layout.md`. Not a wrapper that takes a title string — it takes slots, because three of the fourteen screens need a custom top bar and a parameterised title would force all of them through the same shape.

```kotlin
AppScaffold(
    topBar = { /* slot */ },
    bottomBar = { /* the tab bar */ },
    floating = { /* the composer, if any */ },
) { content }
```

Handles: insets, the content max width, scroll behaviour, and the snackbar host. Does not handle: navigation, state, or the title.

### `Card`

| Parameter | Default | Rule |
|---|---|---|
| `surface` | `surface` | |
| `border` | `border` hairline | Never both a border and a shadow |
| `radius` | `radiusLarge` | |
| `padding` | `space5` | |
| `onClick` | none | A card is not a button unless it is |
| `selected` | false | Adds a 2 dp `accent` border, not a background change |

**A card with `onClick` must be a card with `onClick` set deliberately.** A tap target the size of a card is easy to hit by accident while scrolling, so a clickable card also gets a ripple and a pressed scale, and never wraps a small button inside itself.

### `Button`

Four variants. There is exactly one primary button per screen.

| Variant | Fill | Label | Use |
|---|---|---|---|
| `Primary` | `accent` | `onAccent` | The one main action |
| `Secondary` | transparent | `textPrimary`, `borderStrong` border | Everything else that needs a border |
| `Tertiary` | transparent | `accentText` | Low-stakes actions, links |
| `Danger` | `danger` | `#FFFFFF` | Refusals and destructive confirmations |

Sizes: `medium` (48 dp tall) as default, `small` (36 dp) inside cards and list rows, `large` (56 dp) for the composer's send button and permission sheets.

States, all of which must render: `enabled`, `disabled` (`textTertiary` on `surfaceSubtle`), `loading` (the label stays, a 16 dp indicator replaces the leading icon, the button does not resize), `pressed` (scale 0.97).

**A disabled button carries a reason.** Where a disabled primary action would strand a user, the screen shows the reason next to it. "Starten" disabled with nothing else is a dead end.

### `TextField`

| Property | Value |
|---|---|
| Fill | `surface` |
| Border | `borderStrong` 1 dp. Never `border` — this is a control boundary. |
| Focus | `focusRing` 2 dp, offset 1 dp so both edges are visible |
| Label | `labelMedium` above, not inside. A floating label that animates is decoration. |
| Helper text | `bodySmall`, `textSecondary`, one line |
| Error text | `bodySmall`, `danger`, replaces the helper text |
| Min height | 56 dp |

States: `empty`, `focused`, `filled`, `error`, `disabled`, `readOnly` (same look, no focus ring, `textSecondary` label).

**Error text is never colour alone.** It has the text, and the field has a 1 dp `danger` border, and the helper text position is preserved so the layout does not jump when a field goes from valid to invalid.

### `Chip`

A label with an optional leading icon. Two jobs: a filter or a state, and a removable tag.

| Type | Use |
|---|---|
| `Filter` | Selectable, shows a check when active, `accentSubtle` fill when active |
| `Status` | A run state, coloured per `color-and-contrast.md`, always with a glyph |
| `Tag` | A skill name or a branch, with an optional remove action |

Height 32 dp, `radiusSmall`, `labelMedium`. Status chips never get a remove action: a state is not a tag.

### `Sheet`

The app's secondary surface. A scrim, a grabber, a title, a body, and an action row.

- Never used for a decision that has to be read carefully. That is a dialog.
- Dismissal by swipe, scrim tap, or a labelled action. Never by tapping the title.
- Content scrolls; the action row is pinned, so "Ablehnen" is always reachable.

### `Dialog`

Only for a decision with two options and a consequence. Three uses exist in the whole app: abandoning a run, the two-step confirm for `FULL_AUTO`, and the one audited erase action.

No dialog has more than two options. Three options is a sheet with a list.

### `Snackbar`

For a confirmation of something that already happened, or a transient warning that does not need a decision. Never for an error that needs a decision — that is a sheet.

Duration 4 s, swipe to dismiss, an action only when the action is genuinely one tap and does not need thought.

### `StatusBadge`

The one component that carries a correctness requirement.

| State | Colour | Glyph | Label (de / en) |
|---|---|---|---|
| Verified | `success` | `✓` | Geprüft / Verified |
| Unverified | `warning` | `?` | Ungeprüft / Unverified |
| Failed | `danger` | `✕` | Fehlgeschlagen / Failed |
| Refused | `danger` | `⊘` | Blockiert / Blocked |
| Running | `accent` | spinner | Läuft / Running |
| Retrying | `warning` | `↻` + count | Versuch {n} / Attempt {n} |
| Committed | `success` | `◆` | Gesichert / Committed |
| Pushed | `success` | `↑` | Hochgeladen / Pushed |
| Interrupted | `info` | `‖` | Unterbrochen / Interrupted |

**Every state has a glyph and a word.** The colour is the third signal, not the first. This is a hard requirement, checked by a test that renders each state in greyscale and asserts the states remain distinguishable.

### `EmptyState`

An illustration, a heading in `titleMedium`, one sentence in `bodySmall`, and at most one action.

No three-column grid of feature pills. No "Try one of these!" chips on a screen that should be quiet. The empty state makes a statement and gets out of the way.

### `KeyValueRow`

A label and a value, in a settings list or a details panel. Value is `bodyMedium`, right-aligned, monospace when it is a path, a command, or an identifier.

---

## Feature components

### `StreamingText`

Appends `TextDelta` values as they arrive, in `bodyLarge`, with a caret at the end. Renders markdown. Does not re-animate existing text.

- Inline code, fenced code blocks, lists, headings (at most `titleMedium`), tables, and links are supported.
- A fence that is still open while text streams renders as an open block with a blinking caret inside, not as inline text with backticks.
- Very long messages are not truncated. Chat is not a preview.
- A link opens in the system browser. The app has no in-app browser.

### `ToolCard`

The core of the chat's activity display. Collapsed by default, one line, in plain language.

```
┌─────────────────────────────────────────────┐
│ ⟐  Datei gelesen · build.gradle.kts   1.2s  │
├─────────────────────────────────────────────┤
│  expanded content                            │
└─────────────────────────────────────────────┘
```

| Field | Rule |
|---|---|
| Header | A glyph, a plain-language title, the target, the elapsed time |
| Title source | `titleDe` / `titleEn` from the database, precomputed at record time |
| Target | `monoSmall`, truncated at 52 characters, full path in the expanded body |
| Collapsed state | The user's choice, remembered per tool invocation |
| Expanded content | Output, capped at 2 KB with an explicit "showing the first 2 KB" marker and a link to the full log |
| States | `pending`, `running` (accent bar under the header, elapsed time counting), `done`, `error`, `denied` |
| Destructive marker | A `danger` glyph and a "cannot be undone" note, for a tool the policy flagged |
| Subagent | Nested one level, with the child's tool cards indented and its own collapsed state |

Titles are stored, not computed. "Datei gelesen · build.gradle.kts" is written when the tool call is recorded, in the user's language at that moment. The UI never translates a tool name at render time — see ADR-010.

### `PlanView`

The ordered steps, with state, acceptance criteria, and the retry counter.

- Steps are `labelLarge` with a leading state glyph.
- A completed step is struck through in `textTertiary`, not hidden.
- A checkpoint step is marked with a small `⊟` and a caption; it is where a run stops even at high autonomy.
- The plan is editable before approval and read-only during a run.
- An empty plan renders a warning, not a blank space. An empty plan means the planner failed.

### `CostMeter`

A running total in `labelSmall` with tabular figures, in the composer's footer.

- During a run: the live estimate, marked with a `~`.
- At the end: the reported figure, animating from the estimate to the actual over 320 ms. The transition is visible, because a number that silently changes is a number nobody trusts.
- Tapping opens a sheet with the token breakdown, the model, the provider, and the session id.

### `PermissionSheet`

The most important component in the app, because it is where the user either stays in control or does not.

Contents, in this order:

1. **What will happen**, in one sentence, in plain language. "Claude will modify build.gradle.kts."
2. **What it changes**, the specific diff or command, not a generic warning.
3. **What cannot be undone**, stated honestly, when true. "This rewrites 40 files. The previous state stays in git history." / "This cannot be undone."
4. **Why it is asking**, once: "ASK_RISKY is set to ask before running commands."
5. **The options**: *Erlauben* (once), *Immer erlauben* (this run), *Ablehnen*.

Rules:

- It never times out into an answer. It waits, with a notification, indefinitely.
- *Immer erlauben* is scoped to the run and is never written to a project setting. See `event-protocol.md`.
- A destructive action cannot be granted by *Immer erlauben* if the hard block would refuse it, and the sheet says which rule is in the way and why it cannot be turned off.
- The actions are in the same order always. Muscle memory matters more than visual hierarchy here.

### `DiffView`

Unified and side-by-side, with a per-hunk decision.

| Element | Rule |
|---|---|
| File header | Path, `+n −m`, a binary marker when applicable |
| Hunk header | The range, on `diffHunkHeader` |
| Line | `+` or `−` in the gutter, a background tint, syntax highlighting on the content |
| Unchanged context | 3 lines by default, expandable per hunk |
| Very long lines | Truncated with a `…` in the gutter and the full line in an expandable strip. Never silently. |
| Per-hunk actions | Accept, revert |
| Per-file actions | Accept all, revert all |
| The decision | A pending hunk has a 2 dp `warning` marker on its header; an accepted one is `success`; a reverted one is `danger` with a "reverted" marker |

A binary file shows a single line: "Binäre Datei, {size}, nicht anzeigbar" and a button to open it externally. It never renders an empty box.

### `TerminalView`

The real thing: a `Canvas`-based renderer over a ring buffer.

| Feature | Support |
|---|---|
| Colours | 16 ANSI, plus 256 and truecolor sequences |
| Cursor | Position, shape, blink, visibility |
| Control sequences | Erase line and screen, scroll region, carriage return, backspace, tab stops |
| Scrolling | Vertical, with a configurable scrollback of 5000 lines |
| Selection | Long-press to select, then copy; drag handles |
| Keyboard | A custom row: `Ctrl`, `Alt`, `Tab`, `Esc`, arrows, `Ctrl+C`, and a paste field |
| Resize | Emits `SIGWINCH` with correct `COLUMNS` and `LINES` |
| Font size | A setting, 10–20 sp |
| Always dark | Both themes. See `design-tokens.md` for why. |

`Ctrl+C` is a first-class button, not a key combination, because interrupting a long command is the single most common thing a user does in a terminal and it must never require two thumbs and a modifier.

### `RunSummary`

The end-of-run card: the task, the outcome badge, what changed (files, lines), what was verified (commands, results), the cost, the branch, the commit, and the pull request link.

Ordered by what a person asks in order: did it work, what changed, how do I check it, what did it cost, where is the result.

`UNVERIFIED` leads this card. The badge and the word come before anything else, because "here is your finished work" and "here is work I could not check" are different statements and the second one must not be read as the first.

### `FileTree`

A lazy, expandable tree. Collapsed directories remember their state. Binary files, symlinks, and files over 1 MB are marked without loading. Git status is a coloured marker per file: modified, added, untracked, renamed.

### `ProgressSteps`

For the bootstrap and the verification panel: a vertical list of steps with a state, a one-line description, and a duration once complete. Resumable work uses this, so leaving and returning shows exactly where things stopped.

---

## Component rules

1. **A component takes tokens, never literals.** Checked in CI.
2. **Every interactive state is designed**, not just the default. Focused, pressed, disabled, loading, and error. A component shipped without them is unfinished.
3. **Every component has a reduced-motion variant** where it animates.
4. **Every status is glyph plus text plus colour**, never colour alone.
5. **Every touch target is at least 48 dp**, even when the visible element is smaller.
6. **A component does not read a repository, a ViewModel, or a domain service.** Props in, events out.
7. **A component added here is removed from a screen's inline code in the same change.** No duplication between the library and a screen.
8. **A new component needs a screenshot test in both themes and a UI test covering its states**, or it is not done.
