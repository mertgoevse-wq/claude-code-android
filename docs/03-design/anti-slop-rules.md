# Anti-slop rules

Binding on every screen. A change that violates one of these is rejected in review, and the reviewer subagent is instructed to cite this file.

These rules exist because the default output of a language model asked to "build a nice UI" is a specific, recognisable, bad thing. It is listed here so that it can be recognised and refused.

## The bans

### 1. No stock Material

No `MaterialTheme.colorScheme.primary` without an override. No `OutlinedButton` where a `Button` should be. No `Card` with elevation 6 dp. No `TopAppBar` in its default styling.

Material is a component library, not a design system. Using its defaults produces the app that every generated project produces, and the result looks like a settings screen from 2019.

**What to use instead:** the components in `component-library.md`. When Material is genuinely the right answer — a switch, a slider, a text field on Android — it is used with every token overridden.

### 2. No default typography

No `MaterialTheme.typography.bodyLarge` without an override. No `fontFamily = FontFamily.Default` outside the token layer. No Roboto in a screenshot.

See `typography.md` for the families and the scale.

### 3. No purple

`#6750A4`, `#6200EE`, `#7E57C2`, `Purple500`, "material purple", and anything in that family. The accent is a specific terracotta, and there is exactly one accent.

**The test:** if the screenshot would look equally at home in a different app, the colour is not specific enough.

### 4. No gradients

No linear gradient, no radial gradient, no mesh gradient, no conic gradient, in any surface, button, card, background, or illustration.

One documented exception: the ambient wash behind the empty-chat greeting, defined in `illustration-set.md`. It is a soft radial falloff at 4 % opacity, it appears on exactly one screen, and it is not a gradient between two colours so much as a light source.

### 5. No glassmorphism

No frosted glass, no translucent panel over content, no `blur()` on a background. Translucency over a moving list produces illegibility, and a blurred scrim is a gradient.

### 6. No glowing orbs

No soft blurred circle in a corner. No bokeh. No particle field. No animated background.

### 7. No emoji as icons

Not in a button, not in a list row, not as a status indicator, not as a bullet, not as a placeholder. Not 🚀 ✅ ❌ 🔥 💡 anywhere in the interface.

Status is `StatusBadge`, which uses drawn glyphs with a font that ships with the app. Icons are from one set, drawn as vectors, documented in `illustration-set.md`.

The one exception is text the user types or the engine emits. If the model writes 🎉, that is the model's output and it renders as a glyph in the message. The app does not author emoji.

### 8. No cards inside cards

A card within a card within a card is the single clearest signature of generated UI. Nesting depth is capped at one.

Where grouped content needs structure, it uses spacing and a heading, not a second surface.

### 9. No uniform spacing

Every gap identical. No hierarchy. A layout where everything is `16.dp` apart is a layout where nothing is grouped.

The asymmetry rule in `spacing-and-layout.md` is mandatory: space above a group is at least 1.5× the space inside it.

### 10. No placeholder text

No "Lorem ipsum". No "Your text here". No "Titel hier". No "Beschreibung".

Screenshot tests and previews use real content from the fixtures in `09-testing/fixtures-and-test-data.md`: a real file name, a real command, a real error message, a German compound long enough to test wrapping.

### 11. No `transition: all`

Compose equivalent: no `animate*AsState` without an explicit tween or spring specification, no `Modifier.animateContentSize` on a list, no implicit animation on a value that changes every frame.

An animation with a default duration is an animation nobody decided on.

### 12. No infinite animation except the mark

Loading spinners and indeterminate progress bars are permitted, because they communicate a real wait. Everything else that loops is decoration, and there is none.

### 13. No skeleton screen for unpredictable content

A skeleton is a promise about shape. It is used only where the shape is known and constant: a project card, a settings row, a repository card.

For a chat, a tool output, or terminal content — where the shape genuinely varies — the correct affordance is a spinner or nothing at all. A shimmering grey rectangle standing in for a paragraph is a lie with a loading animation.

### 14. No five identical tab icons with only a colour change

The tab bar is five destinations, and the active one is distinguished by a sliding indicator, a label weight change, and a colour change. The icons differ in shape, because a person with a colour vision deficiency has only the indicator and the weight.

### 15. No wall of text without structure

A message of 800 words in a single unbroken paragraph is a failure even if the words are good. Long content is broken by headings, lists, and code blocks, with generous space between them.

### 16. No centred body text

Centre-aligned paragraphs are unreadable beyond two lines. Centred is for headings, the empty-chat greeting, and nothing else.

### 17. No modal for information

Only two buttons and a real consequence justify a dialog. Everything else is a sheet or inline expansion.

### 18. No shake, no bounce, no confetti

No "success" animation that shakes. No confetti on a finished run. A run that finished is quiet: a status badge, a summary, a notification. The design is calm because the tool is for working.

### 19. No icon that means something else on another platform

The share icon means share, the trash icon does not appear anywhere (the app never deletes), a cloud icon means a remote runner and is never used for "sync" or "backup" or "upload" in an unrelated context.

In particular: **there is no trash icon in this app.** Nothing can be deleted, so a delete affordance would be a lie about what the app can do.

### 20. No fake affordance

No disabled button without a stated reason. No menu item that does nothing. No "Coming soon" in a shipped build. No setting that does not change behaviour.

### 21. No screenshot-only design

A screen that only works at 360 × 640 with the developer's content is not finished. Every screen is tested at compact, medium, and expanded, at font scale 1.0 and 1.3, in both themes, with real and long content.

### 22. No English-only copy

Every user-visible string exists in German and English, and the German is written by a German speaker, not translated by a machine. `writing-guidelines` is applied. "Antwort wird gesendet" is not German UI copy; "Wird gesendet" is.

### 23. No screenshot theft

No screenshot of another application in our documentation, our README, our Play listing, or our design docs. Our screenshots are our screens, captured by Paparazzi from our composables.

### 24. No AI-slop copy

Banned phrases, in the interface and in the README: "Delve", "Elevate", "Supercharge", "Seamlessly", "Effortlessly", "Game-changer", "Revolutionise", "Unleash", "Empower", "Robust", "Cutting-edge", "Next-generation", "Empowering", "Leverage", "Harness", "Journey", "Dive in", "Let's go", "Look no further", "Say goodbye to", "In today's fast-paced world".

Also banned: exclamation marks in the interface, three-bullet feature lists with no content, and any sentence that would make sense on a completely different product's website.

### 25. No borrowed aesthetic

The visual identity is a **calm, dense, warm work tool**. It is not a fashion.
These aesthetics are banned outright — not in the main surfaces, not as an
accent, not "subtly", and not if a design skill suggests them:

| Banned | Why it is banned here |
|---|---|
| **Liquid glass, glassmorphism** | Blurred translucency over text. It costs contrast, it is a platform trend rather than a hierarchy, and it dates a developer tool within six months. It also fights the OLED panel on this device class at low brightness |
| **Neomorphism** | Low-contrast raised surfaces. Fails WCAG AA and is unreadable in sunlight, which is a real constraint for someone reading a diff on a phone outdoors |
| **Neumorphic elevation on cards** | The same failure as neomorphism: the elevation is fake, so the hierarchy is unreadable |
| **Brutalism as a style** | Raw borders, terminal-green-on-black, no spacing. This is *correct* inside the terminal pane, which faithfully reproduces a terminal. It is *wrong* on app chrome, where it makes a work tool look unfinished |
| **Skeuomorphism** — leather, wood, stitching, linen | Not a tool. Dates instantly and signals a demo rather than a product |
| **Glass blur on a scrolling list** | Motion plus transparency is a legibility bug, not a flourish |

When a design skill produces any of the above, the output is **discarded, not
softened**. The rules beat the skill. `CLAUDE.md` and the `ui-design` and
`designer` agent definitions both say so, so the two layers cannot disagree.

### 26. The design skills are mandatory, not optional

Every screen and the README go through the installed design skills. The roster
and the routing table are in `CLAUDE.md` → "Design skills" and
`.claude/skills/ui-design/SKILL.md`. In short: search `design-library` for the
specialist that matches the screen's actual problem, start from `impeccable`,
then apply the specialist. `craft`, `typeset`, `polish`, `critique`, and
`design-review` are the quality floor; `bolder`, `quieter`, `colorize`,
`layout`, `distill`, `animate`, `adapt`, `impeccable`, `craft`,
`high-end-visual-design`, and `website` are the tools for a specific problem.

A screen with no entry in the `ai-usage-policy.md` ledger is a blocker. The
ledger is the evidence that the skills were actually used rather than claimed.

## The review checklist

Run before every UI change is merged. A failure is a rejection, not a nit.

- [ ] Would this screenshot be recognisably this app, and not a generated template?
- [ ] Is it free of liquid glass, neomorphism, glassmorphism, and brutalism? Brutalism is allowed in the terminal pane only
- [ ] Does the `ai-usage-policy.md` ledger name the skill used for this screen?
- [ ] Does every value come from a token?
- [ ] Is there exactly one primary action?
- [ ] Does every status carry a glyph and a word, not only a colour?
- [ ] Is space above a group at least 1.5× the space inside it?
- [ ] Is nesting depth one or less?
- [ ] Are all interactive states designed, not just the default?
- [ ] Is every touch target at least 48 dp?
- [ ] Does the screen work at 360 dp width, at font scale 1.3, and in both themes?
- [ ] Does it work with real content, including a long German compound and a 400-character path?
- [ ] Does it work with reduced motion on?
- [ ] Is there a screen-reader label on every interactive element?
- [ ] Any gradient, glow, blur, emoji, or infinite animation? All banned.
- [ ] Any `dp`, `sp`, or hex literal?
- [ ] Any copy from the banned-phrase list, or an exclamation mark?
- [ ] Any disabled control with no stated reason?
- [ ] Are the German and the English both present, and both natural?

## How this is enforced

| Mechanism | Where |
|---|---|
| Token check | `tools/check_token_usage.py` — fails on a colour, spacing, or type literal in a composable |
| Lint rules | Custom ktlint rules: no `MaterialTheme` without an override, no hard-coded `dp` in a composable |
| Grep checks | Banned hex values, banned phrases, `Emoji` in strings, `Modifier.blur` |
| Screenshot tests | Golden images per screen per theme, reviewed in the pull request |
| Accessibility test | Renders each status in greyscale and asserts states stay distinguishable |
| Reviewer subagent | `.claude/agents/reviewer.md` is instructed to cite this file and to reject, not to suggest |

The grep checks are blunt on purpose. A rule that requires judgement gets ignored under deadline; a rule that fails a build does not.

## The test for whether a screen is good

Show it to someone who has not seen the project. Point at any element. Ask what it is and what happens if you tap it.

If the answer requires reading the code, the screen has failed — regardless of whether it passes the checklist.
