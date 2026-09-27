# Spacing and layout

## The scale

Four points. Named by step, so padding in code reads as a number you can look up.

| Token | dp |
|---|---|
| `space1` | 2 |
| `space2` | 4 |
| `space3` | 8 |
| `space4` | 12 |
| `space5` | 16 |
| `space6` | 20 |
| `space7` | 24 |
| `space8` | 32 |
| `space9` | 40 |
| `space10` | 48 |

No value between steps. `15.dp` is a number nobody chose, and the next person to edit that file has to reverse-engineer it.

## The asymmetry rule

**Space above a group is at least 1.5× the space inside it.**

This is the single most important layout rule in the app, and it is why the interface reads as designed rather than generated. A card with 12 dp inside and 24 dp above it belongs to the section above it less than it belongs to its own content. A card with 12 dp on all four sides floats in ambiguity.

| Relationship | Inside | Above |
|---|---|---|
| Chip with a label | `space3` (8) | — |
| Button | `space4` horizontal, `space3` vertical | — |
| Input field | `space4` | `space3` to its label |
| Card | `space5` (16) | `space7` (24) |
| List item | `space5` horizontal, `space4` vertical | `space2` to the next item |
| Section on a screen | — | `space8` (32) from the previous |
| Screen title | — | `space9` (40) from the top |
| Message in chat | `space5` bottom margin | — |

## Screen margins

| Window size | Horizontal margin | Content max width |
|---|---|---|
| Compact (< 600 dp) | `space5` (16) | fluid |
| Medium (600–840 dp) | `space7` (24) | 640 dp, centred |
| Expanded (> 840 dp) | `space8` (32) | 640 dp, centred |

**Why 640 dp.** At `bodyLarge` (16 sp) that is about 68 characters, the point past which the eye loses the return sweep. On a tablet, a full-width chat column is unreadable. The cap is a reading decision, not a layout one.

## Screen skeleton

Every screen is the same shape. Consistency here is what makes an app feel like one thing.

```
┌─────────────────────────────────────┐
│  top bar (56 dp)                   │  title, and at most two actions
├─────────────────────────────────────┤
│                                     │
│  scrollable content                 │  the screen's one job
│                                     │  max width 640 dp, centred
│                                     │
├─────────────────────────────────────┤
│  pinned element, if any (composer)  │  only on Chat and Terminal
├─────────────────────────────────────┤
│  bottom tab bar (56 dp + inset)     │  5 destinations
└─────────────────────────────────────┘
```

- The top bar never scrolls away on a screen with a title. On Chat it collapses to a small mark after the first scroll; on every other screen it stays.
- Content padding is `space5` at the top, so content never touches the bar.
- The bottom padding is `space5` plus the tab bar height plus the navigation-bar inset. A list that scrolls to its end must not hide its last item behind the tab bar.

## Vertical rhythm on a scrolling screen

```
space9    screen title
space8    first section
space5    ┌ card ─────────────┐
          └───────────────────┘
space7    ┌ card ─────────────┐
          └───────────────────┘
space7    ┌ card ─────────────┐
          └───────────────────┘
space5    bottom padding
```

The gap *before* a card is `space7`; the gap between the title and the first card is `space8`. The first card is closer to its own content than to the title. This is what makes a list feel grouped rather than stacked.

## The chat screen specifically

The chat is the app's main surface and gets the most careful rules.

| Element | Rule |
|---|---|
| Column width | 640 dp max, centred |
| Message max width | 88 % of the column, so bubbles are distinguishable |
| User message | Right-aligned, `surfaceSubtle` fill, `radiusLarge` with the bottom-right corner reduced to `radiusSmall` |
| Assistant message | Full width, no fill, no bubble. Text on background. A bubble around a long answer is a lie about its length. |
| Gap between messages | `space5`, so a new answer is findable while scrolling |
| Tool card | Insets `space5` from the message column, `surface` fill, `border` hairline |
| Streaming caret | Inline at the end of the streaming text, 1 dp wide, `accentText`, no blink on reduced motion |
| Composer | Pinned, `surface` fill, top hairline, `space3` internal, action row below the field |
| Composer height | Grows to 6 lines, then scrolls internally. Never covers more than 40 % of the screen. |

**The composer is the one pinned element on the screen.** Everything else scrolls. This is what lets the user start the next message before reading the last answer.

## Lists

| Rule | Value |
|---|---|
| Item separation | `border` hairline, inset to the text, never a full-bleed divider |
| Item padding | `space5` horizontal, `space4` vertical |
| Item min height | 64 dp for a two-line item, 56 dp for a single line |
| Item min touch target | 48 dp, enforced even when the visual is smaller |
| Leading slot | 40 dp, holds a mark, a status dot, or a repository icon |
| Trailing slot | Metadata, right-aligned, `labelSmall`, `textTertiary` |
| Group headers | `labelMedium`, `textTertiary`, `space7` above, `space3` below |
| Swipe | Never. A swipe action on a coding tool destroys work by accident. The destructive path is a long-press into a confirm sheet. |

**No swipe-to-delete anywhere in the app.** This follows directly from the never-delete rule: a gesture that is easy to perform by accident must not be attached to an irreversible action.

## Cards

| Rule | Value |
|---|---|
| Fill | `surface` |
| Border | `border` hairline, 1 dp |
| Radius | `radiusLarge` (16) |
| Padding | `space5` |
| Shadow | `level0` — none. Cards are separated by the background, not by a shadow. |
| Internal structure | Title (`titleMedium`), then content, then metadata (`labelSmall`, `textTertiary`) |
| Maximum per screen | 4 before the content needs a different shape |

A card with a shadow and a border is both, which is the visual equivalent of shouting and whispering at once.

## Sheets

Bottom sheets are the app's secondary surface. Dialogs are reserved for decisions with exactly two options and real consequences.

| Property | Value |
|---|---|
| Radius | 24 dp top, 0 bottom |
| Max height | 90 % of the viewport, scrollable |
| Grabber | 4 × 36 dp, `borderStrong`, `space3` from the top |
| Dismiss | Swipe down, or the scrim, or a labelled action. Never by tapping the title. |
| Scrim | `background` at 40 %, no blur. A blurred scrim is a gradient, and gradients are banned. |
| Content padding | `space5` sides, `space5` top, `space8` bottom plus the inset |

## Grids

| Context | Columns | Gutter |
|---|---|---|
| Project cards, compact | 1 | — |
| Project cards, medium | 2 | `space5` |
| Project cards, expanded | 3 | `space5` |
| Settings sections, compact | 1 | — |
| Settings sections, medium and up | 2 | `space7` |
| Stat tiles | 2 or 3 | `space4` |

Grids are for things that are genuinely peers. A grid of mixed content is a layout that gave up.

## Insets

| Edge | Handling |
|---|---|
| Status bar | `WindowInsets.statusBars`, consumed, never drawn under content |
| Navigation bar | `WindowInsets.navigationBars`, consumed; the tab bar adds it to its own height |
| Display cutout | `displayCutout`, consumed in landscape |
| IME | `ime()`; the composer lifts above it, and the scroll container gets bottom padding equal to the IME height so the last message is never hidden |
| Gestures | `systemGestures` excluded from the tab bar's back-swipe area |

Edge-to-edge is on. Content draws behind the system bars, padding accounts for them, and no screen ever puts a critical control under a cutout or a gesture bar.

## The checks

`tools/check_token_usage.py` on every build:

- No `dp` literal in a composable. Only a spacing or size token.
- No hard-coded screen margin. Only the size-class helper.
- No `Modifier.padding(` with a raw number.

And `09-testing/ui-tests.md` requires at least one test per layout rule in this document, because a layout rule nobody checks is a rule that decays.
