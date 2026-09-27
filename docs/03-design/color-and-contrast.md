# Colour and contrast

Every colour in the app, with a measured contrast ratio and the rule for where it may be used. No value here was picked by eye.

## How the numbers were produced

WCAG 2.1 relative luminance, standard formula:

```
for each of R, G, B in {0x1F, 0x1E, 0x1B}:
    c = channel / 255
    c = c / 12.92                  if c <= 0.03928
    c = ((c + 0.055) / 1.055)^2.4  otherwise
L = 0.2126·R + 0.7152·G + 0.0722·B
ratio = (L_lighter + 0.05) / (L_darker + 0.05)
```

The script is `tools/check_token_usage.py`; it recomputes every pair in this document on each run and fails if a value is edited without updating the ratio. That is the whole point of keeping the numbers in the document.

## Thresholds applied

| Content | Minimum | Source |
|---|---|---|
| Body text, 14 sp and up | 4.5:1 | WCAG 1.4.3 AA |
| Large text, 18.66 sp bold or 24 sp regular | 3:1 | WCAG 1.4.3 AA |
| Icons carrying meaning | 3:1 | WCAG 1.4.11 |
| Boundary of an interactive control, when the boundary is the only thing identifying it | 3:1 | WCAG 1.4.11 |
| Purely decorative borders | none | Not content |

The last row is why `border` at 1.32:1 is legal. A hairline between two cards is decoration. The same colour on a text field would be a failure, because then it is the only thing telling you where to tap.

## Light theme

| Token | Hex | On surface | On background | Verdict |
|---|---|---|---|---|
| `textPrimary` | `#1F1E1B` | 16.67 | 15.82 | AAA |
| `textSecondary` | `#5C5A54` | 6.90 | 6.55 | AA |
| `textTertiary` | `#6E6B65` | 5.31 | 5.04 | AA |
| `accent` | `#B25133` | 5.10 | 4.84 | AA — legal as a fill and as text on background |
| `accentText` | `#A84C2E` | 5.62 | 5.33 | AA — the text-safe accent |
| `success` | `#3F7D58` | 4.90 | 4.66 | AA |
| `warning` | `#9A6B15` | 4.68 | 4.45 | AA, barely. Not for text below 14 sp. |
| `danger` | `#B23A2F` | 5.93 | 5.62 | AA |
| `info` | `#3A6EA5` | 5.31 | 5.05 | AA |
| `borderStrong` | `#948E82` | 3.09 | 3.09 | Passes the 3:1 control rule |
| `border` | `#E3E0D8` | 1.32 | 1.28 | Decorative only |
| `focusRing` | `#8F3F24` | 7.23 | 6.86 | Exceeds AA comfortably |

Filled surfaces with text on them:

| Pair | Ratio | Verdict |
|---|---|---|
| `onAccent` `#FFFFFF` on `accent` `#B25133` | 5.10 | AA |
| `textPrimary` on `surfaceSubtle` `#F2F0EA` | 14.63 | AAA |
| `accentText` on `accentSubtle` `#F5E6E0` | 4.62 | AA |
| `accent` on `accentSubtle` | 4.20 | **Below AA. Do not use. Use `accentText`.** |
| `textPrimary` on `diffAddBackground` `#E4F0E6` | 14.21 | AAA |
| `textPrimary` on `diffDelBackground` `#F7E4E1` | 13.60 | AAA |
| `textPrimary` on `codeBackground` `#F6F4EE` | 15.10 | AAA |

The `warning` value at 4.68 is the tightest passing value in the light theme. It is why warning is used for a background and a `+` marker, and for text only at `labelMedium` or larger. If a change pushes it below 4.5, darken it rather than relaxing the rule.

## Dark theme

| Token | Hex | On surface | On background | Verdict |
|---|---|---|---|---|
| `textPrimary` | `#F2F0EA` | 13.95 | 15.42 | AAA |
| `textSecondary` | `#B4B0A8` | 7.35 | 8.10 | AAA |
| `textTertiary` | `#99958F` | 5.34 | 5.90 | AA |
| `accent` | `#E08A66` | 6.05 | 6.69 | AA |
| `accentText` | `#E59572` | 6.72 | 7.42 | AA |
| `success` | `#6FBE8C` | 7.13 | 7.88 | AAA |
| `warning` | `#D9A94A` | 7.36 | 8.14 | AAA |
| `danger` | `#E8796B` | 5.58 | 6.17 | AA |
| `info` | `#79AEE0` | 6.77 | 7.47 | AA |
| `borderStrong` | `#74716D` | 3.27 | 3.62 | Passes the 3:1 control rule |
| `border` | `#3A3835` | 1.36 | 1.31 | Decorative only |
| `focusRing` | `#E59572` | 6.72 | 7.42 | AAA |

| Pair | Ratio | Verdict |
|---|---|---|
| `onAccent` `#1A1917` on `accent` `#E08A66` | 6.69 | AA |
| `#FFFFFF` on `accent` `#E08A66` | 2.63 | **Fails badly.** Never. |
| `textPrimary` on `surfaceSubtle` `#2C2A27` | 12.56 | AAA |
| `accent` on `accentSubtle` `#3A2A24` | 5.20 | AA |
| `textPrimary` on `diffAddBackground` `#213026` | 12.16 | AAA |
| `textPrimary` on `diffDelBackground` `#3A2724` | 12.32 | AAA |

### Why the dark primary button has dark text

This is the one place where the design system contradicts an instinct, so it is written down.

The dark theme's accent is a *light* colour, because accent on a dark surface needs to be light to be visible at all. Filling a button with a light colour and writing white on it gives 2.63:1, which is invisible. The correct pairing is a dark label on a light fill: 6.69:1.

If a designer or a developer finds white text on a dark-theme primary button, that is a bug, not a preference. The screenshot tests will catch it.

## Colour roles

| Role | Token | Where |
|---|---|---|
| Background | `background` | Screen |
| Card | `surface` | Cards, sheets, the composer |
| Recessed | `surfaceSubtle` | Chips, inline code, secondary fill |
| Primary action | `accent` fill, `onAccent` label | The send button, the one primary action per screen |
| Active state | `accentSubtle` + `accentText` | Selected tab, selected chip |
| Structure | `border` / `borderStrong` | Separators / control boundaries |
| Verified, done | `success` | The verified badge, the commit row, the PR row |
| Unverified, retried | `warning` | The unverified badge, the retry counter |
| Failed, refused | `danger` | The error row, a blocked action |
| Informational | `info` | System notices that are not a status |

## Rules

1. **One accent per screen.** A screen with two competing accent-coloured actions has two primary actions, which means neither.
2. **Never colour alone.** Every status colour is paired with text or a glyph: `✓ Geprüft`, `? Ungeprüft`, `✕ Fehlgeschlagen`, `⊘ Blockiert`. This is a hard requirement, not a nicety — roughly one in twelve men has a colour vision deficiency, and this app's whole value is honest status reporting.
3. **Semantic colours never brighten on hover.** Hover changes elevation or border, never hue.
4. **No colour is introduced without a row in this table.** A new colour means editing this file with a computed ratio, not adding a hex to a composable.
5. **Status colours are consistent across themes.** `success` means the same thing in both, and in the terminal, and in a notification.
6. **The brand accent appears nowhere else.** No second orange for a different meaning, no "highlight" orange, no gradient into it.

## What is deliberately absent

- **No second brand hue.** One accent, disciplined.
- **No gradients in the app chrome.** A gradient has no semantic meaning and competes with content. The one exception is the ambient background wash on the empty-chat screen, which is documented in `illustration-set.md` and is not used anywhere else.
- **No colour-coded categories.** A red project and a blue project would mean nothing.
- **No red for a non-error.** A destructive-pending indicator is `danger`; so is a failed verification; so is a refused action. They mean the same thing.
- **No pure black and no pure white.** `#000000` on a dark theme is an OLED trick that makes elevation unreadable, and `#FFFFFF` text is harsh. The app uses warm extremes deliberately.
