# Verification

The feature this app exists to get right. Everything else is a convenience.

## The principle

> A run is finished when the project's own checks pass, or when it is honestly labelled as unverified. Never otherwise.

The agent saying "done" is a claim. A build that compiles and a test suite that passes are evidence. The app runs the evidence and reports it, and the word "done" means something specific.

## The four states

| State | Meaning | Colour | Glyph |
|---|---|---|---|
| `PASSED` | Every configured command exited 0 | `success` | `✓` |
| `FAILED` | At least one exited non-zero | `danger` | `✕` |
| `ERROR` | A command could not be run: not found, timed out, storage full | `danger` | `!` |
| `UNVERIFIED` | No commands are configured, or they could not be determined | `warning` | `?` |

`UNVERIFIED` is a first-class outcome, not a soft pass and not an error. It is the state a project without tests reaches, and it is the one most likely to be quietly upgraded to "pass" by a well-meaning implementation. ADR-011 exists because of this.

## Detecting the commands

On adding a project, and on demand, `Verifier.detect()` inspects the project and proposes a set.

| Detected | Proposed commands |
|---|---|
| `build.gradle`, `build.gradle.kts`, `settings.gradle` | `./gradlew assembleDebug`, `./gradlew test`, `./gradlew lint` |
| `package.json` with a `test` script | `npm test` |
| `package.json` with a `build` script | `npm run build` |
| `package.json` with a `lint` script | `npm run lint` |
| `pyproject.toml` or `pytest.ini` | `pytest` |
| `requirements.txt` with a `tests/` directory | `pytest -q` |
| `Cargo.toml` | `cargo build`, `cargo test`, `cargo clippy` |
| `go.mod` | `go build ./...`, `go test ./...` |
| `pom.xml` | `mvn -q test` |
| `CMakeLists.txt` | `cmake --build .`, `ctest` |
| Several toolchains | The union, ordered cheapest first |

| Rule | Reasoning |
|---|---|
| Detection is a proposal, never an application | The app guesses; the user confirms. A project with unusual scripts must not be broken by a confident guess. |
| The proposal is shown with the reasoning | "3 Befehle erkannt: baut, testet, prüft Stil. Sie kannst sie ändern oder löschen." |
| Commands are ordered cheapest first | A compile error should be found in twenty seconds, not after a full test run. |
| A build command comes first, then lint, then tests | The ordering that fails fastest |
| An empty proposal is normal | "Keine Prüfbefehle erkannt. Du kannst eigene angeben." The app does not insist. |
| A proposed command that fails to even start is reported specifically | "Befehl nicht gefunden: ./gradlew" is a different problem from a test failure, and the retry logic treats them differently. |

## Running the commands

| Property | Behaviour |
|---|---|
| Working directory | The project root, or the subdirectory the command needs |
| Shell | The runtime profile's shell. No shell-string building: the command is parsed into argv and executed directly. |
| Environment | The project's, plus the run's context variables, plus `CI=true` so tools take the non-interactive path |
| Timeout | Per command, default 15 minutes, configurable per project. A Gradle build on a cold phone cache can be slow, and killing it at 5 minutes would be wrong. |
| Output | Streamed live to the terminal pane and to a log file. The UI shows a per-command summary line, not a firehose. |
| Order | Sequential. A failing command stops the sequence, because the next one will fail for the same reason. |
| Working tree | **Not** reset between runs. A failing build's changes stay, because the app never deletes. |
| Cancellation | Available, kills the process tree, records the partial output |

### Parsing the output

A per-toolchain parser extracts what a person would quote.

| Toolchain | Extracted |
|---|---|
| Gradle | The failing task, the number of failed tests, the first error with its file and line |
| Espresso / JUnit XML | The failed test names, the class, the assertion message |
| npm / vitest / jest | The failed suite and test names, the count |
| pytest | The failed test ids, the count |
| cargo | The failed test names, the compiler error with its file and line |
| go test | The failing package, the test name |
| Lint tools | The rule id, the count, the file and line of the first |
| Anything else | The last 20 lines, marked "Ausgabe nicht ausgewertet" |

**When the parser finds nothing, it says so.** "Ausgabe nicht ausgewertet · Exit-Code 1" is honest. A parser that guesses a failure from the presence of the word "error" in a test fixture name is worse than no parser.

## Judging

`Judge` takes the `VerificationRun` and decides.

```
all PASSED          → the run may proceed to commit
any FAILED or ERROR → RETRYING, or FAILED
no commands at all   → UNVERIFIED, may proceed, labelled
```

| Rule | Reasoning |
|---|---|
| The decision is from exit codes only | Never from the agent's description, never from a log keyword, never from a model |
| A partial pass is a fail | "2 of 3 passed" is a failure, and the UI says which one |
| An `ERROR` and a `FAILED` are distinguished | A missing tool is a different retry strategy from a broken test |
| `UNVERIFIED` may proceed | Blocking would make the app useless for personal projects. Labelling it is the honest middle. |
| The judgement is recorded with a timestamp | So a summary can say when it was decided, not just what it decided |

## The `UNVERIFIED` treatment

Because this state is easy to render as a soft pass and hard to render honestly:

| Rule | Detail |
|---|---|
| The badge leads the run summary | It is the first row, above the changes and the cost |
| The wording is explicit | "Nicht geprüft — für dieses Projekt sind keine Prüfbefehle hinterlegt." Not "Keine Fehler". |
| The `?` glyph | Not a tick, not a dash, not grey. `warning` with a `?`. |
| No green anywhere in the card | A `UNVERIFIED` summary contains no success colour at all |
| The verification row says what is missing | "Keine Prüfbefehle" with a "Einrichten" action |
| The notification is different | "Fertig, aber ungeprüft" rather than "Fertig" |
| The commit still happens | With the message including "ungeprüft", so the history says so too |
| The mark is `UNVERIFIED`, not `DONE` | Per `03-design/logo-animation.md` |
| A PR is still opened | With the unverified state in the body |

That last one matters: somebody reading the repository in six months must be able to see that a commit was never verified, from the commit message alone.

## The verification panel

After the last tool call, a panel appears with one row per command.

```
Prüfung                                    3 Befehle · 2:14
─────────────────────────────────────────────────────
✓  ./gradlew assembleDebug            0:48   Exit 0
✕  ./gradlew test                     1:12   Exit 1
✕  ./gradlew lint                        …    nicht gestartet
      src/test/…/ProjectRepositoryTest.kt:87
      expected:<true> but was:<false>
─────────────────────────────────────────────────────
Fehlgeschlagen: 1 von 3
```

| Element | Behaviour |
|---|---|
| Header | The count, the total duration, and the state |
| Each row | The glyph, the command in mono, the duration, the exit code |
| A failure | The extracted summary, tappable to expand to the full output |
| A skipped command | "nicht gestartet" in `textTertiary`, not a red row. A command that never ran did not fail. |
| The footer | "1 von 3 fehlgeschlagen", or "Alle bestanden", or "Nicht geprüft" |
| Tapping a row | Expands to the full output, with a copy action and a link to the terminal |
| Retrying | Shows "Versuch 2" and the previous attempt's outcome collapsed beneath |

## The commit message

The commit records the verification state, so the repository carries the truth.

```
feat: Unterstützung für benutzerdefinierte Prüfbefehle

Aufgabe: Prüfbefehle konfigurierbar machen
Projekt: claude-code-android
Run: 01HQ…
Prüfung: 2 von 3 bestanden (./gradlew test fehlgeschlagen)
Autonomie: ASK_RISKY
Claude Code: 2.1.283
```

And for an unverified run:

```
chore: Ausgangslage für …

Aufgabe: Projekt einrichten
Prüfung: NICHT GEPRÜFT (keine Prüfbefehle hinterlegt)
```

**No co-author trailer.** The commit describes work the user asked for, done by a tool the user runs. Adding a "generated with" trailer is a judgement call about attribution, and it belongs to the user — so the app makes it a project setting, default off, and the setting is documented in the About screen rather than hidden.

## Running verification without an agent

`Verifizieren` on the project detail screen runs the commands directly, with no model call and no cost.

| Why it exists | A person who just edited a file by hand wants to know whether it still builds, and should not have to ask a model to find out. |
| Result | The same panel, recorded as a `VerificationRun` with no owning `Run` |
| Cost | Zero |
| Interaction with a running task | If a run is active, the button is disabled with "Ein Lauf arbeitet bereits an diesem Projekt." |
| Interaction with the retry budget | Manual verification does not consume the budget, because it is not a retry |

## What verification does not do

| Not | Reason |
|---|---|
| Judge code quality | It runs the project's checks. It has no opinion about the code beyond what the project's own tools say. |
| Add checks the project does not have | The app proposes them; the user adds them. Silently adding a linter to somebody's project is an unrequested change. |
| Run tests in a way the project does not intend | No injected coverage, no mutation testing, no framework detection overrides. It runs what is configured. |
| Retry a failed test automatically | A flaky test that passes on the fourth run is a flaky test. Retrying it inside verification hides the flakiness. The app offers to rerun once, manually. |
| Pass a run that only partially built | A partial build is a failed build. |
| Verify on the runner and claim it verified locally | The panel says where it ran: "Geprüft auf Oracle Free" |

## Testing

| Test | Type |
|---|---|
| `DetectionMatrix` | Unit — every project fixture in `09-testing/fixtures-and-test-data.md` produces exactly the documented commands |
| `DetectionNeverApplies` | Integration — detection produces a proposal; nothing is written without confirmation |
| `JudgeMatrix` | Unit — every combination of command outcomes produces the right state. Exhaustive. |
| `UnverifiedIsNotPassed` | Unit — no code path maps zero commands to `PASSED` |
| `UnverifiedRendersHonestly` | UI — no success colour anywhere in an unverified summary, the badge leads, the wording is asserted |
| `UnverifiedCommitMessage` | E2E — the commit contains "NICHT GEPRÜFT" |
| `ParserFixtures` | Unit — real output from every supported toolchain yields the documented extraction, and an unparseable one yields "Ausgabe nicht ausgewertet" |
| `VerifierStopsAtFirstFailure` | E2E — a failing first command does not run the second |
| `VerifierTimeout` | E2E — a hanging command times out with a specific error, not a generic failure |
| `VerifierNoReset` | E2E — a failing build's working-tree changes are still there afterwards |
| `VerifierDirectNoCost` | E2E — running verification without an agent costs nothing and produces a panel |
| `VerifierDoesNotConsumeBudget` | E2E — manual verification leaves the retry counter unchanged |
| `VerifierLocationStated` | UI — a verification performed on a runner says so |
| `NoDoneWithoutVerification` | Integration — the walk from ADR-004's invariant, over every run state transition |
