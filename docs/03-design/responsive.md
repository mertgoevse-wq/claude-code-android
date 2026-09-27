# Responsive

The app is built for a phone first. Everything here is about what happens when the phone is not a phone: a tablet, a foldable, a landscape screen, a resized free-form window, or a split screen with something else taking half of it.

## Window size classes

Following the standard breakpoints, driven by the width of the window rather than by a device category.

| Class | Width | Typical |
|---|---|---|
| `Compact` | < 600 dp | A phone in portrait, or a phone with a split screen |
| `Medium` | 600–840 dp | A small tablet in portrait, a large phone in landscape |
| `Expanded` | > 840 dp | A tablet in landscape, a desktop-mode phone, a foldable unfolded |

A foldable unfolded in portrait reports `Expanded`; folded it reports `Compact`. Both are the same app and both are tested.

## The change per class

| Aspect | Compact | Medium | Expanded |
|---|---|---|---|
| Horizontal margin | `space5` (16) | `space7` (24) | `space8` (32) |
| Content max width | fluid | 640 dp, centred | 640 dp, centred |
| Project grid | 1 column | 2 columns | 3 columns |
| Settings sections | 1 column | 2 columns | 2 columns |
| Chat | Single column | Single column, centred, with a visible boundary | Single column, centred |
| Chat with terminal | Terminal as a bottom sheet | Split 60 / 40 | Split 65 / 35, side by side |
| Tab bar | 5 items, labels always | 5 items, labels always | 5 items, labels always |
| Top bar | Title and 2 actions | Title and 2 actions | Title and 3 actions |
| Diff viewer | Unified only | Unified, split optional | Split by default, unified a toggle |
| Stat tiles | 2 across | 3 across | 4 across |

**The tab bar does not become a rail.** A navigation rail on a tablet is a Material pattern that would look like a generated app, and it moves the primary navigation away from the thumb. The app keeps its bottom bar at every size. This is a deliberate refusal of the platform convention in favour of consistency, and it is the right call for an app that is mostly a single column of text.

**The content cap is the same 640 dp everywhere.** This is a reading decision, not a layout one. A chat column across a 1280 dp tablet is 130 characters per line and unreadable.

## Foldables

| Posture | Behaviour |
|---|---|
| Folded, portrait | `Compact`. Identical to a normal phone. |
| Folded, cover screen | `Compact`, reduced height. The chat list shows fewer rows; the composer and the top bar are never displaced. |
| Unfolded, portrait | `Expanded`. Two-pane content where it helps, capped reading width. |
| Unfolded, landscape | `Expanded`. Chat plus terminal, side by side. |
| Folded, landscape | `Medium`. |

**The hinge.** On a device that reports a hinge or a fold separating the two halves, the layout treats it as a gutter. Nothing important is ever placed across it, and on the chat screen the two panes get equal space with a `space8` gap. A message split by a hinge is a bug.

**The table of contents matters more than the layout.** The interesting states are: a chat interrupted across a fold, a terminal pane on the wrong half, a diff viewer's two sides separated by a fold. Each is a screenshot test.

## Landscape on a phone

| Aspect | Behaviour |
|---|---|
| Top bar | Stays. It does not hide; the user needs the stop button. |
| Tab bar | Stays. |
| Composer | Stays above the keyboard. Capped at 3 lines, then scrolls internally. |
| Content | The screen is short, so lists scroll more. Nothing is removed. |
| Keyboard | Opens on focus, covering about 60 % of the height. The scroll container gets bottom padding equal to the IME height. |
| The mark | Shrinks to 24 dp. It is decorative in the top bar, not information — the state is in the text next to it. |

Landscape is where a "hide everything to gain space" design would win, and it is exactly the design that removes the stop button from reach. Nothing that stops a run is ever hidden.

## Multi-window and split screen

| Situation | Behaviour |
|---|---|
| App in split screen, `Compact` | Full functionality. The foreground service keeps the run alive. |
| App in split screen, very short | The composer remains pinned and visible; the content area shrinks. A run's status is in the notification, not only on screen. |
| Free-form window, resized across a breakpoint | The layout follows the new class immediately. State is in ViewModels, so nothing is lost. |
| App in a picture-in-picture-like small window | The chat shows the last message and the run state. Everything else is reachable by expanding. |

**Multi-window is a real use case for this app**: a person working on a laptop with a Linux VM side by side, checking a run's progress on the phone's split screen while the code is on the other device. The design supports it rather than treating it as an edge case.

## Split view: chat and terminal

| Size | Layout |
|---|---|
| Compact portrait | Terminal as a bottom sheet over the chat, or a tab within the chat screen |
| Compact landscape | 50 / 50 vertical split. Both usable. |
| Medium | 60 / 40 vertical |
| Expanded portrait | 60 / 40 vertical |
| Expanded landscape | 65 / 35 horizontal, side by side |

The terminal keeps its own scrollback and its own focus. Scrolling the terminal does not scroll the chat. This matters: a person comparing an error in the log with the assistant's explanation of it needs both visible and independent.

## Fonts and display scaling

| Scale | Behaviour |
|---|---|
| 0.85–1.0 | As designed |
| 1.0–1.15 | As designed |
| 1.15–1.3 | Tab labels switch to icon-only; the composer's action row wraps; cards grow |
| Above 1.3 | Layout clips rather than reflowing unpredictably; the app remains fully usable; a note appears in Settings offering a smaller default app text size |

The app's own text size setting is independent of the system scale, so someone who needs 130 % system scale and 85 % app text has both. The two are not the same setting and the app treats them separately.

## Orientation and configuration changes

| Change | Behaviour |
|---|---|
| Rotation | No state loss. The scroll position, the expanded tool cards, and the terminal scrollback survive. |
| Dark mode toggle | Immediate, no recreation of the run, no flicker. |
| Language change | Immediate, the app restarts into the new locale with the run unaffected. |
| Font scale change | Immediate; the layout adapts without a restart. |
| Screen size change (fold, unfold, split) | Immediate; the size class is observed. |

All of these are configuration changes that Android will apply while a run is in progress. A run is never interrupted by a rotation, and a conversation is never reloaded from scratch.

## Safe areas

| Edge | Rule |
|---|---|
| Status bar | Content never draws under it |
| Navigation bar | Consumed; the tab bar adds the inset to its height |
| Cutout, portrait | Consumed |
| Cutout, landscape | Consumed on both sides; the composer never extends into the cutout region |
| Hinge | Treated as a gutter; nothing important crosses it |
| IME | The composer lifts; the scroll container gets matching bottom padding so the last message is never hidden behind the keyboard |

## Testing

| Test | Cases |
|---|---|
| `ScreenshotSuite` | Every screen at compact / medium / expanded, both themes |
| `FoldableTest` | Chat, terminal split, and diff at each fold posture |
| `LandscapeCompactTest` | Every screen in landscape on a phone, including the composer and the permission sheet |
| `FontScaleTest` | Every screen at 1.0 and 1.3 |
| `MultiWindowTest` | The chat at 40 %, 60 %, and 100 % of a tablet screen, with a run in progress |
| `ReflowTest` | Every screen at 320 dp width, asserting no horizontal scroll outside code and the terminal |
| `RotationTest` | Rotation mid-stream, mid-diff, mid-permission, mid-terminal, asserting no state loss |
| `SizeClassTest` | A unit test that the size class helper returns the documented values at the documented breakpoints |

`tools/check_token_usage.py` also fails if a screen hard-codes a margin instead of using the size-class helper, because a hard-coded margin is how a screen ends up correct on the developer's device and broken on a foldable.
