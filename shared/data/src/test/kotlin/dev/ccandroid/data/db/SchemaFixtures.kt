package dev.ccandroid.data.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Reads the schema Room exported, and builds a real on-disk database from it.
 *
 * A migration test against a database this code created itself proves nothing —
 * it would pass against a broken migration because the broken state is the only
 * state it ever saw. The schema JSON is the artifact from the version that
 * actually shipped, so a database built from it is a real predecessor.
 */
object SchemaFixtures {

    private val json = Json { ignoreUnknownKeys = true }

    val databaseName: String = AppDatabase.DATABASE_NAME

    /** The committed schema export for [version]. */
    fun schema(version: Int): JsonObject {
        val file = File(schemaDirectory, "$version.json")
        check(file.isFile) {
            "No committed schema for version $version at ${file.path}. " +
                "A migration without a fixture from its real predecessor is not finished."
        }
        return json.parseToJsonElement(file.readText()).jsonObject
    }

    /** The version number the exported schema claims to be. */
    fun schemaVersion(version: Int): Int =
        schema(version)["database"]!!.jsonObject["version"]!!.jsonPrimitive.content.toInt()

    /** Every table name the exported schema declares, in schema order. */
    fun declaredTableNames(version: Int): List<String> =
        entities(version).map { it.jsonObject["tableName"]!!.jsonPrimitive.content }

    /** Every table the exported schema says to create, in schema order. */
    fun createStatements(version: Int): List<String> =
        entities(version).map { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            val statement = entity.jsonObject["createSql"]!!.jsonPrimitive.content
            check(statement.isNotBlank()) { "Entity $table has no create statement." }
            resolveTable(statement, table)
        }

    /** Every index the exported schema says to create, including the unique ones. */
    fun indexStatements(version: Int): List<String> =
        entities(version).flatMap { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            val indices = entity.jsonObject["indices"]?.jsonArray ?: return@flatMap emptyList()
            indices.map { resolveTable(it.jsonObject["createSql"]!!.jsonPrimitive.content, table) }
        }

    /**
     * Room writes the table into `${'$'}{TABLE_NAME}` and substitutes it itself.
     * Building the database by hand means doing that substitution here, or the
     * statement creates a table literally named `${'$'}{TABLE_NAME}`.
     */
    private fun resolveTable(statement: String, table: String): String =
        statement.replace("\${TABLE_NAME}", table)

    /** The columns of [table] as SQLite reports them, which is the truth. */
    fun columnNames(connection: SQLiteConnection, table: String): Set<String> {
        val columns = mutableSetOf<String>()
        connection.prepare("PRAGMA table_info($table)").use { statement ->
            while (statement.step()) {
                val name: String = statement.getText(1)
                columns += name
            }
        }
        return columns
    }

    private fun entities(version: Int) =
        schema(version)["database"]!!.jsonObject["entities"]!!.jsonArray

    /**
     * A real SQLite file holding the schema of [version] and nothing else.
     *
     * The name carries a per-JVM id. A test that failed halfway through building
     * its fixture leaves a half-built file behind, and a fixed name would hand
     * that to the next run — the tests would pass against a database they never
     * created. Files accumulate under `build/`, which is gitignored, and nothing
     * is deleted, because deleting is one of the five things this project never
     * does.
     */
    fun createDatabaseAtVersion(version: Int, name: String): File {
        val file = File(scratchDirectory, "$name-v$version-$runId.db")
        if (file.isFile) return file
        BundledSQLiteDriver().open(file.absolutePath).use { connection ->
            connection.execSQL("BEGIN")
            createStatements(version).forEach { connection.execSQL(it) }
            indexStatements(version).forEach { connection.execSQL(it) }
            connection.execSQL("COMMIT")
        }
        return file
    }

    /** The same file for the whole test run, for callers that seed it themselves. */
    fun databaseFile(name: String): File = File(scratchDirectory, "$name-$runId.db")

    /** Opens [file] with a fresh connection the caller owns. */
    fun open(file: File): SQLiteConnection = BundledSQLiteDriver().open(file.absolutePath)

    /** The tables actually present, which is not the same as the ones declared. */
    fun tableNames(connection: SQLiteConnection): Set<String> {
        val names = mutableSetOf<String>()
        connection.prepare("SELECT name FROM sqlite_master WHERE type = 'table'").use { statement ->
            while (statement.step()) {
                val name: String = statement.getText(0)
                names += name
            }
        }
        return names
    }

    /** One row, or null. A SQL NULL reads as null, not as an empty string. */
    fun queryText(connection: SQLiteConnection, sql: String): String? =
        connection.prepare(sql).use { statement ->
            if (!statement.step() || statement.isNull(0)) {
                null
            } else {
                val value: String = statement.getText(0)
                value
            }
        }

    fun queryInt(connection: SQLiteConnection, sql: String): Int? =
        connection.prepare(sql).use { statement ->
            if (statement.step()) statement.getInt(0) else null
        }

    private val schemaDirectory: File
        get() = File(moduleDirectory, "schemas/dev.ccandroid.data.db.AppDatabase")

    /** Where the real database files live. Under `build/`, so nothing is tracked. */
    val scratchDirectory: File
        get() = File(moduleDirectory, "build/tmp/migration-tests").apply { mkdirs() }

    /** `shared/data`, found by walking up to the module that declares the schema. */
    private val moduleDirectory: File by lazy {
        ancestors()
            .firstOrNull { File(it, "schemas/dev.ccandroid.data.db.AppDatabase").isDirectory }
            ?: error("Could not find the shared/data schema directory from ${File(".").absolutePath}")
    }

    /** Unique per test run, so a half-built fixture is never reused. */
    private val runId: String by lazy { System.nanoTime().toString(36) }

    /** This directory and every one above it, nearest first. */
    fun ancestors(): Sequence<File> =
        generateSequence(File(".").absoluteFile) { it.parentFile ?: null }
}
