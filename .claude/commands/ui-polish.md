---
description: Run the installed design skills over the changed screens and apply the anti-slop rules
argument-hint: [screen-or-composable-path]
---

# /ui-polish

Apply the design skills to `$ARGUMENTS`, then hold the result against the
anti-slop rules. Design work is not finished when it looks good to the author;
it is finished when it passes the checklist and the reviewer accepts it.

## 1. Load the design skills, in this order

The skills are installed globally. Load them; do not work from memory.

1. `design-library` — find the right specialist for this screen's actual
   problem. Search it rather than guessing from its name.
2. `impeccable` — the general visual and UX pass.
3. Then the specialist, chosen per problem:

| The screen's problem | Skill |
|---|---|
| Looks bland, generic, no personality | `bolder` |
| Looks like a template | `craft`, then `redesign-existing-projects` |
| Too loud, overstimulating | `quieter` |
| Flat, grey, lifeless | `colorize` |
| Type is unmotivated, sizes are arbitrary | `typeset` |
| Spacing and rhythm are monotonous | `layout` |
| Too complex, too much on screen | `distill` |
| No motion where motion would explain | `animate` |
| Small misalignments before shipping | `polish` |
| Breakpoints, window sizes, foldables | `adapt` |
| Android-specific UI patterns | `mobile-android-design`, `edge-to-edge` |
| Critical review of a finished screen | `critique`, `design-review` |
| Interface guidelines and accessibility | `web-design-guidelines`, `accessibility` |

## 2. Then apply the binding rules

`docs/03-design/anti-slop-rules.md` overrides any skill's suggestion. If a
skill produces something the anti-slop rules ban, the rules win and the skill's
output is discarded.

Banned aesthetics, no exceptions and no "subtle use":

| Banned | Why it is banned here |
|---|---|
| **Liquid glass / glassmorphism** | Blurred translucency over text. It costs contrast, it is a platform trend rather than a hierarchy, and it is exactly the look that dates a developer tool in six months |
| **Neomorphism** | Low-contrast raised surfaces. Fails WCAG AA and is unreadable in sunlight — a real constraint for someone reading diffs on a phone outdoors |
| **Brutalism** (raw, unstyled, terminal-green-on-black as a *style*) | Belongs to the terminal pane, which is a faithful reproduction of a terminal. Applying it to app chrome makes a work tool look unfinished |
| Neumorphic shadows on cards | Same failure as neomorphism: the elevation is fake, so the hierarchy is unreadable |
| Glass blur on scrolling lists | Motion plus transparency is a legibility bug on OLED at low brightness |
| Skeuomorphic texture, leather, wood, stitching | Not a tool. It dates instantly |

Also banned by the same rules: default Material purple, default Roboto, stock
gradient heroes, card-inside-card-inside-card nesting, emoji as UI icons, and
infinite motion other than the logo.

## 3. Tokens, not values

Every colour, size, radius, spacing, and duration comes from
`docs/03-design/design-tokens.md`. A hardcoded value in a composable is a
review blocker. If a design needs a token that does not exist, add the token
first, in this order: the token doc, the theme, then the component.

## 4. Check the whole screen, not the screenshot

- [ ] All four states exist: loading, empty, error, content
- [ ] Light and dark, both reviewed by eye
- [ ] 360 dp width, font scale 1.3, long content, real content
- [ ] Touch targets >= 48 dp, text >= 14 sp
- [ ] Every interactive element has a screen-reader label
- [ ] Reduce-motion produces a sensible fallback
- [ ] Exactly one primary action per screen
- [ ] Every status carries a glyph and a word, not only a colour
- [ ] Would this be recognisably this app, and not a generated template?

## 5. Record and re-run

Add the row to `docs/13-process/ai-usage-policy.md` naming the skill used and
the verdict. Then regenerate the Paparazzi baselines and **look at the
diffs** — a baseline accepted without being looked at is not a review.

Then `/verify` and `/ship`. The doc change ships with the UI change.
