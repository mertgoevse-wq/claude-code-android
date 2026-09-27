# Task breakdown

Every task in the build, with an ID, a phase, an estimate, its dependencies, and the acceptance criterion that closes it. A task is done when `13-process/definition-of-done.md` §1 holds *and* the acceptance line below is met.

Estimates are in hours of focused work and are **rough**. They exist to make the plan plannable, not to be promised. A task marked `TBD` means the estimate was not worth guessing at; see `14-build-plan/milestones.md`.

## Conventions

| Field | Meaning |
|---|---|
| ID | `P<phase>-<n>`. Stable. Referenced from commits, PRs, and the progress log. Never reused |
| Est | Hours. `S` = under 2, `M` = 2–8, `L` = 8–24, `XL` = over 24, usually split |
| Dep | Task IDs that must be complete first |
| Acc | The specific, checkable thing that makes this task closeable |

## Phase 0 — Foundation

| ID | Task | Est | Dep | Acceptance |
|---|---|---|---|---|
| P0-1 | Repository init, `.editorconfig`, `.gitignore`, `LICENSE`, `NOTICE` | S | — | A fresh clone has a clean `git status` |
| P0-2 | Write all 135 documents, or stub with purpose + status | XL | P0-1 | `tools/check_doc_manifest.py` green |
| P0-3 | Gradle wrapper, `settings.gradle.kts`, `gradle.properties` | M | P0-1 | `./gradlew --version` works with the pinned JDK |
| P0-4 | Version catalog with **looked-up** versions, each with a check date | M | P0-3 | `10-build/dependency-versions.md` records every version and its date |
| P0-5 | Convention plugin: module structure and dependency rules | M | P0-3 | A module cannot exist without applying the plugin |
| P0-6 | Convention plugin: test, Compose, and coverage configuration | M | P0-5 | `koverVerify` runs with the threshold from the spec |
| P0-7 | KMP module skeletons, all eight `shared/*` plus `androidApp` | L | P0-5 | Each module compiles; `check_no_android_imports_in_shared.py` green |
| P0-8 | `Result`, dispatchers, redaction, and platform gateways in `shared/core` | M | P0-7 | Unit tests, ≥ 90 % coverage |
| P0-9 | `AppError` hierarchy from the error taxonomy | M | P0-8 | Every taxonomy entry has a type and a test |
| P0-10 | CI workflow: build, check, tests, lint | M | P0-6 | Green on an empty app |
| P0-11 | `tools/check_doc_manifest.py` | S | P0-2 | Fails when a doc is deleted |
| P0-12 | `tools/check_source_manifest.py` | S | P0-2 | Fails when a listed source file is missing |
| P0-13 | `tools/check_no_android_imports_in_shared.py` | S | P0-7 | Fails on a deliberate violation |
| P0-14 | `scripts/check-no-secrets.sh` | S | P0-1 | Fails on a planted key |
| P0-15 | `scripts/check-no-analytics.sh` | S | P0-1 | Fails on a planted SDK symbol |
| P0-16 | `.claude/settings.json` with permissions, deny rules, hooks, env | M | P0-10 | **Built.** 30 deny entries, four hook events, valid JSON. A denied command is actually denied; the hooks fire. Verify on a real session |
| P0-17 | 8 slash commands | M | P0-16 | **Built.** phase-start, verify, fix, ship, doc-sync, ui-polish, release, status |
| P0-18 | 6 subagents | M | P0-16 | **Built.** architect, implementer, tester, debugger, designer, reviewer |
| P0-19 | 8 project skills | M | P0-16 | **Built.** android-compose, kmp-shared, runtime-bootstrap, ui-design, github-safety, docs-authoring, test-authoring, release |
| P0-20 | `tools/ci.sh`, `tools/format.sh` | S | P0-10 | Both run green locally |
| P0-21 | `.gitignore` covering build output, `local.properties`, keystores, `.claude/settings.local.json` | S | P0-1 | **Built.** Committed in the root commit; no keystore, `local.properties`, or build output is tracked |
| P0-22 | Wire the commit-and-push loop: `git add -p` staging, pre-push secret scan, branch guard, `Stop` hook | M | P0-16, P0-21 | **Hooks written** (`tools/hook_pre_bash.sh`, `hook_post_bash.sh`, `hook_stop.sh`), syntax-checked. Not yet observed firing in a live session — that is the remaining half |

## Phase 1 — Design system

| ID | Task | Est | Dep | Acceptance |
|---|---|---|---|---|
| P1-1 | `design-tokens.md`, binding, written before any UI code | M | P0-2 | Reviewed and referenced by every later UI task |
| P1-2 | Colour ramp, light and dark, with measured contrast | M | P1-1 | Every pair in `color-and-contrast.md` measured, AA or better |
| P1-3 | Type scale, families, weights, line heights | M | P1-1 | No default Roboto; families documented with the reason |
| P1-4 | 4 pt spacing scale, grid, gutters, edge behaviour | S | P1-1 | Matches `spacing-and-layout.md` |
| P1-5 | Radii, elevation, motion durations, curves, haptics | S | P1-1 | Matches `motion.md`; every state change ≤ 300 ms |
| P1-6 | Theme implementation in `shared/ui` | M | P1-2, P1-3 | Both themes render; a token resolves in one place only |
| P1-7 | Primitives: buttons, inputs, chips, cards, sheets | L | P1-6 | Each has all states; each is used by at least one later screen |
| P1-8 | The collapsible card, the streaming text block, the code block | L | P1-7 | Streaming text does not reflow the whole list per delta |
| P1-9 | The terminal block with the ANSI palette | M | P1-7 | 16, 256, and truecolor render; contrast verified |
| P1-10 | Icon set selected and documented | M | P1-1 | One set, no emoji, licence recorded |
| P1-11 | The animated mark: idle breathing | M | P1-6 | Screenshot test per state; respects reduce motion |
| P1-12 | The animated mark: working character state machine | L | P1-11, P0-9 | Fed by real `AgentEvent`s, never by a random timer |
| P1-13 | Anti-slop review pass over everything in this phase | M | P1-7, P1-10 | `03-design/anti-slop-rules.md` checklist green |
| P1-14 | Screenshot baselines for the design system, both themes | M | P1-7 | Baselines committed; diffs reviewed, not accepted |

## Phase 2 — Data and core

| ID | Task | Est | Dep | Acceptance |
|---|---|---|---|---|
| P2-1 | Domain models for all 28 entities | L | P0-7, P1-1 | Every field in `data-model.md` exists; no framework types leak |
| P2-2 | Use cases: project lifecycle | M | P2-1 | Covered; create/clone/rename/archive, no delete |
| P2-3 | Use cases: chat and run lifecycle | M | P2-1 | Covered |
| P2-4 | Use cases: permission policy and autonomy levels | L | P2-1 | Every level's permits and blocks tested; hard blocks unreachable |
| P2-5 | Use cases: budget, plans, verification | M | P2-1 | Cost math tested against the engine's own numbers |
| P2-6 | Room schema and DAOs | L | P2-1 | Schema matches `data-model.md` |
| P2-7 | Migrations, with tests | M | P2-6 | Every version step tested, including the destructive-change rule |
| P2-8 | Repositories in `shared/data` | XL | P2-6 | ≥ 80 % coverage against in-memory Room |
| P2-9 | `SecretStore` on the Keystore, `SecretRef` everywhere | L | P2-8 | Key round-trips; never in a log, an export, or a diff |
| P2-10 | `RedactingLogTree` and the redaction pass | M | P0-8 | 40 fixtures; a key never survives it |
| P2-11 | Provider configuration and the connection test | L | P2-8 | Anthropic, OpenAI chat, OpenAI responses, custom; clear pass/fail reasons |
| P2-12 | Settings in DataStore, i18n skeleton, German default + English | M | P2-8 | Language switch changes every string |
| P2-13 | `AppError` wired through every repository | M | P0-9, P2-8 | No bare exception crosses a repository boundary |

## Phase 3 — Runtime

| ID | Task | Est | Dep | Acceptance |
|---|---|---|---|---|
| P3-1 | `ExecutionBackend` interface and capabilities | M | P2-1 | Matches `06-runtime/execution-backends.md` exactly |
| P3-2 | Bootstrap state machine: states, transitions, progress | L | P3-1 | Every transition tested, including the failure transitions |
| P3-3 | Download and checksum verification | M | P3-2 | A mismatch aborts with a clear message and never proceeds |
| P3-4 | Native profile: glibc-runner, patchelf, ELF patch | XL | P3-3 | `claude --version` runs on a real device |
| P3-5 | Engine version check and transparent update | M | P3-4 | At most once a day; rollback to the last known-good on failure |
| P3-6 | Process supervision: tree tracking, cancel, signals, exit codes | L | P3-1 | A cancelled run kills shell children too |
| P3-7 | The output ring buffer | M | P3-6 | Nothing is hidden; always visible in the terminal pane |
| P3-8 | The PTY bridge: allocate, feed, read, resize, exit code | L | P3-6 | `COLUMNS`/`LINES` propagate |
| P3-9 | The terminal renderer: monospace, ANSI, cursor, selection | XL | P1-9, P3-8 | 16/256/truecolor; copy and paste work |
| P3-10 | The terminal keyboard row, with a reachable `Ctrl+C` | M | P3-9 | Every key sends the right sequence |
| P3-11 | Headless prompt execution and event streaming | L | P3-4, P2-1 | A headless prompt streams back into the app |
| P3-12 | Pre-flight health checks with specific fixes | M | P3-11 | Each failure names the actual problem and the actual fix |
| P3-13 | proot profile: Ubuntu bootstrap | XL | P3-2 | Completes its own bootstrap on a real device; cost shown up front |
| P3-14 | AVF profile: detection and limits | M | P3-2 | Detected where supported; never required; Snapdragon stated as unsupported |
| P3-15 | Foreground service and wake lock for long runs | M | P3-6 | Screen off for 10 minutes mid-run; the run survives |

## Phase 4 — Chat and projects

| ID | Task | Est | Dep | Acceptance |
|---|---|---|---|---|
| P4-1 | Message model and streaming renderer | XL | P3-11, P1-8 | Text and thinking deltas stream without list reflow churn |
| P4-2 | Tool cards: start, progress, result, collapse | L | P4-1 | Each state visible; collapse state remembered per session |
| P4-3 | Live plan display, editable | M | P4-1 | Steps with acceptance criteria; edits persist |
| P4-4 | Cost meter | M | P2-5, P4-1 | Accurate against the engine; per run, project, day, month |
| P4-5 | Interrupt | M | P4-1, P3-6 | Partial work on a `wip/` branch, recoverable |
| P4-6 | Chat list, grouped by day and project | M | P4-1 | Empty, loading, error, content all implemented |
| P4-7 | New chat with the animated mark | M | P1-12, P4-6 | Project selector and composer present |
| P4-8 | Chat detail: the full screen | XL | P4-1…P4-5 | All five states; large font does not clip |
| P4-9 | Chat with terminal split view | L | P4-8, P3-9 | Works in portrait, landscape, and tablet |
| P4-10 | Project list | M | P2-2 | Density reviewed; not a dashboard |
| P4-11 | Project detail: branches, PRs, run history | L | P4-10 | Every state |
| P4-12 | Add project: clone, local, remote | L | P4-10 | Privacy indicator shown before any remote is configured |
| P4-13 | Diff viewer: side-by-side and unified | XL | P4-11, P1-3 | Accept and revert per hunk; syntax highlighted |
| P4-14 | Run history | M | P4-11 | Inspect, export, revisit |
| P4-15 | The transparency log | L | P4-8 | Append-only, complete, exportable |
| P4-16 | Autonomy levels wired to the policy | M | P2-4, P4-8 | Changing a level takes effect on the next tool call |
| P4-17 | Search | M | P4-6, P4-10 | Conversations, projects, and skills |
| P4-18 | **E2E journey 1**: open app → new chat → pick project → order → stream → interrupt → resume | L | P4-8, P4-5 | Passes on an emulator |

## Phase 5 — GitHub, skills, remote runners

| ID | Task | Est | Dep | Acceptance |
|---|---|---|---|---|
| P5-1 | OAuth device flow | L | P2-8 | Scopes are minimal; refresh and revocation work |
| P5-2 | Fine-grained PAT entry, with a scope checklist | M | P2-8 | Validation button gives a real reason on failure |
| P5-3 | Clone, branch, commit after a run | L | P5-1, P4-12 | Commit message names the task |
| P5-4 | Test-gated push | L | P5-3, P2-5 | A red suite blocks the push and says why |
| P5-5 | Open a PR | M | P5-4 | Only when green |
| P5-6 | The default-branch guard and hard-block enforcement in git | M | P5-3 | Refused, with a test asserting the refusal |
| P5-7 | GitHub notifications: PR, comment, check, review | M | P5-5 | Polling fallback with a configurable interval |
| P5-8 | Skill install from a GitHub URL | L | P2-8 | Detects `SKILL.md`, `.claude/skills/*`, plugin manifest |
| P5-9 | The install diff preview | M | P5-8 | Every file and its destination shown before confirming |
| P5-10 | Skill validation | M | P5-8 | Broken skills show a repair button, never silent ignore |
| P5-11 | Skill scoping: global and per project | M | P5-8 | The UI always shows where a skill lives and what it affects |
| P5-12 | Skill creation with AI | L | P5-10 | Produces valid frontmatter; previewed before saving |
| P5-13 | Uninstall to quarantine | S | P5-11 | No deletion — hard block 1 |
| P5-14 | The marketplace index | M | P5-8 | Static JSON in-app; optional remote index |
| P5-15 | `SshBackend` for a home PC | L | P3-1, P5-1 | A full run round-trips |
| P5-16 | `CloudRunnerBackend` for Oracle Always Free | L | P3-1, P5-1 | Live quota check at setup; graceful degradation |
| P5-17 | `GithubActionsBackend` | L | P3-1, P5-1 | Free-minute accounting shown |
| P5-18 | Offload: same UI, same protocol, one flag | M | P5-15, P5-16, P5-17 | The session record names the backend |
| P5-19 | **E2E journey 2**: a task on a private test repo yields a green branch and an open PR | L | P5-5 | Passes against a real private test repo |
| P5-20 | **E2E journey 3**: install a skill from a URL and use it in a run | M | P5-9, P5-12 | Passes |

## Phase 6 — Verification and self-healing

| ID | Task | Est | Dep | Acceptance |
|---|---|---|---|---|
| P6-1 | Verification command detection per project type | L | P2-5 | Gradle, npm, cargo, pytest; editable by the user |
| P6-2 | The verifier: run and report | M | P6-1 | Results structured; failures show the actual output |
| P6-3 | The judge: did the task actually succeed | L | P6-2 | A unit test where the judge's verdict differs from the agent's claim |
| P6-4 | The retry controller and budget (3/10/50/unlimited) | M | P6-3 | The counter is always visible; budget exhaustion is reported correctly |
| P6-5 | Anti-loop detection | M | P6-4 | A genuinely stuck loop is aborted with a clear reason |
| P6-6 | Checkpoints: after plan, before push, before dependency install | M | P6-3 | Each stops the run and reports |
| P6-7 | Context budgeting and compaction | L | P6-1 | A long run does not exceed the budget silently |
| P6-8 | Self-update: check, verify, install, roll back | L | P2-9 | Signature and checksum verified; a broken engine rolls back |
| P6-9 | Field-error triage | M | P4-15 | A recurring signature becomes a one-tap fix task |
| P6-10 | **E2E journey 4**: a broken project reaches green within budget | L | P6-4 | Passes; the exhaustion path also reports correctly |

## Phase 7 — Autonomy, polish, delivery

| ID | Task | Est | Dep | Acceptance |
|---|---|---|---|---|
| P7-1 | Background execution: service, notifications, wake locks | L | P3-15 | Screen off, app backgrounded, and battery saver all keep the run alive |
| P7-2 | Notification channels and actionable permission prompts | M | P7-1 | Approve/Deny/Always from the notification |
| P7-3 | Remaining screens, all five states each | XL | P4-11 | Every screen in `04-screens/` implemented |
| P7-4 | Screenshot polish across every screen, both themes | L | P7-3, P1-13 | Design-review pass per screen; no template-looking output |
| P7-5 | Accessibility pass | L | P7-3 | No critical findings; AA contrast; ≥ 48 dp; labels on every control |
| P7-6 | Performance pass against the budgets | L | P7-3 | Startup, cold bootstrap, scrolling, memory all inside budget |
| P7-7 | Security pass against the threat model | M | P7-3 | Every threat's control verified; the model updated if anything changed |
| P7-8 | The README, to its own checklist | M | P7-3 | All 12 checklist items green |
| P7-9 | Play and F-Droid metadata | M | P7-8 | A clean-room F-Droid build succeeds |
| P7-10 | The release checklist, box by box | L | P7-5, P7-6, P7-7, P7-8 | Every box ticked by whoever ran it |
| P7-11 | **E2E journey 5**: the self-update check | M | P6-8 | Passes |
| P7-12 | A non-technical user completes the whole flow unaided | L | P7-10 | This is the project's definition of done |

## Sizing summary

| Phase | Tasks | Estimated hours |
|---|---|---|
| 0 Foundation | 22 | ~130 |
| 1 Design system | 14 | ~120 |
| 2 Data and core | 13 | ~130 |
| 3 Runtime | 15 | ~180 |
| 4 Chat and projects | 18 | ~190 |
| 5 GitHub, skills, runners | 20 | ~150 |
| 6 Verification, self-healing | 10 | ~60 |
| 7 Autonomy, polish, delivery | 12 | ~120 |
| **Total** | **124** | **~1080 h** |

Roughly six months of one person's focused work, or a fraction of that for an agent that does not get tired. The number is here to make the plan honest, not to make it feel small. Two tasks are the risk, and neither is measured well: **P3-4** (the ELF patch) and **P3-13** (the proot bootstrap). Both are XL, both could be larger, and both are why Phase 3 is a hard stop.

## Depends on

`14-build-plan/phase-plan.md` · `14-build-plan/dependency-graph.md` · `14-build-plan/milestones.md` · `14-build-plan/risk-register.md` · `14-build-plan/progress-log.md`
