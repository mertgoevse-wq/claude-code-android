# The mark

The animated mark in the top-left. Calm when idle, character-like when working. It is the app's most recognisable asset and the only thing on screen allowed to move indefinitely.

**This is our own design.** It is geometrically distinct from the reference mark; see `brand-assets.md` and `01-research/legal-and-trademark.md`. The test we hold it to: side by side with any other logo, nobody would confuse them.

## Geometry

An eight-pointed form built from four rotated teardrops arranged around a centre, contained in a circle.

| Property | Value |
|---|---|
| Canvas | 48 × 48 dp in the top bar; 96 × 96 dp on the empty-chat screen |
| Form | Four teardrops, each a 90° arc pair, rotated 0 / 45 / 90 / 135° |
| Inner radius | 0.30 of the canvas |
| Outer radius | 0.46 of the canvas |
| Point sharpness | Teardrop tips reach the outer radius; the valleys between reach 0.34 |
| Stroke | None. Filled paths. |
| Rotation symmetry | Four-fold, not eight-fold, so the form has a recognisable direction |
| Construction | Paths, not an image. Resolution-independent, themeable, animatable. |

Why teardrops and not a star: a star has hard points that fight small sizes, and it is the shape most logos of this kind use. A teardrop has a soft shoulder that reads well at 20 dp and still has structure at 96 dp.

## The two modes

### Idle

A slow breath. Nothing else.

| Property | Value |
|---|---|
| Cycle | 2800 ms |
| Scale | 1.00 → 1.04 → 1.00 |
| Opacity | 1.0 constant. The breath is scale only; a pulsing opacity would flicker against a light background. |
| Blink | Optional, every 4–7 s, 120 ms closed |
| Easing | Sine, in-out |
| Amplitude | 4 % |

**4 % is the ceiling.** Anything larger is a heartbeat, and a heartbeat on every screen of a tool the user leaves open for hours becomes the thing they notice and dislike. The mark should be the last thing you see, not the first.

### Working

The mark becomes a character. Two eyes appear, and its attention follows the work.

| Property | Value |
|---|---|
| Eye diameter | 0.11 of the canvas |
| Eye separation | 0.24 of the canvas, centred |
| Eye colour | `textPrimary` in light, `background` in dark — always the maximum contrast against the form |
| Pupil | A dot at 0.4 of the eye, offset by gaze |
| Eye entry | 320 ms, `easeOut`, eyes scale from 0 with a 1.12 overshoot settling to 1.0 |
| Expression transition | `emphasized` spring, 380 stiffness, 0.7 damping |
| Breath | Continues underneath, at 60 % amplitude |

The eyes are drawn in the *background* colour, which means they read as holes in the form. On the dark theme that means dark eyes on a light form. This is deliberate: drawn in a contrasting colour they would be two dots sitting on top of a shape, not eyes belonging to it.

## The state machine

The mark is driven by real events, never by a random timer. Its state is a pure function of the current run state.

| State | Trigger | Expression | Gaze |
|---|---|---|---|
| `IDLE` | No run, no pending work | No eyes | — |
| `THINKING` | `ThinkingDelta` | Eyes present, slightly narrowed (the lid lowers 12 %) | Up and to the left, the "recalling" direction |
| `READING` | `ToolStarted(Read, Grep, Glob)` | Neutral, lids up | Toward the tool card's position, clamped to the card's vertical centre |
| `WRITING` | `ToolStarted(Edit, Write, NotebookEdit)` | Neutral, lids up | Toward the tool card |
| `RUNNING` | `ToolStarted(Bash)` | Eyes narrow further (18 %), one eye 1 px wider than the other — the asymmetry is what makes it read as concentration rather than a squint | Down, toward the terminal indicator |
| `WAITING` | `PermissionRequested` | Eyes wide (lids up 20 %), still | At the centre, directly at the user |
| `VERIFYING` | Verification start | Neutral, lids slightly raised (an "eyebrow" of 1 px) | Down, toward the verification panel |
| `PAUSED` | Run suspended, user present | Eyes half-closed (50 %) | At the centre |
| `ERROR` | `Failed` with severity ERROR | One eye closes briefly, then both narrow to 24 % | Down, then to the error |
| `DONE` | Run finished, verified | Eyes open wide, a 1.06 scale pulse 320 ms, then relaxed | At the centre |
| `UNVERIFIED` | Run finished, unverified | Neutral, lids at 50 %, no pulse | At the centre |

**`DONE` and `UNVERIFIED` are visually different, and this is load-bearing.** A run that passed its checks gets a satisfied pulse. A run that could not be checked gets a flat, unsatisfied settle. The mark is the first thing a user sees when they pick up the phone, and it tells them whether the last run was actually checked. This is the `UNVERIFIED` principle from ADR-011 expressed as a single 24 dp image.

## Transitions

| From → to | Animation | Duration |
|---|---|---|
| `IDLE` → any working | Eyes scale in with overshoot | 320 ms |
| Any → `IDLE` | Eyes scale out and fade | 220 ms |
| Working → working | Expression crossfades, gaze eases | 320 ms |
| Working → `DONE` | Pulse then settle | 320 + 320 ms |
| Working → `ERROR` | Blink then narrow | 220 + 220 ms |
| Any → any, on a size change | Instant rescale, no animation | 0 ms |

**Transitions are never queued.** A state change while a transition is in flight interrupts and starts the new one from the current interpolated value. A run that flips between `RUNNING` and `THINKING` twenty times a second must not queue twenty animations.

## Gaze

Gaze is a vector from the mark's centre to a point on screen, clamped to a 6 dp radius, eased over 320 ms.

- The target is the **vertical centre of the thing being worked on**: the tool card, the terminal, the verification panel.
- The pupil never leaves the eye. Clamped, and the clamp is visible as the eye edge, which is what makes it read as a real eye.
- Gaze is only computed while the mark is on screen and visible. Off-screen, it is frozen at the last value and the state machine keeps running.
- **No eye tracking, no head tracking, no parallax.** The mark does not follow the user's finger. That is a toy, and it is also a privacy question the app should not be asking.

## Sizes and contexts

| Context | Size | Eyes | Notes |
|---|---|---|---|
| Top bar, idle | 28 dp | No | The mark at rest is small and quiet |
| Top bar, working | 28 dp | Yes | Same size. It grows attention, not pixels. |
| Empty chat, idle | 72 dp | No | The one large instance |
| Empty chat, working | 72 dp | Yes | |
| App launcher icon | — | No | Static. The launcher does not animate. |
| Notification small icon | 24 dp | No | Silhouette only, in `onAccent` |
| Notification large icon | 48 dp | No | Static render of the current state, monochrome-safe |
| Widget | 32 dp | Yes | Live state, no gaze target |

The notification icon is a **static** render. Android does not animate notification icons, and a notification that appears to be mid-blink looks broken.

## Reduced motion

| Normally | Reduced |
|---|---|
| Breath at 4 % | Breath at 2 % |
| Eyes scale in with overshoot | Fade in, 150 ms, no scale |
| Expression crossfade 320 ms | Crossfade 150 ms |
| Gaze easing 320 ms | Gaze snaps |
| `DONE` pulse | `DONE` holds a still, slightly enlarged state for 1 s |
| Idle loop | **Still loops** |

The idle loop is not reduced away. It is the only continuous indicator that the app is alive and a process is attached, and a completely static mark reads as a crashed app. The amplitude is halved, which honours the preference; the loop itself is information.

## Implementation

```kotlin
@Composable
fun AnimatedClaudeMark(
    state: MarkState,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
)
```

| Rule | Detail |
|---|---|
| Pure | A function of `MarkState` and time. No internal mutable state beyond the animation driver. |
| Testable without a clock | `MarkState` is an enum; the geometry is pure functions; the animation is a thin layer over `Animatable`. The expression tests need no Android. |
| One clock | All values derive from one `withFrameNanos` loop. Two loops would drift. |
| Canvas, not images | Four `Path`s, drawn each frame. A vector drawable cannot do gaze. |
| The ViewModel supplies the state | The mark holds no knowledge of runs, tools, or verification. A composable that knows what a `ToolInvocation` is, is a composable that will be tested by accident. |
| Battery | The loop pauses when the mark is off-screen and when the app is not visible. A 60 fps loop on a background screen is a battery bug. |
| Reduced cost | The geometry is precomputed `Path`s; only the transform matrix changes per frame. No path rebuilding. |

## Tests

| Test | Type | What it proves |
|---|---|---|
| `MarkIdleBreath` | Screenshot | The idle frame at scale 1.00 and 1.04, both themes |
| `MarkExpressions` | Screenshot | All 11 states, both themes, 3 sizes |
| `MarkStateMapping` | Unit | Every `RunState` and `ToolName` maps to the right `MarkState`. Exhaustive over the enums. |
| `MarkReducedMotion` | Screenshot | Halved amplitude, no overshoot |
| `MarkNoIdleCost` | Integration | The frame loop stops when the mark leaves the composition |
| `MarkThemeContrast` | Unit | The eye colour against the form is above 3:1 in both themes |

`MarkStateMapping` is the test that matters. A new `ToolName` that nobody mapped must fail the build, or the mark will sit in `THINKING` while a shell command runs — which is exactly the small dishonesty this app exists to avoid.

## Palette

| Part | Light | Dark |
|---|---|---|
| Form | `accent` `#B25133` | `accent` `#E08A66` |
| Eyes | `background` `#FAF9F5` | `background` `#1A1917` |
| Running indicator bar | `accentText` at 30 % | `accentText` at 30 % |
| Notification icon | Solid `onAccent` on transparent | Solid, single colour |

One colour for the form, taken from the accent token. The mark does not get a private colour. A brand element that is a slightly different orange from the buttons reads as a mistake, and would still be someone else's brand colour.
