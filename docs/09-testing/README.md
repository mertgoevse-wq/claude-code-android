# Testing

Nine documents describing how this app is tested, and — more importantly — what the tests are not allowed to do.

## Read in this order

| # | File | For |
|---|---|---|
| 1 | `test-strategy.md` | Everyone. Start here. The layers, the three laws, the CI gates. |
| 2 | `unit-tests.md` | Whoever writes logic. The ~620 JVM tests and the cases per subject. |
| 3 | `integration-tests.md` | Whoever touches Android, files, or processes. |
| 4 | `ui-tests.md` | Whoever writes a screen. |
| 5 | `screenshot-tests.md` | Whoever changes a pixel. |
| 6 | `e2e-journeys.md` | Whoever changes a flow. The six paths that must never break. |
| 7 | `contract-tests.md` | Whoever touches the event parser or the CLI invocation. |
| 8 | `performance-budgets.md` | Whoever works near the hot paths. |
| 9 | `fixtures-and-test-data.md` | Whoever needs test data. |
| 10 | `test-data-safety.md` | Everyone. The non-negotiables. |

## The short version

- A test answers one of three questions: right answer, right behaviour with collaborators, right look and feel. Anything else is deleted.
- No real keys, no real repositories, no real network, no real user data. Ever.
- A failing test is never weakened to make a build green. That is a defect in the build.
- No sleeps, no wall clock, no real randomness, no ordering between tests.
- Flaky tests are deleted, not retried.
- Coverage is published, not targeted.
