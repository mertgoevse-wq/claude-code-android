# Typography

## The families

| Role | Family | Why |
|---|---|---|
| UI | **Inter** (variable) | A neutral, slightly humanist grotesque with unusually even weight distribution. It holds up at 14 sp on a small screen, has true small-caps-free numerals, and its x-height is large enough to survive being read outdoors. The default Android face is Roboto, which is competent and utterly anonymous; shipping it means the app looks like a settings screen. |
| Mono | **JetBrains Mono** | Open, designed for long reading sessions, wide enough that a terminal line does not feel cramped, and licensed for embedding. |

Both are bundled in the APK. No runtime font download, no FOUT, no network dependency for rendering text. This matters more than usual for this app, which is the one thing that must work offline.

**A third family is the only exception:** a serif is used for the empty-chat greeting and the app's own name, in a single weight, in one place. See below.

## Why a serif, and only there

Two places use a serif, at `displayLarge`:

- The greeting on the empty chat screen
- The app name in About

One weight, two places, both moments where the app is speaking rather than operating. A serif in a settings list would be decoration; a serif here is a change of voice. It is a deliberate contrast with the rest of the interface, which is why it is restricted: if the app used a serif for headings everywhere, this would be nothing.

## The scale

| Token | Size / line | Weight | Tracking | Use |
|---|---|---|---|---|
| `displayLarge` | 34 / 42 | 400 | -0.5 | Greeting, the app name |
| `displaySmall` | 28 / 36 | 400 | -0.4 | Screen titles on the new-chat screen |
| `titleLarge` | 22 / 30 | 600 | -0.2 | Project name, conversation title |
| `titleMedium` | 17 / 24 | 600 | 0 | Section headers, the composer's project name |
| `bodyLarge` | 16 / 25 | 400 | 0 | Assistant message text, the main reading size |
| `bodyMedium` | 15 / 23 | 400 | 0 | User message text, list item titles |
| `bodySmall` | 14 / 21 | 400 | 0 | Tool summaries, secondary descriptions |
| `labelLarge` | 15 / 20 | 600 | 0 | Buttons |
| `labelMedium` | 13 / 18 | 600 | 0.1 | Chips, tab labels, field labels |
| `labelSmall` | 12 / 16 | 500 | 0.2 | Timestamps, counts, badges |
| `mono` | 14 / 21 | 400 | 0 | Code, paths, commands |
| `monoSmall` | 12 / 18 | 400 | 0 | Terminal, inline code, log lines |
| `code` | 13 / 20 | 400 | 0 | Syntax-highlighted code blocks |

## Rules that are not obvious

**Negative tracking above 24 sp, positive below 13 sp.** Large text needs its letters pulled closer or it looks loose; small text needs a hair of space or it looks cramped and blurry. This is why `displayLarge` is -0.5 and `labelSmall` is +0.2, and it is the single most visible difference between a designed interface and a default one.

**Body text is never below 14 sp**, and never lighter than 400. Light weights fail outdoors, and this app is used outdoors.

**One message, one size.** Assistant messages are `bodyLarge` throughout, including inside lists and blockquotes. A message that changes size mid-way reads as an error.

**Code blocks are 13 sp, mono, horizontally scrollable, never wrapped by default.** A line of code that wraps is a line of code that lies about its length. A toggle to wrap exists, because long lines do happen, but it is off.

**Numerals are tabular everywhere a number changes.** Cost counters, token counts, elapsed time, file counts. Proportional figures make a number jitter as it updates, which during a live run is genuinely distracting. `FontFeatureSettings("tnum")` is set on every numeric token.

**No all-caps.** Not for labels, not for section headers, not as an eyebrow above a title. `labelSmall` at 12 sp with 500 weight and +0.2 tracking already reads as a quiet label. All-caps is a shout that means nothing here.

**No justified text.** Ragged right is correct on every screen width. Justification creates rivers of white space in German, which is worse than in English because compound words are longer.

**German is a first-class citizen in the type scale.** The longest common German compound easily runs 30 % longer than its English equivalent. `bodyLarge` at 16 sp wraps German comfortably at 360 dp; a 15 sp body would produce three-word lines in the settings list. Test strings in the screenshot suite include `Datenschutz-Grundverordnung`, `Benachrichtigungseinstellungen`, and `Zusammenfassungsansicht`.

## Line length

| Context | Max | Why |
|---|---|---|
| Message text | 68 characters | Beyond about 75 the eye loses the return sweep |
| Settings descriptions | 60 | These are read once, carefully |
| Terminal | No cap | The terminal scrolls horizontally |
| Tool summary | 52 | Two lines maximum, then ellipsis with the full text on tap |

Message text sits in a centred column capped at 640 dp, which at `bodyLarge` gives roughly 68 characters on a phone and comfortably more on a tablet. See `responsive.md`.

## Weight budget

Only three weights ship: 400, 500, 600. 700 is not used anywhere.

| Weight | Role |
|---|---|
| 400 | Body, code, the serif display text |
| 500 | `labelSmall` only |
| 600 | Titles, buttons, chips, tab labels |

Four weights would be four more artefacts in the APK and four more things to get subtly wrong. Hierarchy comes from size, weight, colour, and space before it comes from another weight.

## Font scaling

- The full system font scale is honoured, up to 130 %. Beyond that, layouts clip rather than reflow unpredictably.
- At 130 %+, the bottom tab bar labels switch to icon-only with a content description, and the composer's action row wraps to two lines.
- Fixed-height text containers are forbidden. Anything holding text grows.
- The terminal is exempt: it is a grid, and it truncates rather than reflows. That is what a terminal does.
- Screenshot tests include a run at font scale 1.3, because the failure mode is a clipped button and clipped buttons are found by users.

## Where each size is used

| Screen | Tokens |
|---|---|
| Chat | `bodyLarge` messages, `bodyMedium` user, `bodySmall` tool summaries, `labelSmall` timestamps, `mono` paths |
| New chat | `displaySmall` title, `displayLarge` greeting (serif) |
| Projects | `titleLarge` project name, `bodyMedium` description, `labelSmall` metadata |
| Skills | `titleMedium` name, `bodySmall` description, `monoSmall` path |
| Terminal | `monoSmall`, plus the user's font-size setting |
| Settings | `titleMedium` sections, `bodyMedium` values, `bodySmall` descriptions, `labelMedium` field labels |
| Verification panel | `monoSmall` commands, `bodySmall` summaries, `labelSmall` exit codes |

## The checking tool

`tools/check_token_usage.py` verifies two things on every build:

1. No `fontSize =` literal appears in a composable. Only a type token.
2. Every type token in this document has a consumer. An unused token is removed from the document, not left as dead weight.

The second check is the one that keeps the scale honest. A design system grows by accretion; this is the brake.
