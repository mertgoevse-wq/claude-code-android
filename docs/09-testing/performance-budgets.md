# Performance budgets

Every budget has a number, a measurement method, and a device class it is measured on. A budget without a method is a wish.

## Reference device classes

| Class | Device | Used for |
|---|---|---|
| **Low** | The slowest device in the supported range — a 2021 mid-range phone, 4 GB RAM | The budgets below, unless stated otherwise. Designing for the low class is what makes the high class feel fast. |
| **Mid** | A 2023 mid-range phone, 8 GB | The nightly performance gate |
| **High** | Current flagship | Smoke only |

## Startup

| Budget | Target | Measured how |
|---|---|---|
| Cold start to first frame | < 900 ms | `Process.getStartUptimeMillis()` to the first `Choreographer` frame, on the low class |
| Warm start to first frame | < 250 ms | Activity recreation with the process alive |
| Time to interactive chat list | < 1 400 ms | First frame plus the first database query returning |
| Time to a run being startable | < 2 000 ms | Tap "send" to the first engine process spawned |

Cold start is dominated by nothing we control except class loading and the first query. If we are over, the fix is almost always an initialisation bug, not a code-size problem; a baseline profile is the tool, not a lazy-load framework.

## Interaction

| Budget | Target |
|---|---|
| Frame time, 60 fps | 16.6 ms, 95th percentile |
| Frame time, 90 fps setting on a 120 Hz panel | 8.3 ms, 95th percentile |
| Input-to-visual-response | < 100 ms |
| Scroll jank rate in the chat | < 1 % of frames over 32 ms |
| Scroll jank rate in a 10 000-line diff | < 3 % |
| Sheet open/close | 320 ms total, matching the slow motion token |
| Tap ripple appears | < 50 ms |

The mark's ambient animation must never cost more than 2 % of a frame. It is a `Canvas` loop with no allocation in the draw path, and a test asserts no allocation in the animation lambda.

## Streaming

| Budget | Target | Notes |
|---|---|---|
| Engine event to on-screen text | < 50 ms, 95th percentile | The whole point of the product |
| Under sustained streaming (50 events/s) | < 80 ms | |
| Batch flush interval | 16 ms, aligned to the frame | Text is flushed once per frame, never per event |
| Scroll-to-bottom lag under streaming | < 1 frame | |
| Cost meter update | Once per 500 ms, not per token | Updating it per token is the classic jank source |
| Token counter update | Once per 200 ms | |

Two budgets here are anti-patterns written down so nobody re-discovers them: no per-token state updates for anything that is not visible text, and no `LazyColumn` key computation that hashes the message content.

## Memory

| Budget | Target |
|---|---|
| Baseline RSS, empty chat | < 180 MB |
| RSS with a 5 000-message session loaded | < 320 MB |
| RSS with a 10 000-line diff open | < 400 MB |
| Terminal scrollback | Bounded at 2 MB of text, with a stated notice when trimming |
| Transparency log in memory | Never loaded in full; always paged |
| Per-image attachment | Downscaled to fit 2 048 px on the long edge, then compressed; a 12 MP photo never enters memory at full size |
| Growth after 50 chat open/close cycles | < 5 % over baseline |

The last row is the one that catches real leaks: a repeated journey, measured, with a threshold. It runs nightly.

## Storage

| Budget | Target |
|---|---|
| APK size | < 25 MB (no bundled Claude Code binary; the runtime is downloaded separately) |
| Downloaded native runtime, compressed | < 60 MB |
| Downloaded native runtime, installed | < 300 MB |
| proot profile, installed | < 2 GB, and stated before the user chooses it |
| Database after 1 000 runs | < 80 MB |
| Attachments | Stored outside the database; the database holds paths only |

## Battery and thermal

| Budget | Target |
|---|---|
| Idle with no run | < 0.5 % per hour |
| Active run, screen off | < 8 % per hour |
| Notification polling | Exponential, starting at 15 min when idle |
| Thermal | Below the SoC's sustained-performance threshold; when the OS reports throttling, the UI shows it and the run continues slower rather than silently |

## Build-time budgets

| Budget | Target |
|---|---|
| `./gradlew assembleDebug` from clean | < 6 min on a developer laptop |
| Incremental Kotlin compile | < 25 s |
| Unit test suite | < 90 s |
| Screenshot suite | < 60 s |
| E2E suite | < 8 min |
| Lint + static analysis | < 3 min |

## Measurement method

- **Frame times:** `Choreographer` frame callbacks collected in a debug-only overlay, plus Macrobenchmark on the nightly run. Never `System.nanoTime()` around a composable.
- **Startup:** Macrobenchmark's `StartupTimingMetric`, cold and warm, 10 iterations, median.
- **Memory:** Macrobenchmark `MemoryUsageMetric` at the same points, plus a per-run `Debug.getNativeHeapAllocatedSize()` delta in a soak test.
- **Network:** Measured against a recording host with a configured artificial delay, so the numbers are comparable day to day.
- **Storage:** `du` on the app's data directory in a scripted test.

## The nightly gate

The performance suite runs on the mid-class device on a release build, not a debug build, and fails the build when any budget regresses by more than 10 % against the recorded baseline. Between 3 % and 10 % is a warning that a human looks at; above 10 % fails. The baseline is committed and updated deliberately, with the diff explained in the pull request — the same rule as golden images.
