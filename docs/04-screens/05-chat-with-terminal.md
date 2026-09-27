# Screen 05 — Chat and terminal, side by side

Two surfaces, one screen, independent state. The specialist view, for someone who wants the raw stream next to the explanation.

## Purpose

Let a user watch a run and read the raw output at the same time, without one scrolling the other away. The reason this view exists is comparison: an error in the log next to the agent's explanation of it.

## Layout

| Window size | Layout | Split |
|---|---|---|
| Compact portrait | Terminal as a bottom sheet over the chat | 60 % height |
| Compact landscape | Vertical split | 50 / 50 |
| Medium portrait | Vertical split | 60 / 40 |
| Medium landscape | Horizontal split | 60 / 40 |
| Expanded portrait | Vertical split | 60 / 40 |
| Expanded landscape | Horizontal split | 65 / 35 |
| Foldable unfolded | Split at the hinge, with a `space8` gutter | Equal |

**The hinge is respected.** Nothing crosses it, and neither pane's content is cut by it.

## The split control

A 1 dp `border` line between the panes, draggable between 30 % and 80 % of the available space.

| Property | Rule |
|---|---|
| Drag | Resizes, with a haptic tick at 50 % and at each 10 % step |
| Double tap | Snaps to 50 / 50 |
| Persisted | The ratio is remembered per screen size class, per device |
| Accessible | A TalkBack user adjusts the panes with adjustable actions, and the terminal pane can be focused independently |
| Reset | In the overflow menu: "Auf 50 / 50 zurücksetzen" |

The draggable divider is not a nicety. Somebody reading a 200-line stack trace wants a narrow chat and a wide terminal; somebody watching a plan wants the reverse. Making that a two-finger pinch on a hardcoded ratio would be a decision made once, by us, for everyone.

## The terminal pane

The real terminal. See `component-library.md` for the renderer's features and `06-runtime/pty-and-terminal.md` for the bridge.

| Element | Rule |
|---|---|
| Session | One PTY session per project, shared with the standalone terminal screen. Opening the split view attaches to the existing session rather than creating a new shell. |
| Scrollback | 5000 lines, configurable |
| Font size | A setting, 10–20 sp, shared with the terminal screen |
| Colour | Always dark, both themes, per `design-tokens.md` |
| Keyboard row | Present when the terminal has focus, hidden when the chat composer does |
| Selection | Long press to select, then copy; a copy confirmation |
| Scroll position | Preserved when the pane is collapsed and reopened, and when the split ratio changes |
| Emptiness | A prompt and a cursor. Never a skeleton. |

**Opening the split view must not start a new shell.** A person opening this view during a run expects to see the run's output, not an idle prompt. The pane attaches to the run's PTY if one exists, and to the project's persistent session otherwise. A line in the pane header makes which one clear: "Lauf · vor 2 Min." or "Sitzung · idle".

## The chat pane

The chat screen, unmodified, in a narrower column.

| Property | Rule |
|---|---|
| Column width | The pane's width, capped at 640 dp |
| Composer | Present, pinned at the bottom of the chat pane |
| Stop button | Present. It moves with the pane. |
| Auto-scroll | Unchanged |

## Keyboard behaviour

One screen, two possible focus targets, one keyboard. This is the hard part.

| Focus | Keyboard | Keyboard row | Enter |
|---|---|---|---|
| Chat composer | Standard, with a send key | Hidden | Sends a message |
| Terminal | Standard, no send key | Visible | Sends a newline to the shell |

The focus moves to the terminal when it is tapped, and back to the composer when the composer is tapped. A hardware keyboard's Tab key cycles between the two panes, and the focused pane has a visible focus ring on its edge.

**Losing focus mid-command is the most common way this view becomes unusable.** If the terminal has focus and the user taps the chat composer while a command is running, the terminal keeps the process; only the keyboard focus moves. There is no "sending enter to the wrong pane" failure, because there is only one pane with keyboard focus at a time and it is always visible.

## States

| State | Behaviour |
|---|---|
| Run active | The terminal is the run's output stream, live. A status line at the top: "Lauf · liest build.gradle.kts · 2 Min." |
| Run active, output quiet | The pane says "Keine Ausgabe seit 30 Sekunden" — quiet is not the same as stuck, and the difference matters |
| No run | The project's persistent session. The header says "Sitzung". |
| No session yet | The shell prompt, ready |
| Terminal session died | "Die Sitzung wurde beendet." with "Neu starten". The chat is unaffected. |
| Chat empty | The chat pane shows the new-chat content, centred in the pane. Both panes are usable. |
| Both panes narrow (< 280 dp) | Below this, the split is refused and the terminal opens as a sheet. Two 280 dp panes are both unusable. |
| Rotation | The split ratio is restored for the new orientation, from the per-size-class store |
| Fold | The panes re-split at the hinge |

## Interaction

| Action | Result |
|---|---|
| Drag the divider | Resize, with haptics |
| Double tap the divider | Snap to 50 / 50 |
| Tap a line in the terminal | Focus the terminal, position the cursor there, show the selection toolbar |
| Tap the chat | Focus the chat |
| Long press a terminal line | Select and copy |
| Overflow menu | Auf 50/50 zurücksetzen · Neue Sitzung (confirms; does not kill the run) · Schriftgröße · In Datei kopieren |
| Back gesture | Exits the split view and returns to the plain chat. It does not exit the app. |

**"Neue Sitzung" cannot kill a run.** The confirm sheet says so explicitly when a run is active: "Der laufende Auftrag läuft weiter. Eine neue Sitzung startet nur eine leere Eingabeaufforderung." A person opening a fresh shell to type one command should not have to read that first — they should only be asked when a run is actually attached.

## Accessibility

| Requirement | Implementation |
|---|---|
| The divider | An adjustable action: "Verhältnis 60 zu 40. Zurück zum Vergrößern des Terminals" |
| Pane focus | Announced: "Terminal, fokussiert" / "Chat, fokussiert" |
| The terminal | Not announced live. It is a byte stream, and a screen reader cannot follow one. A button offers "Letzte Ausgabe vorlesen", which reads the last 20 lines — the tool cards in the chat pane are the accessible equivalent of the same information. |
| The status line | A live region, updated on state change only, not per line |
| The keyboard row | Every key labelled by its actual character, so a screen reader user can compose `Ctrl` + `C` |
| Font scale | The terminal is exempt: it is a grid, and it truncates rather than reflows. The chat pane above it reflows normally. |

## Testing

| Test | Type |
|---|---|
| `SplitLayouts` | Screenshot at all six size and orientation combinations |
| `SplitOnFoldable` | Screenshot at each fold posture, asserting no content crosses the hinge |
| `SplitDivider` | UI — drag, snap, persistence, reset, and the below-280 dp refusal |
| `SplitFocus` | UI — tapping either pane focuses it, only one has focus, the keyboard row follows |
| `SplitDoesNotRestartShell` | E2E — opening the split during a run attaches to the run's PTY, with the process id unchanged |
| `SplitIndependentScroll` | UI — scrolling the terminal 200 lines does not move the chat viewport, and the reverse |
| `SplitRotation` | E2E — the ratio is restored per size class after a rotation |
| `SplitTerminalCopy` | UI — select, copy, confirmation |
| `SplitAccessibilityRatio` | UI — the adjustable action is present and changes the ratio |
