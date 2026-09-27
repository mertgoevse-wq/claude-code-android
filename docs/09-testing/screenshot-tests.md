# Screenshot tests

Golden images for every screen in every state, in both themes, at two font scales. The point is to catch the changes nobody noticed: a colour that stopped passing contrast, a padding that drifted, a card that gained a shadow it should not have.

## Tooling

Paparazzi for light/dark and font-scale rendering on the host JVM — no emulator, ~40 s for the full matrix. Roborazzi for the handful of cases that need a real `WindowInsets` or a real `LayoutDirection`, which Paparazzi handles less faithfully.

Both run as part of `testDebugUnitTest`; no device, no separate pipeline stage.

## The matrix

| Dimension | Values | Why |
|---|---|---|
| Theme | Light, Dark | Dark is not a filter; it is a different palette (see `03-design/color-and-contrast.md`) |
| Font scale | 100 %, 200 % | 200 % is where every clipping bug lives |
| Direction | LTR | v1 is not localised to an RTL language; the layout must not hard-code left/right, and one RTL render on `en-XA` proves it |
| Size class | Phone (411×891), Foldable open (841×1104), Tablet (1280×800) | See `03-design/responsive.md` |

Full cross-product is 12 renders per screen. With 14 screens × 4 states each that is 672 images. That is the right number — a golden-image suite that only covers the happy path catches almost nothing.

## What gets a golden image

| Screen | States captured |
|---|---|
| Onboarding | Step 1, API-key entry filled, biometric prompt, runtime download progress, runtime error, finish |
| Chat list | Empty, one chat, many chats, a chat with a failure badge, search active, search empty |
| New chat | Idle, project selected, model selected, validation error |
| Chat detail | Empty greeting, streaming, tool card collapsed, tool card expanded with output, tool denied, plan visible, verification `PASSED`, verification `FAILED`, verification `UNVERIFIED`, interrupted, error with retry, cost meter present, long unbroken token stream |
| Chat with terminal | Split, terminal-only, terminal with a full-screen redraw, a line with ANSI bold and colour |
| Diff viewer | Added/removed/context, a very long line, a binary file, a large diff, no changes |
| Project list | Empty, projects, an archived project, a project with a failing last run |
| Project detail | Four autonomy levels, verification commands set, no verification commands (the warning state), recent runs, hard-block list |
| Add project | Empty, path typed, path invalid, cloning progress, clone failed |
| Skills browser | Empty, installed, remote list loading, remote error, a skill with a permission declaration |
| Skill editor | Empty, filled, validation error, AI-generating state, diff preview |
| Terminal | Basic, full-colour, long line, scrollback, bell |
| Settings | Root, each sub-page, a destructive action awaiting confirmation |
| Remote runner setup | Each provider, OAuth in progress, connected, connection failed, host key changed warning |

That is 84 named states. The file naming is stable and never hand-renamed:

```
docs/../screenshots/light/100/phone/chat-detail__tool-card-expanded.png
```

## Review, not just diff

A golden-image diff that is only machine-checked will pass a hundred regressions nobody wanted. So:

- **A diff is a code review item.** The pull request shows before/after side by side. The reviewer answers: is this what we meant?
- **The threshold is zero for structural change** and per-pixel below 0.1 % for the rest. A changed corner radius changes hundreds of pixels; that is not "noise" and it must be looked at.
- **Font rendering differences between machines do not exist here** because rendering happens on the host with bundled fonts, not on a device. This is a large part of why Paparazzi is used instead of screenshots.
- **Goldens are committed.** They are part of the review artifact, not a build artifact.

## Updating goldens

```bash
./gradlew recordPaparazziDebug --variant=debug        # theme/font/size matrix
./gradlew recordRoborazziDebug                        # the inset cases
git add screenshots/
git commit -m "Update goldens: terminal long-line wrapping"
```

Rules:

- Goldens are updated **in the same commit** as the deliberate change. A commit that changes both the code and the golden is reviewable. A commit that changes only the goldens is a smell unless it is explicitly a "re-baseline after an intentional design change" commit, which must say so in the message.
- **A failing golden cannot be updated without a written reason in the pull request.** The check is whether the machine or the design changed, and the answer decides whether the golden moves.
- Deleting a golden requires the state to be removed from the screen spec too, not just the file.

## What golden images are explicitly not used for

- **Not for behaviour.** That is `ui-tests.md`.
- **Not for contrast.** Contrast is computed from the tokens by a unit test, because a screenshot cannot be measured.
- **Not for the whole app in one image.** One giant board hides changes. Each state is its own file.
- **Not for dark mode as an inversion.** Dark mode is captured separately because it is a different palette, and an inverted light screenshot would pass while shipping unreadable text.

## Anti-slop enforcement through goldens

Several rules from `03-design/anti-slop-rules.md` are directly visible in a golden and are therefore mechanically enforced:

| Rule | How the golden catches it |
|---|---|
| No gradients | A gradient changes hundreds of pixels |
| No drop shadows on cards | A shadow changes the entire card perimeter |
| No cards inside cards | A new border appears where none was |
| No purple | A hue shift is obvious at a glance and is a review conversation |
| No emoji | Renders as a coloured glyph or a tofu box, both immediately visible |
| Consistent 4 pt spacing | A 5 dp gap is a 1-pixel band in 12 images |
| Reading measure on long text | Lines visibly exceed the measure |
