# UI tests

Compose UI tests. Every one drives the app the way a person does — through the semantics tree, not through internal state — and every one asserts something a person could notice.

## How the tests are written

```kotlin
@PreviewTest
class ChatDetailTest {
    @get:Rule val harness = Harness(fakeEngine = FakeEngine.deterministic())

    @Test
    fun `a streaming answer appends text without a jump`() = harness.test {
        send("Wie geht es?")
        engine.emitTextDelta("Mir ")
        assertVisible("Mir")
        engine.emitTextDelta("geht es gut.")
        assertVisible("Mir geht es gut.")
    }
}
```

The rules:

- **Interact through semantics.** `onNodeWithText`, `onNodeWithContentDescription`, `performClick`. Never `viewModel.state.value` as the only assertion.
- **Every interactive node has a test id or a semantic label.** A node a test cannot find is a node a screen-reader user may also not understand.
- **Assert the state, then the action.** "The button is disabled" and "clicking it does nothing" are two different tests, and the second is the one that matters.

## Coverage per screen

Four screens carry most of the interaction surface. The rest are covered by the same four states plus their specific actions.

### Chat detail (`04-screens/04-chat-detail.md`) — 22 tests

| Group | Rows | Cases |
|---|---|---|
| Streaming | 5 | Text appears token by token without reflow jitter; the streaming cursor is present and then gone; scrolling follows the stream only while the user is at the bottom; a tool card appears before its result; a result resolves the card it belongs to |
| Interrupt | 4 | The stop button appears only while running; tapping it disables itself immediately and shows "wird abgebrochen"; the run ends `CANCELLED`, not `FAILED`; the partial answer stays |
| Tool cards | 5 | Collapsed by default; expand shows the command and its output; a long output is scrollable, not truncated silently; a running tool shows a spinner; a denied tool shows the reason |
| Verification banner | 4 | `PASSED` shows the command and duration; `FAILED` shows the failing output; `UNVERIFIED` is visually distinct from both; a run cannot show `DONE` without one of them |
| Plan | 2 | Steps render with their state; a completed plan is collapsible |
| Composer | 2 | Sending clears it; it grows to a maximum and scrolls |

### Diff viewer (`04-screens/06-diff-viewer.md`) — 16 tests

| Group | Rows | Cases |
|---|---|---|
| Rendering | 5 | Added, removed, and context lines are visually distinct without relying on colour alone (redundant +/- markers present); very long lines scroll horizontally; a file with no changes is stated; a binary file is stated, not rendered as garbage; whitespace-only changes are visible |
| Navigation | 4 | Next/previous file; jump to a change list; the change list is ordered by file; returning preserves scroll position |
| Actions | 4 | Stage all stages nothing destructive; "discard" is offered only as an explicit, confirmed action and never as a swipe; export shares a patch; apply applies |
| Huge diffs | 3 | 10 000-line diff renders progressively; scrolling stays at 60 fps; memory does not spike |

### Terminal (`04-screens/12-terminal.md`) — 14 tests

| Group | Rows | Cases |
|---|---|---|
| Rendering | 5 | ANSI colours map to the dark palette; cursor blink; a full-screen redraw (alt-screen) works; a very long line wraps correctly; a bell character is handled without a popup |
| Keyboard | 4 | Arrow keys move the cursor, not the scroll; Ctrl-C sends `0x03`; Ctrl-D; Tab |
| Scrollback | 3 | Pinch-zoom changes the font size; the buffer is bounded and says so; scrolling back and returning to the live tail |
| Lifecycle | 2 | The terminal survives rotation; it is restored on return to the screen |

### Project detail (`04-screens/08-project-detail.md`) — 18 tests

| Group | Rows | Cases |
|---|---|---|
| Autonomy | 5 | The level selector shows exactly what changes; raising a level requires confirmation listing the newly permitted actions; lowering is immediate; the effective level is visible at all times |
| Verification setup | 5 | Add/remove a build command; remove a test command; the "detected automatically" badge is accurate; a command that cannot be run is marked, not silently accepted; the verify button reflects the configured commands |
| Recent runs | 4 | Each run shows its verification result; a `UNVERIFIED` run is distinguishable; tapping a run opens it; the list loads more |
| Hard blocks | 4 | The five blocks are listed where the user can find them; the list is not dismissible into ignorance; the safety section explains each in one sentence |

### Onboarding (`04-screens/01-onboarding.md`) — 12 tests

Biometric setup, first key profile, first project, and the runtime choice. Each step's back path loses nothing already entered. The "skip" path leaves a valid, working state, not a broken one.

### Settings, skills browser, skill editor, chat list, file browser — 30 tests total

Mostly the four states plus the specific actions from their screen documents. The skill editor gets extra attention: an unsaved-changes guard, a validation surface, and a preview of the manifest as the parser will read it.

## Accessibility tests

Accessibility is a requirement here, not a nice-to-have, so it is tested rather than assumed. These run on every push, on the fast-lane emulator.

| Check | Rows | How |
|---|---|---|
| Every actionable node has a label | 20 | Walk the semantics tree, assert no node with a click action has an empty content description |
| Touch targets are at least 48 dp | 12 | Assert the touch bounds of every clickable node |
| Heading order is sane | 6 | Assert that headings nest without skipping levels |
| TalkBack reading order is visual order | 6 | Compare the semantics traversal order with the visual bounds |
| Text is not clipped at 200 % font scale | 8 | Set `fontScale = 2f`, assert no `TextLayoutException` and no cut-off text |
| Reduce motion is honoured | 4 | Enable the setting, assert animations are disabled except the ambient mark |
| Colour is never the only signal | 6 | Screenshot diffs cannot check this; instead, each status component is asserted to carry a non-colour marker |
| Focus order on a keyboard | 4 | Tab through a screen, assert the order follows the layout |

## The rules UI tests must not break

These are asserted *in* the UI test suite, because they are the things a redesign breaks first:

1. No `SwipeToDismiss` on anything a user authored. A test asserts that no node in the tree has a dismiss action.
2. No long-press-only action. Every action available by long press is also available from an explicit visible control.
3. No animation longer than 320 ms except the ambient mark.
4. No content wider than the reading measure; at 200 % font scale, text wraps rather than scrolls horizontally.
5. Every destructive-looking affordance is either absent or confirmed. The five hard blocks are the extreme case: they are not exposed as a setting at all.

## Snapshot policy for UI tests

UI tests do not use golden images — that is `screenshot-tests.md`. These assert semantics, text, and enabled state. The split matters: a UI test that fails on a 2-pixel change is noise that trains people to re-run, which is the opposite of what a gate is for.
