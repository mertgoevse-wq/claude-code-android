# Task breakdown

Every task in the build, with an ID, a phase, an estimate, its dependencies, the skill used to execute it, and the acceptance criterion that closes it. A task is done when `13-process/definition-of-done.md` §1 holds *and* the acceptance line below is met.

Estimates are in hours of focused work and are **rough**. They exist to make the plan plannable, not to be promised. A task marked `TBD` means the estimate was not worth guessing at; see `14-build-plan/milestones.md`.

## Conventions

| Field | Meaning |
|---|---|
| ID | `P<phase>-<n>`. Stable. Referenced from commits, PRs, and the progress log. Never reused |
| Est | Hours. `S` = under 2, `M` = 2–8, `L` = 8–24, `XL` = over 24, usually split |
| Dep | Task IDs that must be complete first |
| Skill | The project skill used to execute the task (D39, `full-port-spec.md`). The progress-log entry names the skill actually used; a task with no recorded skill is not done |
| Acc | The specific, checkable thing that makes this task closeable |

## Phase 0 — Foundation

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P0-1 | Repository init, `.editorconfig`, `.gitignore`, `LICENSE`, `NOTICE` | S | — | github-safety | A fresh clone has a clean `git status` |
| P0-2 | Write all 135 documents, or stub with purpose + status | XL | P0-1 | docs-authoring | `tools/check_doc_manifest.py` green |
| P0-3 | Gradle wrapper, `settings.gradle.kts`, `gradle.properties` | M | P0-1 | docs-authoring | `./gradlew --version` works with the pinned JDK |
| P0-4 | Version catalog with **looked-up** versions, each with a check date | M | P0-3 | docs-authoring | `10-build/dependency-versions.md` records every version and its date |
| P0-5 | Convention plugin: module structure and dependency rules | M | P0-3 | kmp-shared | A module cannot exist without applying the plugin |
| P0-6 | Convention plugin: test, Compose, and coverage configuration | M | P0-5 | kmp-shared | `koverVerify` runs with the threshold from the spec |
| P0-7 | KMP module skeletons, all eight `shared/*` plus `androidApp` | L | P0-5 | kmp-shared | Each module compiles; `check_no_android_imports_in_shared.py` green |
| P0-8 | `Result`, dispatchers, redaction, and platform gateways in `shared/core` | M | P0-7 | test-authoring | Unit tests, ≥ 90 % coverage |
| P0-9 | `AppError` hierarchy from the error taxonomy | M | P0-8 | test-authoring | Every taxonomy entry has a type and a test |
| P0-10 | CI workflow: build, check, tests, lint | M | P0-6 | github-safety | Green on an empty app |
| P0-11 | `tools/check_doc_manifest.py` | S | P0-2 | test-authoring | Fails when a doc is deleted |
| P0-12 | `tools/check_source_manifest.py` | S | P0-2 | test-authoring | Fails when a listed source file is missing |
| P0-13 | `tools/check_no_android_imports_in_shared.py` | S | P0-7 | test-authoring | Fails on a deliberate violation |
| P0-14 | `scripts/check-no-secrets.sh` | S | P0-1 | test-authoring | Fails on a planted key |
| P0-15 | `scripts/check-no-analytics.sh` | S | P0-1 | test-authoring | Fails on a planted SDK symbol |
| P0-16 | `.claude/settings.json` with permissions, deny rules, hooks, env | M | P0-10 | github-safety | **Built.** 30 deny entries, four hook events, valid JSON. A denied command is actually denied; the hooks fire. Verify on a real session |
| P0-17 | 8 slash commands | M | P0-16 | docs-authoring | **Built.** phase-start, verify, fix, ship, doc-sync, ui-polish, release, status |
| P0-18 | 6 subagents | M | P0-16 | docs-authoring | **Built.** architect, implementer, tester, debugger, designer, reviewer |
| P0-19 | 8 project skills | M | P0-16 | docs-authoring | **Built.** android-compose, kmp-shared, runtime-bootstrap, ui-design, github-safety, docs-authoring, test-authoring, release |
| P0-20 | `tools/ci.sh`, `tools/format.sh` | S | P0-10 | test-authoring | Both run green locally |
| P0-21 | `.gitignore` covering build output, `local.properties`, keystores, `.claude/settings.local.json` | S | P0-1 | github-safety | **Built.** Committed in the root commit; no keystore, `local.properties`, or build output is tracked |
| P0-22 | Wire the commit-and-push loop: `git add -p` staging, pre-push secret scan, branch guard, `Stop` hook | M | P0-16, P0-21 | github-safety | **Hooks written** (`tools/hook_pre_bash.sh`, `hook_post_bash.sh`, `hook_stop.sh`), syntax-checked. Not yet observed firing in a live session — that is the remaining half |

## Phase 1 — Design system

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P1-1 | `design-tokens.md`, binding, written before any UI code | M | P0-2 | ui-design | Reviewed and referenced by every later UI task |
| P1-2 | Colour ramp, light and dark, with measured contrast | M | P1-1 | ui-design | Every pair in `color-and-contrast.md` measured, AA or better |
| P1-3 | Type scale, families, weights, line heights | M | P1-1 | ui-design | No default Roboto; families documented with the reason |
| P1-4 | 4 pt spacing scale, grid, gutters, edge behaviour | S | P1-1 | ui-design | Matches `spacing-and-layout.md` |
| P1-5 | Radii, elevation, motion durations, curves, haptics | S | P1-1 | ui-design | Matches `motion.md`; every state change ≤ 300 ms |
| P1-6 | Theme implementation in `shared/ui` | M | P1-2, P1-3 | android-compose | Both themes render; a token resolves in one place only |
| P1-7 | Primitives: buttons, inputs, chips, cards, sheets | L | P1-6 | android-compose | Each has all states; each is used by at least one later screen |
| P1-8 | The collapsible card, the streaming text block, the code block | L | P1-7 | android-compose | Streaming text does not reflow the whole list per delta |
| P1-9 | The terminal block with the ANSI palette | M | P1-7 | android-compose | 16, 256, and truecolor render; contrast verified |
| P1-10 | Icon set selected and documented | M | P1-1 | ui-design | One set, no emoji, licence recorded |
| P1-11 | The animated mark: idle breathing | M | P1-6 | android-compose | Screenshot test per state; respects reduce motion |
| P1-12 | The animated mark: working character state machine | L | P1-11, P0-9 | android-compose | Fed by real `AgentEvent`s, never by a random timer |
| P1-13 | Anti-slop review pass over everything in this phase | M | P1-7, P1-10 | ui-design | `03-design/anti-slop-rules.md` checklist green |
| P1-14 | Screenshot baselines for the design system, both themes | M | P1-7 | test-authoring | Baselines committed; diffs reviewed, not accepted |

## Phase 2 — Data and core

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P2-1 | Domain models for all 28 entities | L | P0-7, P1-1 | kmp-shared | Every field in `data-model.md` exists; no framework types leak |
| P2-2 | Use cases: project lifecycle | M | P2-1 | test-authoring | Covered; create/clone/rename/archive, no delete |
| P2-3 | Use cases: chat and run lifecycle | M | P2-1 | test-authoring | Covered |
| P2-4 | Use cases: permission policy and autonomy levels | L | P2-1 | test-authoring | Every level's permits and blocks tested; hard blocks unreachable |
| P2-5 | Use cases: budget, plans, verification | M | P2-1 | test-authoring | Cost math tested against the engine's own numbers |
| P2-6 | Room schema and DAOs | L | P2-1 | kmp-shared | Schema matches `data-model.md` |
| P2-7 | Migrations, with tests | M | P2-6 | test-authoring | Every version step tested, including the destructive-change rule |
| P2-8 | Repositories in `shared/data` | XL | P2-6 | test-authoring | ≥ 80 % coverage against in-memory Room |
| P2-9 | `SecretStore` on the Keystore, `SecretRef` everywhere | L | P2-8 | test-authoring | Key round-trips; never in a log, an export, or a diff. **Committed 2026-09-30 on `task/phase-2-data-and-core`** |
| P2-10 | `RedactingLogTree` and the redaction pass | M | P0-8 | test-authoring | 40 fixtures; a key never survives it |
| P2-11 | Provider configuration and the connection test | L | P2-8 | test-authoring | Anthropic, OpenAI chat, OpenAI responses, custom; clear pass/fail reasons. Partial slices P2-11a/b are committed |
| P2-12 | Settings in DataStore, i18n skeleton, German default + English | M | P2-8 | test-authoring | Language switch changes every string |
| P2-13 | `AppError` wired through every repository | M | P0-9, P2-8 | test-authoring | No bare exception crosses a repository boundary. The checker `tools/check_typed_errors_at_boundaries.py` is green and wired into CI |

## Phase 2 — Amendment tasks (`full-port-spec.md`)

New work from the 2026-09-30 interview, sequenced into the existing phase structure. IDs use an `A` so no existing ID moves. The remaining Phase-2 tasks P2-10…P2-13 keep their IDs and run alongside.

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P2-A1 | Resync `task_plan.md`, fold the amendment decisions into this breakdown, write the nine new docs, correct the Exynos fact, record the skill-routing table in `13-process/ai-usage-policy.md` | M | P2-9 | docs-authoring | All new docs non-stub; `tools/check_doc_manifest.py` green; this table carries a Skill column |
| P2-A2 | `DeviceProfile` domain model, Room table, migration, repository | M | P2-8 | kmp-shared | Migration test proves the new table; profile round-trips; `check_no_android_imports_in_shared.py` green |
| P2-A3 | Device survey in `androidApp` (SoC, cores, RAM, storage, GPU, accelerator, Android version, refresh rate, thermal, battery), one-time on first launch, re-runnable from Settings | M | P2-A2 | kmp-shared | Two devices (or one device + one Robolectric profile) produce different persisted profiles; the survey explains every field it reads, in German, before reading it |
| P2-A4 | CI checker: every done task in this file has a skill recorded in the progress log; the progress-log entry for a task names the skill actually used | S | P0-20 | test-authoring | The checker fails on a planted done-task without a skill, and is wired into `tools/ci.sh` |
| P2-A5 | Plan-B ladder from D40 recorded in `phase-plan.md` Phase 3 and the risk register: native → proot → remote runners → own engine, the last only after (1)–(2) are documented as failed on the A56 | S | P2-A1 | docs-authoring | The blocker report format names the ladder, so a Phase-3 stop presents pre-costed options, not research |

## Phase 3 — Runtime

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P3-1 | `ExecutionBackend` interface and capabilities | M | P2-1 | kmp-shared | Matches `06-runtime/execution-backends.md` exactly |
| P3-2 | Bootstrap state machine: states, transitions, progress | L | P3-1 | runtime-bootstrap | Every transition tested, including the failure transitions |
| P3-3 | Download and checksum verification | M | P3-2 | runtime-bootstrap | A mismatch aborts with a clear message and never proceeds |
| P3-4 | Native profile: glibc-runner, patchelf, ELF patch | XL | P3-3 | runtime-bootstrap | `claude --version` runs on a real device |
| P3-5 | Engine version check and transparent update | M | P3-4 | runtime-bootstrap | At most once a day; rollback to the last known-good on failure |
| P3-6 | Process supervision: tree tracking, cancel, signals, exit codes | L | P3-1 | runtime-bootstrap | A cancelled run kills shell children too |
| P3-7 | The output ring buffer | M | P3-6 | runtime-bootstrap | Nothing is hidden; always visible in the terminal pane |
| P3-8 | The PTY bridge: allocate, feed, read, resize, exit code | L | P3-6 | runtime-bootstrap | `COLUMNS`/`LINES` propagate |
| P3-9 | The terminal renderer: monospace, ANSI, cursor, selection | XL | P1-9, P3-8 | android-compose | 16/256/truecolor; copy and paste work |
| P3-10 | The terminal keyboard row, with a reachable `Ctrl+C` | M | P3-9 | android-compose | Every key sends the right sequence |
| P3-11 | Headless prompt execution and event streaming | L | P3-4, P2-1 | kmp-shared | A headless prompt streams back into the app |
| P3-12 | Pre-flight health checks with specific fixes | M | P3-11 | runtime-bootstrap | Each failure names the actual problem and the actual fix |
| P3-13 | proot profile: Ubuntu bootstrap | XL | P3-2 | runtime-bootstrap | Completes its own bootstrap on a real device; cost shown up front |
| P3-14 | AVF profile: detection and limits | M | P3-2 | runtime-bootstrap | Detected where supported; never required. **The Galaxy A56 is an Exynos 1580, so support is probed at runtime (`DeviceProfile`, P2-A3) — no "unsupported" claim without the probe** |
| P3-15 | Foreground service and wake lock for long runs | M | P3-6 | runtime-bootstrap | Screen off for 10 minutes mid-run; the run survives |

## Phase 4 — Chat and projects

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P4-1 | Message model and streaming renderer | XL | P3-11, P1-8 | android-compose | Text and thinking deltas stream without list reflow churn |
| P4-2 | Tool cards: start, progress, result, collapse | L | P4-1 | android-compose | Each state visible; collapse state remembered per session |
| P4-3 | Live plan display, editable | M | P4-1 | android-compose | Steps with acceptance criteria; edits persist |
| P4-4 | Cost meter | M | P2-5, P4-1 | test-authoring | Accurate against the engine; per run, project, day, month |
| P4-5 | Interrupt | M | P4-1, P3-6 | android-compose | Partial work on a `wip/` branch, recoverable |
| P4-6 | Chat list, grouped by day and project | M | P4-1 | android-compose | Empty, loading, error, content all implemented |
| P4-7 | New chat with the animated mark | M | P1-12, P4-6 | android-compose | Project selector and composer present |
| P4-8 | Chat detail: the full screen | XL | P4-1…P4-5 | android-compose | All five states; large font does not clip |
| P4-9 | Chat with terminal split view | L | P4-8, P3-9 | android-compose | Works in portrait, landscape, and tablet |
| P4-10 | Project list | M | P2-2 | android-compose | Density reviewed; not a dashboard |
| P4-11 | Project detail: branches, PRs, run history | L | P4-10 | android-compose | Every state |
| P4-12 | Add project: clone, local, remote | L | P4-10 | android-compose | Privacy indicator shown before any remote is configured |
| P4-13 | Diff viewer: side-by-side and unified | XL | P4-11, P1-3 | android-compose | Accept and revert per hunk; syntax highlighted |
| P4-14 | Run history | M | P4-11 | android-compose | Inspect, export, revisit |
| P4-15 | The transparency log | L | P4-8 | github-safety | Append-only, complete, exportable |
| P4-16 | Autonomy levels wired to the policy | M | P2-4, P4-8 | test-authoring | Changing a level takes effect on the next tool call |
| P4-17 | Search | M | P4-6, P4-10 | android-compose | Conversations, projects, and skills |
| P4-18 | **E2E journey 1**: open app → new chat → pick project → order → stream → interrupt → resume | L | P4-8, P4-5 | test-authoring | Passes on an emulator |

## Phase 4 — Amendment tasks (`full-port-spec.md`)

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P4-A1 | Model picker: compact chip above the composer (provider, model, reasoning level) + full bottom sheet on tap; per-provider reasoning mapping; cost hint where the API provides one, otherwise "unknown" | L | P2-11, P4-1 | android-compose | Both surfaces render in both themes; the sheet lists every configured provider; a model without a price shows "unbekannt", never a guessed number |
| P4-A2 | Provider catalog (local JSON: OpenRouter, Groq, Google AI Studio first; free-tier flag; quotas never stated, linked) + guided setup wizard (pre-filled URL, key paste, connection test), free-first sort | L | P2-11, P4-A1 | android-compose | A layperson can add OpenRouter from the catalog without typing a URL; the test button gives the real failure reason |
| P4-A3 | Onboarding wizard: device survey → engine install → provider setup → test message, German-first copy, every step skippable and completable later from Settings | L | P2-A3, P3-4, P4-A2 | android-compose | Every step has skip, later, and failure states; skipping all steps still lands in a usable chat with `NO_PROVIDER_CONFIGURED` handled |
| P4-A4 | Sandbox folder grants: SAF persistable grants, Settings list with revoke, engine file gateway enforces [app-private ∪ project dirs ∪ granted], typed error on refusal | M | P2-13 | android-compose | A path outside the fence is refused with a taxonomy code, and the refusal is visible in the transparency log |
| P4-A5 | Backup export/import: versioned file with a content manifest; keys excluded by construction; import is additive and reports conflicts | M | P2-9 | test-authoring | An export contains no key material (test with a stored key), and import restores projects, settings, and provider configs on a fresh profile |

## Phase 5 — GitHub, skills, remote runners

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P5-1 | OAuth device flow | L | P2-8 | github-safety | Scopes are minimal; refresh and revocation work |
| P5-2 | Fine-grained PAT entry, with a scope checklist | M | P2-8 | github-safety | Validation button gives a real reason on failure |
| P5-3 | Clone, branch, commit after a run | L | P5-1, P4-12 | github-safety | Commit message names the task |
| P5-4 | Test-gated push | L | P5-3, P2-5 | github-safety | A red suite blocks the push and says why |
| P5-5 | Open a PR | M | P5-4 | github-safety | Only when green |
| P5-6 | The default-branch guard and hard-block enforcement in git | M | P5-3 | github-safety | Refused, with a test asserting the refusal |
| P5-7 | GitHub notifications: PR, comment, check, review | M | P5-5 | android-compose | Polling fallback with a configurable interval |
| P5-8 | Skill install from a GitHub URL | L | P2-8 | test-authoring | Detects `SKILL.md`, `.claude/skills/*`, plugin manifest |
| P5-9 | The install diff preview | M | P5-8 | android-compose | Every file and its destination shown before confirming |
| P5-10 | Skill validation | M | P5-8 | test-authoring | Broken skills show a repair button, never silent ignore |
| P5-11 | Skill scoping: global and per project | M | P5-8 | android-compose | The UI always shows where a skill lives and what it affects |
| P5-12 | Skill creation with AI | L | P5-10 | docs-authoring | Produces valid frontmatter; previewed before saving |
| P5-13 | Uninstall to quarantine | S | P5-11 | github-safety | No deletion — hard block 1 |
| P5-14 | The marketplace index | M | P5-8 | docs-authoring | Static JSON in-app; optional remote index |
| P5-15 | `SshBackend` for a home PC | L | P3-1, P5-1 | kmp-shared | A full run round-trips |
| P5-16 | `CloudRunnerBackend` for Oracle Always Free | L | P3-1, P5-1 | kmp-shared | Live quota check at setup; graceful degradation |
| P5-17 | `GithubActionsBackend` | L | P3-1, P5-1 | kmp-shared | Free-minute accounting shown |
| P5-18 | Offload: same UI, same protocol, one flag | M | P5-15, P5-16, P5-17 | kmp-shared | The session record names the backend |
| P5-19 | **E2E journey 2**: a task on a private test repo yields a green branch and an open PR | L | P5-5 | test-authoring | Passes against a real private test repo |
| P5-20 | **E2E journey 3**: install a skill from a URL and use it in a run | M | P5-9, P5-12 | test-authoring | Passes |

## Phase 5 — Amendment tasks (`full-port-spec.md`)

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P5-A1 | Connectivity supervision: a run enters `PAUSED_OFFLINE` on network loss, resumes on reconnect, notification on both; every network operation idempotent/resumable | M | P3-11, P4-8 | kmp-shared | A killed radio mid-run (or a simulated offline state in a test) pauses without data loss and resumes; the state is visible in the UI and the transparency log |
| P5-A2 | Accelerator capability registry from the `DeviceProfile` + local-model spike (LiteRT / LiteRT-LM / vendor SDK / llama.cpp — chosen by what the survey finds). **TBD — verify at build time** | L | P2-A3, P3-1 | kmp-shared | A dated research note in `10-build/dependency-versions.md` names the chosen stack with sources; the registry exposes the capabilities the survey found |

## Phase 6 — Verification and self-healing

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P6-1 | Verification command detection per project type | L | P2-5 | test-authoring | Gradle, npm, cargo, pytest; editable by the user |
| P6-2 | The verifier: run and report | M | P6-1 | test-authoring | Results structured; failures show the actual output |
| P6-3 | The judge: did the task actually succeed | L | P6-2 | test-authoring | A unit test where the judge's verdict differs from the agent's claim |
| P6-4 | The retry controller and budget (3/10/50/unlimited) | M | P6-3 | test-authoring | The counter is always visible; budget exhaustion is reported correctly |
| P6-5 | Anti-loop detection | M | P6-4 | test-authoring | A genuinely stuck loop is aborted with a clear reason |
| P6-6 | Checkpoints: after plan, before push, before dependency install | M | P6-3 | test-authoring | Each stops the run and reports |
| P6-7 | Context budgeting and compaction | L | P6-1 | test-authoring | A long run does not exceed the budget silently |
| P6-8 | Self-update: check, verify, install, roll back | L | P2-9 | runtime-bootstrap | Signature and checksum verified; a broken engine rolls back |
| P6-9 | Field-error triage | M | P4-15 | test-authoring | A recurring signature becomes a one-tap fix task |
| P6-10 | **E2E journey 4**: a broken project reaches green within budget | L | P6-4 | test-authoring | Passes; the exhaustion path also reports correctly |

## Phase 6 — Amendment tasks (`full-port-spec.md`)

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P6-A1 | Navigation retrofit to the Claude-app layout: left switcher (Chat / Projects / Claude Code), the bottom bar retired after the switcher passes its states | L | P4-8, P4-10 | ui-design | Both themes, four states each; the switcher is a UI-parity trial per `03-design/ui-parity.md` and is reviewed like any generated screen |
| P6-A2 | Rebrand pass + release-checklist legal gate: own name and logo, trademark review, "unofficial, unaffiliated" notice retained — required before any public distribution (D31/D46) | M | P7-3 | release | `release-checklist.md` has the gate; a distribution without it is blocked |
| P6-A3 | Device-profile-driven tuning: UI fluidity tiers and the `gradle.properties` template (Gradle workers, daemon heaps, test parallelism) for on-device builds, written from the `DeviceProfile` | M | P2-A3, P7-6 | test-authoring | The A56's profile produces a written properties template and a measured before/after build time on the device |
| P6-A4 | Monetisation seam only: a licensing/entitlements interface and feature-flag plumbing. No paywall, no licence code, no store billing ships now (D47) | S | P4-8 | kmp-shared | The seam compiles, is unused by default, and its test proves flags default to off |

## Phase 7 — Autonomy, polish, delivery

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P7-1 | Background execution: service, notifications, wake locks | L | P3-15 | android-compose | Screen off, app backgrounded, and battery saver all keep the run alive |
| P7-2 | Notification channels and actionable permission prompts | M | P7-1 | android-compose | Approve/Deny/Always from the notification |
| P7-3 | Remaining screens, all five states each | XL | P4-11 | ui-design | Every screen in `04-screens/` implemented |
| P7-4 | Screenshot polish across every screen, both themes | L | P7-3, P1-13 | ui-design | Design-review pass per screen; no template-looking output |
| P7-5 | Accessibility pass | L | P7-3 | ui-design | No critical findings; AA contrast; ≥ 48 dp; labels on every control |
| P7-6 | Performance pass against the budgets | L | P7-3 | test-authoring | Startup, cold bootstrap, scrolling, memory all inside budget |
| P7-7 | Security pass against the threat model | M | P7-3 | github-safety | Every threat's control verified; the model updated if anything changed |
| P7-8 | The README, to its own checklist | M | P7-3 | docs-authoring | All 12 checklist items green |
| P7-9 | Play and F-Droid metadata | M | P7-8 | release | A clean-room F-Droid build succeeds |
| P7-10 | The release checklist, box by box | L | P7-5, P7-6, P7-7, P7-8 | release | Every box ticked by whoever ran it |
| P7-11 | **E2E journey 5**: the self-update check | M | P6-8 | test-authoring | Passes |
| P7-12 | A non-technical user completes the whole flow unaided | L | P7-10 | release | This is the project's definition of done |

## Phase 7 — Amendment tasks (`full-port-spec.md`)

| ID | Task | Est | Dep | Skill | Acceptance |
|---|---|---|---|---|---|
| P7-A1 | **Acceptance journeys on the A56** (D45): (A) one sentence → small app built, tested, green branch + open PR on a private test repo without hand-holding; (B) existing repo, described bug, located and fixed, diff presented for review. Both recorded with transcripts | L | P5-19, P7-10 | release | Both journeys pass on the device, are recorded in the progress log, and join the per-project definition of done |

## Sizing summary

| Phase | Tasks | Estimated hours |
|---|---|---|
| 0 Foundation | 22 | ~130 |
| 1 Design system | 14 | ~120 |
| 2 Data and core | 13 + 5 amendment | ~130 + ~20 |
| 3 Runtime | 15 | ~180 |
| 4 Chat and projects | 18 + 5 amendment | ~190 + ~40 |
| 5 GitHub, skills, runners | 20 + 2 amendment | ~150 + ~15 |
| 6 Verification, self-healing | 10 + 4 amendment | ~60 + ~25 |
| 7 Autonomy, polish, delivery | 12 + 1 amendment | ~120 + ~15 |
| **Total** | **124 + 17 amendment** | **~1080 h + ~115 h** |

Roughly six months of one person's focused work, or a fraction of that for an agent that does not get tired. The number is here to make the plan honest, not to make it feel small. Two tasks are the risk, and neither is measured well: **P3-4** (the ELF patch) and **P3-13** (the proot bootstrap). Both are XL, both could be larger, and both are why Phase 3 is a hard stop. **P3-4 and P3-13 are also the Plan-B decision point** (`full-port-spec.md` D40): if both are documented as failed on the A56, the blocker report presents the ladder — remote runners next, own engine last — and the build stops for that decision.

The amendment tasks come from `full-port-spec.md` (decisions D30–D48, interview of 2026-09-30). They keep their own ID space (`P<n>-A<m>`) so no existing ID moved, and their estimates are part of the totals above.

## Depends on

`14-build-plan/phase-plan.md` · `14-build-plan/dependency-graph.md` · `14-build-plan/milestones.md` · `14-build-plan/risk-register.md` · `14-build-plan/progress-log.md`
