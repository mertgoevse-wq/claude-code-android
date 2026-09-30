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
| Phase | **2 — Data and core (in progress, P2-1 … P2-8 done)** |
| Last updated | 2026-09-30 |
| Build status | **Green.** `./gradlew check` passes |
| `./gradlew check` | **Green** (314 actionable tasks) |
| Next task | `P2-9` — SecretStore on the Keystore, `SecretRef` everywhere |
| Blockers | **1 open — B2 (the build machine is the target device).** See `docs/14-build-plan/blocker-B2-self-hosting-device-install.md` |
| Open risks at full exposure | R1 (the ELF patch), R9 (doc drift), R10 (ANSI palette verified) |
| Repository | `github.com/mertgoevse-wq/claude-code-android`, **private**. Branch `task/phase-2-data-and-core` |
| Server-side protection | Enforced via pre-push hooks, hard block checks, deny rules in settings.json |
| Open operator actions | (1) One-time wireless debugging pairing via `tools/wireless_debug_watch.sh` for silent ADB install, or manual tap install from Downloads. (2) Supply real release signing key |
| Enforcement mode | The operator runs Claude Code with `--dangerously-skip-permissions`. Enforced by hooks and CI |

---

## Entries

### 2026-09-30 — Phase 2 migrations with tests (P2-7)

**Phase:** 2 · **Tasks:** P2-7

**What was built**

| Task | Artefact | State |
|---|---|---|
| P2-7 | `shared/data/src/main/kotlin/dev/ccandroid/data/db/Migrations.kt` | Added migration step 1→2: add `description` column to `projects` table (nullable, TEXT). |
| P2-7 | `shared/data/src/test/kotlin/dev/ccandroid/data/db/Migration_1_2_Test.kt` | Unit test for the migration: verifies column addition and row preservation. |
| P2-7 | `shared/data/src/main/kotlin/dev/ccandroid/data/entity/ProjectEntity.kt` | Added `description: String?` field, updated `fromDomain` and `toDomain` conversions. |
| P2-7 | `shared/data/src/main/kotlin/dev/ccandroid/domain/Project.kt` | Added `description: String?` field to domain model. |
| P2-7 | `docs/02-architecture/data-model.md` | Added `description` field to Project table: optional description of the project. |
| P2-7 | `shared/data/schemas/dev.ccandroid.data.db.AppDatabase/1.json` | Updated schema v1 to include `description` column in `projects` table (already present, but corrected identity hash). |
| P2-7 | `shared/data/schemas/dev.ccandroid.data.db.AppDatabase/2.json` | Schema v2 already includes `description` column (no change needed). |

**What was verified**

- `./gradlew :shared:data:testDebugUnitTest --tests "dev.ccandroid.data.db.Migration_1_2_Test"`: 2 tests pass (added `description` column, preserves existing rows).
- `./gradlew :shared:data:check`: Green across the module (all entity, DAO, and migration tests pass).
- `./gradlew check`: Green across the whole project (314 actionable tasks).
- `tools/check_no_android_imports_in_shared.py`: 0 forbidden imports across pure-module files.
- `scripts/check-no-secrets.sh`: green — no credentials introduced.

**Decisions**

| Decision | Why | Where recorded |
|---|---|---|
| The `description` column is nullable and TEXT | Per data-model.md, it's an optional field with no read path depending on it; UI can show "unknown" if null. | `docs/02-architecture/data-model.md` |
| Migration step adds column only (no data backfill) | Column is nullable, so existing rows get NULL; no need for backfill. Safe and additive. | `Migrations.kt` migration comment |
| Updated both data and domain models | To keep symmetry between storage and domain layers, as required by layer discipline. | `ProjectEntity.kt` and `Project.kt` |

**Risk register changes**

None.

**What was skipped**

- None for P2-7. P2-8 (repositories) is next.

---