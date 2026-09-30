package dev.ccandroid.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules that make a migration chain trustworthy, per
 * `docs/02-architecture/data-migrations.md`.
 *
 * A chain is only as good as the moment someone adds a step to it and gets one
 * thing wrong, so every rule is tested against a chain that breaks it on
 * purpose. The chain itself is currently empty — version 1 is the first release —
 * so the tests that matter here are the ones about what a future step must not
 * be allowed to do.
 */
class MigrationChainTest {

    /** A step that moves the schema from [from] to [to] and does nothing else. */
    private fun step(from: Int, to: Int) = Step(
        from = from,
        to = to,
        migration = object : Migration(from, to) {
            override fun migrate(db: SQLiteConnection) = Unit
        },
    )

    @Test
    fun `an empty chain is valid and ends at the first version`() {
        val chain = MigrationChain(emptyList()).requireContinuousFromFirstVersion()

        assertEquals(Migrations.FIRST_VERSION, chain.lastVersion)
    }

    @Test
    fun `the shipped chain covers the version the database declares`() {
        val chain = Migrations.chain(currentVersion = Migrations.FIRST_VERSION)

        assertEquals(Migrations.FIRST_VERSION, chain.lastVersion)
        assertEquals(Migrations.roomMigrations().size, Migrations.STEPS.size)
    }

    @Test
    fun `a version with no migration is refused rather than erased`() {
        val error = runCatching { Migrations.chain(currentVersion = 2) }.exceptionOrNull()

        assertTrue(
            "A schema version the chain cannot reach must be a loud failure, was: $error",
            error is IllegalStateException,
        )
        assertTrue(
            "The message must name both versions, was: ${error?.message}",
            error?.message?.contains("2") == true && error.message?.contains("1") == true,
        )
    }

    @Test
    fun `a gap in the chain is refused`() {
        val chain = MigrationChain(listOf(step(1, 2), step(3, 4)))

        val error = runCatching { chain.requireContinuousFromFirstVersion() }.exceptionOrNull()

        assertTrue("A gap must be a loud failure, was: $error", error is IllegalStateException)
        assertTrue(
            "The message must point at the missing step, was: ${error?.message}",
            error?.message?.contains("expected a step from 2") == true,
        )
    }

    @Test
    fun `a chain that does not start at the first version is refused`() {
        val chain = MigrationChain(listOf(step(2, 3)))

        val error = runCatching { chain.requireContinuousFromFirstVersion() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
    }

    @Test
    fun `a step that does not move forward is refused`() {
        val chain = MigrationChain(listOf(step(1, 1), step(1, 2)))

        val error = runCatching { chain.requireContinuousFromFirstVersion() }.exceptionOrNull()

        assertTrue(
            "A step from a version to itself is a mistake, was: $error",
            error is IllegalStateException,
        )
        assertTrue(error?.message?.contains("does not move forward") == true)
    }

    @Test
    fun `a step arriving out of order is refused`() {
        val chain = MigrationChain(listOf(step(2, 3), step(1, 2)))

        val error = runCatching { chain.requireContinuousFromFirstVersion() }.exceptionOrNull()

        assertTrue(
            "Room applies steps in list order, so order is the chain, was: $error",
            error is IllegalStateException,
        )
    }

    @Test
    fun `a continuous chain walks from the first version to the end`() {
        val chain = MigrationChain(listOf(step(1, 2), step(2, 3), step(3, 4)))
            .requireContinuousFromFirstVersion()

        assertEquals(4, chain.lastVersion)
        chain.requireReachable(from = 1, to = 4)
        chain.requireReachable(from = 2, to = 3)
    }

    @Test
    fun `reaching a version the chain never passes is refused`() {
        val chain = MigrationChain(listOf(step(1, 2)))

        val error = runCatching { chain.requireReachable(from = 1, to = 3) }.exceptionOrNull()

        assertTrue(
            "Silently landing on a different schema than the app expects is the bug, was: $error",
            error is IllegalStateException,
        )
    }

    @Test
    fun `a downgrade is refused because down migrations do not exist`() {
        val chain = MigrationChain(listOf(step(1, 2)))

        val error = runCatching { chain.requireReachable(from = 2, to = 1) }.exceptionOrNull()

        assertTrue(
            "A rollback is a reinstall, not a migration, was: $error",
            error is IllegalStateException,
        )
        assertTrue(error?.message?.contains("reinstall") == true)
    }

    @Test
    fun `a database already at the target version needs no step`() {
        MigrationChain(listOf(step(1, 2))).requireReachable(from = 2, to = 2)
    }
}
