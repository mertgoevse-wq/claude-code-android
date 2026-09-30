# Data migrations

A migration strategy that survives contact with a user who upgrades mid-task.

## Principles

1. **Never lose a run.** A user upgrading from a working version to a broken one is the worst outcome available to us. Migrations are tested against a database containing real-shaped data, not an empty one.
2. **Never destroy history without saying so.** If a migration must drop data, the user is told what, in the update notes and in the app, before the upgrade applies.
3. **Forward-only.** We never ship a down migration. Rolling back an app is a user's problem to solve with a reinstall, and a reinstall is clean by design.
4. **Additive first.** New columns are nullable or defaulted. Destructive work is a separate, later migration.
5. **Tested at every version.** A migration test walks the full chain from version 1 to current, one step at a time, verifying data after each step.

## Versioning

`AppDatabase.version` is the single source. Migrations live in `shared/data/db/Migrations.kt`, one `Step` per version, composed in order:

```kotlin
val STEPS: List<Step> = listOf(
    Step(from = 1, to = 2, migration = Migration_1_2),
    Step(from = 2, to = 3, migration = Migration_2_3),
    // ...
)
```

A step carries its own `from` and `to` rather than reading them back off the
`Migration`, whose version accessors are internal to Room and unreadable from
Kotlin. The two numbers are the whole point of a step, so they live where they can
be checked.

`MigrationChain.requireContinuousFromFirstVersion()` fails unless the steps run
back to back from version 1, and `Migrations.chain(AppDatabase.version)` fails
unless the chain ends at the version the database declares. A schema version with
no step is therefore a loud failure at startup, not a silent erase.

Work that has to happen *after* a step succeeds — marking runs interrupted by the
update, for example — is an `AutoMigrationSpec` registered with
`addAutoMigrationSpec`, not a migration. A migration is a data transformation; the
spec is a callback.

`.fallbackToDestructiveMigration()` is **banned**. It appears nowhere in the code,
and `MigrationFixtureTest` greps every `src/main/kotlin` for the call, ignoring
comments so the documents and the KDoc can still say what is banned and why. An
unhandled version gap is a crash on upgrade, which is loud, survivable, and
reported — silently erasing a user's project history is neither.

## The chain as declared

The chain is currently **empty**: version 1 is the first released schema, so
there is nothing to migrate from. It is declared and validated anyway, because a
chain that first appears when it is needed is a chain that has never been
validated.

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

This is not hypothetical — it is exactly what happens after every app update that
ships a new enum value, on every device that skipped an intermediate version.

The fallback is chosen by one rule, and only one: **the fallback is never the
value that proceeds. It is the value that asks.** An unrecognised value is a
reason to slow down and ask, never a reason to assume permission, a pass, or a
decision the user owns. The full table, one row per enum, is the `fromStorage`
fallback list in `shared/data/.../converter/TypeConverters.kt`; `UnknownEnumTest`
asserts every row.

| Enum | Fallback | Why this one |
|---|---|---|
| `AutonomyLevel` | `ASK_EVERYTHING` | A level the app cannot read must ask, not act |
| `PermissionMode` | `DEFAULT` | Never a bypass, whatever the stored value claimed |
| `DiffDecision` | `PENDING` | The per-hunk decision is the user's, not the app's |
| `VerificationState` | `UNVERIFIED` | A run this version cannot interpret must not read as passing |
| `RunState` | `INTERRUPTED` | The user is offered a resume instead of a fabricated continuation |
| `OffloadPolicy` | `NEVER` | Do not move a run to another machine on an unread setting |
| `SkillSourceKind` | `BUILTIN` | Never claim a skill came from GitHub when the row says otherwise |
| `MessageRole` | `SYSTEM` | Do not attribute text to the user or to the agent |
| `MessagePartKind` | `TEXT` | Never render an unknown part as a tool card, which claims an action happened |
| `TestParserType` | `GENERIC` | Show the raw output rather than skip parsing and hide it |
| `NotificationChannel` | `RUNNER` | The least-claiming channel; a wrong channel is a wrong notification |
| `ToolInvocationStatus` | `PENDING` | Re-evaluate rather than assume a tool finished |
| `PlanStepState` | `SKIPPED` | Never claim a step completed on the app's initiative |
| `FileChangeType` | `MODIFIED` | The neutral glyph, when the real change type is unknown |
| `ProviderKind` | `CUSTOM` | An unknown provider is a custom endpoint, which is what the row must be treated as |
| `RemoteTargetKind` | `SSH` | The kind the app can actually act on |
| `RemoteTargetStatus` | `UNKNOWN` | Never assert reachability the app could not read |
| `ProjectKind` | `LOCAL` | The conservative kind; nothing leaves the device on an unread value |
| `SessionLogCategory` | `SYSTEM` | A log line is always a system line |
| `SessionLogSeverity` | `INFO` | Do not raise an alarm on a value the app cannot read |
| `SkillInstallScope` | `GLOBAL` | The scope the app can reason about without knowing better |

`RunState.isTerminal` in the domain is the one place that decides which runs have
ended, so the post-migration spec and any query that needs the answer read the
same set.

## Migration testing

| Test | What it covers | State |
|---|---|---|
| `MigrationChainTest` | The chain rules: contiguity from version 1, forward-only, a version the chain cannot reach is refused, a step that does not move forward is refused | Done |
| `MigrationFixtureTest` | A database built from the committed schema export, a real additive step applied to it, an interrupted step rolled back, 10,000 rows without a quadratic step, and the `fallbackToDestructiveMigration` ban | Done |
| `UnknownEnumTest` | A row written by a later version, read back through a DAO, plus every converter's documented fallback | Done |
| `InterruptedRunSpecTest` | The post-migration spec: in-flight runs become `INTERRUPTED` with a recorded reason, terminal runs are untouched, a second pass changes nothing | Done |
| `DedupeTest` | The unique-index migration on a database full of duplicates | Waiting on a step that adds a unique index to existing data |
| Screen-level enum rendering | A future enum value rendered in each screen that can show one | Phase 4, with the screens |

**The fixture rule:** a database fixture is captured from a real released version,
scrubbed of anything personal, and committed. When we ship a version, we capture
a fixture for the next migration test. A migration without a fixture from its real
predecessor is not finished.

The fixture is the exported schema JSON, not a database built from the current
entities. Building the "old" database out of today's code would make every
migration test pass against a broken migration, because the broken state would be
the only state it had ever seen.

Fixture files are named with a per-run id. A test that dies halfway through
building its fixture leaves a half-built file behind, and a fixed name would hand
that file to the next run. They accumulate under `build/`, which is gitignored,
and nothing is deleted — deleting is one of the five things this project never
does.

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
