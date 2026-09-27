# Data migrations

A migration strategy that survives contact with a user who upgrades mid-task.

## Principles

1. **Never lose a run.** A user upgrading from a working version to a broken one is the worst outcome available to us. Migrations are tested against a database containing real-shaped data, not an empty one.
2. **Never destroy history without saying so.** If a migration must drop data, the user is told what, in the update notes and in the app, before the upgrade applies.
3. **Forward-only.** We never ship a down migration. Rolling back an app is a user's problem to solve with a reinstall, and a reinstall is clean by design.
4. **Additive first.** New columns are nullable or defaulted. Destructive work is a separate, later migration.
5. **Tested at every version.** A migration test walks the full chain from version 1 to current, one step at a time, verifying data after each step.

## Versioning

`AppDatabase.version` is the single source. Migrations live in `shared/data/db/Migrations.kt`, one object per version, composed in order:

```kotlin
val MIGRATIONS: Array<AutoMigrationSpec> = arrayOf(
    AutoMigrationSpec(from = 1, to = 2, spec = Migration_1_2),
    AutoMigrationSpec(from = 2, to = 3, spec = Migration_2_3),
    // ...
)
```

`.fallbackToDestructiveMigration()` is **banned**. It appears nowhere in the codebase, and a test greps for it. An unhandled version gap is a crash on upgrade, which is loud, survivable, and reported — silently erasing a user's project history is neither.

## The current chain

| From → To | Change | Data safety |
|---|---|---|
| 1 → 2 | Add `Project.defaultBranch`, nullable | Safe. No read path depends on it; the UI falls back to "unknown". |
| 2 → 3 | Add `Run` and `VerificationRun` | Safe. New tables. |
| 3 → 4 | Add `CostRecord` with the token columns | Safe. Nullable, then defaulted. |
| 4 → 5 | Add `RemoteTarget` | Safe. New table. |
| 5 → 6 | Add `SkillInstall` with a unique index on `(name, scope, projectId)` | Needs care: existing duplicate skills. The migration dedupes, keeping the most recently installed, and records the removal in the log. |
| 6 → 7 | Rename `ToolInvocation.title` to `titleDe` / `titleEn` | Mechanical rename, plus a backfill of the English label from a lookup table. |
| 7 → 8 | Add `Message.isRedacted` | Safe, defaults to false. |

The exact chain is written as the schema evolves; this table is the shape it takes. The rule that matters is that every step is additive or mechanical. When a destructive step becomes unavoidable, it gets its own document and an entry in the release notes.

## Writing a migration

Checklist, in order:

1. **Does this need a new column, or can the data be derived?** Derive. A computed value should not be stored if it can be computed, and storing it is what makes migrations necessary.
2. **Is the new column nullable or defaulted?** If neither, the migration needs a backfill before the constraint. Write the backfill as part of the migration, not as a lazy fix-up at read time.
3. **Does a unique index now apply to data that may already violate it?** If yes, dedupe in the migration, deterministically, and log what was removed.
4. **Does an enum gain a new value?** Enums are stored as strings, so old rows keep their old value. The reader must handle unknown values. This is why every enum read goes through a `fromStorage()` that returns null for an unrecognised value rather than throwing.
5. **Does a foreign key now point somewhere new?** Add nullable, backfill, then add the constraint. SQLite cannot add a constraint to an existing table.

## Unknown enum values

The rule from point 4, stated as a policy:

> An enum stored in the database may contain a value this version of the app does not know. Reading it must return null or a documented default, never throw, and never crash a list.

The test for this: the migration test chain includes a fixture row with a deliberately future enum value, and every screen that could render it is exercised. This is not hypothetical — it is exactly what happens after every app update that ships a new enum value, on every device that skipped an intermediate version.

## Migration testing

| Test | What it covers |
|---|---|
| `MigrationChainTest` | Walks version 1 → current, one step at a time, asserting data after every step |
| `MigrationFixtureTest` | A representative database from each released version, migrated forward |
| `UnknownEnumTest` | Future enum values in every table, read by every repository, no crash |
| `DedupeTest` | The unique-index migration on a database full of duplicates |
| `LargeDatabaseTest` | 10,000 turns, to catch an accidentally O(n²) migration |
| `InterruptedMigrationTest` | Kill the process mid-migration; the next launch recovers. Room's transaction guarantees this, and the test proves it rather than assuming it. |

**The fixture rule:** a database fixture is captured from a real released version, scrubbed of anything personal, and committed. When we ship a version, we capture a fixture for the next migration test. A migration without a fixture from its real predecessor is not finished.

## Interaction with a running task

An app update kills the process. A run in progress at that moment is `INTERRUPTED`, and the state survives because it was written to the database on every event, not at the end of the run.

On upgrade:

1. The database migrates before the UI appears.
2. Any run in a non-terminal state is marked `INTERRUPTED` with a reason of `APP_UPDATED`.
3. The user is offered a resume, and the resume uses the stored session id.
4. Work that had not been committed is still in the working tree, and `wip/` is untouched, because we never delete anything.

This is the reason the run state is written eagerly. A cheap write per event is worth more than a fast one per run.

## Interaction with the engine

The engine has its own on-disk session files. Those are not ours to migrate. If a CLI version changes its session format:

- The app stores the version alongside the session id.
- A session from a different version is not resumed; the user is told, and offered a new conversation that carries the previous text as context, not as a session.
- The old session file is left on disk, not deleted.

## What we never do

| Never | Why |
|---|---|
| `fallbackToDestructiveMigration` | Silent data loss on an unknown version |
| A migration that runs work | A migration must be a data transformation. Anything slow belongs in a background job after the upgrade. |
| A migration that depends on app logic | It runs before the DI graph exists. Migrations use only Room APIs and plain Kotlin. |
| A migration that writes to a network | No. The database migration is offline and must work on a plane. |
| A migration bundled with a feature | Schema changes and features ship together only when inseparable. Otherwise the migration ships first, one release early, so a rollback never hits a database the old code cannot read. |
