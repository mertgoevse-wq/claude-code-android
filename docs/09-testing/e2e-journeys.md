# End-to-end journeys

Six complete paths, from a cold app launch to a finished outcome, driven through the real UI against a deterministic fake engine. These are the tests that answer "does the product work for a person", and they are the last line before a release.

## The fake engine

End-to-end tests do not talk to a provider. They run against `FakeEngine`, a deterministic implementation of the `ExecutionBackend` seam that:

- Replays a scripted transcript of `AgentEvent`s per prompt, matched by a keyword.
- Runs verification commands for real, in a temporary project directory, so the verification path is genuine.
- Enforces the same `PermissionEvaluator` as production.
- Records every outbound message, so a test can assert what the agent was told to do, not just what the user saw.

Determinism comes from seeded randomness and a fixed clock (`VirtualClock`), injected through the same `TimeSource` seam production uses.

## Journey 1 — First run, project with a failing test

**The core promise of the product.** A person adds a project, asks for a bug fix, the agent finds it, changes it, runs the tests, sees them pass, and the run is marked `DONE`.

| Step | Assertion |
|---|---|
| Cold launch, onboarding | Key profile stored in the Keystore-backed store; biometric enabled |
| Create a project from a fixture repo | Cloned or imported; `Project` row exists; working tree is the fixture |
| Verification detection | The project's test command is detected and shown with the "detected automatically" badge |
| Send "the login test fails, fix it" | A `PLAN` appears with steps; the user approves |
| Streaming | Assistant text arrives token by token; a tool card appears for each command |
| A test command runs | The card shows the real exit code and the real failing output |
| Self-healing | The agent is prompted to fix; a second verification run happens |
| Second verification | Real `./gradlew test` on the fixture; genuinely fails before the fix and passes after |
| Final state | `PASSED` banner; run state `DONE`; the transparency log contains every command |
| Persistence | Force-stop the app, relaunch; the run is still there, still `DONE` |

The fixture repo is a tiny Gradle project with one deliberately failing test. Not a mock — a real build, so the verification judge is exercised for real.

## Journey 2 — The retry budget is exhausted

The failure path, and the one that must never be dishonest.

| Step | Assertion |
|---|---|
| Same setup as Journey 1 | |
| The fixture's test cannot pass without a network dependency that is unavailable | |
| Retry loop | Each attempt is visible with its error signature; the budget decrements visibly |
| Anti-loop detection | After the configured signature repeats, the run stops early rather than burning the whole budget |
| Exhaustion | Run state `FAILED`; the UI says the budget is exhausted; it does **not** say `DONE` |
| The user's escape hatch | "Show me what it tried" opens the transparency log, filtered to this run |
| Nothing weakened | The test file is unchanged; a "weakening" attempt would have been refused by the hard block |

The negative assertion is the important one: a failed run must never be renderable as `DONE`, at any point in the run's history, including after a restart.

## Journey 3 — A hard block refuses a real request

The safety promise.

| Step | Assertion |
|---|---|
| Project set up, autonomy level 3 (the most permissive) | The level selector confirms what it permits |
| Send "delete the build directory and clean everything up" | |
| Refusal | The agent's tool call for `rm -rf build` is **refused by the app's executor**, not by the model, not by a prompt instruction |
| Message | The user sees which block fired, in German, with the reason |
| The agent continues | It proposes an alternative and the run continues; the app did not crash and did not silently drop the message |
| Log | The refusal is in the transparency log as a first-class event |

Repeated for all five blocks in the same journey, as a table. The assertion for each is identical in shape: *the command never reached a shell*. That is checked by the fake engine recording its invocations, not by the UI text.

## Journey 4 — Session resume after a kill

| Step | Assertion |
|---|---|
| Start a long run (the fake engine streams 200 events with pauses) | |
| Kill the process (`adb shell am force-stop`) mid-stream at event 120 | |
| Relaunch | The app offers resumption — it never silently restarts |
| User accepts | `CLAUDE_CODE_RESUME_INTERRUPTED_TURN=1` is set; the transcript is continuous, not duplicated |
| Message numbering | The first 120 messages are unchanged; nothing appears twice |
| Verification | Runs from the last incomplete step, not from the beginning |

Also run with the screen off for 5 minutes and with the network disabled for 2 minutes mid-run, asserting the run reports honestly instead of failing.

## Journey 5 — Skills, end to end

| Step | Assertion |
|---|---|
| Browse skills; the list loads from a local index (no network in tests) | |
| Install a skill from a fixture Git repository | Cloned to the skills directory; the manifest is parsed and validated; failures are shown with the actual parse error |
| Inspect the skill | Its declared permissions are visible before it is used |
| Use it in a run | The skill's instructions reach the engine; the transparency log names the skill |
| Edit it with AI assistance | A deterministic completion; the editor shows a diff; saving persists |
| Uninstall | Removed; a run that referenced it says so rather than failing silently |

## Journey 6 — Remote runner, switching mid-project

| Step | Assertion |
|---|---|
| Set up a runner (the fake SSH transport; the handshake logic, host-key storage, and command execution are all real) | Host key recorded on first connect; a changed host key refuses with both fingerprints shown |
| Configure the project to run remotely | The UI says where the code will run and that the phone is no longer executing it |
| Start a run | Commands execute on the fake remote; results stream back identically to local |
| Kill the connection mid-run | The run pauses with a clear state; it does not report success or failure |
| Switch back to the phone | Requires confirmation, because the remote may hold changes; the state is reconciled or the conflict is stated |
| Runner goes offline | A clear stop, never a silent fallback to the phone |

## Coverage matrix

| Journey | UI states | Components | Verifier | Retry | Hard blocks | Persistence | Resume |
|---|---|---|---|---|---|---|---|
| 1 | ✓ | ✓ | ✓ | ✓ | | ✓ | |
| 2 | ✓ | ✓ | ✓ | ✓ | ✓ | | |
| 3 | ✓ | ✓ | | | ✓ | | |
| 4 | ✓ | ✓ | | | | ✓ | ✓ |
| 5 | ✓ | ✓ | | | | ✓ | |
| 6 | ✓ | ✓ | ✓ | | ✓ | ✓ | ✓ |

A blank in that table is a known gap, and each blank is either closed in a later phase or written into `14-build-plan/risk-register.md` with a mitigation. It is not left to be discovered.

## Runtime budget

| Journey | Wall time | Notes |
|---|---|---|
| 1 | < 90 s | Includes two real Gradle builds on the fixture |
| 2 | < 60 s | Retries use the virtual clock; the retry *delays* do not elapse |
| 3 | < 45 s | |
| 4 | < 60 s | Includes the force-stop and relaunch |
| 5 | < 45 s | |
| 6 | < 60 s | |

The whole suite targets under 8 minutes on the fast-lane emulator. If a journey needs longer, the reason is a missing seam, not a bigger timeout.
