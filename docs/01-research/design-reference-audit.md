# Design reference audit

What the reference app is doing, described so we can build our own interface with the same level of care. This is a **teardown of design decisions**, not a source of assets. No logo, illustration, screenshot, or proprietary asset is copied; see `01-research/legal-and-trademark.md`.

**The reference was inspected 2026-09-27.** Reference may change; re-inspect before the design phase closes.

## What we took, and what we did not

| Took | Did not take |
|---|---|
| Overall calm: warm neutral surface, low chroma, generous whitespace | Any asset |
| One accent colour used sparingly and meaningfully | The wordmark or the starburst mark |
| Rounded cards with hairline borders instead of shadows | Exact corner radii or spacing values, which we re-derive |
| Asymmetric type: large quiet headings, small calm body | Typefaces |
| Motion that is short and purposeful | Animation timings, which we re-derive |
| Progressive disclosure: summaries first, detail on demand | The information architecture, which is ours |

## Surface and colour

The reference sits on a warm, near-neutral background — cream, bone, sand — rather than pure white or a cool grey. The warmth is what makes it read as calm rather than clinical, and it is the single most transferable decision here.

- Background is warm and very light; cards are marginally lighter or equal, separated by a **hairline border** rather than a drop shadow. Elevation is expressed as a border and a slight tonal step, not as a blurred rectangle floating over the page.
- Exactly one accent colour: a saturated orange, used for the mark, the primary action, and the active state. It appears nowhere else. Discipline here is what makes it look expensive.
- Semantic colours (error, warning, success) are present but muted, and never compete with the accent.
- Dark mode exists and is not an inversion. Backgrounds are dark warm-neutral, not black; borders gain contrast; the accent shifts slightly lighter so it stays legible on a dark surface.

Our values are in `03-design/color-and-contrast.md`, measured, not eyeballed.

## Layout

- **The composer is the anchor.** On an empty conversation the interface resolves to one input and one button, centred, with generous space above. Not a list of prompts, not a feature grid. The empty state makes a statement and then gets out of the way.
- **Content is one column, centred, width-capped.** Long lines of text are hard to read; the cap is not decoration.
- **Bottom tab bar** for top-level navigation, with the active tab carrying the accent.
- **Sheets, not dialogs**, for anything secondary. Dialogs interrupt; sheets continue.
- **Asymmetric spacing.** Space above a section is larger than space inside it. A uniform grid would flatten everything; the hierarchy comes from the rhythm, not from boxes.
- **The mark sits top-left and is small.** Identity through consistency, not through size.

## Type

- Large, confident headings. Short.
- Body text at a comfortable reading size with generous line height. Not dense.
- Monospace is reserved for code, file paths, commands, and numbers. Nothing else.
- No all-caps labels, no letter-spaced eyebrows, no badge soup.

## Motion

- Short. Most state changes are 150–250 ms.
- Ease-out on entry, ease-in on exit, no bounce, no overshoot.
- The one long-running animation is the mark, and it is the only thing on screen that is allowed to move indefinitely.
- No skeleton screens that shimmer forever. A spinner means "waiting"; a skeleton means "this shape is coming"; we use the right one.
- Streaming text appears progressively, but without a per-character cursor animation that flickers. Caret presence is subtle.

## The mark

The reference mark is a stylised starburst that reads as a claw or an asterisk. We do not reproduce it. What we take is the *idea* of a small animated identity in the corner that reflects state.

Ours: our own geometric mark with eyes, described in `03-design/logo-animation.md`. Calm breathing when idle, character-like behaviour when working. It is our most recognisable asset and it is legally ours.

## States we must design carefully

The reference handles states well. These are the ones that separate a real app from a demo:

| State | What the reference does | What we do |
|---|---|---|
| Empty conversation | One input, centred, quiet | Same. No prompt suggestions, no "try this" chips. |
| Loading | Nothing fake; no layout-shifting skeleton | A skeleton only where the shape is predictable; a spinner elsewhere |
| Streaming | Text grows smoothly, no jump | Same, plus a stable composer so the layout does not move |
| Long tool output | Collapsed with a one-line summary | Same, and the summary names the file and the action in plain language |
| Error | Plain language, actionable, not raw | Two lines: what happened, what to do. Raw text beneath, collapsed. |
| Offline | Says so, does not pretend | Explicit. A run continues; new runs are blocked with a reason. |
| Permission | A sheet naming the exact action and its consequences | Same, plus what cannot be undone |
| Cost | Present, understated | Running total in the composer footer, detailed in the summary |

## Copy tone

- Short sentences. Present tense. No exclamation marks.
- Says what happened, then what to do. Never "Oops!" or "Something went wrong 😕".
- Buttons name the action: "Lauf starten", not "OK" or "Los".
- Errors address the situation, not the user. "Der Server ist gerade überlastet" — not "Du hast zu viele Anfragen gestellt".

## Anti-patterns we are deliberately not copying

Some chat interfaces have a look. We are not building it:

- Purple-blue gradients on a dark background
- Glowing orbs, blurred blobs, glassmorphism panels
- Cards nested inside cards inside cards
- A hero section with a big centred headline above a row of feature pills
- Emoji as icons
- Bottom navigation with five identical glyphs and no state change beyond colour
- Progress bars that reset to zero on every step

## Verifiability

Every claim above about the reference was made by looking at the app. Where we did not verify something, it is not asserted. The one thing we deliberately do not do is screenshot the reference into our design docs; we describe, we do not copy.

## Open items

- Confirm the reference's exact behaviour for streaming with very long single messages; ours may need a different solution if the jump is visible. **TBD — verify during design phase 1.**
- Confirm how the reference handles the terminal case of a message containing a very wide code block on a narrow phone. Ours will scroll horizontally within the block; that is the plan regardless. **TBD.**
