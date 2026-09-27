# Illustration and icons

One icon set, one illustration language, and a hard rule about emoji. This document specifies them so that fourteen screens do not end up with fourteen visual dialects.

## The rule that motivates all of it

**No emoji anywhere in the interface.** Not as an icon, not as a status, not as a bullet, not as decoration, not as a placeholder.

Emoji in an interface is a signal that an icon was not designed. It also renders differently on every device, cannot be tinted, has no accessible name that is not a long emoji name read aloud, and its meaning is negotiated rather than specified. A status badge that says "✅" is ambiguous between "this worked" and "I am happy about this".

In the app's own copy: no emoji. In text the user typed, or that the engine emitted, emoji render as glyphs in a message — that is content, not interface, and the app does not police it.

## Icons

### Source

A single open-source icon set with an Apache 2.0 or MIT licence, vendored as SVG, converted to Compose `ImageVector` at build time. Not a Material icon, not the system icons, not a hand-drawn mixture.

**Chosen: Tabler Icons.** Outlined style, 24 dp grid, 2 dp stroke, consistent geometry, Apache 2.0, and a complete set including the specific things this app needs — terminal, git branch, git commit, shield, key, plug, bug, test tube, file diff, package, robot.

Material Symbols was rejected because it is the default and the default is the problem. Feather was rejected for being too thin at small sizes. Lucide is a defensible alternative and switching to it is a one-file change plus a screenshot diff.

### Rules

| Rule | Value |
|---|---|
| Grid | 24 × 24 dp |
| Stroke | 2 dp, round cap, round join |
| Sizes used | 16 (inline, in text), 20 (in labels), 24 (default), 28 (empty states), 32 (feature icons) |
| Tinting | Always `currentColor`, resolved to a token. Never a baked-in colour. |
| Fill | Never filled, except a `filled` variant for a selected state, where the fill is 12 % of the accent |
| Optical weight | Slightly reduced at 16 dp; a 2 dp stroke at 16 dp reads heavier than the same stroke at 24 |
| Accessibility | Every icon is decorative (`null` content description) when the adjacent text carries the meaning, and labelled when it is alone |

### The icon inventory

Only what is used. An unused icon is dead weight in the APK and a hint that a feature was planned and not built.

| Context | Icons |
|---|---|
| Navigation | chat, projects (folder), skills (puzzle), terminal, settings |
| Actions | send, stop (square), plus, close, chevron left / right / down, more (dots), search, filter, refresh, external link, copy, paste, edit, download, upload, check, x |
| Domain | file, folder, folder-open, git-branch, git-commit, git-pull-request, shield, key, plug, cloud, device, bug, flask (tests), package, shield-check, shield-x, alert-triangle, info, clock, coins (cost), zap (offload) |
| Status | See `StatusBadge` in `component-library.md` — drawn glyphs, not icons, so they are consistent with the text next to them |

**There is no trash icon in the inventory.** Nothing in this app deletes. A delete affordance anywhere would be a false promise, and its absence is a design statement that matches the rule in `vision.md`.

### Sizing discipline

- An icon in a button is 20 dp inside a 48 dp button.
- An icon in a list row's leading slot is 24 dp inside a 40 dp slot.
- An icon alone as a control is at least 48 dp, with the icon centred.
- An icon is never scaled to fill a container. A 24 dp icon stretched to 64 dp is a poster, not an icon.
- Stroke width scales inversely with size below 20 dp, so a 16 dp icon does not look bolder than a 24 dp one next to it.

## Empty-state illustrations

Four illustrations exist. Not one per screen — four, reused, because fourteen variants would be fourteen opportunities for inconsistency.

### The style

| Property | Value |
|---|---|
| Technique | Line art. Stroke, no fill, no gradient. |
| Stroke | 2 dp at 96 dp canvas, scaling proportionally |
| Colour | `textTertiary`, 60 % opacity. The illustration is quieter than the text it accompanies. |
| Complexity | At most 12 distinct strokes. A detailed illustration competes with the heading. |
| Aspect | Square, centred in a 96 dp box |
| Motion | None. Empty states do not animate. |

### The four

| File | Where | Depicts |
|---|---|---|
| `empty-chat.svg` | New chat, no conversations yet | A single line rising from a baseline, with a mark at the end. Implies the start of something. |
| `empty-projects.svg` | No projects yet | An outlined folder with a plus, offset. Implies adding. |
| `empty-skills.svg` | No skills installed | Two puzzle pieces, one floating above the other, separated. Implies installable. |
| `empty-terminal.svg` | No terminal session yet | A prompt and a blinking cursor as a static glyph. Implies readiness. |

Each is deliberately a *line* drawing of its subject rather than a rendering of a real object. The alternative — a small illustration of a folder with a drop shadow — is a sticker.

### The ambient wash

The empty chat screen has one soft radial light source behind the greeting: the `accent` token at 4 % opacity, falling to nothing across a 320 dp circle, positioned above and behind the mark. It is a light, not a gradient between two colours, and it is the only such effect in the app.

At 4 % it is barely perceptible, which is the point: it makes the screen feel less empty without becoming a thing the user looks at. It is a single `drawBehind` with a radial brush and no other screen has one.

## Icons in the terminal

The terminal does not use our icon set. It uses the terminal's own glyphs: the prompt, the cursor, the box-drawing characters, and the ANSI palette. Mixing our icon language into terminal output would be wrong — the terminal is showing another program's output, and our icons are not part of it.

## Implementation

| Concern | Approach |
|---|---|
| Source of icons | Vendored SVGs in `assets/icons/`, with the licence file |
| Conversion | A Gradle task converts each to an `ImageVector` at build time, into generated Kotlin. No runtime SVG parsing. |
| Access | A generated `AppIcons` object. `AppIcons.GitBranch`, not a string lookup. A typo is a compile error. |
| Tree shaking | Only referenced icons end up in the APK |
| Colour | Tinted at the call site from a token. No colour in the source SVG. |
| Licence | `assets/icons/LICENSE` plus an entry in `THIRD_PARTY_NOTICES.md` |

## Checks

| Check | Mechanism |
|---|---|
| No emoji in source | Grep for emoji code-point ranges in `res/values*/strings.xml` and in the UI source. Fails the build. |
| No hand-drawn icons | Every icon in the app resolves to `AppIcons.*`. A raw `ImageVector` built in a composable is a review finding. |
| No Material icon usage | Grep for `Icons.Default` and `Icons.Filled`. Fails the build. |
| No colour in a source SVG | A Gradle check parses the SVGs. Fails the build. |
| Icon renders at 16 dp | A screenshot test of the 16 dp row, reviewed for legibility |
| Empty state fits at font scale 1.3 | A screenshot test, because a heading that wraps pushes the illustration off the vertical centre |

## What we deliberately do not have

| Absent | Why |
|---|---|
| Animated illustrations | One infinite animation in the app, and it is the mark |
| Illustrated onboarding | The onboarding is text and a real progress list. A cartoon between steps is a tax on someone who wants to start. |
| Mascot beyond the mark | A character in a hat, holding a laptop, next to a headline |
| Emoji in the README | The README uses screenshots and a diagram. Emoji in a README is the same failure as emoji in the interface |
| Illustrations in error states | An error with a cartoon is an error the user does not take seriously. Errors get a glyph, a heading, and a sentence. |
| Illustration as a brand device | The mark is the identity. Adding a second visual character splits it |
