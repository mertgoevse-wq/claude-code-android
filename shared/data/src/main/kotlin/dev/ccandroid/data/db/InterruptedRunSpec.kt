package dev.ccandroid.data.db

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import dev.ccandroid.core.IdGenerator
import dev.ccandroid.core.TimeProvider
import dev.ccandroid.domain.RunState
import dev.ccandroid.domain.SessionLogCategory
import dev.ccandroid.domain.SessionLogSeverity
import dev.ccandroid.domain.isTerminal

/**
 * Runs after the migration succeeds, per `docs/02-architecture/data-migrations.md`
 * §Interaction with a running task.
 *
 * An app update kills the process. Whatever the engine was doing stops with it,
 * so every run caught mid-flight is marked [RunState.INTERRUPTED] and a session
 * log entry records why. A terminal run is left alone: it already ended, and
 * rewriting a finished run would be a lie about what happened.
 *
 * The reason is written down rather than left to be inferred. An interrupted run
 * with no recorded reason is indistinguishable from a crash, and the user is
 * asked to resume it.
 */
public class InterruptedRunSpec(
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : AutoMigrationSpec {

    override fun onPostMigrate(connection: SQLiteConnection) {
        val stale = readInFlightRuns(connection)
        if (stale.isEmpty()) return

        val now = time.currentTimeMillis()
        connection.prepare("UPDATE runs SET state = ?, endedAt = ? WHERE id = ?").use { update ->
            connection.prepare(
                "INSERT INTO session_log_entries " +
                    "(id, timestamp, runId, projectId, conversationId, category, severity, message) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            ).use { insert ->
                for (run in stale) {
                    // reset before rebinding: a statement that has run to
                    // completion is not reusable until it is rewound, and the
                    // second run in a batch fails loudly without this.
                    update.reset()
                    update.clearBindings()
                    update.bindText(1, RunState.INTERRUPTED.name)
                    update.bindLong(2, now)
                    update.bindText(3, run.id)
                    update.step()

                    insert.reset()
                    insert.clearBindings()
                    insert.bindText(1, ids.newId("log"))
                    insert.bindLong(2, now)
                    insert.bindText(3, run.id)
                    insert.bindText(4, run.projectId)
                    insert.bindText(5, run.conversationId)
                    insert.bindText(6, SessionLogCategory.STATE.name)
                    insert.bindText(7, SessionLogSeverity.WARN.name)
                    insert.bindText(8, REASON)
                    insert.step()
                }
            }
        }
    }

    private fun readInFlightRuns(db: SQLiteConnection): List<StaleRun> {
        val runs = mutableListOf<StaleRun>()
        // The state list is a compile-time constant from the domain enum, never
        // a value read from the database, so there is nothing to bind or escape.
        val placeholders = IN_FLIGHT.joinToString(", ") { "'${it.name}'" }
        db.prepare("SELECT id, projectId, conversationId FROM runs WHERE state IN ($placeholders)")
            .use { statement ->
                while (statement.step()) {
                    runs += StaleRun(
                        id = statement.getText(0),
                        projectId = statement.getText(1),
                        conversationId = statement.getText(2),
                    )
                }
            }
        return runs
    }

    private data class StaleRun(
        val id: String,
        val projectId: String,
        val conversationId: String,
    )

    private companion object {
        const val REASON = "Run interrupted by an app update."

        /**
         * Everything that has not ended yet. A run the user cancelled and a run
         * that failed are finished; a run that is merely queued is not. The
         * terminal set is the domain's, so this cannot drift from it.
         */
        val IN_FLIGHT: List<RunState> = RunState.entries.filterNot { it.isTerminal }
    }
}
