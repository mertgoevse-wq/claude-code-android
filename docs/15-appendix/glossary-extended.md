# Extended glossary

`00-vision/glossary.md` explains terms in plain language for a non-technical reader. This file is the other half: the precise, technical definitions used throughout the documentation set, including the terms that only exist because of how this project works.

Read the plain-language one first if you are new. Read this one if you are about to change code and need to know what a word means *here* rather than in general.

## A

**ABI** — Application Binary Interface. The contract between compiled code and the CPU/system it runs on. `arm64-v8a` is the only ABI targeted; the engine is a `linux-arm64` binary, so the two must match or nothing runs.

**AgentEvent** — A sealed type in `shared/domain` representing one thing that happened during a run: a text delta, a tool start, a permission request, a plan update. The single vocabulary by which every `ExecutionBackend` talks to the UI. Defined in `02-architecture/event-protocol.md`. The single most important type in the codebase.

**Android Keystore** — Hardware- or software-backed key storage. The app holds an AES key there and uses it to encrypt secrets; the key material itself is not extractable on a device with a secure element. A rooted device weakens this — the plaintext on disk is still readable. Stated in `11-operations/privacy.md`.

**ANTHROPIC / OPENAI_CHAT / OPENAI_RESPONSES / CUSTOM** — The four `ProviderKind` values. They differ in URL shape, request shape, and streaming format. `CUSTOM` means the user picks the dialect, which is what makes any OpenAI-compatible server usable.

**Autonomy level** — The per-project policy controlling which tool calls stop for a tap. Four values in `05-features/autonomy-levels.md`. It sits **above** `HardBlockPolicy` in the stack, so no level can reach the hard blocks.

## B

**Baseline** — A committed golden image for a screenshot test. Accepted deliberately, never auto-updated. A diff with no explanation is a defect, in either direction.

**Bootstrap** — The state machine that makes Claude Code runnable on the device: download, verify, patch, install, check. Described in `06-runtime/bootstrap-state-machine.md`. Idempotent and resumable, always.

**Bionic** — Android's C library. It is not glibc, which is why an official glibc-linked Linux binary does not run unmodified. The entire reason this project's hard problems exist.

## C

**Checkpoint** — A defined point where an autonomous run stops and reports: after the plan, before a push, before installing dependencies. Distinct from a permission request, which is a question about a specific action.

**CI** — Continuous integration. The GitHub Actions workflow. A red gate is a failed build.

**Clean-room build** — A build in a container with no caches, no Google services, and no prior state. The F-Droid verification in `12-delivery/f-droid.md`.

**Configuration cache** — Gradle's cache of the configuration phase. Enabled here. A build script that reads the environment silently disables it, so build scripts do not read the environment.

**Contract test** — A test that pins an external interface's shape against recorded fixtures. In this project, the `AgentEvent` schema. It exists so an upstream change fails loudly in CI instead of quietly in production (R4).

**Cost meter** — The running per-run, per-project, per-day, and per-month spend display. A feature, not telemetry: local, resettable, never transmitted.

## D

**Dependency graph** — In the build-plan sense, the ordering of tasks (`14-build-plan/dependency-graph.md`), not the Gradle sense. The word is overloaded in this repository; the section number disambiguates.

**Diff** — The set of changes to a project's files, shown in `docs/04-screens/06-diff-viewer.md`. Acceptable per hunk; the git commit after a run is the authoritative record.

**Domain** — `shared/domain`: the rules of the product with no framework, no database, no platform. If it imports `android.*`, it is in the wrong place, and CI says so.

## E

**E2E** — End-to-end: a test that drives the real app through a real user journey on an emulator or device. Five of them, in `09-testing/e2e-journeys.md`.

**ExecutionBackend** — The interface that abstracts "where Claude Code runs": on the phone, over SSH to a home PC, on a cloud VM, or in GitHub Actions. Four implementations, one UI. Defined in `06-runtime/execution-backends.md`. The second most important type in the codebase after `AgentEvent`.

**Exposure** — Likelihood × impact, in `14-build-plan/risk-register.md`. A score of 15 or more requires a mitigation with a named owner.

## F

**F-Droid** — The FOSS app distribution channel. Also the strictest build constraint we support: no proprietary dependencies, buildable from source. `12-delivery/f-droid.md`.

**Foreground service** — An Android service with a persistent user-visible notification, which the OS will not kill under memory pressure. How a long run survives. `05-features/background-execution.md`.

**Friction** — The number of things a user must do before the product does something for them. The project's central metric, informally: one sentence, one project, one pull request.

## G — I

**glibc-runner** — The Termux-provided userspace shim that lets glibc-linked binaries execute on Android. The core of Profile A. Verify the exact package name and version at build time; see `15-appendix/references.md` §1.

**Hard block** — One of the five rules that no setting, mode, or user can override: never delete, never spend, never make public, never push to the default branch, never hide. Implemented in `HardBlockPolicy`, tested, and enforced at three layers. `08-orchestration/permissions.md`, spec §22.1.

**Idempotent** — Safe to run twice with the same result. Every bootstrap step is idempotent, which is what makes resume possible.

**Judge** — The step that decides whether a task actually succeeded, as distinct from the agent's claim that it did. A judge that always agrees with the agent is decoration. `05-features/verification.md`.

## J — M

**Koin** — The dependency injection framework. Multiplatform, no code generation. Picked in the spec's D-stack for that reason.

**Kover** — The Kotlin coverage tool. Produces the numbers the gates in `09-testing/test-strategy.md` compare against.

**Mapper** — `AgentEventMapper`: the one place that parses engine output. Nothing else parses it. This is the mechanism by which R4 is mitigated.

**Milestone** — A checkpoint with a verification that someone who did not do the work can run. `14-build-plan/milestones.md`.

**MVP / M0–M8** — Milestones M0 through M8 in `14-build-plan/milestones.md`. Distinct from the seven phases; a milestone is an outcome, a phase is a body of work.

## N — P

**Native profile** — Profile A: the patched glibc binary, no full userland. Fast, small, the default. The trade-off is that it lacks a complete Linux environment, which is what Profile B exists for.

**Non-goal** — Something deliberately not built in v1, listed in `00-vision/scope.md`. A non-goal that gets quietly built is a scope change; one that gets quietly dropped is a broken promise.

**OutboundMessage** — The other direction of the protocol: what the app sends to a backend. Interrupt, permission answer, stdin. The counterpart to `AgentEvent`. `02-architecture/event-protocol.md`.

**Paparazzi** — Screenshot tests that render Compose on the JVM, with no emulator. Fast enough to run on every PR.

**Permission request** — A tool call that the current autonomy level stops and asks about. Distinct from a hard block, which is never asked about — it is refused.

**proot** — A user-space implementation of `ptrace`-based syscall interception, used to run a full Linux distribution on Android without root. Profile B. Slow, large, more compatible.

**Profile A / B / C** — The three runtime profiles: native (glibc-patched), proot Ubuntu, and the experimental AVF virtual machine. One `RuntimeProfile` data class describes all three; feature code never branches on which.

**PTY** — Pseudo-terminal. The device pair that makes a program believe it is talking to a terminal. Needed for an interactive shell, colours, and correct signal handling. `06-runtime/pty-and-terminal.md`.

**Pushdown of hard blocks** — The rule that `HardBlockPolicy` is checked below the autonomy level, so raising autonomy cannot reach it. The reason the safety claim holds at `FULL_AUTO`.

## Q — S

**Retry budget** — The number of diagnose/fix/re-verify cycles a run may consume: 3, 10, 50, or unlimited, user-configured (`D18`). The counter is always visible. Exhaustion produces a report and a notification, not a loop.

**Ring buffer** — The last N KB of raw stdout/stderr kept per session, always visible in the terminal pane. Nothing is hidden (D25). `06-runtime/process-supervision.md`.

**Rollback** — Two different things, often confused: the *engine* rollback, which restores the last known-good binary after a failed launch, and a *release* rollback, which halts a staged rollout. Both are in `12-delivery/apk-distribution.md` §8.

**Runner** — A machine that executes runs for this app: the phone itself, a home PC over SSH, an Oracle VM, or a GitHub Actions job. A `RemoteTarget` in the data model.

**SecretRef** — An opaque reference to a secret. Everything except the Keystore refers to a key by `SecretRef`, so no code path can log the key by accident. `07-integrations/secrets.md`.

**Skill** — A markdown file with frontmatter that gives the agent a capability, installed globally or per project. The unit of extensibility. `shared/skills/`.

**Stage / staged rollout** — Releasing to 10 % of users first and raising it by hand. A Play Store mechanism, used here as a risk control.

**State machine** — The project's preferred way to model anything with more than two states: bootstrap, session, run, skill install. Every transition is defined and tested, including the failure transitions. `02-architecture/state-machines.md`.

**Step-up auth** — Requiring a fresh biometric prompt for a sensitive action, such as revealing a key. The reveal then auto-hides after 15 seconds.

**Structured concurrency** — The rule that a coroutine's lifetime is bound to its scope, so cancelling the scope cancels the work and there are no orphans. A `GlobalScope` in production code is a review blocker.

**Subagent** — A nested agent invocation with its own context, used for bounded parallel work: `architect`, `implementer`, `tester`, `debugger`, `designer`, `reviewer`. `08-orchestration/subagents-and-parallelism.md`.

## T — W

**Termux** — The Android terminal and package ecosystem whose `glibc-runner` package Profile A depends on. **Not** a runtime dependency: the app sets up its own environment, and no Termux installation is required. Mentioned here because the source of the glibc shim is often confused with a dependency on Termux itself.

**Transparency log** — The append-only record of every command, diff, decision, error, and cost in a session. The implementation of D25. Never edited, never pruned, always exportable. `05-features/transparency-log.md`.

**Turbine** — A testing library for Kotlin `Flow`, making a stream of emissions assertable.

**Verification command** — The build, typecheck, lint, and test commands a project declares. Detected heuristically by project type and editable by the user. `05-features/verification.md`.

**Verifier / Judge** — Two different steps. The **verifier** runs the commands and reports results. The **judge** decides whether the task succeeded. Conflating them is how an agent reports success on a broken build.

**Wake lock** — A CPU lock preventing the device from sleeping. Held **only** while a run is active. Holding one otherwise is both a battery bug and a review finding.

**WIP branch** — The `wip/` branch a partial run is committed to when the user interrupts. The work is preserved and reachable; nothing is discarded.

**Zero telemetry** — Not a policy about data minimisation but about there being no mechanism at all: no analytics SDK, no event path, no device identifier, no remote config. `11-operations/telemetry.md` states it in full, including the escape hatch for changing the decision later.

## Depends on

`00-vision/glossary.md` · `02-architecture/event-protocol.md` · `06-runtime/execution-backends.md` · `08-orchestration/permissions.md`
