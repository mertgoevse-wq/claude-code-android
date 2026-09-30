package dev.ccandroid.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import dev.ccandroid.data.db.SchemaFixtures.columnNames
import dev.ccandroid.data.db.SchemaFixtures.createDatabaseAtVersion
import dev.ccandroid.data.db.SchemaFixtures.declaredTableNames
import dev.ccandroid.data.db.SchemaFixtures.indexStatements
import dev.ccandroid.data.db.SchemaFixtures.open
import dev.ccandroid.data.db.SchemaFixtures.queryInt
import dev.ccandroid.data.db.SchemaFixtures.queryText
import dev.ccandroid.data.db.SchemaFixtures.schemaVersion
import dev.ccandroid.data.db.SchemaFixtures.tableNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Migrations against a database built from the schema a released version shipped,
 * per `docs/02-architecture/data-migrations.md` §Migration testing.
 *
 * The fixture is the exported schema JSON, which is a real artifact from the
 * real predecessor. Building the "old" database out of the current entities
 * would make every migration test pass against a broken migration, because the
 * broken state would be the only state it had ever seen.
 */
class MigrationFixtureTest {

    @Test
    fun `the fixture is the first released version, the only predecessor that exists`() {
        assertEquals(1, schemaVersion(1))
    }

    @Test
    fun `every declared table is really created by the committed schema`() {
        val file = createDatabaseAtVersion(1, "fixture-tables")

        open(file).use { connection ->
            val missing = declaredTableNames(1).toSet() - tableNames(connection)
            assertTrue("The schema declares tables its own SQL never creates: $missing", missing.isEmpty())
        }
    }

    @Test
    fun `every declared index is really created by the committed schema`() {
        val file = createDatabaseAtVersion(1, "fixture-indices")

        open(file).use { connection ->
            val actual = queryInt(
                connection,
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name LIKE 'index_%'",
            )
            assertEquals(
                "An index the schema declares and SQLite never created.",
                indexStatements(1).size,
                actual,
            )
        }
    }

    @Test
    fun `destructive migration fallback appears nowhere in the source`() {
        val offenders = sourceRoots()
            .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.toList() }
            .filter { file -> stripComments(file.readText()).contains(BANNED) }
            .map { it.path }

        assertTrue(
            "$BANNED silently erases the user's projects on a version the chain does not " +
                "reach. An unhandled gap is loud, survivable, and reportable; a silent " +
                "erase is neither. Found in: $offenders",
            offenders.isEmpty(),
        )
    }

    @Test
    fun `an additive step preserves the rows it does not touch`() {
        val file = createDatabaseAtVersion(1, "step-preserves-rows")
        seedProjects(file)

        applyStep(file, addLastOpenedAtAndBackfill)

        open(file).use { connection ->
            assertEquals(
                "Every project must survive a step that only adds a column.",
                2,
                queryInt(connection, "SELECT COUNT(*) FROM projects"),
            )
            assertEquals(
                "The backfilled value has to come from the row, not from a constant.",
                "1000",
                queryText(connection, "SELECT lastOpenedAt FROM projects WHERE id = 'proj_01'"),
            )
            assertEquals("Werk", queryText(connection, "SELECT name FROM projects WHERE id = 'proj_01'"))
        }
    }

    @Test
    fun `a step that dies mid-way leaves the previous schema intact`() {
        val file = createDatabaseAtVersion(1, "step-interrupted")
        seedProjects(file)

        val error = runCatching {
            open(file).use { connection ->
                // Room runs a migration inside a transaction. Mirroring that here is
                // the point: the test proves the guarantee, it does not assume it.
                connection.execSQL("BEGIN")
                try {
                    interruptedStep.migrate(connection)
                } finally {
                    connection.execSQL("ROLLBACK")
                }
            }
        }.exceptionOrNull()

        assertEquals("process died mid-migration", error?.message)
        open(file).use { connection ->
            assertTrue(
                "A half-applied step must not survive; the next launch has to find the " +
                    "schema it left, not a mixture.",
                "lastOpenedAt" !in columnNames(connection, "projects"),
            )
            assertEquals(
                "The rows written before the upgrade must not be lost.",
                2,
                queryInt(connection, "SELECT COUNT(*) FROM projects"),
            )
            val name: String? = queryText(connection, "SELECT name FROM projects WHERE id = 'proj_01'")
            assertEquals("A read outside the failed step must still work.", "Werk", name)
        }
    }

    @Test
    fun `ten thousand rows migrate without a quadratic step`() {
        val file = createDatabaseAtVersion(1, "step-large")
        open(file).use { connection ->
            connection.execSQL("BEGIN")
            connection.prepare(
                "INSERT INTO projects (id, name, kind, path, isPrivate, isArchived, createdAt, updatedAt) " +
                    "VALUES (?, ?, 'LOCAL', '/tmp/p', 1, 0, 0, ?)",
            ).use { statement ->
                for (index in 0 until 10_000) {
                    statement.reset()
                    statement.clearBindings()
                    statement.bindText(1, "proj_$index")
                    statement.bindText(2, "Project $index")
                    statement.bindLong(3, index.toLong())
                    statement.step()
                }
            }
            connection.execSQL("COMMIT")
        }

        val startedAt = System.nanoTime()
        applyStep(file, addLastOpenedAtAndBackfill)
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

        open(file).use { connection ->
            assertEquals(
                10_000,
                queryInt(connection, "SELECT COUNT(*) FROM projects WHERE lastOpenedAt = updatedAt"),
            )
        }
        assertTrue(
            "A migration took ${elapsedMs}ms on 10k rows, which is the shape of an " +
                "accidentally quadratic backfill.",
            elapsedMs < 10_000,
        )
    }

    /**
     * The shape the doc's chain describes: a new nullable column, then a backfill
     * that derives its value from the row instead of inventing one.
     */
    private val addLastOpenedAtAndBackfill = object : Migration(1, 2) {
        override fun migrate(db: SQLiteConnection) {
            db.execSQL("ALTER TABLE projects ADD COLUMN lastOpenedAt INTEGER")
            db.execSQL("UPDATE projects SET lastOpenedAt = updatedAt WHERE lastOpenedAt IS NULL")
        }
    }

    private val interruptedStep = object : Migration(1, 2) {
        override fun migrate(db: SQLiteConnection) {
            db.execSQL("ALTER TABLE projects ADD COLUMN lastOpenedAt INTEGER")
            error("process died mid-migration")
        }
    }

    private fun applyStep(file: File, step: Migration) {
        open(file).use { connection ->
            connection.execSQL("BEGIN")
            step.migrate(connection)
            connection.execSQL("COMMIT")
        }
    }

    private fun seedProjects(file: File) {
        open(file).use { connection ->
            connection.execSQL(
                "INSERT INTO projects (id, name, kind, path, isPrivate, isArchived, createdAt, updatedAt) " +
                    "VALUES ('proj_01', 'Werk', 'LOCAL', '/tmp/werk', 1, 0, 1000, 1000)",
            )
            connection.execSQL(
                "INSERT INTO projects (id, name, kind, path, isPrivate, isArchived, createdAt, updatedAt) " +
                    "VALUES ('proj_02', 'Spiel', 'LOCAL', '/tmp/spiel', 1, 0, 2000, 2000)",
            )
        }
    }

    private fun sourceRoots(): List<File> =
        SchemaFixtures.ancestors()
            .map { File(it, "src/main/kotlin") }
            .filter { it.isDirectory }
            .toList()

    /**
     * The ban is on the call, not on the words. The documents and the KDoc have
     * to be able to say what is banned and why, or nobody can review the rule.
     */
    private fun stripComments(source: String): String = source
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""//[^\n]*"""), " ")

    private companion object {
        const val BANNED = "fallbackToDestructiveMigration"
    }
}
