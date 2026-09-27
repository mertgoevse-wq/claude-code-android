# Phase plan

Seven phases, run in order, each ending green or the build stops and reports. No phase is skipped and no phase is merged into the next. This is the plan from the spec's §21, expanded with the entry conditions, the exit criteria, and what each phase is allowed to assume.

The rule that makes the plan mean anything: **a phase that is not green is not done.** A phase reported as 90 % complete is a phase with one named missing item, not a phase that is nearly finished.

## Phase 0 — Foundation

| Field | Value |
|---|---|
| Goal | A repository that builds, a doc set that exists, and gates that fail when they should |
| Entry | Nothing. This is the first phase |
| Exit | `./gradlew check` runs on an empty app; the doc manifest check passes; CI is green |

**Work**

- All 135 documents written, or stubbed with a purpose and a status. Non-stub is required before the release checklist, not before the build
- Repository initialised, `.editorconfig`, `.gitignore`, `LICENSE`, `NOTICE`, `THIRD_PARTY_NOTICES.md`
- Gradle wrapper, `settings.gradle.kts`, `gradle/libs.versions.toml` with **looked-up** versions, configuration cache on
- Convention plugins: module structure, dependency rules, test setup, Compose configuration
- KMP module skeletons: `shared/core`, `shared/domain`, `shared/data`, `shared/runtime`, `shared/orchestration`, `shared/skills`, `shared/vcs`, `shared/ui`, plus `androidApp`
- CI: build, check, all tests, lint, screenshot diff, nightly E2E, manifest checkers
- The manifest checkers themselves: `tools/check_doc_manifest.py`, `tools/check_source_manifest.py`, `tools/check_no_android_imports_in_shared.py`, `scripts/check-no-secrets.sh`, `scripts/check-no-analytics.sh`
- `.claude/` kit: settings, 8 commands, 6 agents, 8 skills
- The git automation: `.gitignore`, the pre-push secret scan, the branch guard, and the `Stop`/`SessionStart` hooks that make "commit and push after every step" true. The operator runs with `--dangerously-skip-permissions`, so these hooks are the enforcement mechanism, not a convenience. See `13-process/git-strategy.md` §6

**Exit criteria**

- ☐ `./gradlew check` green on a fresh clone
- ☐ `tools/check_doc_manifest.py` green
- ☐ `tools/check_no_android_imports_in_shared.py` green and demonstrably failing when violated
- ☐ `scripts/check-no-secrets.sh` green
- ☐ CI green on an empty app
- ☐ Every version in the catalog was looked up, and the date of the check is recorded

**What this phase may assume:** nothing. It may not assume a library exists, a flag is real, or a quota is known.

## Phase 1 — Design system

| Field | Value |
|---|---|
| Goal | Tokens, theme, primitives, and the animated mark, so that no screen is ever written without them |
| Entry | Phase 0 green |
| Exit | Every token is used by at least one component; both themes pass contrast; the mark's state machine is covered by screenshot tests |

**Work**

- `03-design/design-tokens.md` written and treated as binding: colour ramp, type scale, 4 pt spacing, radii, elevation, motion durations and curves, haptics
- Theme implementation in `shared/ui`, light and dark
- Primitives: buttons, inputs, cards, chips, sheets, list rows, the collapsible card, the streaming text block, the code block, the terminal block
- The animated mark: Compose `Canvas`, one animation clock, idle breathing and the working character state machine fed by real `AgentEvent`s, with a reduce-motion fallback
- `03-design/anti-slop-rules.md` applied to a design-review pass over everything built in this phase
- Icon set chosen and documented. No emoji

**Exit criteria**

- ☐ Every token in the token set is referenced by at least one component
- ☐ Light and dark both pass WCAG AA, verified by the checker in `03-design/color-and-contrast.md`
- ☐ The mark's state machine has a screenshot test per state
- ☐ Reduce-motion produces a static mark plus a progress ring
- ☐ No emoji, no default purple, no default Roboto, anywhere
- ☐ An entry per component in `13-process/ai-usage-policy.md`

**Depends on:** nothing beyond Phase 0. This phase exists so that no screen phase has to invent a colour.

## Phase 2 — Data and core

| Field | Value |
|---|---|
| Goal | The domain model, the database, the repositories, and secrets that provably do not leak |
| Entry | Phase 1 green |
| Exit | ≥ 90 % unit coverage on `shared/domain`, migrations tested, a key round-trips through the Keystore and never appears in a log |

**Work**

- Domain models for all 28 entities in `02-architecture/data-model.md`
- Use cases: project lifecycle, chat, run lifecycle, permissions, budget, plans, verification
- Room schema, migrations, and migration tests
- Repositories in `shared/data`, with in-memory tests
- `SecretStore` on the Keystore, `SecretRef` everywhere else, `RedactingLogTree`
- Provider configurations and the connection test
- Settings, `DataStore`, and the i18n skeleton with German default and English
- `02-architecture/error-taxonomy.md` implemented as `AppError`

**Exit criteria**

- ☐ `shared/domain` coverage ≥ 90 %
- ☐ `shared/data` coverage ≥ 80 %
- ☐ Migration tests pass, including the destructive-change rule
- ☐ A key round-trips: stored, retrieved, never logged, never in an export
- ☐ No DAO is called from a ViewModel — checked by a static rule, not by review
- ☐ `Result` and error types used consistently; no bare exceptions at the domain boundary

## Phase 3 — Runtime

| Field | Value |
|---|---|
| Goal | Claude Code actually running on the phone |
| Entry | Phase 2 green |
| Exit | The app downloads, verifies, installs, launches `claude --version`, runs a headless prompt, and streams the output back |

**Work**

- `ExecutionBackend` and the `BackendCapabilities` model
- `AndroidLocalBackend`: download, checksum, patch, install, verify, update
- The native profile: `glibc-runner`, `patchelf-glibc`, the official `linux-arm64` binary, checksum verification, version check
- The bootstrap state machine: every step, progress, resumability, cancel, failure and recovery
- The proot profile as an opt-in second path
- The AVF profile as an experimental third path, detected and never required
- Process supervision: process tree tracking, cancellation, signals, the ring buffer, exit codes
- The PTY bridge and the terminal renderer
- `06-runtime/environment-diagnostics.md` pre-flight checks with specific, human fixes

**Exit criteria**

- ☐ `claude --version` runs on a real device or emulator
- ☐ A headless prompt streams output back into the app
- ☐ Killing the app mid-bootstrap resumes rather than restarts
- ☐ A cancelled run kills the whole process tree, including shell children
- ☐ A checksum mismatch aborts with a clear message and never proceeds
- ☐ The proot profile completes its own bootstrap on a real device
- ☐ Nothing is written outside `filesDir/cca/`

**This phase is a hard stop.** If the engine does not run, the build stops and records the blocker. There is no mock, no stub, and no "the UI is ready, the binary comes later" — an app that cannot run the agent is not a partial success, it is a failure with extra steps.

## Phase 4 — Chat and projects

| Field | Value |
|---|---|
| Goal | The heart of the product, end to end |
| Entry | Phase 3 green |
| Exit | The E2E journey "open app → new chat → pick project → send order → watch stream → interrupt → resume" passes on an emulator |

**Work**

- Message model and streaming: text deltas, thinking deltas, tool cards, plan display, cost meter, interrupt
- Chat list, new chat, chat detail, chat-with-terminal split view
- Project list, project detail, add project, clone
- The diff viewer, side-by-side and unified, accept and revert per hunk
- Run history and the transparency log
- Autonomy levels wired to the permission policy
- Search across conversations and projects

**Exit criteria**

- ☐ The named E2E journey passes on an emulator
- ☐ All five UI states exist for every screen in scope
- ☐ The cost meter is accurate against the engine's own accounting
- ☐ Interruption leaves partial work on a `wip/` branch, recoverable
- ☐ The transparency log is complete and exportable

## Phase 5 — GitHub, skills, remote runners

| Field | Value |
|---|---|
| Goal | The result leaves the phone |
| Entry | Phase 4 green |
| Exit | A task against a private test repo produces a green branch and an open PR, and a skill installs from a URL and works in a run |

**Work**

- Both auth flows: OAuth device flow and fine-grained PAT, with scopes, refresh, and revocation
- Clone, branch, commit, test-gated push, PR
- GitHub notifications: PR opened, comment, CI check, review requested
- Skills: install from a GitHub URL, generate with AI, list, toggle, edit, scope, validate, uninstall to quarantine
- The marketplace index, static JSON plus an optional remote index
- Remote runners: Oracle Always Free, GitHub Actions, home PC, with a live quota check and graceful degradation
- Offload: the same UI, the same event protocol, one flag in the session record

**Exit criteria**

- ☐ A private test repo yields a green branch and an open PR
- ☐ A red suite blocks the push, with the reason shown
- ☐ A push to the default branch is refused
- ☐ A skill installs from a URL and is usable in a run
- ☐ The install diff is shown before anything is written
- ☐ A remote runner round-trips a full run, or degrades with an honest message

## Phase 6 — Verification and self-healing

| Field | Value |
|---|---|
| Goal | The loop closes: the app can tell whether it succeeded, and try again |
| Entry | Phase 5 green |
| Exit | A deliberately broken project is given a task and reaches green within budget, and the exhaustion path reports correctly |

**Work**

- The verifier: detect and run the project's build, typecheck, lint, and test commands
- The judge: did the task actually succeed, separate from the agent's claim
- The retry controller, with a user-configurable budget: 3, 10, 50, unlimited
- Anti-loop detection: a step repeating with no progress
- Checkpoints: after plan, before push, before dependency install
- Context budgeting and compaction
- Self-update and the self-build flow
- Field-error triage: recurring error signatures into one-tap fix tasks

**Exit criteria**

- ☐ A deliberately broken project reaches green within the configured budget
- ☐ Budget exhaustion produces a clear report and a notification, and does not loop
- ☐ Anti-loop detection aborts a genuinely stuck loop
- ☐ The judge's verdict differs from the agent's claim at least once in testing — a judge that always agrees is not a judge
- ☐ The self-update path verifies signature and checksum before installing

## Phase 7 — Autonomy, polish, delivery

| Field | Value |
|---|---|
| Goal | Everything a person needs to trust it and install it |
| Entry | Phase 6 green |
| Exit | Every gate in spec §22.2 is green, the README passes its own checklist, and a signed release APK exists |

**Work**

- Background execution: foreground service, notifications, wake locks, screen-off behaviour, resumption
- All remaining screens and their five states
- Screenshot polish across every screen, both themes, with the design skills
- Accessibility pass: contrast, targets, screen-reader labels, reduce motion, font scaling
- Performance pass against `09-testing/performance-budgets.md`
- Security pass against `11-operations/security-threat-model.md`
- The README, written to `12-delivery/github-readme-guide.md`
- Play and F-Droid metadata
- The release checklist, run box by box

**Exit criteria**

- ☐ Every §22.2 gate green
- ☐ A signed release APK from a fresh clone
- ☐ The README passes its own 12-point checklist
- ☐ The E2E journeys pass on a physical device, not only an emulator
- ☐ A non-technical user completes the full flow unaided — this is the project's definition of done

## Phase dependencies

```
0 Foundation
  └─► 1 Design system
        └─► 2 Data and core
              └─► 3 Runtime          ← hard stop
                    └─► 4 Chat and projects
                          └─► 5 GitHub, skills, remote runners
                                └─► 6 Verification and self-healing
                                      └─► 7 Autonomy, polish, delivery
```

The chain is strict on purpose. Two facts force it: the design system must exist before any screen is written, or every screen invents its own colours; and the runtime must work before chat is built, or the chat is built against a fictional event stream.

Within a phase, the ordering is in `14-build-plan/dependency-graph.md` and the task detail in `task-breakdown.md`.

## Depends on

`14-build-plan/task-breakdown.md` · `14-build-plan/dependency-graph.md` · `14-build-plan/milestones.md` · `14-build-plan/progress-log.md` · `13-process/definition-of-done.md` · `claude-code-android-spec.md` §21
