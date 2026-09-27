# Motion

Motion in this app has exactly four jobs: confirm an action, show that something is happening, show that something changed, and express the app's state. Anything else is removed.

## The rules

1. **Nothing exceeds 320 ms**, with one exception: the mark's ambient loop. A user waiting for a result perceives a long transition as a hang, and this app is mostly waiting.
2. **Nothing bounces.** No overshoot, no spring bounce, no jelly. Springs are used for the mark's expression changes and nothing else.
3. **Nothing loops** except the mark and a genuine indeterminate progress indicator.
4. **Motion never blocks input.** An animation in flight never prevents a tap. If a control is disabled, it is disabled for a reason that is stated on screen.
5. **Every animation has a reduced-motion variant.** See `accessibility.md`.
6. **Motion explains, it does not decorate.** If removing an animation loses no information, it is removed.

## Durations

| Token | ms | Use |
|---|---|---|
| `instant` | 100 | Press scale, ripple, checkbox tick |
| `fast` | 150 | Chip toggle, switch, tab indicator |
| `standard` | 220 | Card appear, sheet present, list item insert, content swap |
| `slow` | 320 | Screen transition, mark state change, diff expand |
| `ambient` | 2800 | The mark at idle, one full breath cycle |

## Curves

| Token | Curve | Use |
|---|---|---|
| `easeOut` | `FastOutSlowInEasing` | Everything entering. Fast to start, so a response feels immediate. |
| `easeIn` | `FastInSlowOutEasing` | Everything leaving. Fast to finish, so the app feels responsive to a dismissal. |
| `easeInOut` | `FastOutSlowInEasing` | Movement within a screen: tab indicator, expansion, reorder. |
| `emphasized` | spring, dampingRatio 0.7, stiffness 380 | The mark's expression change only |

`easeOut` on entry and `easeIn` on exit is the standard that feels right, and it is worth being pedantic about: entry that is slow to start feels laggy, exit that is slow to finish feels stuck. Using one curve for both is the most common motion mistake.

## The catalogue

Every animation in the app, with its purpose. If an animation is not in this table, it does not belong in the app.

### Press feedback

| Animation | Duration | Curve | Purpose |
|---|---|---|---|
| Button scale 1.0 → 0.97 → 1.0 | 100 | `easeOut` | Confirms the touch landed |
| Ripple from the touch point | 250 | `easeOut` | Familiar, expected, invisible as an animation |
| Icon button scale 0.94 on long-press | 100 | `easeOut` | Confirms a long-press was registered |

Ripple and scale both, not either. The ripple is local and conventional; the scale is global and confirms. On a primary button, the scale alone is enough and the ripple is suppressed, because the button already changes colour.

### Navigation

| Animation | Duration | Curve | Purpose |
|---|---|---|---|
| Screen push: fade + 8 dp slide | 320 | `easeOut` | Direction is clear |
| Screen pop: fade + 8 dp slide | 220 | `easeIn` | Faster out than in, so a back gesture feels snappy |
| Tab switch: crossfade only, no slide | 220 | `easeInOut` | Tabs are peers; sliding implies a hierarchy between them |
| Tab indicator | 220 | `easeInOut` | A 2 dp line that slides, plus a label colour change |
| Tab switch: content scroll position preserved | — | — | Returning to Chat must not lose the reader's place |

### Sheets and dialogs

| Animation | Duration | Curve | Purpose |
|---|---|---|---|
| Sheet present: slide from bottom + scrim fade | 320 | `easeOut` | Physical metaphor: it comes from where it will leave |
| Sheet dismiss | 220 | `easeIn` | |
| Scrim fade | 220 | `easeOut` | |
| Dialog: scale 0.9 → 1 + fade | 220 | `emphasized` | The only spring, because a dialog is an interruption and should feel distinct |
| Dialog scrim | 220 | `easeOut` | |

### Content

| Animation | Duration | Curve | Purpose |
|---|---|---|---|
| List item insert | 220 | `easeOut` | Fade + 4 dp rise, staggered 20 ms, capped at 8 items | 
| Card expand / collapse | 320 | `easeInOut` | Height animation, not a crossfade; a crossfade of a tall card looks like a glitch |
| Tool card expand | 220 | `easeInOut` | |
| Diff hunk reveal | 220 | `easeOut` | Staggered 30 ms per hunk, capped at 5 |
| New message appears | 220 | `easeOut` | Fade + 4 dp rise |
| Plan step check | 220 | `emphasized` | The checkbox fills, then a 1.06 scale pulse |

The stagger caps matter. A list of 40 items with a 20 ms stagger takes 800 ms to finish appearing, which is longer than any animation is allowed to be. Past the cap, items appear without delay.

### Streaming text

| Animation | Duration | Curve | Purpose |
|---|---|---|---|
| Text append | none | — | Text appears as it arrives. Animating it would put it behind the stream. |
| Caret | 530 ms cycle | linear | A 1 dp × 1.15 em line at the end of the streaming text |
| Tool card appears | 220 | `easeOut` | Appears on `ToolStarted`, not on completion, so the user sees work starting |
| Tool card running state | 1400 ms cycle | `easeInOut` | A 1 dp accent-coloured bar under the header, 30 % opacity, travelling left to right |

**The tool card appears on start, not on completion.** A card that appears when a tool finishes tells the user nothing for the thirty seconds the tool takes. A card that appears immediately and fills tells them the tool is running. This is the difference between "it is working" and "it is thinking", and only one of them is true.

### Progress

| Animation | Duration | Curve | Purpose |
|---|---|---|---|
| Indeterminate linear progress | 1400 ms cycle | `easeInOut` | For a wait with no known length |
| Determinate progress | 320 | `easeInOut` | When the length is known; used by the bootstrap steps |
| Cost counter | 320 | `easeInOut` | The number counts rather than jumping |
| Retry counter | 220 | `emphasized` | A pulse when the attempt increments |

Progress bar rules: a determinate bar is used the moment the total is known, and never a determinate bar that jumps backwards. A bar that goes from 80 % to 20 % is a bug, not a re-estimate.

### The mark

Detailed in `logo-animation.md`. Summarised: `ambient` loop at 2.8 s when idle, `slow` transitions when its state changes, `emphasized` spring for expression changes. The only indefinite animation in the app.

### System feedback

| Animation | Duration | Curve | Purpose |
|---|---|---|---|
| Snackbar in | 220 | `easeOut` | Slide from the bottom, 12 dp |
| Snackbar out | 150 | `easeIn` | |
| Haptic pairing | — | — | See `design-tokens.md`; haptics are never the only signal |
| Notification badge | 220 | `emphasized` | A scale pulse when a count changes |

## Reduced motion

When the system setting is on, `reduceMotion` is true and:

| Normally | With reduced motion |
|---|---|
| Slides and fades | Opacity change only, 150 ms |
| Staggered list insertion | No delay, no movement |
| Spring dialogs | `standard` curve, no scale |
| Mark's expression changes | Crossfade between expressions, no movement |
| Mark's ambient loop | **Still loops.** It is the app's only indicator of a live process, and a still mark reads as a stopped app. The amplitude is reduced by half; the loop is not removed. |
| Progress bars | The indeterminate bar keeps animating. It is information, not decoration. |
| Cost counter | Counts instantly |

**The two exceptions are deliberate.** A still mark and a still progress bar both communicate "something is happening", so stopping them removes information. Everything else in this table is either decoration or an affordance the user can live without.

## Implementation

| Rule | Mechanism |
|---|---|
| One animation clock | `withFrameNanos`, never `System.currentTimeMillis` in an animation |
| No `LaunchedEffect(Unit)` for an animation that should restart | Key the effect on the value that changes |
| Animatable cancelled on disposal | The Compose framework does this; no manual `Animatable` in a `remember` without disposal |
| `animateContentSize` for expanding cards | Not a manual height `animateDpAsState`, which fights the content |
| Derived state reads, not writes | `derivedStateOf` for scroll-linked effects, so recomposition is not on every frame |
| No `LaunchedEffect` that can restart a stream | Streaming state comes from a flow in the ViewModel, not from an effect |
| Every animation has a test | Screenshot tests at the start and end; a timing test for anything with a duration |

## What was deliberately not animated

| Not animated | Why |
|---|---|
| The chat message list during streaming | An item moving while the user is reading it is worse than no feedback |
| The cost meter continuously | It animates on change, not on every token. A number moving 40 times a second is unreadable. |
| The verification panel | It appears when verification starts, then updates per command. Cross-fading a list the user is reading is hostile. |
| Skeleton loaders on the terminal | A terminal has a defined shape: lines. A skeleton of grey rectangles is a lie about what is coming. |
| The tab bar icons | They change colour. An icon that morphs is a toy. |
| Diffs on load | They appear when asked for. A diff that animates in is a diff the user has to wait to read. |
