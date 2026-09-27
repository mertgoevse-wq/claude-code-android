# Success metrics

Each metric has a definition, how it is measured, a target, and a deadline. A metric without a measurement method is a wish, so every one here names its source.

**There is no telemetry in this app** (see `11-operations/telemetry.md`). Local metrics are computed on the device from the local run history and shown to the user. The only figure that leaves the device is a crash report the user explicitly exports. Targets are therefore engineering commitments verified in CI or in the app, not growth numbers.

## The four that decide whether this succeeded

| ID | Metric | Definition | Measured by | Target | By |
|---|---|---|---|---|---|
| M1 | **First-task success** | Share of new users whose first task ends in a green, verified result without any support interaction | Local run history: runs where verification passed on attempt 1 or 2, divided by all first runs per install | ≥ 70 % | v1.1 |
| M2 | **Bootstrap completion** | Share of users who finish the runtime setup in one sitting | Local event: onboarding step `COMPLETED` reached | ≥ 85 % | v1.0 |
| M3 | **Crash-free runs** | Share of runs that complete without an unhandled crash | Local event: run ended in `DONE` or `FAILED_BY_JUDGE`, not `CRASHED` | ≥ 99 % | v1.0 |
| M4 | **Time to first result** | Median wall-clock time from "send" to a green verification on a warm device with the runtime already installed | Local run history | ≤ 6 min for a one-file change | v1.0 |

M2 and M4 are the honest ones. A beautiful interface that takes an hour to set up, or forty minutes to produce anything, is not a working product.

## Correctness

| ID | Metric | Target | Measured by |
|---|---|---|---|
| M5 | Hard-block integrity | Zero bypasses in the entire test suite, including direct calls to the executor | Unit tests that attempt each of the five blocks and require a refusal |
| M6 | Secret leakage | Zero occurrences of a key-shaped string in logs, fixtures, crash reports, or exported bundles | A redaction test that seeds known key patterns and asserts they are absent from every sink |
| M7 | Diff accuracy | The diff viewer never disagrees with `git diff` on the same tree | Property test comparing the renderer's output against parsed `git diff --numstat` |
| M8 | Cost accuracy | Displayed cost within 10 % of the provider's reported total | Test against recorded `total_cost_usd` from real runs |
| M9 | Event protocol stability | A change to `AgentEvent` cannot ship without a fixture update | Contract tests fail on any enum or field change |

## Quality gates (hard, measured in CI)

| ID | Gate | Target |
|---|---|---|
| M10 | Line coverage, `shared/domain` | ≥ 90 % |
| M11 | Line coverage, `shared/data` | ≥ 80 % |
| M12 | Screen coverage | Every screen has tests for content, loading, empty, error, light, dark |
| M13 | E2E journeys | All 5 pass on every merge |
| M14 | Screenshot diffs | No unexplained diff; every change either intended or reverted |
| M15 | Accessibility | No critical findings; all contrast ≥ 4.5:1; all touch targets ≥ 48 dp |
| M16 | Cold start | ≤ 1.2 s to interactive on a mid-range device (measured by macrobenchmark) |
| M17 | Idle memory | ≤ 180 MB while a run streams (measured by macrobenchmark) |
| M18 | Build reproducibility | Fresh clone, one command, signed APK. Verified weekly in CI from a clean checkout |

## Honesty (the ones nobody measures, which is why they are here)

| ID | Metric | Target | Measured by |
|---|---|---|---|
| M19 | Log completeness | Every command, diff, permission decision, error, and cost has a log entry | Test: for a synthetic run, the log entry count equals the event count, no drops |
| M20 | "Done" accuracy | A run is labelled done if and only if verification passed | Test: a run with a failing suite can never produce the "fertig" state |
| M21 | Unverified labelling | A run with no verification commands is labelled *unverified*, never *done* | UI test asserting the state |
| M22 | Undo completeness | Accepting or reverting a change always leaves a clean, documented state with no deletion of history | Test: revert a hunk, assert history intact |

## What we explicitly do not measure

- Time in app, sessions per day, retention, conversion, or any growth figure. This is a tool, not a feed.
- Prompt count, message count, or tokens per user. None of them indicate that the tool works.
- Cost reduction per run as a headline. A user who gets a correct result on the first try matters more than a user who is cheap. Cost is displayed to the user; that is all.

## How each number is surfaced to the user

In the app's About screen, a small diagnostics block shows the user's own last-30-day figures: tasks run, verification pass rate, total cost, average time to result, and time spent on failed runs. This is not a dashboard for us — it is the user checking whether the tool is earning its place, and it uses only their data on their device.

## Review cadence

These targets are reviewed at the end of every phase. A target that is missed twice in a row gets either a fix, or a documented decision to lower it with a reason. Silent erosion of a target is worse than an honest reduction.
