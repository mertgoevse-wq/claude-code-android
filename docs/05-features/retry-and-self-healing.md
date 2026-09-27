# Retry and self-healing

What happens when the work does not work. This is where most of the app's real value is, and where an unattended tool is most likely to burn money in a loop.

## The loop

```
verification FAILED
  → classify the failure
  → diagnose: read the error, find the cause
  → fix: edit or run
  → re-verify
  → PASSED ? → commit
             → attempt < budget ? → loop
                                → FAILED, with a report
```

## Classification

Before anything is retried, the failure gets a type. The type decides the strategy.

| Type | Recognised by | Strategy |
|---|---|---|
| `CompileError` | A compiler in the output, a file and line | Read the error, fix the file, re-verify |
| `TestFailure` | A test framework in the output, a failed test name | Read the assertion, read the test, fix either |
| `LintError` | A linter in the output, a rule id | Read the rule, fix the violation, or configure the rule |
| `TypeError` | A type checker in the output | The same as a compile error |
| `InstallError` | A dependency resolution failure | A different fix: network, version conflict, or a missing repository |
| `PermissionError` | A filesystem or permission failure in the output | Escalate to the user, do not retry blindly |
| `EnvironmentMissing` | A tool is not found | Do not retry. Offer the install, or the runner. |
| `Timeout` | Our own timeout | Retry once, then escalate |
| `Flaky` | The same command fails differently on a second run | Do not retry automatically. Report it as flaky. |
| `Unknown` | Nothing matched | Read the output, act generically, escalate sooner |

`EnvironmentMissing` is the important one. A missing Gradle is not a bug to retry; it is a capability gap, and retrying it nine times costs nine turns and fixes nothing. The app detects it, names the tool, and offers the two real answers: install it, or run this on a runner.

## The budget

| Setting | Value | Reasoning |
|---|---|---|
| Default | 3 | Enough for a compile error, few enough to not waste a subscription |
| Options | 3, 10, 50, unlimited | The user chose to make this configurable |
| Per project | Yes, not global | A throwaway script and a production migration want different numbers |
| Display | "Versuch 2 von 10", always visible | A counter the user cannot see is a counter they cannot trust |
| Consumption | Only on a failed verification, and on a failed fix | A successful turn costs nothing from the budget |
| Manual verification | Does not consume it | It is not a retry |
| Persistence | Survives an interrupt | A resumed run continues its count |

### What "unlimited" actually means

It means the app will keep trying. It does not mean the app will spend without limit — but the user should understand that it will, so:

| Protection | Detail |
|---|---|
| Anti-loop detection | Below, and it fires regardless of the budget |
| A failure-type change resets nothing | A budget is for one failure. If the failure type changes three times, the run is making progress and the counter is not a sign of a loop. The counter is per failure type, so a compile error fixed and a test failure fixed are two independent budgets. |
| A stalled failure escalates early | `Unknown` failures escalate after 2 attempts regardless of the budget, because an unclassified failure is usually not a bug |
| The cost advisory | A soft threshold, per project, default off. A warning, never a stop. |
| A notification at exhaustion | Always, with the report |

## Anti-loop detection

A separate mechanism from the budget, because a budget can be set to unlimited and a loop is not a retry.

### What counts as no progress

| Signal | Rule |
|---|---|
| Identical error | The normalised error text is byte-identical to the previous attempt |
| Same file, same line | The failure is at the same file and line as the previous two |
| No file changes | Nothing was modified between attempts |
| Same command, same output | The verification command produced identical output three times |
| A repeated identical edit | The same file was edited with identical content twice |

"Normalised" means timestamps, paths that contain the run id, durations, and line numbers that shift by pure noise are stripped before comparison. A run that fails identically in a file it did not touch is looping; a run that fails in a new place each time is investigating.

### Thresholds

| Pattern | Action |
|---|---|
| Same error twice | Logged, continue. Two failures are normal. |
| Same error three times | The run is aborted, regardless of the budget, with "Der gleiche Fehler ist dreimal aufgetreten." |
| No file change across two attempts | Escalated to the user before the third |
| The same edit applied twice | The second attempt is not applied; the diagnosis is treated as failed |

### The report

Not "it failed". A report that answers: what was tried, what each attempt did, what the error was, and what the app thinks is needed next.

```
Fehlgeschlagen nach 3 Versuchen

Aufgabe
  Unterstützung für benutzerdefinierte Prüfbefehle hinzufügen

Fehler
  ./gradlew test → Exit 1
  ProjectRepositoryTest.shouldReturnProjectByName  (ProjectRepositoryTest.kt:87)
  expected:<true> but was:<false>

Versuche
  1  Die Repository-Abfrage gelesen, Test gelesen, keine Ursache gefunden
  2  Mock angepasst, Test schlägt weiterhin an derselben Stelle fehl
  3  Gleicher Fehler, Abbruch nach Regel "gleicher Fehler dreimal"

Was vermutlich fehlt
  Der Test erwartet true, aber `getProjectByName` gibt null zurück, wenn
  das Repository leer ist. Der Test setzt keinen Seed. Das ist vermutlich
  ein Fehler im Test, nicht im Code.

Nächster Schritt
  Im Test vor der Abfrage einen Seed einsetzen:
  repository.insert(project)
  Zeile 82
```

**A report that names a likely cause is worth ten that list attempts.** The app's own inference is included and labelled as a guess, because a user can evaluate a guess in seconds and cannot evaluate nothing.

## Escalation

Some failures should not be retried at all.

| Situation | Action |
|---|---|
| `EnvironmentMissing` | Stop. Offer the install, or the runner. |
| `PermissionError` | Stop. The user has to fix a permission, or grant something. |
| The same error as the previous run of the same task | Stop, and say so: "Dieser Fehler trat im vorherigen Lauf mit derselben Aufgabe auf." Repeated identical tasks are usually a user mistake, and continuing would cost money. |
| The budget for this failure type is exhausted | Stop, with the report |
| The anti-loop rule fires | Stop, with the report |
| A permission request is pending for 24 hours | The run is marked `AWAITING_PERMISSION`, not failed. A phone in a drawer is not a failure. |

## Interaction with the autonomy level

The retry loop is the agent working on its own, so the level applies.

| Level | Retry behaviour |
|---|---|
| `ASK_EVERYTHING` | Every attempt asks for permission. Tedious by design. |
| `ASK_RISKY` | Each fix asks about the command. |
| `AUTO_WITH_CHECKPOINTS` | A retry at attempt 3 and the exhaustion are checkpoints. The user finds out that something needed four attempts. |
| `FULL_AUTO` | Nothing asks. The counter and the report are the only signals. |

**At `FULL_AUTO` a stuck run notifies twice, not once**: once when it starts retrying, and once when it gives up. Somebody who left a run unattended should learn that it is looping, not only that it failed — because the difference is money already spent.

## The fix, and its scope

| Rule | Reasoning |
|---|---|
| A fix edits only files inside the project | The project directory is the boundary. A fix that wants to change something outside it stops and says so. |
| A fix never weakens a check to make it pass | Changing a test to pass, adding a lint suppression, or relaxing a compiler setting is **detected and refused**. This is a hard block, and it is the single most important one for a self-healing tool. |
| A fix never deletes a file | Hard block |
| A fix never skips a test | Same class of refusal: a change that removes a test is a change that hides a failure. |
| A fix that touches more than N files escalates | Default N is 30. A fix that touches 200 files is a rewrite, and a rewrite deserves a human. |
| The diff is always available | Every attempt's changes are in the diff viewer, with the attempt number |
| An attempt's changes are committed together at the end | One commit per run, not per attempt, so the branch has a coherent history. Intermediate attempts are visible in the log. |

The "never weaken a check" rule deserves its own note, because it is the failure mode a self-healing system actually has. A model asked to make a failing test pass will, often enough, make the test not fail. Detecting it means watching the diff for: a test assertion weakened, a test deleted, a test skipped, a lint rule disabled, a compiler setting relaxed, a coverage threshold lowered, a dependency version changed to dodge a failure.

**When detected:** the attempt is refused, the run stops, and the app says exactly what it saw: "Der 2. Versuch hat den Test so geändert, dass er nicht mehr fehlschlägt: die Prüfung `expected true` wurde entfernt. Das wird nicht angewendet." That is a hard block, not a warning, and it is not configurable.

## What the user can do

| Action | Where | Effect |
|---|---|---|
| Extend the budget | The run header | The retry loop continues |
| Reduce the budget | The run header | The loop stops at the new count, with a report |
| Change the level | The composer | Applies from the next attempt |
| Stop | The composer | Interrupt, `wip/` commit, resumable |
| Look at the diff | The run summary | Every attempt's changes |
| Look at the report | The failure card | Attempts, errors, the inferred cause |
| Mark as flaky | The failure card | Records it; no automatic retry, and it appears in the project's known-flaky list |
| Re-run the whole task | The run summary | A new run on a new branch |

**There is no "skip the failing test" action.** The absence is deliberate and it is a hard block, not a missing feature. A user who genuinely needs to ship with a failing test can edit the project's own configuration in the terminal, where their authorship is explicit and in git history.

## Metrics

| Metric | Where | Why |
|---|---|---|
| Attempts per successful run | The project detail, per project | A rising number means a project is drifting |
| Failure type distribution | The project detail | Most common failure types, so the user knows what to fix |
| Time in the retry loop | The About screen, last 30 days | Whether the self-healing is earning its cost |
| Exhausted runs | The project detail | Runs that gave up, with links |
| The never-weakened count | The About screen | Should be zero, ever. A non-zero value is a bug in the detector, and it is reported as one. |

## Testing

| Test | Type |
|---|---|
| `Classification` | Unit — every output fixture maps to the documented type, including an unrecognised one |
| `BudgetMatrix` | Unit — 3, 10, 50, unlimited × every failure type, asserting the attempt count |
| `BudgetPerFailureType` | Unit — fixing a compile error does not consume the test budget |
| `BudgetSurvivesInterrupt` | E2E — interrupt at attempt 2, resume, the count continues from 2 |
| `AntiLoopIdenticalError` | E2E — three identical failures abort regardless of an unlimited budget |
| `AntiLoopNormalisation` | Unit — an error differing only in a timestamp or a run id is still identical |
| `AntiLoopProgressAllowed` | E2E — three failures in three different places do not trigger the rule |
| `EnvironmentMissingDoesNotRetry` | E2E — a missing Gradle stops after one attempt and offers the runner |
| `RepeatedTaskWarning` | E2E — the same task failing with the same error as the previous run stops and says so |
| `NeverWeakenACheck` | E2E — six weakening attempts (weakened assertion, deleted test, skipped test, disabled lint rule, relaxed compiler flag, lowered threshold) are each refused with the specific reason |
| `FixScopeBoundary` | E2E — a fix wanting to edit outside the project stops and says so |
| `FixFileCountEscalation` | E2E — a fix touching more than 30 files escalates |
| `ReportQuality` | Unit — the report contains the task, the error, every attempt with its action, and a labelled inference |
| `FullAutoNotifiesTwice` | E2E — at `FULL_AUTO`, a retrying run and a failed run each notify |
| `ManualExtendBudget` | E2E — extending from the run header continues the loop |
| `NoSkipTestAction` | UI — asserts no "Test überspringen" affordance exists in any failure UI |
