---
name: ui-design
description: The design system, the anti-slop bans, and the mapping from a screen's problem to the right installed design skill. Use for any UI work, any design review, and for the README. Delegates to the globally installed design skills; never improvises a visual direction.
---

# ui-design

Load the installed design skills and apply them under this project's rules.
This skill does not replace them; it routes to them and then holds the result
against `docs/03-design/anti-slop-rules.md`.

## 1. Route to the right skill

Search `design-library` rather than guessing from a name. Start with
`impeccable` for the general pass, then:

| The screen's actual problem | Skill |
|---|---|
| Bland, generic, no personality | `bolder` |
| Looks like a generated template | `craft`, `redesign-existing-projects` |
| Too loud, overstimulating | `quieter` |
| Grey, lifeless, no warmth | `colorize` |
| Type sizes and weights are arbitrary | `typeset` |
| Monotonous spacing, weak rhythm | `layout` |
| Too much on screen at once | `distill` |
| No motion where motion would explain | `animate` |
| Small misalignments before shipping | `polish` |
| Window sizes, foldables, split views | `adapt` |
| Android patterns, insets, focus, TV | `mobile-android-design`, `edge-to-edge` |
| Critical review of finished work | `critique`, `design-review` |
| Guidelines, WCAG, screen readers | `web-design-guidelines`, `accessibility` |
| High-end agency feel, anti-generic | `high-end-visual-design` |
| Full page from scratch | `website` |

The **README** gets the same treatment as a screen: `website`, `craft`, then
the 12-point checklist in `docs/12-delivery/github-readme-guide.md`.

## 2. Then the binding rules

`docs/03-design/anti-slop-rules.md` overrides any skill's suggestion. If a skill
produces something the rules ban, the output is discarded — not softened.

**Banned aesthetics.** No exceptions, no subtle use:

| Banned | Why here |
|---|---|
| **Liquid glass / glassmorphism** | Blurred translucency over text. Costs contrast, is a platform trend rather than a hierarchy, and dates a developer tool in six months |
| **Neomorphism** | Low-contrast raised surfaces. Fails WCAG AA, unreadable in sunlight — real for someone reading a diff outdoors |
| **Brutalism as a style** | Terminal-green-on-black and raw borders belong in the terminal pane, which faithfully reproduces a terminal. On app chrome it makes a work tool look unfinished |
| Neumorphic elevation | Fake elevation means unreadable hierarchy |
| Glass blur on a scrolling list | Motion plus transparency is an OLED legibility bug at low brightness |
| Skeuomorphism, leather, wood, stitching | Not a tool. Dates instantly |

Also banned: default Material purple, default Roboto, stock gradient heroes,
card-in-card-in-card nesting, emoji as icons, infinite motion other than the
logo, shake or confetti celebration, and any screen with a trash icon — nothing
in this app can be deleted, so the affordance would be a lie.

## 3. Tokens, then states

Every colour, size, radius, spacing, duration, and elevation comes from
`docs/03-design/design-tokens.md`. A hardcoded value in a composable is a
blocker. New token: doc first, then theme, then component.

Every screen: **loading, empty, error, content.** The empty state is where a
screen shows whether anyone cared.

## 4. The check

- [ ] Four states, both themes reviewed by eye
- [ ] 360 dp width, font scale 1.3, real and long content
- [ ] Touch targets >= 48 dp, text >= 14 sp, contrast AA
- [ ] Screen-reader label on every interactive element
- [ ] Reduce-motion fallback
- [ ] Exactly one primary action
- [ ] Every status carries a glyph and a word
- [ ] Would this be recognisably this app, and not a generated template?

## 5. Record it, then ship it

Add the row to the table in `docs/13-process/ai-usage-policy.md`: the screen,
the skill, the verdict. Then regenerate the Paparazzi baselines and **look at
the diffs**. Then `/verify` and `/ship` — the doc change ships with the UI
change, not after it.
