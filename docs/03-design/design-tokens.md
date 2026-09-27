# Design tokens

**These are binding.** A component takes tokens, never literals. A literal colour or spacing value inside a composable is a review finding, and `tools/check_token_usage.py` fails the build on one.

The values here are the source of truth for `shared/ui/theme/Color.kt`, `Type.kt`, `Spacing.kt`, `Shape.kt`, `Motion.kt`. If a value changes, it changes there and the docs follow.

---

## Colour

Full values and the measurement method are in `color-and-contrast.md`. Every pair below has a computed contrast ratio; none was chosen by eye.

### Light

| Token | Value | Used for |
|---|---|---|
| `background` | `#FAF9F5` | Screen background. Warm, never pure white. |
| `surface` | `#FFFFFF` | Cards, sheets, the composer |
| `surfaceSubtle` | `#F2F0EA` | Chips, inline code, secondary fills |
| `border` | `#E3E0D8` | Decorative separators only |
| `borderStrong` | `#948E82` | The boundary of any interactive control (3.09:1) |
| `textPrimary` | `#1F1E1B` | Body and headings (16.67:1) |
| `textSecondary` | `#5C5A54` | Supporting text, tool summaries (6.90:1) |
| `textTertiary` | `#6E6B65` | Timestamps, metadata (5.31:1) |
| `accent` | `#B25133` | Primary button fill, active state, the mark (5.10:1 with white text) |
| `accentText` | `#A84C2E` | Accent-coloured **text** on light surfaces (5.33:1) |
| `accentSubtle` | `#F5E6E0` | Accent-tinted fill, e.g. the active tab's background |
| `onAccent` | `#FFFFFF` | Text on the accent fill |
| `success` | `#3F7D58` | Verified, committed, pushed |
| `warning` | `#9A6B15` | Unverified, retried, degraded |
| `danger` | `#B23A2F` | Failed, refused, destructive-pending |
| `info` | `#3A6EA5` | Neutral system messages |
| `codeBackground` | `#F6F4EE` | Code blocks, the terminal in light theme |
| `diffAddBackground` | `#E4F0E6` | Added lines |
| `diffDelBackground` | `#F7E4E1` | Removed lines |
| `focusRing` | `#8F3F24` | Keyboard focus outline (6.86:1) |

### Dark

| Token | Value | Notes |
|---|---|---|
| `background` | `#1A1917` | Warm dark, never pure black |
| `surface` | `#232220` | Cards, sheets |
| `surfaceSubtle` | `#2C2A27` | Chips, inline code |
| `border` | `#3A3835` | Decorative separators |
| `borderStrong` | `#74716D` | Interactive boundaries (3.27:1) |
| `textPrimary` | `#F2F0EA` | 13.95:1 |
| `textSecondary` | `#B4B0A8` | 7.35:1 |
| `textTertiary` | `#99958F` | 5.34:1 |
| `accent` | `#E08A66` | Primary fill — **with dark text** (6.05:1 with `onAccent`) |
| `accentText` | `#E59572` | Accent text on dark surfaces (6.69:1) |
| `accentSubtle` | `#3A2A24` | Accent-tinted fill |
| `onAccent` | `#1A1917` | **Dark, not white.** White on this accent is 2.63:1 and would fail. |
| `success` | `#6FBE8C` | |
| `warning` | `#D9A94A` | |
| `danger` | `#E8796B` | |
| `info` | `#79AEE0` | |
| `codeBackground` | `#1F1F1C` | |
| `diffAddBackground` | `#213026` | |
| `diffDelBackground` | `#3A2724` | |
| `focusRing` | `#E59572` | 7.42:1 |

### Two rules that are not obvious

**`accent` and `accentText` are different tokens.** The first is a fill, the second is for text. Using `accent` for text on `background` gives 4.84:1, which passes; using it on `accentSubtle` gives 4.2:1, which does not. On `accentSubtle`, text uses `accentText`.

**The dark primary button has dark text.** This looks wrong in a preview and is correct: the dark accent is light, so dark text on it is 6.69:1 and white text is 2.63:1. There is no way to have white text on the dark theme's accent fill and pass AA.

### The terminal is always dark

Both themes render the terminal on `terminalBackground` with the dark ANSI palette. An ANSI palette has fixed assignments — "white" is light, "black" is dark — and a light terminal background makes the light ANSI colours invisible (white on light: 1.04:1). Inverting the palette produces colours that do not match what a shell expects. So: the terminal does not follow the theme. This is a deliberate decision and it is visible in the screenshots.

| ANSI | Value | ANSI | Value |
|---|---|---|---|
| `black` | `#1F1E1B` | `brightBlack` | `#6E6B65` |
| `red` | `#A63228` | `brightRed` | `#B23A2F` |
| `green` | `#2F6B45` | `brightGreen` | `#3F7D58` |
| `yellow` | `#8A6114` | `brightYellow` | `#9A6B15` |
| `blue` | `#2F5C96` | `brightBlue` | `#3A6EA5` |
| `magenta` | `#8A4380` | `brightMagenta` | `#A05296` |
| `cyan` | `#1F6A6E` | `brightCyan` | `#2F8085` |
| `white` | `#D8D5CD` | `brightWhite` | `#F2F0EA` |

### Syntax highlighting

| Token | Light | Dark |
|---|---|---|
| `keyword` | `#8F3F24` | `#E59572` |
| `string` | `#2F6B45` | `#6FBE8C` |
| `number` | `#2F5C96` | `#79AEE0` |
| `comment` | `#6E6B65` | `#99958F` |
| `type` | `#8A4380` | `#A05296` |
| `function` | `#1F6A6E` | `#79AEE0` |
| `punctuation` | `#5C5A54` | `#B4B0A8` |
| `plain` | `#1F1E1B` | `#F2F0EA` |

Only the eight token classes the highlighter actually emits. A language with more classes maps onto these; a language needing more than these is not syntax-highlighted rather than highlighted badly.

### Diff

| Token | Light | Dark |
|---|---|---|
| `diffAddBackground` | `#E4F0E6` | `#213026` |
| `diffAddText` | `#1F1E1B` | `#F2F0EA` |
| `diffAddMarker` | `#3F7D58` | `#6FBE8C` |
| `diffDelBackground` | `#F7E4E1` | `#3A2724` |
| `diffDelText` | `#1F1E1B` | `#F2F0EA` |
| `diffDelMarker` | `#B23A2F` | `#E8796B` |
| `diffHunkHeader` | `#F2F0EA` | `#2C2A27` |

Backgrounds for the diff, plus coloured markers and a `+`/`−` gutter. Never colour alone: the gutter sign carries the meaning for anyone who cannot see the colour.

---

## Typography

Full scale in `typography.md`. The shape of it:

| Role | Size | Weight | Line height |
|---|---|---|---|
| `displayLarge` | 34 sp | 400 | 42 |
| `displaySmall` | 28 sp | 400 | 36 |
| `titleLarge` | 22 sp | 600 | 30 |
| `titleMedium` | 17 sp | 600 | 24 |
| `bodyLarge` | 16 sp | 400 | 25 |
| `bodyMedium` | 15 sp | 400 | 23 |
| `bodySmall` | 14 sp | 400 | 21 |
| `labelLarge` | 15 sp | 600 | 20 |
| `labelMedium` | 13 sp | 600 | 18 |
| `labelSmall` | 12 sp | 500 | 16 |
| `mono` | 14 sp | 400 | 21 |
| `monoSmall` | 12 sp | 400 | 18 |

Nothing below 12 sp. Nothing below 400 weight for body text, because light weight on a warm low-contrast background is the fastest way to make text unreadable outdoors.

---

## Spacing

A 4 pt base scale. Named by the step, not by intent, so a component's padding is obvious from reading it.

| Token | Value | Typical use |
|---|---|---|
| `space1` | 2 | Icon-to-label inside a chip |
| `space2` | 4 | Between related inline elements |
| `space3` | 8 | Inside a button, chip padding |
| `space4` | 12 | Card internal padding |
| `space5` | 16 | Between cards, screen horizontal margin |
| `space6` | 20 | Card internal, generous |
| `space7` | 24 | Between sections of a list |
| `space8` | 32 | Between major sections |
| `space9` | 40 | Above a page title |
| `space10` | 48 | Screen vertical rhythm |

**The asymmetry rule:** space *above* a group is at least 1.5× the space *inside* it. That is how hierarchy is expressed without boxes. A uniform grid flattens everything into a spreadsheet.

**Screen margin:** `space5` (16) on compact, `space7` (24) on medium, `space8` (32) on expanded. See `responsive.md`.

---

## Radii

| Token | Value | Use |
|---|---|---|
| `radiusSmall` | 8 | Chips, badges, small inline elements |
| `radiusMedium` | 12 | Buttons, inputs |
| `radiusLarge` | 16 | Cards |
| `radiusSheet`` | 24 top, 0 bottom | Bottom sheets |
| `radiusFull` | 50% | Avatars, the mark's container, the send button |

Nothing between 12 and 16. A 14 dp radius is a decision nobody made.

---

## Elevation

Elevation is a **border and a tonal step**, not a drop shadow. This is the reference app's trick and it is why it looks flat rather than cheap.

| Token | Light | Dark |
|---|---|---|
| `level0` | `background`, no border | `background` |
| `level1` | `surface`, `border` | `surface`, `border` |
| `level2` | `surface`, `border`, 2 dp shadow at 6 % | `surfaceSubtle`, `border` |
| `level3` | `surface`, `border`, 8 dp shadow at 10 % | `surfaceSubtle`, `borderStrong`, 12 dp shadow at 30 % |

Dark mode needs *more* shadow to read as elevation, because a dark surface on a dark background is distinguished by luminance, not by occlusion. Two shadows in the codebase, both defined here.

---

## Motion

Full spec in `motion.md`.

| Token | Value | Use |
|---|---|---|
| `instant` | 100 ms | Press feedback, ripple |
| `fast` | 150 ms | Chip toggle, checkbox |
| `standard` | 220 ms | Card appearance, sheet present, list item insert |
| `slow` | 320 ms | Screen transition, mark state change |
| `ambient` | 2.8 s loop | The mark at idle |
| `easeOut` | `FastOutSlowInEasing` | Anything entering |
| `easeIn` | `FastInSlowOutEasing` | Anything leaving |
| `easeInOut` | `FastOutSlowInEasing` | Anything moving within |
| `emphasized` | spring, damping 0.7, stiffness 380 | The mark's expression changes |

**No animation exceeds 320 ms except the mark's idle loop.** A user waiting for a result will perceive a long transition as a hang.

---

## Haptics

| Token | Trigger | Pattern |
|---|---|---|
| `hapticTap` | Button press, chip toggle | `TextHandleMove` |
| `hapticSuccess` | Verification passed, run done | Double tap, 40 ms apart |
| `hapticWarning` | Unverified, retried | Single long |
| `hapticError` | Failed, refused | Triple short |
| `hapticPermission` | A permission sheet appears | Single medium |

All of them respect the system haptic setting. Haptics are never the only signal for something; they reinforce a visual or textual state that already exists.

---

## What a component may not do

- Hard-code a value that exists here as a token.
- Introduce a new colour without adding it to this document with a measured ratio.
- Use `accent` for text on `accentSubtle`; use `accentText`.
- Use `border` for an interactive boundary; use `borderStrong`.
- Use white text on `accent` in dark theme; use `onAccent`.
- Animate anything longer than `slow` except the mark.
- Use elevation instead of a border to separate two things at the same level.
