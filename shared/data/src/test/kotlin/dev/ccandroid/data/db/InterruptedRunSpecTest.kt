package dev.ccandroid.data.db

import androidx.sqlite.execSQL
import dev.ccandroid.core.IdGenerator
import dev.ccandroid.core.TimeProvider
import dev.ccandroid.data.db.SchemaFixtures.open
import dev.ccandroid.data.db.SchemaFixtures.queryInt
import dev.ccandroid.data.db.SchemaFixtures.queryText
import dev.ccandroid.domain.RunState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * What an app update does to a run that was in flight, per
 * `docs/02-architecture/data-migrations.md` §Interaction with a running task.
 *
 * The update kills the process, so whatever the engine was doing stops with it.
 * The run is marked interrupted and the reason is written to the session log,
 * because a run the user is asked to resume has to say why it stopped.
 */
class InterruptedRunSpecTest {

    private val fixedTime = 1_700_000_000_000L

    private val time = object : TimeProvider {
        override fun currentTimeMillis(): Long = fixedTime
        override fun nanoTime(): Long = 0L
    }

    private val ids = object : IdGenerator {
        var counter = 0
        override fun newId(prefix: String): String = "${prefix}_log_$counter".also { counter++ }
    }

    private val spec = InterruptedRunSpec(time, ids)

    @Test
    fun `a run caught mid-flight becomes interrupted, with the reason on record`() {
        val file = database("spec-in-flight", RunState.RUNNING)

        migrate(file)

        open(file).use { connection ->
            assertEquals(
                RunState.INTERRUPTED.name,
                queryText(connection, "SELECT state FROM runs WHERE id = 'run_01'"),
            )
            assertEquals(
                "The user is asked to resume this run, so it needs an end time.",
                fixedTime.toString(),
                queryText(connection, "SELECT endedAt FROM runs WHERE id = 'run_01'"),
            )
            assertEquals(
                "WARN",
                queryText(connection, "SELECT severity FROM session_log_entries WHERE runId = 'run_01'"),
            )
            assertEquals(
                "STATE",
                queryText(connection, "SELECT category FROM session_log_entries WHERE runId = 'run_01'"),
            )
        }
    }

    @Test
    fun `a run that had already finished is left exactly as it was`() {
        val file = database("spec-finished", RunState.DONE, RunState.FAILED, RunState.CANCELLED)

        migrate(file)

        open(file).use { connection ->
            assertEquals("DONE", queryText(connection, "SELECT state FROM runs WHERE id = 'run_01'"))
            assertEquals("FAILED", queryText(connection, "SELECT state FROM runs WHERE id = 'run_02'"))
            assertEquals("CANCELLED", queryText(connection, "SELECT state FROM runs WHERE id = 'run_03'"))
            assertEquals(
                "Rewriting a finished run would be a lie about what happened.",
                0,
                queryInt(connection, "SELECT COUNT(*) FROM session_log_entries"),
            )
            assertNull(
                "A finished run does not gain an end time it never had.",
                queryText(connection, "SELECT endedAt FROM runs WHERE id = 'run_01'"),
            )
        }
    }

    @Test
    fun `a run that was merely queued is in flight and is interrupted too`() {
        val file = database("spec-queued", RunState.CREATED, RunState.AWAITING_PERMISSION)

        migrate(file)

        open(file).use { connection ->
            assertEquals(RunState.INTERRUPTED.name, queryText(connection, "SELECT state FROM runs WHERE id = 'run_01'"))
            assertEquals(RunState.INTERRUPTED.name, queryText(connection, "SELECT state FROM runs WHERE id = 'run_02'"))
        }
    }

    @Test
    fun `a run moved to another machine is not resumed here`() {
        val file = database("spec-offloaded", RunState.OFFLOADED)

        migrate(file)

        open(file).use { connection ->
            assertEquals(
                "An offloaded run is running elsewhere; marking it interrupted here would be a lie.",
                RunState.OFFLOADED.name,
                queryText(connection, "SELECT state FROM runs WHERE id = 'run_01'"),
            )
        }
    }

    @Test
    fun `no runs means nothing is written`() {
        val file = database("spec-empty")

        migrate(file)

        open(file).use { connection ->
            assertEquals(0, queryInt(connection, "SELECT COUNT(*) FROM session_log_entries"))
        }
    }

    @Test
    fun `running the spec twice does not record the same interruption twice`() {
        val file = database("spec-twice", RunState.RUNNING)

        migrate(file)
        migrate(file)

        open(file).use { connection ->
            assertEquals(
                "A second pass finds the run already interrupted and must leave it alone.",
                1,
                queryInt(connection, "SELECT COUNT(*) FROM session_log_entries"),
            )
        }
    }

    private fun migrate(file: File) {
        open(file).use { spec.onPostMigrate(it) }
    }

    /** A real database from the shipped schema, holding one run per given state. */
    private fun database(name: String, vararg states: RunState): File {
        val file = SchemaFixtures.createDatabaseAtVersion(1, name)
        open(file).use { connection ->
            connection.execSQL("BEGIN")
            states.forEachIndexed { index, state ->
                connection.execSQL(
                    "INSERT INTO runs (id, projectId, conversationId, turnId, backendId, backendProfile, " +
                        "state, taskText, attemptCount, maxAttempts, isOffloaded, startedAt) VALUES " +
                        "('run_0${index + 1}', 'proj_01', 'conv_01', 'turn_01', 'backend_01', 'proot', " +
                        "'${state.name}', 'Aufgabe', 1, 10, 0, 1000)",
                )
            }
            connection.execSQL("COMMIT")
        }
        return file
    }
}
