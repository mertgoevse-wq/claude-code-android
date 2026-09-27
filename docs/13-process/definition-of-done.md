# Definition of done

A single, testable answer to "is this finished?" — for a task, for a phase, and for the project. If an item below cannot be verified, the work is not done, regardless of how finished it looks.

## 1. A task is done when all of these are true

| # | Criterion | How it is verified |
|---|---|---|
| 1 | The behaviour exists and works | A test that fails without the change and passes with it |
| 2 | The full gate is green | `./gradlew check` |
| 3 | Coverage thresholds hold | `koverVerify` on the affected module |
| 4 | Layer rules hold | `tools/check_no_android_imports_in_shared.py`; no DAO in a ViewModel, no platform import in `shared/domain` or `shared/core` |
| 5 | Error paths are handled | Every new failure has a user-facing message, a retryability, and a recovery action, matching `02-architecture/error-taxonomy.md` |
| 6 | The docs match the code | The behaviour's doc is updated **in the same commit** |
| 7 | No hard block was relaxed | Nothing deleted, nothing paid for, nothing made public, nothing pushed to the default branch, nothing hidden |
| 8 | No secrets in the diff | `scripts/check-no-secrets.sh` |
| 9 | The commit says why | `13-process/git-strategy.md` |
| 10 | The change is reviewable | One logical change, under ~400 non-test lines, with a description that names the risk |

## 2. A UI task additionally requires

| # | Criterion |
|---|---|
| 11 | Every state is designed and implemented: loading, empty, error, content — an "empty" state that was forgotten is an unfinished screen |
| 12 | UI tests for content, loading, empty, error, light, and dark |
| 13 | Screenshot baselines for both themes, reviewed rather than accepted blindly |
| 14 | Touch targets ≥ 48 dp, text ≥ 14 sp, contrast AA in both themes |
| 15 | Both themes reviewed by eye, not only by the contrast checker |
| 16 | An `ai-usage-policy.md` entry naming the design skill used |
| 17 | Screen-reader labels on every interactive element |
| 18 | Reduce-motion behaviour defined if the screen animates |
| 19 | Large font (200 %) does not clip or overlap |

## 3. A runtime or orchestration task additionally requires

| # | Criterion |
|---|---|
| 20 | The state machine has a test for every transition, including the failure transitions |
| 21 | A killed process resumes rather than restarts, and there is a test for that |
| 22 | Cancellation kills the whole process tree, verified against a real spawn, not a mock |
| 23 | Nothing is hidden: the raw output ring buffer is complete and visible |
| 24 | A fixture-based contract test exists if the change touches event parsing |

## 4. A phase is done when

| # | Criterion |
|---|---|
| 25 | Every task assigned to the phase in `14-build-plan/task-breakdown.md` meets §1 |
| 26 | Every "done when" clause in `14-build-plan/phase-plan.md` for that phase is met, verified on a real device or emulator where the clause says so |
| 27 | `./gradlew check` is green, and the E2E journeys that exist pass |
| 28 | `14-build-plan/progress-log.md` records what was built, what was skipped, and why |
| 29 | Any deviation from the spec is written as a spec amendment, not as a silent difference |
| 30 | The next phase's entry conditions hold: the repo is green, the docs are current, no blocker is outstanding |

**A phase that is 95 % done is not done.** It is reported as 95 % with the specific remaining item named. The alternative — declaring it green and moving on — is how a project ends up with a green build and a missing feature.

## 5. The project is done when

Straight from the spec's §24, restated as checkable items:

1. ☐ All 135 documents exist, are non-stub, and match the code.
2. ☐ Every source file in the spec's manifest exists and does what its one-line description says.
3. ☐ All seven phases are green against the §22.2 gates.
4. ☐ A fresh clone plus one command produces an installable, signed APK.
5. ☐ On a real device, a non-technical user can open the app, let it set up, paste a key, connect GitHub, pick a private repo, type an order, watch it work, review the diff, and get the PR — **without being asked a single question**.
6. ☐ The README is beautiful and honest, and passes its own checklist.
7. ☐ The hard blocks hold, and their tests pass.

Item 5 is the real test. Every other item is a proxy for it. If a user has to answer a question the app should have answered, the project is not done, however green the build is.

## 6. What is explicitly not "done"

| Tempting claim | Why it is not done |
|---|---|
| "The build is green" | Green with skipped tests, or green because a threshold was lowered |
| "The tests pass" | On the happy path only, or without a test that fails without the change |
| "It works on my device" | One device, one Android version, one provider |
| "The UI is done" | One theme, one font scale, no empty state |
| "The runtime works" | Until `claude --version` runs from a killed-and-resumed bootstrap on a real device |
| "Documentation is written" | Until it matches the code; a wrong document is worse than none |
| "The feature is implemented" | Until the failure path is implemented, named, and tested |
| "It's ready to ship" | Until every box in `12-delivery/release-checklist.md` is ticked by someone who ran it |

## 7. Who decides

| Case | Decided by |
|---|---|
| A task | The author, then the reviewer against §1 |
| A phase | The phase owner, with the log entry in `14-build-plan/progress-log.md` |
| The project | The spec's §24 list, verified in a fresh session by someone who did not build it |

The last row matters. A project cannot mark itself done; the person or agent that verifies §5 item 5 has never run the build and decides. If they cannot get from a fresh clone to a working task without asking a question, the answer is not done.

## Depends on

`13-process/development-workflow.md` · `13-process/code-review.md` · `14-build-plan/phase-plan.md` · `12-delivery/release-checklist.md` · `claude-code-android-spec.md` §22–24
