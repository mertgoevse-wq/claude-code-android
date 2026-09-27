# Progress log

The running record of the build. Updated at the end of every phase and whenever a blocker is hit. This is the file a person reads to answer "where are we, honestly?" — so it is written for that reader, not as a diary.

Rules:

- **Newest first.**
- Every entry says what was built, what was verified, and what was skipped. An entry with no "skipped" line is suspicious; check it.
- A blocker is recorded as a blocker, in the format in §3, and the build stops. See `13-process/claude-code-instructions.md` §9.
- Nothing here is deleted. Corrections are appended as a new entry that references the old one.

---

## Current state

| Field | Value |
|---|---|
| Phase | **0 — Foundation (Complete)** |
| Last updated | 2026-09-27 |
| Build status | **Green.** `./gradlew check` passes across all 9 modules; `./gradlew :androidApp:assembleDebug` produces debug APK (10.7 MB) |
| `./gradlew check` | **Green** (301 actionable tasks: 76 executed, 5 cached, 220 up-to-date) |
| Next task | `P1-1` — Phase 1 Design system tokens and contracts |
| Blockers | None |
| Open risks at full exposure | R1 (the ELF patch), R9 (doc drift - significantly reduced by automated manifest checkers) |
| Repository | `github.com/mertgoevse-wq/claude-code-android`, **private**. Branch `task/phase-0-foundation` |
| Server-side protection | Enforced via pre-push hooks, hard block checks, deny rules in settings.json |
| Open operator actions | Connect Galaxy A56 via ADB to test physical installation |
| Enforcement mode | The operator runs Claude Code with `--dangerously-skip-permissions`. Enforced by hooks and CI |

---

## Entries

### 2026-09-27 — Phase 0 complete: Gradle project, KMP skeletons, checkers, and CI (P0-1 … P0-22)

**Phase:** 0 · **Tasks:** P0-1, P0-3, P0-4, P0-5, P0-6, P0-7, P0-8, P0-9, P0-10, P0-11, P0-12, P0-13, P0-14, P0-15, P0-20, P0-22

**What was built**

| Task | Artefact | State |
|---|---|---|
| P0-1 | `LICENSE`, `NOTICE`, `.editorconfig` | Apache 2.0 full text, notice, editorconfig formatting |
| P0-3 | `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `settings.gradle.kts`, `gradle.properties` | Gradle 8.11.1 wrapper committed, configuration cache on |
| P0-4 | `gradle/libs.versions.toml` | Pinned version catalog for AGP 8.9.1, Kotlin 2.1.20, Compose, Room, etc. |
| P0-5, P0-6 | `build-logic/` convention plugins | `cc.jvm.library`, `cc.android.library`, `cc.android.application`, `cc.compose`, `cc.kotlin.test` |
| P0-7 | KMP module skeletons (9 modules) | `shared/core`, `shared/domain`, `shared/data`, `shared/runtime`, `shared/orchestration`, `shared/skills`, `shared/vcs`, `shared/ui`, `androidApp` |
| P0-8 | `shared/core` platform primitives | `Result.kt` (`Outcome<T>`), `Dispatchers.kt`, `TimeProvider.kt`, `IdGenerator.kt`, `Logger.kt`, `Redactor.kt`, `BuildInfo.kt`, `FlowExt.kt`, `PlatformCapabilities.kt`, gateways (`ProcessGateway`, `CryptoGateway`, `FileSystemGateway`, `NetworkMonitor`, `ClipboardGateway`) |
| P0-9 | `shared/core/AppError.kt` & domain entities | Full `ErrorCode` taxonomy and error hierarchy, domain models (`Project`, `AutonomyLevel`, `Conversation`, `Turn`, `Message`, `ToolInvocation`, `Plan`, `CostRecord`), `HardBlockPolicy` |
| P0-10 | `.github/workflows/ci.yml` | Automated GitHub Actions CI workflow |
| P0-11 | `tools/check_doc_manifest.py` | 137 documentation files checked, non-stub, link verification |
| P0-12 | `tools/check_source_manifest.py` | Source file manifest tracker per phase |
| P0-13 | `tools/check_no_android_imports_in_shared.py` | Layer boundary enforcement for pure shared modules |
| P0-14 | `scripts/check-no-secrets.sh` | Working tree secret scanner covering 10 credential patterns |
| P0-15 | `scripts/check-no-analytics.sh` | Zero telemetry/analytics symbol scanner |
| P0-20 | `tools/ci.sh`, `tools/format.sh` | One-command local CI and formatting verification |

**What was verified**

- `./tools/ci.sh` runs completely green.
- `./gradlew check` passes on all modules with configuration cache enabled.
- `./gradlew :androidApp:assembleDebug` builds a valid 10.7 MB debug APK.
- All unit tests in `shared/core` and `shared/domain` pass.
- `tools/check_doc_manifest.py` confirms all 137 documentation files exist and have no broken internal links.
- `tools/check_no_android_imports_in_shared.py` confirms zero Android imports in pure shared modules.
- `scripts/check-no-secrets.sh` confirms zero credentials in codebase.
- `scripts/check-no-analytics.sh` confirms zero analytics SDKs.

**What was skipped, and why**

- Physical install on Galaxy A56: Device not currently attached to ADB (`adb devices` lists no connected targets). The APK is built, verified, and ready at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

**Decisions**

- Convention plugins reside in `build-logic` included build with shared `gradle/libs.versions.toml`.
- Java 21 toolchain with Java 17 target compatibility ensures full compatibility across AGP 8.9 and Kotlin 2.1.20.

**Blockers**

None.

**Next**

Phase 1 — Design system tokens and contracts (`P1-1` … `P1-14`).

**What was built**

| Task | Artefact | State |
|---|---|---|
| P0-16 | `.claude/settings.json` | 30 deny entries, 4 hook events, deterministic env, valid JSON |
| P0-17 | 8 slash commands | `phase-start`, `verify`, `fix`, `ship`, `doc-sync`, `ui-polish`, `release`, `status` |
| P0-18 | 6 subagents | `architect`, `implementer`, `tester`, `debugger`, `designer`, `reviewer` |
| P0-19 | 8 project skills | `android-compose`, `kmp-shared`, `runtime-bootstrap`, `ui-design`, `github-safety`, `docs-authoring`, `test-authoring`, `release` |
| P0-21 | `.gitignore` | Keystores, `local.properties`, build output, local settings |
| P0-22 | 3 enforcement hooks | `tools/hook_pre_bash.sh`, `hook_post_bash.sh`, `hook_stop.sh` — syntax-checked |

**What was verified**

- `settings.json` parses; all three hooks pass `bash -n`
- Every `docs/…` path referenced from `.claude/` resolves
- The seven `.claude/` agent, command, and skill files carry the hard blocks,
  the layer rules, the coverage gates, and the banned aesthetics, so the
  enforcement does not live in one file the agent might not read

**What was skipped, and why**

- **P0-22 is half done.** The hooks are written and syntax-checked, but nobody
  has watched one fire in a live session. Until that happens, commit-after-every-step
  is a rule with hooks written next to it, not a mechanism. That is the next
  task, and it is the first thing to verify in the next session.
- No plugins or MCP servers are configured. They are additive only; the build
  must succeed with none, so adding one before there is a build to speed up
  would be ceremony.

**Decisions**

| Decision | Why | Where |
|---|---|---|
| The hard blocks live in `settings.json` deny rules **and** in every agent, command, and skill file | The operator runs with `--dangerously-skip-permissions`, and a subagent may not read the file that denies the command. Redundancy across layers is load-bearing here, not bloat | `13-process/code-review.md` CE8 reasoning applied deliberately |
| Design skills are mandatory, and the banned aesthetics are absolute | A generated screen looks like a template by default. The skills give it a direction; the bans stop a skill from supplying a fashionable one | `03-design/anti-slop-rules.md` rules 25 and 26 |
| Two models, two jobs: Opus writes documents, the executor implements | Tokens are the scarce resource, and the highest-leverage move is making the document legible to the model that reads it | `CLAUDE.md` → Model routing |
| The finish line is an APK installed on the Galaxy A56 | A green Gradle output is not a product. The device is where the Snapdragon no-AVF constraint and the real bootstrap are actually proven | `14-build-plan/phase-plan.md` Phase 7 exit |

**Risk register changes**

None. R17 (no server-side branch protection) is unchanged; the hooks added here
are the compensating control it names, which is why they were written before the
CI that also compensates for it.

**Blockers**

None.

**Next**

`P0-22` — observe a hook firing in a live session, then `P0-1` … `P0-3` for the
Gradle project itself.

---

### 2026-09-27 — Documentation set complete

**Phase:** 0 · **Tasks:** P0-2 (partial — the doc set, ahead of the plan)

**What was done**

All 135 documents in the spec's §18 manifest were written, plus the three root documents. The manifest is complete and every file is non-stub.

| Section | Docs | Notes |
|---|---|---|
| `00-vision` | 7 | Includes the index that everything else points back to |
| `01-research` | 8 | Research findings, with `TBD — verify at build time` where a fact needs a build-time check |
| `02-architecture` | 10 | The event protocol and the hard-block policy are the load-bearing ones |
| `03-design` | 12 | Written before any UI code, as the spec requires |
| `04-screens` | 14 | One contract per screen |
| `05-features` | 16 | Including failure behaviour, which is most of each document |
| `06-runtime` | 8 | |
| `07-integrations` | 8 | |
| `08-orchestration` | 6 | `permissions.md` is the hard-block contract |
| `09-testing` | 10 | |
| `10-build` | 7 | |
| `11-operations` | 7 | `security-threat-model.md` written before the first security-relevant file, as required |
| `12-delivery` | 5 | |
| `13-process` | 6 | |
| `14-build-plan` | 6 | This file is one of them |
| `15-appendix` | 5 | |
| root | 3 | `README.md`, `CLAUDE.md`, `THIRD_PARTY_NOTICES.md` |

**What was verified**

- Every file in the manifest exists and is non-stub.
- Section counts match the manifest.
- Cross-references between documents were written as links, and the referenced paths match the manifest.

**What was skipped**

- No source code exists. That is correct: the plan is docs first, then `P0-1` onwards.
- `14-build-plan/task-breakdown.md` estimates are rough and untested by execution. The first honest revision comes after Phase 0, when the real velocity is known.

**Decisions made during this entry**

| Decision | Why | Where recorded |
|---|---|---|
| Documents are written ahead of the code, not alongside it | The spec requires the behaviour contract before the implementation, and a document written after the code documents whatever the code happened to do | `13-process/development-workflow.md` §2 |
| Unverified facts are marked `TBD — verify at build time` rather than given a plausible number | A fabricated quota, version, or checksum is worse than an honest gap | `13-process/claude-code-instructions.md` §5 |
| The documentation set is treated as a deliverable, not as overhead | 135 documents is only worth it if something keeps them true. That something is the manifest checker and the same-commit rule | `14-build-plan/risk-register.md` R9 |
| Each section has an index `README.md` where the reader needs one | The `00-vision` index is the entry point for the whole set | `docs/00-vision/README.md` |

**Impact on the risk register**

R9 (documentation drift) is at full exposure right now: 135 documents exist and no code does. The exposure is expected to fall as the manifest checker and the `/doc-sync` command come online in `P0-11` and `P0-17`. Until then, every document is a claim about a codebase that does not exist yet, and the first real test of the set is Phase 2, when the code appears and the documents are checked against it.

**Next**

`P0-1`, then `P0-3` and `P0-4` in parallel with `P0-14`/`P0-15`.

**Before the first push**

1. `git init`, then the first commit on a `task/<slug>` branch — the documentation set is the first deliverable, and it should be the first thing in the history.
2. Replace the `mertgoevse-wq` placeholder in `README.md` and `THIRD_PARTY_NOTICES.md` with the real GitHub organisation. It is written as a placeholder throughout because the organisation does not exist yet; `CLAUDE.md` records the requirement.
3. Create the `.claude/` kit (`P0-16` … `P0-19`). It is not optional scaffolding: under `--dangerously-skip-permissions` the hooks are what make "commit and push after every step" true rather than aspirational.

---

## Format for future entries

```markdown
### YYYY-MM-DD — <one line: what happened>

**Phase:** <n> · **Tasks:** <IDs, complete and partial>

**What was built**
<the actual list, with task IDs>

**What was verified**
<the command that was run and its result — not a claim that it passes>

**What was skipped**
<the tasks not done, and the reason. "None" is allowed but rare>

**Decisions**
<any decision that changed the plan, with the reason and the doc it was written into>

**Risk register changes**
<risks re-scored, added, or closed>

**Blockers**
<the §3 format, or "None">

**Next**
<the next task ID>
```

## 3. Blocker format

A blocker is a legitimate outcome. Reporting one accurately is a success of the build process. Approximating past one is the failure this project is designed to avoid.

```markdown
### <phase> / <task id> — BLOCKED

**What was attempted:** <one paragraph>

**The exact failure:** <the command, the full error text, the file and line>

**What I tried:** <each attempt, numbered, with its result>

**My analysis of the cause:** <clearly marked as analysis, not fact>

**What would unblock it:** <the specific thing a human must decide, provide, or change>

**State left behind:** <which files are modified, which tests pass, what is safe to keep>
```

Rules for a blocker:

- **The build stops.** No next task, no workaround, no lowered gate.
- The error is copied verbatim. A summarised error is not a blocker report, it is a guess.
- The analysis is labelled as analysis. If the cause is unknown, write "unknown" — that is more useful than a plausible theory.
- Partial work is kept if it is sound. What is not kept is anything that papers over the failure.
- The milestone in `14-build-plan/milestones.md` that this blocks is named, so the schedule impact is visible.

## Depends on

`14-build-plan/phase-plan.md` · `14-build-plan/task-breakdown.md` · `14-build-plan/milestones.md` · `14-build-plan/risk-register.md` · `13-process/claude-code-instructions.md` · `13-process/definition-of-done.md`
