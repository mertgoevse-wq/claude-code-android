# Test strategy

How the app is tested, in what order, and what "tested" is allowed to mean at each layer. Depends on nothing; everything else in this section depends on it.

## The one question a test answers

Every test in this project answers one of exactly three questions:

1. **Does this compute the right answer?** — pure logic, no Android, no I/O.
2. **Does this do the right thing with its collaborators?** — real classes, fake edges.
3. **Does this look and behave right to a person?** — on a device or an emulator.

A test that answers none of these is a test we delete. Tests that exist only to raise a coverage number are worse than no test, because they change when someone renames a private function and they stop anyone from doing that.

## The layers

| Layer | Lives in | Runs on | Speed target | What it may touch | What it must never touch |
|---|---|---|---|---|---|
| `test/` (JVM unit) | `src/test/` | Host JVM, no device | Full suite < 90 s | Pure Kotlin, Room in-memory, Robolectric | Network, real files outside temp, real clock, real randomness |
| `androidTest/` (instrumented) | `src/androidTest/` | Emulator / device | < 4 min | Real Android framework, real Room file, real PTY | Network, the real key store, the user's home directory |
| `screenshotTest/` (Paparazzi/Roborazzi) | `src/test/` with a `paparazzi` source set | Host JVM, layoutlib | < 60 s | Compose, resources, design tokens | Anything needing a real device |
| `contractTest/` | `src/test/` | Host JVM, replayed fixtures | < 30 s | Event parser, permission evaluator, verification judge | A live provider |
| `fuzz/` | `src/test/` | Host JVM | Nightly | Every parser on the critical path | Anything else |

Layers are listed fastest first. A new test goes in the fastest layer that can honestly answer its question.

## The pyramid, in numbers

The shape is deliberately not a pyramid but a hexagon, because the middle layer is where this product's risk lives:

```
        screenshot tests      ~180 tests   visual regressions
      ─────────────────────────────────────────────────────
    e2e journeys                 ~24 tests   the six real user paths
   ───────────────────────────────────────────────────────────
  instrumented tests             ~140 tests  Android behaviour
 ───────────────────────────────────────────────────────────────
 contract + integration          ~260 tests  our seams and protocols
───────────────────────────────────────────────────────────────
      unit tests                 ~620 tests  logic, pure and fast
```

Roughly 1,200 tests. A unit test that is hard to write usually means a class with two reasons to change, and the fix is a design change, not a mocking library.

## The four seams that carry the weight

Most of this app is process supervision, event parsing, permission evaluation, and verification judging. Those four are the seams we test hardest, because everything else is a view or a wrapper.

| Seam | Interface | Tested with | Why it is risky |
|---|---|---|---|
| Engine events in | `EventParser` | Recorded JSON fixtures + fuzzing | Upstream format can change without warning; one bad line must not kill a run |
| Tool calls out | `PermissionEvaluator` | Table-driven, exhaustive | A false allow is a safety incident; a false deny is a broken product |
| Verification | `VerificationJudge` | Synthetic command output | A false `PASSED` is the worst bug this app can have |
| Process lifecycle | `ProcessSupervisor` | Real processes in a JVM sandbox | Races, orphaned children, exit-code semantics |

## The three laws

**1. No real keys, no real repos, no real network in any test.**
Not in unit tests, not in "just this one manual check". Keys live in a fake Keystore, repos in a fixture directory, network behind a `MockEngine` that replays a recording. See `test-data-safety.md`.

**2. A test that cannot fail is deleted.**
A test that asserts only `assertNotNull(result)` on a result we never inspect is a placebo. Every test names the behaviour it protects in its display name. If a test cannot fail today, it is either wrong or obsolete.

**3. A failing test is never weakened to pass.**
Deleting an assertion, loosening a matcher, or adding `@Ignore` to make a build green is a defect in the build, not in the code. This is the same rule the product enforces on the agent (`05-features/retry-and-self-healing.md`), and it exists here for the same reason: a green build that lies is worse than a red one.

## Determinism rules

Flaky tests are deleted, not retried. To make that possible:

- **No wall clock in logic.** Time is passed as a `TimeSource` parameter; tests use a fake that advances explicitly. The only places real time is allowed are timeout/retry delays, and those are tested with a virtual time source.
- **No real randomness.** Anything that needs randomness takes a seeded `Random`. The seed is in the test name when the test depends on it.
- **No network on the default path.** Every network call goes through an interface. Tests use the recording; the recording is committed.
- **No ordering between tests.** Each test builds its own fixture. No `TestCase1` sets up state for `TestCase2`.
- **No sleeps.** `Thread.sleep` in a test is replaced with an await on a real signal (a deferred completing, a file appearing). Where a real delay is unavoidable, it is bounded and asserted.

## What each document in this section covers

| File | Contents |
|---|---|
| `unit-tests.md` | The ~620 JVM tests, organised by subject, with the specific cases per subject |
| `integration-tests.md` | Instrumented tests: real Android, real files, real processes |
| `ui-tests.md` | Compose UI tests, semantics, the interactions we assert |
| `screenshot-tests.md` | Golden images: which screens, which states, how reviewed, how updated |
| `e2e-journeys.md` | The six complete user paths, end to end, with a fake engine |
| `contract-tests.md` | The event protocol and CLI surface, pinned against recorded fixtures |
| `performance-budgets.md` | Every measurable budget, the method, and the CI gate |
| `fixtures-and-test-data.md` | Where the fixtures live and how a new one is added |
| `test-data-safety.md` | The rules that keep real secrets and real repositories out of tests |

## CI gates

| Suite | Trigger | Blocking |
|---|---|---|
| Unit + contract | Every push | Yes |
| Static analysis (see `10-build/static-analysis.md`) | Every push | Yes |
| Screenshot diff | Every push | Yes, above a pixel threshold |
| Instrumented | Every push, one emulator | Yes |
| E2E | Every push to `main`, nightly elsewhere | Yes |
| Fuzz | Nightly | Yes, but a 24 h grace for new findings |
| Performance | Nightly on a reference device | Yes, above 10 % regression |

Nothing is advisory except new fuzz findings. An advisory gate is a gate nobody reads.

## Coverage

Coverage is reported and published, but it is not a target and it is not a gate. The review question is instead: *for this change, which behaviours are now unprotected?* A pull request that adds a parser branch without a test is rejected regardless of the coverage percentage, and a pull request that adds well-tested code without raising coverage by much is accepted.
