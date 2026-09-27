# Accessibility

The target is WCAG 2.1 level AA, plus the things WCAG does not cover that matter for an app used one-handed, outdoors, on a phone, for hours.

## Compliance, not aspiration

| Criterion | Level | How it is met |
|---|---|---|
| 1.1.1 Non-text Content | A | Every icon, image, and decorative element has either a content description or is marked decorative |
| 1.3.1 Info and Relationships | A | Headings are headings, lists are lists, form labels are programmatically associated |
| 1.3.2 Meaningful Sequence | A | The reading order is the visual order; no `invisibleToUser` reordering |
| 1.3.3 Sensory Characteristics | A | No instruction relies on position, shape, or colour alone |
| 1.3.4 Orientation | AA | Every screen works in portrait and landscape |
| 1.3.5 Identify Input Purpose | AA | Text fields carry the correct `autofill` semantics and input type |
| 1.4.1 Use of Color | A | Every status is glyph plus text plus colour. Enforced by the greyscale test. |
| 1.4.3 Contrast (Minimum) | AA | Measured, in `color-and-contrast.md`. Minimum 4.5:1, 3:1 for large text and control boundaries. |
| 1.4.4 Resize Text | AA | 200 % text scaling supported; layout adapts up to 130 % and clips beyond with no loss of function |
| 1.4.10 Reflow | AA | No horizontal scrolling at 320 CSS px equivalent, except code blocks and the terminal, which are explicitly exempt as they are pre-formatted content with an alternative view |
| 1.4.11 Non-text Contrast | AA | Control boundaries at 3:1, focus indicators at 3:1 |
| 1.4.12 Text Spacing | AA | No content is lost when the user applies their platform's text spacing |
| 1.4.13 Content on Hover or Focus | AA | No hover-only content in this app |
| 2.1.1 Keyboard | A | Every action reachable with a keyboard or switch device; the terminal has a hardware-keyboard path |
| 2.1.2 No Keyboard Trap | A | No focus trap outside a modal, which is escapable |
| 2.2.2 Pause, Stop, Hide | A | The mark's ambient loop and indeterminate progress can be paused by the system setting; see below |
| 2.3.1 Three Flashes | A | Nothing flashes more than three times a second. The running indicator bar is a 1400 ms cycle. |
| 2.4.1 Bypass Blocks | A | The top bar is a heading; TalkBack can jump past it |
| 2.4.2 Page Titled | A | Every screen has a title announced on entry |
| 2.4.3 Focus Order | A | Follows the visual order |
| 2.4.4 Link Purpose | A | Every link's destination is clear from its text |
| 2.4.6 Headings and Labels | AA | Headings describe their section |
| 2.4.7 Focus Visible | AA | 2 dp `focusRing` at 1 dp offset, on every focusable element |
| 2.5.3 Label in Name | A | The visible label is the first part of the accessible name |
| 2.5.5 Target Size | AAA | 48 dp minimum, enforced not by eye but by a test that measures every clickable node |
| 2.5.8 Target Size (Minimum) | AA | 24 dp minimum, exceeded everywhere by the 48 dp rule |
| 3.1.1 Language of Page | A | The app's language is set from `contentDescription` context, and the system locale drives the UI language |
| 3.2.1 On Focus | A | Focus does not change context |
| 3.2.2 On Input | A | No context change on input |
| 3.3.1 Error Identification | A | Errors are identified in text, not only by colour |
| 3.3.2 Labels or Instructions | A | Every input has a persistent label above it |
| 3.3.3 Error Suggestion | AA | Errors suggest a fix, per `error-taxonomy.md` |
| 3.3.4 Error Prevention | AA | Legal, financial, and destructive submissions are reversible or confirmed. Every git operation is confirmed or on a branch. |

## The four things WCAG does not cover

### A long-running process on a phone

A run can take twenty minutes. The user will switch apps, lock the phone, and come back. The accessibility requirements for that:

- The foreground-service notification is the accessible status surface. It names the project, the current step, and the elapsed time, and its actions work from the shade.
- Every state the mark expresses visually has a text equivalent somewhere on screen, permanently. A user who cannot see the mark's expression still reads "Läuft · Versuch 2 von 10".
- Nothing is communicated only by a progress bar. The step name and the elapsed time are text.

### One-handed use

The thumb reaches the bottom and side arcs of a phone, not the top. Accessibility here is a reachability question.

- Anything frequently used is in the lower 60 % of the screen or in the bottom bar.
- The composer, the send button, the stop button, and the permission actions are all thumb-reachable.
- The top bar holds only navigation and rarely-used actions.
- Destructive and privileged actions are reachable by design, not placed in the hardest corner, because a control nobody can hit safely is a control that gets mis-hit.

### Outdoors

- Body text never below 14 sp, never lighter than 400 weight.
- The minimum contrast is 4.5:1 and most pairs are above 5:1, because a phone in sunlight loses far more than the 2:1 a lab measures.
- Status is never colour alone, which matters doubly in direct sunlight where all colours wash toward white.

### Glanceable state

The user picks up the phone after an hour away. Within two seconds they need to know: is something running, and did the last thing work.

The chat list row shows a `StatusBadge`. The mark's state is a summary. The notification said something. Three channels, each sufficient on its own.

## Screen reader specifics

| Case | Behaviour |
|---|---|
| The mark | "Claude ist aktiv · liest Datei: build.gradle.kts". Updated on state change, not per frame. |
| The streaming caret | Not announced. It would produce continuous, useless speech. |
| Streaming text | Announced as the message completes, not per delta. A screen reader reading a growing paragraph is unusable. |
| Tool cards | Collapsed: the title, the target, the state, the elapsed time. Expanded: the output, as a single announcement with a "more" affordance rather than a 4000-character dump. |
| Cost meter | "Kosten so far 0,04 US-Dollar". Not announced on every update; on completion and on demand. |
| Progress steps | Announced on state change only. A running step announces "Schritt 3 von 7, läuft". |
| Permission sheets | Focus moves to the sheet, the action is read, and the background is marked as such. |
| The terminal | A linear stream, announced on demand rather than live. A screen reader cannot follow a terminal; the tool cards are the accessible equivalent, which is one reason they exist. |
| Diffs | Announced as "Zeile 42 geändert, +3 −1" per hunk, not per line. |
| Verification results | "3 Prüfungen, 2 bestanden, 1 fehlgeschlagen: Kompilierung". |

**Live regions are used sparingly.** A `liveRegion` that updates on every token is the most common screen-reader failure in chat interfaces. Live regions are used for: a run's state change, a permission request, an error, and a completed message.

## Reduced motion

Handled per `motion.md`, with two documented exceptions: the mark's idle loop and indeterminate progress both continue, at reduced amplitude or unchanged respectively, because both communicate "something is happening" and a still mark reads as a stopped app.

The system setting is the source of truth. The app has no independent motion toggle, because two sources of truth for one behaviour produce a screen where a user has set the wrong one.

## Switch access and external keyboards

| Requirement | Status |
|---|---|
| Full switch access | Every action is reachable by scanning focus order. No infinite animation blocks scanning; the mark's loop is not a focusable element and does not add a scan stop. |
| Hardware keyboard | Ctrl-C is bound to interrupt. Ctrl-Z to undo the last assistant message. Esc closes a sheet. The terminal is fully usable with `tools/pty-and-terminal.md`'s key mapping. |
| Voice control | Every interactive element has a visible text label, so a spoken label matches. Icons alone would break voice control entirely. |

## Testing

| Test | Tool | Threshold |
|---|---|---|
| Automated audit | Accessibility Scanner (Play) and Espresso accessibility checks | Zero critical findings, zero serious findings in a committed baseline |
| Contrast | `tools/check_token_usage.py` recomputes every pair | All pass, per the table in `color-and-contrast.md` |
| Colour independence | Screenshot tests rendered through a greyscale transform | Every `StatusBadge` state distinguishable by glyph alone |
| Target size | A UI test measuring every clickable node's bounds | 48 dp, no exceptions |
| Font scale | Screenshot suite at scale 1.0 and 1.3 | No clipped text, no lost action |
| Reduced motion | Screenshot suite with the system setting on | Per the table in `motion.md` |
| TalkBack walkthrough | Manual, once per release, on the five E2E journeys | Every journey completable without sight |
| Reflow | Screenshot at 320 dp width | No horizontal scroll outside code and the terminal |
| Focus order | A UI test asserting the focus traversal order matches the visual order | Exact match |

**The manual TalkBack walkthrough is not optional and is not automated away.** Five journeys, once per release, by a person who is not the developer. It is on the release checklist in `12-delivery/release-checklist.md` and it is the only accessibility test that finds the real problems.

## Known limitations

Stated rather than discovered later.

| Limitation | Why | Mitigation |
|---|---|---|
| The terminal is not screen-reader navigable | It is a byte stream | The tool cards are the accessible equivalent of the same information; the terminal is a specialist tool with a stated requirement |
| Diff review needs a large visual field | Line-by-line comparison is inherently visual | Hunk-level announcements with counts, so a summary is available non-visually; full per-line reading is a documented gap |
| Code blocks overflow at font scale 1.3 | Monospace cannot shrink | Horizontal scroll, which is the expected behaviour for pre-formatted content, plus a copy action |
| Long German compounds can exceed a chip | Chips size to content | Chips wrap to two lines rather than truncating a word |
| The mark's expression is not described in a tooltip | There is no hover on a phone | The state is always available as text next to it |
