# Brand assets

What this project is called, what it looks like, and what may not be used. This document exists because "we were only being similar" is not a legal position, and because a fork needs to know exactly what to change.

## Identity

| Property | Value |
|---|---|
| Project name | `claude-code-android` |
| App display name | `Claude Code Android` |
| Package | `dev.claudecode.android` |
| Status | Unofficial, independent, not affiliated with Anthropic |
| Licence | Apache 2.0 |
| Mark | Ours. Four teardrops around a centre, described in `logo-animation.md`. |
| Wordmark | System sans, `titleLarge`, sentence case. No custom lettering. |
| Unofficial notice | In the app's name subtitle, on the About screen, in the Play listing, in the README's first screen, and in `NOTICE` |

The wordmark is set in the app's own typeface at a normal weight. A custom logotype for an independent project that already looks like another project's category is a liability, not an asset.

## The mark

| Property | Value |
|---|---|
| Construction | Four teardrop paths, rotated 0 / 45 / 90 / 135°, around a centre |
| Geometry | Inner radius 0.30, outer 0.46, valleys 0.34, in units of the canvas |
| Source | Vector paths in code, in `shared/ui/component/AnimatedClaudeMark.kt`. No image asset. |
| Master files | `brand/mark.svg` (three sizes), `brand/mark-monochrome.svg`, `brand/mark-constructions.svg` |
| Adaptive icon | A 108 dp foreground with the mark inside the 66 dp safe zone, plus a solid `background` colour layer |
| Notification icon | The silhouette, solid, one colour, 24 dp |
| Favicon / web | Not applicable. This is not a web app. |
| Minimum size | 16 dp legible. Below 20 dp the form loses its valleys and reads as a blob, so 16 dp is the floor and nothing in the app uses it that small. |

### What the mark is not

| Not | Why |
|---|---|
| A starburst | The closest common form; a lookalike is the risk this document exists to manage |
| A claw, crab, or asterisk | Those are the reference mark's readings |
| Orange, if the accent token changes | The mark takes `accent` at runtime. A brand asset that must be updated by hand when the palette changes is not integrated. |
| Recoloured per screen | One colour, one meaning |

### The test we hold it to

Place our mark next to the reference mark at 32 dp. If a viewer would describe them as the same design, the mark is wrong and this section needs work. The current construction passes: a four-fold teardrop arrangement reads as a distinct form, and the addition of eyes is a character decision the reference does not make.

## Colour

There is no separate brand palette. The mark uses the `accent` token, and the palette is defined and measured in `color-and-contrast.md`.

| Context | Value |
|---|---|
| Mark, light | `#B25133` |
| Mark, dark | `#E08A66` |
| Eyes | The background token, so they read as holes in the form |

**No private brand colour exists.** A brand colour that is 3 % different from the app's accent is a second source of truth, and it drifts the first time the accent is adjusted for contrast.

## Usage rules

### Required

| Context | Rule |
|---|---|
| Unofficial status | The notice appears before any description of the app's function |
| Attribution | Where the reference is discussed, it is named as the reference and the app is described as independent |
| The licence | Apache 2.0 in the repository, in the About screen, and in the Play listing |

### Prohibited

| Prohibited | Reason |
|---|---|
| Using Anthropic's or Claude's logo, wordmark, or starburst | Not ours |
| A mark that is a deliberate lookalike | A lookalike is the failure mode; the current mark exists to avoid it |
| Modifying the mark's proportions, or its colour outside the tokens | It is a system element, not artwork |
| Placing the mark next to a Claude or Anthropic logo in a way implying partnership | Implies an affiliation that does not exist |
| "Claude Code" as the sole branding of a product that is not this app | Only this app may use the name, descriptively, with the unofficial notice |
| Using a subscription logo or a claude.ai mark | Not ours, and implies a relationship that is both false and prohibited |

## Naming for forks

A fork should change:

1. The app display name — required.
2. The package id — required, and it must differ from ours in the Play Store.
3. The mark — recommended, and this document's geometry section is what makes it easy.
4. The `NOTICE` file and the unofficial status — required.
5. The OAuth client id, if used — recommended; see `01-research/github-auth-options.md`.

A fork may keep the repository name. A fork that keeps the app name, the package id, and the mark is distributing something that is not this project, which is both a naming problem and a confusion problem for users.

## Assets inventory

| File | Purpose |
|---|---|
| `brand/mark.svg` | The master, 512 × 512 |
| `brand/mark-24.svg` | 24 × 24, the notification icon source |
| `brand/mark-monochrome.svg` | Single-path silhouette for monochrome contexts |
| `brand/mark-constructions.svg` | The geometry grid and the safe zone, for anyone rebuilding it |
| `androidApp/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | The adaptive icon |
| `docs/03-design/logo-animation.md` | The animated behaviour |

**No PNG raster exports are committed.** The mark is vector everywhere: in the app as paths, in documentation as SVG, in the store listing as a Play-generated raster from the supplied asset. A rasterised logo in a repository goes stale the moment the geometry changes.

## Store listing assets

| Asset | Specification | Source |
|---|---|---|
| App icon | 512 × 512 PNG, no transparency, no rounded corners — Play applies the mask | Generated from `mark.svg` by `tools/` |
| Feature graphic | 1024 × 500 | Composed in code from the mark and the wordmark, on `background` |
| Phone screenshots | At least 4, 1080 × 1920 | Paparazzi, real screens, real content |
| Tablet screenshots | At least 1, landscape | Paparazzi |
| Screenshots content | The chat mid-run, the diff view, the verification panel, the project list | No mock-ups, no marketing art |

**Every screenshot is a real render of the app.** No illustrations, no device mock-ups with fake content, no staged screenshots. The screenshots are how a user decides whether to install, and a fabricated one is a lie that costs an install and a review.

The unofficial statement appears in the Play description's first line, not in a collapsed section.

## Where the notice appears

| Place | Wording |
|---|---|
| App name subtitle | "Inoffizielle Portierung" |
| Onboarding, first screen | One line, plain language |
| About screen | Full paragraph, with a link to this repository |
| Play description | First line |
| README | Above the first screenshot |
| `NOTICE` | The canonical text |
| GitHub repository description | One line |

The wording is "unofficial, independent, not affiliated with, endorsed by, or sponsored by Anthropic". It is not a footnote, and it is not apologetic. It is a fact about the project, stated early, and it does not appear anywhere else on the screen more than once.
