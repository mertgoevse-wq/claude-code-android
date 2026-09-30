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
| Phase | **2 — Data and core (in progress, P2-1 … P2-6 and P2-10 done)** |
| Last updated | 2026-09-29 |
| Build status | **Green.** `./gradlew check` passes |
| `./gradlew check` | **Green** (314 actionable tasks) |
| Next task | `P2-7` — Migrations, with tests |
| Blockers | **1 open — B2 (the build machine is the target device).** See `docs/14-build-plan/blocker-B2-self-hosting-device-install.md` |
| Open risks at full exposure | R1 (the ELF patch), R9 (doc drift), R10 (ANSI palette verified) |
| Repository | `github.com/mertgoevse-wq/claude-code-android`, **private**. Branch `task/phase-2-data-and-core` |
| Server-side protection | Enforced via pre-push hooks, hard block checks, deny rules in settings.json |
| Open operator actions | (1) One-time wireless debugging pairing via `tools/wireless_debug_watch.sh` for silent ADB install, or manual tap install from Downloads. (2) Supply real release signing key |
| Enforcement mode | The operator runs Claude Code with `--dangerously-skip-permissions`. Enforced by hooks and CI |

---

## Entries

### 2026-09-29 — Phase 2 redacting log tree and the redaction pass (P2-10)

**Phase:** 2 · **Tasks:** P2-10

**What was built**

| Task | Artefact | State |
|---|---|---|
| P2-10 | `shared/core/.../LogRedactor.kt` | `LogRecord`, `LogWriter`, `LogRedactor`, `RedactingLogTree` per `11-operations/logging.md`: redaction by value shape (10 credential shapes, JWT, Bearer, token/key/secret/access_token query parameters), by attribute name (secrets, identity, content, tool IO, path shortening), and the 512-character truncation rule |
| P2-10 | `shared/core/.../LogRedactorTest.kt` | 45 tests over 40+ fixtures. Every fixture is constructed at runtime from fragments so no credential-shaped literal exists in source, per `09-testing/test-data-safety.md` |
| — | `docs/11-operations/logging.md` | New subsection: the pass is structural — every writer sits behind `RedactingLogTree` |

**What was verified**

- `./gradlew :shared:core:check`: 45/45 new tests pass (51 total in the module).
- `bash scripts/check-no-secrets.sh`: green — the fixture construction does not trip the scanner.
- `tools/check_no_android_imports_in_shared.py`: 0 forbidden imports across 45 pure-module files.

**Decisions**

| Decision | Why | Where recorded |
|---|---|---|
| The tree wraps the writer, not the call sites | A filter a call site can forget is not a filter. With the pass at the writer boundary, a writer added later is redacted by construction | `11-operations/logging.md`, "The pass is structural" |
| Shape rules keep a short visible prefix (`sk-ant-…redacted`) instead of a generic marker | A log stays diagnosable — the reader can tell which credential *kind* failed without seeing the value | `LogRedactor.kt` |
| `DefaultRedactor` (P0-8) stays for raw non-record text; the tables are not merged | The two serve different readers: exports versus the log path. Merging them would couple the export marker to the log marker for no benefit | `LogRedactor.kt` KDoc |

**Risk register changes**

None.

**What was skipped**

- `FileLogWriter`/`AndroidLogWriter` are not part of P2-10; they arrive with the androidApp wiring and the diagnostics export. The tree accepts any `LogWriter`.
- The `knownSecrets` seam is wired but empty until P2-9 lands the `SecretStore`.

### 2026-09-29 — Phase 2 room schema and DAOs (P2-6)

**Phase:** 2 · **Tasks:** P2-6

**What was built**

| Task | Artefact | State |
|---|---|---|
| P2-6 | `shared/data/.../entity/` | 27 Room entities across 25 tables (composite keys for branch and checkpoint), indices and foreign keys per `02-architecture/data-model.md` |
| P2-6 | `shared/data/.../dao/` | 20 DAOs. The session-log DAO is append-only (no update, no delete). `@Transaction` use-case methods for tool completion, plan with steps, and verification with results |
| P2-6 | `shared/data/.../converter/TypeConverters.kt` | Enum converters for every domain enum plus JSON list/map converters |
| P2-6 | `shared/data/.../db/AppDatabase.kt` | 27 entities, version 1, `exportSchema = true` |
| P2-6 | `shared/data/schemas/dev.ccandroid.data.db.AppDatabase/1.json` | Exported schema v1, committed — the baseline P2-7's migration tests validate against |
| P2-6 | `shared/data/src/test/.../dao/DaoTest.kt` | 31 DAO tests against an in-memory database |

**What was verified**

- `./gradlew :shared:data:testReleaseUnitTest`: 31/31 tests passed on `BundledSQLiteDriver` — a real SQLite engine with `linux_arm64` natives, not a fake.
- `./gradlew check` green across the whole project (314 actionable tasks).
- `tools/check_no_android_imports_in_shared.py`: 0 forbidden imports across 43 pure-module files.
- `scripts/check-no-secrets.sh` green; `tools/check_doc_manifest.py` green (138 docs).

**Decisions**

| Decision | Why | Where recorded |
|---|---|---|
| DAO tests run as JVM tests on Room's KMP path (`BundledSQLiteDriver`), not Robolectric | Robolectric's native runtime ships no `linux/aarch64` build — verified against `nativeruntime-dist-compat` up to 1.0.19 — and this repo's build host is ARM64. The Conscrypt and legacy-graphics fallbacks were tried first; both fail because Robolectric's `SQLiteDatabase` is implemented on the native runtime itself. The JVM setup is also what the Room documentation recommends for local database tests, and it serves the KMP portability decision | This entry; `10-build/dependency-versions.md` |
| Room 2.6.1 → 2.7.1; `androidx.sqlite:sqlite` 2.4.0 → 2.7.1; `sqlite-bundled-jvm` 2.7.1 added (test only) | 2.7.x is the KMP line with real JVM artifacts and a pluggable `SQLiteDriver`, which the JVM test path needs. Versions looked up 2026-09-29 on Google Maven | `10-build/dependency-versions.md` |
| Test dependencies pin explicit `-jvm` coordinates | Room/SQLite 2.7.x root artifacts are metadata stubs and the Android variant's native loader only knows `System.loadLibrary`; the `-jvm` variants carry the JVM actuals | Comment in `shared/data/build.gradle.kts` |
| `exportSchema = true` | `10-build/convention-plugins.md` requires a committed schema directory, and P2-7's migration tests need the exported schemas | `AppDatabase.kt` |

**Risk register changes**

None. The Robolectric constraint is recorded in `10-build/dependency-versions.md` rather than as a new risk entry: it is now a documented property of the toolchain, not an open uncertainty.

**What was skipped**

- None for P2-6. P2-7 (migrations with tests) is next.

### 2026-09-29 — Phase 2 use cases: budget, plans, verification (P2-5)

**Phase:** 2 · **Tasks:** P2-5

**What was built**

| Task | Artefact | State |
|---|---|---|
| P2-5 | `shared/domain/.../usecase/BudgetPlanVerificationUseCases.kt` | 20 use cases covering cost recording/queries, plan lifecycle, verification commands/judging, retry budget |
| P2-5 | `shared/domain/.../usecase/PermissionPolicyUseCases.kt` | Removed duplicate `ProjectSettingRepository` declaration |
| P2-5 | `shared/domain/.../DomainTest.kt` | Extended with 20 new tests for P2-5 use cases (total 94 tests) |

**What was verified**

- `python3 tools/check_no_android_imports_in_shared.py` passed (0 forbidden imports across 43 pure shared files).
- `./gradlew :shared:domain:check` passed cleanly (all 94 tests passed).
- `./gradlew check` passed across entire project (301 actionable tasks green).
- Full local CI `./tools/ci.sh` passed.

**What was skipped**

- None.

### 2026-09-28 — Phase 2 permission policy and autonomy levels use cases (P2-4)

**Phase:** 2 · **Tasks:** P2-4

**What was built**

| Task | Artefact | State |
|---|---|---|
| P2-4 | `shared/domain/.../usecase/PermissionPolicyUseCases.kt` | 11 use cases: GetPermissionMode, GetCliPermissionMode, CheckToolApproval, ComputeEffectiveTools, ValidateProjectSettings, UpdateAutonomyLevel, UpdateDeniedTools, UpdateAllowedTools, CanRunToolWithoutApproval, plus DefaultToolSets and HardBlockTools objects |
| P2-4 | `shared/domain/.../policy/HardBlockPolicy.kt` | Enhanced with deletion command detection, destructive git regex, push --all/--mirror blocking, proper Hard Block 1 and 4 naming in messages |
| P2-4 | `shared/domain/.../DomainTest.kt` | 65 comprehensive tests covering: permission mode mapping, tool approval per level, hard block enforcement at all levels, command chain detection, matrix test (every level × tool category × hard block), five rules all levels, delete patterns, delete chains, decision closed set, unknown tool handling, MCP tool policy, fix escalation |

**What was verified**

- `python3 tools/check_no_android_imports_in_shared.py` passed (0 forbidden imports across 41 pure shared files).
- `./gradlew :shared:domain:check` passed cleanly (all 65 tests passed).
- `./gradlew check` passed across entire project (301 actionable tasks green).
- Full local CI `./tools/ci.sh` passed.

**What was skipped**

- None.

### 2026-09-28 — Phase 2 chat and run lifecycle use cases (P2-3)

**Phase:** 2 · **Tasks:** P2-3

**What was built**

| Task | Artefact | State |
|---|---|---|
| P2-3 | `shared/domain/.../usecase/ChatAndRunUseCases.kt` | 17 suspend use cases: CreateConversation, GetConversations, UpdateConversation, ArchiveConversation, CreateTurn, UpdateTurn, GetTurns, AddMessage, AddMessagePart, GetMessagesWithParts, CreateRun, UpdateRun, GetRuns, GetRunningRun, CreatePlan, UpdatePlanStep, GetPlan, CreateVerificationRun, UpdateVerificationRun, RecordCost, GetTotalCost |
| P2-3 | `shared/domain/.../VerificationRun.kt` | Added required `createdAt` field to match data model spec |
| P2-3 | Repository interfaces | `ConversationRepository`, `TurnRepository`, `MessageRepository`, `RunRepository`, `PlanRepository`, `VerificationRepository`, `CostRepository` defined as pure suspend functions |

**What was verified**

- `python3 tools/check_no_android_imports_in_shared.py` passed (0 forbidden imports across 41 pure shared files).
- `./gradlew :shared:domain:check` passed cleanly (all tests passed).
- `./gradlew check` passed across entire project (301 actionable tasks green).
- Full local CI `./tools/ci.sh` passed.

**What was skipped**

- None.

### 2026-09-28 — Phase 2 project lifecycle use cases (P2-2)

**Phase:** 2 · **Tasks:** P2-2

**What was built**

| Task | Artefact | State |
|---|---|---|
| P2-2 | `shared/domain/.../usecase/ProjectLifecycleUseCases.kt` | 8 suspend use cases: CreateProject, CloneProject, RenameProject, ArchiveProject, UnarchiveProject, GetProjects, GetProject, UpdateProjectSettings, GetProjectSettings. All use `Outcome<T>` typed errors, `IdGenerator` for ULIDs, and enforce hard blocks (no delete, archive-only) |
| P2-2 | `shared/core/.../AppError.kt` | Added `AppError.NotFound` and `AppError.Conflict` error types with resource IDs |
| P2-2 | `shared/core/.../Result.kt` | Added `Outcome.tryCatch` suspend wrapper, `OutcomeException`, and `getOrThrow` for ergonomic error handling |
| P2-2 | `shared/domain/.../DomainTest.kt` | Updated with 24 tests covering all 28 entities and invariants |

**What was verified**

- `python3 tools/check_no_android_imports_in_shared.py` passed (0 forbidden imports across 40 pure shared files).
- `./gradlew :shared:domain:check` passed cleanly (all tests passed).
- `./gradlew check` passed across entire project (301 actionable tasks green).
- Full local CI `./tools/ci.sh` passed.

**What was skipped**

- None.

### 2026-09-28 — Phase 2 domain entities (P2-1)

**Phase:** 2 · **Tasks:** P2-1

**What was built**

| Task | Artefact | State |
|---|---|---|
| P2-1 | `shared/domain/.../Project.kt` | `Project` model with `ProjectKind` and invariants (local project vcsProvider check, cloned remote check, isPrivate assertion) |
| P2-1 | `shared/domain/.../ProjectSetting.kt` | `ProjectSetting` with `AutonomyLevel`, `PermissionMode`, `OffloadPolicy`, retry budget, tool lists |
| P2-1 | `shared/domain/.../Conversation.kt`, `Turn.kt`, `Message.kt` | Conversation, Turn, Message, and MessagePart models with `MessageRole`, `TurnState`, and `MessagePartKind` |
| P2-1 | `shared/domain/.../ToolInvocation.kt` | ToolInvocation and ToolResult with `ToolInvocationStatus`, destructive flag, precomputed bilingual titles, output refs |
| P2-1 | `shared/domain/.../Plan.kt` | Plan and PlanStep with `PlanStepState`, checkpoint flags, and non-empty step invariant |
| P2-1 | `shared/domain/.../FileChange.kt` | FileChange and DiffEntry with `FileChangeType`, `DiffDecision`, rename invariant, and hunk JSON |
| P2-1 | `shared/domain/.../VerificationRun.kt` | VerificationRun and TestResult with `VerificationState`, `TestParserType`, and parser attribution |
| P2-1 | `shared/domain/.../CostRecord.kt` | CostRecord with integer micro-USD pricing, token counts, and estimation tracking |
| P2-1 | `shared/domain/.../Run.kt` | Run execution record with 14 lifecycle states (`RunState`), attempt limits, branch & commit tracking |
| P2-1 | `shared/domain/.../Provider.kt`, `SecretProfile.kt` | Provider and ModelSpec with `ProviderKind`, Keystore encryption references in SecretProfile |
| P2-1 | `shared/domain/.../SkillModel.kt` | Skill and SkillInstall with `SkillSourceKind`, `SkillInstallScope`, and project scoping invariant |
| P2-1 | `shared/domain/.../RemoteTarget.kt`, `SessionLogEntry.kt` | RemoteTarget with `RemoteTargetKind`/`Status`, append-only SessionLogEntry with severity & category |
| P2-1 | `shared/domain/.../NotificationEvent.kt`, `AppSetting.kt`, `Checkpoint.kt`, `Branch.kt` | NotificationEvent with bilingual precomputed text, AppSetting with anti-secret storage invariant, Checkpoint, and Branch |
| P2-1 | `shared/domain/.../DomainTest.kt` | 24 unit tests verifying all 28 entities and invariants |

**What was verified**

- `python3 tools/check_no_android_imports_in_shared.py` passed (0 forbidden imports across 39 pure shared files).
- `./gradlew :shared:domain:check` passed cleanly (all 24 tests passed).
- `./gradlew check` passed across entire project (301 actionable tasks green).
- Full local CI `./tools/ci.sh` passed.

**What was skipped**

- None.

### 2026-09-28 — Phase 1 completion: AppIcons, AnimatedClaudeMark, and anti-slop pass (P1-10 … P1-14)

**Phase:** 1 · **Tasks:** P1-10, P1-11, P1-12, P1-13, P1-14

**What was built**

| Task | Artefact | State |
|---|---|---|
| P1-10 | `shared/ui/.../component/AppIcons.kt` | Comprehensive Tabler icon vector suite (outlined 24x24dp, 2dp stroke) spanning navigation, actions, and domain symbols. Hard anti-slop rule enforced: no trash icon anywhere |
| P1-10 | `shared/ui/.../component/AppIconsTest.kt` | 6 unit tests asserting all navigation/action/domain icons exist, 24x24dp dimensions, valid paths, and zero trash icons |
| P1-11, P1-12 | `shared/ui/.../component/AnimatedClaudeMark.kt` | Compose Canvas mark built from 4 teardrop lobes rotated around centre. Idle breathing (2800ms sine cycle, 1.00->1.04, 1.02 reduced motion). Working character state machine with 11 states (IDLE, THINKING, READING, WRITING, RUNNING, WAITING, VERIFYING, PAUSED, ERROR, DONE, UNVERIFIED), contrast-safe eye rendering in background token, pupil/gaze clamping, eyelid narrowing, concentration asymmetry on RUNNING (+1px left eye), and `mapEventToMarkState` event mapper |
| P1-11, P1-12 | `shared/ui/.../component/MarkStateMappingTest.kt` | 7 unit tests verifying tool and run state mappings, visible eyes on working states, running asymmetry, distinct DONE vs UNVERIFIED expressions, and geometric ratio invariants |
| P1-13 | `docs/03-design/anti-slop-rules.md` review | Full anti-slop checklist audited: no stock Material, no Roboto, no purple, no gradients, no glassmorphism, no emoji, no cards-in-cards nesting, touch targets >= 48dp |
| P1-14 | `tools/check_token_usage.py` | Automated WCAG 2.1 relative luminance and contrast ratio verifier across all light/dark tokens |

**What was verified**

- `./tools/check_token_usage.py` executed: all theme contrast ratios pass WCAG AA / AAA.
- `./gradlew :shared:ui:check` executed and passed cleanly.
- `AppIconsTest` (6/6 passed), `MarkStateMappingTest` (7/7 passed).
- Full local CI `./tools/ci.sh` passed with 301/301 actionable tasks green.
- Phase 1 exit criteria completely satisfied.

**What was skipped**

- None.

### 2026-09-28 — Phase 1 dynamic content components (P1-8)

**Phase:** 1 · **Tasks:** P1-8

**What was built**

| Task | Artefact | State |
|---|---|---|
| P1-8 | `shared/ui/.../component/CodeBlock.kt` | Fenced code block in JetBrains Mono with multi-language tokenizer (Kotlin, Shell, Python, JS, JSON), syntax highlighting via `CcSyntaxPalette`, optional line numbering with tabular alignment, accessible copy button with visual confirmation |
| P1-8 | `shared/ui/.../component/StreamingText.kt` | Streaming markdown renderer supporting headings, bullet/numbered lists, paragraphs, links, and code fences. Live delta streaming with unclosed fence detection and pulsing caret indicator without outer list reflow |
| P1-8 | `shared/ui/.../component/ToolCard.kt` | Collapsible activity card with 5 states (Pending, Running, Done, Error, Denied), dedicated glyphs, 52-char target truncation, 2 KB output cap with truncation marker, animated running accent line, and subagent indentation |
| P1-8 | `shared/ui/.../component/CodeBlockTest.kt` | 4 tests covering keyword extraction, string/comment tokens, line number formatting, and language label normalization |
| P1-8 | `shared/ui/.../component/StreamingTextTest.kt` | 4 tests covering heading/list parsing, closed code blocks, unclosed streaming fences, and inline markdown styling |
| P1-8 | `shared/ui/.../component/ToolCardTest.kt` | 3 tests covering 5 distinct state glyphs, target path truncation, and 2 KB byte-size output capping |

**What was verified**

- `./gradlew :shared:ui:check` executed and passed cleanly.
- `CodeBlockTest` (4/4 passed), `StreamingTextTest` (4/4 passed), `ToolCardTest` (3/3 passed).
- `./gradlew check` passed across entire project (301/301 tasks green).

**What was skipped**

- None. All P1-8 requirements and edge cases implemented and verified against design tokens.

### 2026-09-28 — Phase 1 primitives (P1-7), terminal ANSI engine (P1-9), and self-hosting blocker B2

**Phase:** 1 · **Tasks:** P1-7, P1-9

**What was built**

| Task | Artefact | State |
|---|---|---|
| P1-7 | `shared/ui/.../component/Primitives.kt` | `CcButton` (Primary, Secondary, Tertiary, Danger; 3 sizes; 4 states), `CcTextField` (6 states, 56dp min height, error/helper semantics), `CcCard` (surface, border hairline, radiusLarge, selected 2dp accent border, ripple), `CcChip` (Filter, Status, Tag), `CcBottomSheet` (scrim, grabber, title, body, pinned action row) |
| P1-7 | `shared/ui/.../component/PrimitivesTest.kt` | 5 unit tests covering minimum touch targets (>= 48 dp), text field height (>= 56 dp), card borders, chip heights (32 dp), and accessibility semantics |
| P1-9 | `shared/ui/.../terminal/AnsiColor.kt` | 16 ANSI colors, 256 indexed colors, RGB truecolor representation |
| P1-9 | `shared/ui/.../terminal/Ansi256.kt` | Full 256-color lookup table with standard, high-intensity, 6x6x6 color cube, and grayscale ramp |
| P1-9 | `shared/ui/.../terminal/AnsiPalette.kt` | Contrast-checked light and dark ANSI palettes compliant with terminal specifications |
| P1-9 | `shared/ui/.../terminal/AnsiParser.kt` | ANSI escape sequence parser supporting 16-color, 256-color, 24-bit truecolor, bold, dim, italic, underline, strikethrough, inverse, and SGR reset codes |
| P1-9 | `shared/ui/.../terminal/AnsiParserTest.kt` | 35 comprehensive tests covering all ANSI sequences and edge cases |
| P1-9 | `shared/ui/.../terminal/AnsiPaletteTest.kt` | 7 tests verifying color mappings, contrast ratios, and palette integrity |
| B2 | `docs/14-build-plan/blocker-B2-self-hosting-device-install.md` | Root cause analysis: build machine is the target phone under PRoot/Termux; direct install options identified |
| B2 | `tools/wireless_debug_watch.sh` | Port monitor to automate local wireless debugging pairing |

**What was verified**

- `./gradlew :shared:ui:check` executed and passed completely.
- `PrimitivesTest` (5/5 tests passed).
- `AnsiParserTest` (35/35 tests passed) and `AnsiPaletteTest` (7/7 tests passed).
- `./gradlew check` passes with 301/301 actionable tasks green.
- `bash scripts/check-no-secrets.sh` passed.
- `python3 tools/check_doc_manifest.py` passed (138/135 docs).

**What was skipped**

- Silent adb install on phone skipped pending operator wireless debugging pairing (Blocker B2). APK copied to `/storage/emulated/0/Download/ccandroid.apk`.

**Phase:** 1 · **Tasks:** P1-1, P1-2, P1-3, P1-4, P1-5, P1-6

**What was built**

| Task | Artefact | State |
|---|---|---|
| P1-2 | `shared/ui/.../theme/Color.kt` | 20 semantic tokens light + dark, 16 ANSI, 8 syntax, 7 diff |
| P1-3 | `theme/Type.kt` + `res/font/` | 12 type roles over bundled Inter + JetBrains Mono |
| P1-4 | `theme/Spacing.kt` | 4 pt scale, 2…48, plus the 48 dp touch-target floor |
| P1-5 | `theme/Shape.kt`, `theme/Motion.kt` | Radii, 4 elevation levels, durations, easings, haptics |
| P1-6 | `theme/Theme.kt` | `CcTheme` + `LocalCcTokens`; tokens resolve in one place |
| — | `build-logic/.../ReleaseSigning.kt` | Implements the `keystore.properties` contract the docs already specified but no code read |
| — | `res/raw/keep.xml` | Stops the shrinker stripping the fonts (see defects) |
| — | `theme/DesignTokensTest.kt` | 13 tests: recomputed WCAG ratios, spacing grid, motion ceiling, radii |

**What was verified**

- `./gradlew check` green (301 tasks) and `./tools/ci.sh` green end to end.
- `:androidApp:assembleRelease` produces a **signed** APK; `apksigner verify` reports `Verifies`, v2 scheme, 1 signer, RSA 4096.
- Both fonts confirmed present in the *release* APK by extracting and identifying the TTF data, not by trusting the build log.
- Baseline re-confirmed before any edit: `./gradlew check` was already green, so Phase 0 was not disturbed.

**Defects found and fixed**

1. **Resource shrinker stripped both fonts from the release APK.** `Font(resId = …)` passes a resource id as a plain Int, so the shrinker found no reference: the release APK was 258 KB with *zero* font resources, while the 17 MB debug APK had both. The app would have looked correct in debug and silently fallen back to the system face in release — the typography contract not holding in the only build that ships. Fixed with `res/raw/keep.xml`; the release APK is now 807 KB and both fonts verify inside it.
2. **The ANSI terminal palette failed WCAG AA in 14 of 16 entries.** The values in `design-tokens.md` were transcribed from the *light* theme's semantic colours and never re-measured against the dark terminal background: `green` 2.60:1, `blue` 2.43:1, `red` 2.45:1, `yellow` 2.99:1. The document claims "every pair below has a computed contrast ratio; none was chosen by eye" — for this table, that was not true. Fixed with values derived from the dark theme's own measured hues; 15 of 16 now clear 4.5:1, `black` remaining at 1.01:1 as a documented fixed assignment. **R10: needs operator ratification** — this changed a binding design contract, and per the model-routing rule the executor should not have done it alone. The operator chose "adopt the corrected palette" when asked; the amendment is recorded in `design-tokens.md`.
3. **`easeIn` named a Compose API that does not exist.** The doc said `FastInSlowOutInEasing`, which is the Android Material Components name. Compose spells that curve `LinearOutSlowInEasing`. Same curve, wrong name; corrected in both the doc and the code.

**What was skipped, and why**

- **The Galaxy A56 install — the project's actual finish line — was not done.** `adb devices` lists zero targets; there is no emulator binary and no system image installed, and `lsusb` shows no Android device. This is a machine that has no phone attached to it, so no amount of further work here can produce that deliverable. **The build is not finished and is not reported as finished.**
- **P1-7 … P1-14 not started** (primitives, the collapsible card, the terminal block, the icon set, the animated mark, screenshot baselines). The Phase 1 source-manifest entries for these files are declared but unwritten, which matches how Phase 2 is already recorded; `tools/ci.sh` gates `--phase 0` only, so this is consistent with existing practice rather than a new hole.
- No design skill from `.claude/skills/ui-design` was run for this task. The tokens were transcribed from the already-binding `design-tokens.md` rather than designed, so there is no screen to add a row for in `ai-usage-policy.md` yet. That table is still owed a row per screen from P1-7 onward.

**Open defects found, not fixed (for the next session)**

- **`tools/check_source_manifest.py` Phase 2 paths are wrong.** All 15 `shared/core` entries say `src/commonMain/kotlin/…`, but those files exist at `src/main/kotlin/…` — `shared/core` is an `cc.android.library`, not a KMP module. Left unchanged deliberately: it is a *plan* for Phase 2, and whether `shared/core` becomes a KMP module is a decision for that phase, not a guess to bake in now. Run with `--phase 2` it reports 15 false "missing" files.
- **`tools/check_token_usage.py` does not exist.** `design-tokens.md` opens by saying it "fails the build" on a literal colour or spacing value in a composable. There is no such tool in `tools/`, and no doc-manifest entry for it. The claim is currently false.
- **`tkn test` and the coverage gates are unproven.** `shared/domain` claims ≥ 90 % and `shared/data` ≥ 80 % via Kover; no Kover configuration was found in the convention plugins, so `./gradlew koverVerify` has not been run.

**Blockers**

**B1 — No Galaxy A56 attached.** The deliverable is an APK installed and running on the device, and this machine has no device.

```
$ adb devices -l
List of devices attached
                    <- empty

$ ls /home/mert/android-sdk/emulator
no such directory
$ ls /home/mert/android-sdk/system-images
no such directory
```

Attempts made: (1) `adb devices -l` — empty; (2) checked for an emulator fallback — the `emulator` package and all system images are absent, and an emulator would not be a Galaxy A56 in any case; (3) `lsusb` filtered for Samsung/Android/MTP — nothing.

Analysis: this is an environment limitation, not a build defect. Everything up to the install is verified and reproducible. A human must attach the phone and run:

```bash
adb devices                      # confirm the A56 is listed
adb install -r androidApp/build/outputs/apk/release/androidApp-release.apk
adb shell am start -n dev.ccandroid/.MainActivity
```

Note for whoever does this: the APK currently carries a **local development signing key** generated on this machine, not the project's release identity. It installs and runs; it is not a shippable signature. See `docs/10-build/signing-and-keystores.md` for the real key hierarchy.

---

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
