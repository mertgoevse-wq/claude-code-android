package dev.ccandroid.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import dev.ccandroid.data.db.SchemaFixtures.columnNames
import dev.ccandroid.data.db.SchemaFixtures.createDatabaseAtVersion
import dev.ccandroid.data.db.SchemaFixtures.open
import dev.ccandroid.data.db.SchemaFixtures.queryInt
import dev.ccandroid.data.db.SchemaFixtures.queryText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * Test for migration 1 -> 2: Add Project.defaultBranch column, nullable.
 * Per docs/02-architecture/data-migrations.md, this is safe as no read path depends on it;
 * the UI falls back to "unknown".
 */
class Migration_1_2_Test {

    /** The migration step under test. */
    private val addDefaultBranch = object : Migration(1, 2) {
        override fun migrate(db: SQLiteConnection) {
            db.execSQL("ALTER TABLE projects ADD COLUMN defaultBranch TEXT")
        }
    }

    @Test
    fun `adds defaultBranch column to projects table`() {
        val file = createDatabaseAtVersion(1, "migration_1_2_test")
        seedProjects(file)

        applyStep(file, addDefaultBranch)

        open(file).use { connection ->
            assertEquals(2, queryInt(connection, "SELECT COUNT(*) FROM projects"))
            assertNull("defaultBranch should be NULL by default",
                       queryText(connection, "SELECT defaultBranch FROM projects WHERE id = 'proj_01'"))
            assertEquals("Werk", queryText(connection, "SELECT name FROM projects WHERE id = 'proj_01'"))
            assertEquals("Spiel", queryText(connection, "SELECT name FROM projects WHERE id = 'proj_02'"))
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
}