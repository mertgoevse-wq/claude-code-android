package dev.ccandroid.data.db

import androidx.room.migration.Migration

/**
 * The migration chain, per `docs/02-architecture/data-migrations.md`.
 *
 * Version 1 is the first released schema, so the chain is empty. It is declared
 * anyway, because a chain that first appears when it is needed is a chain that
 * has never been validated.
 *
 * Adding a step is three edits in this order:
 *
 * 1. bump `AppDatabase.version`,
 * 2. commit the schema JSON that Room exports for the new version,
 * 3. add the step here.
 *
 * [chain] refuses a database whose version the chain does not reach, so step 3
 * cannot be forgotten: an unreached version is a crash on upgrade, which is
 * loud, survivable, and reportable. It is not a silent erase of the user's
 * projects, which is what `fallbackToDestructiveMigration` would buy.
 */
public object Migrations {

    /** The first released schema. Nothing predates it, so nothing migrates from it. */
    public const val FIRST_VERSION: Int = 1

    /** Every step, ascending. Empty while the schema is still at [FIRST_VERSION]. */
    public val STEPS: List<Step> = emptyList()

    /**
     * The validated chain for a database that declares [currentVersion].
     *
     * @throws IllegalStateException if a version in the middle of the chain has
     *   no step, or if the chain does not end at [currentVersion].
     */
    public fun chain(currentVersion: Int): MigrationChain {
        val chain = MigrationChain(STEPS)
        chain.requireContinuousFromFirstVersion()
        check(currentVersion == chain.lastVersion) {
            "AppDatabase.version is $currentVersion but the chain ends at ${chain.lastVersion}. " +
                "A schema version with no migration erases the user's data on upgrade."
        }
        return chain
    }

    /** Forwards to `RoomDatabase.Builder.addMigrations`. */
    public fun roomMigrations(): Array<Migration> = STEPS.map { it.migration }.toTypedArray()
}

/**
 * One version step.
 *
 * The versions are carried here rather than read back off the [Migration],
 * whose accessors are internal to Room and unreadable from Kotlin. The two
 * numbers are the whole point of the step, so they are declared where they can
 * be checked.
 */
public data class Step(
    val from: Int,
    val to: Int,
    val migration: Migration,
)

/**
 * An ordered, gap-free set of steps from [Migrations.FIRST_VERSION] forward.
 * There are no down migrations, so the chain is a line, not a graph.
 */
public class MigrationChain(public val steps: List<Step>) {

    /** The version this chain leaves the database at. */
    public val lastVersion: Int
        get() = steps.lastOrNull()?.to ?: Migrations.FIRST_VERSION

    /**
     * Fails unless the steps run back to back from the first released version.
     * A gap is the failure this whole file exists to prevent.
     */
    public fun requireContinuousFromFirstVersion(): MigrationChain {
        var expected = Migrations.FIRST_VERSION
        for (step in steps) {
            check(step.to > step.from) {
                "Migration ${step.from}..${step.to} does not move forward."
            }
            check(step.from == expected) {
                "Migration chain has a gap: expected a step from $expected, " +
                    "found ${step.from}..${step.to}."
            }
            expected = step.to
        }
        return this
    }

    /** Fails unless a database at [from] can reach [to] by walking the chain. */
    public fun requireReachable(from: Int, to: Int) {
        if (from == to) return
        check(from < to) {
            "Down migrations do not exist. Asked for $from -> $to; a rollback is a reinstall."
        }
        var version = from
        for (step in steps) {
            if (step.from == version) {
                version = step.to
                if (version == to) return
            }
        }
        check(false) {
            "No migration path from version $from to $to. The chain ends at $lastVersion."
        }
    }
}
