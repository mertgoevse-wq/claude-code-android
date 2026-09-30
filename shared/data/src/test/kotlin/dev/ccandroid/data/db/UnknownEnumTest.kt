package dev.ccandroid.data.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import dev.ccandroid.data.converter.TypeConverters
import dev.ccandroid.data.db.AppDatabase_Impl
import dev.ccandroid.data.db.SchemaFixtures.open
import dev.ccandroid.data.entity.ProjectEntity
import dev.ccandroid.domain.AutonomyLevel
import dev.ccandroid.domain.DiffDecision
import dev.ccandroid.domain.FileChangeType
import dev.ccandroid.domain.MessagePartKind
import dev.ccandroid.domain.MessageRole
import dev.ccandroid.domain.NotificationChannel
import dev.ccandroid.domain.OffloadPolicy
import dev.ccandroid.domain.PermissionMode
import dev.ccandroid.domain.PlanStepState
import dev.ccandroid.domain.ProjectKind
import dev.ccandroid.domain.ProviderKind
import dev.ccandroid.domain.RemoteTargetKind
import dev.ccandroid.domain.RemoteTargetStatus
import dev.ccandroid.domain.RunState
import dev.ccandroid.domain.SessionLogCategory
import dev.ccandroid.domain.SessionLogSeverity
import dev.ccandroid.domain.SkillInstallScope
import dev.ccandroid.domain.SkillSourceKind
import dev.ccandroid.domain.TestParserType
import dev.ccandroid.domain.ToolInvocationStatus
import dev.ccandroid.domain.VerificationState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

/**
 * An enum stored by a newer app version, read by an older one.
 *
 * This is not hypothetical: it is what happens after every update that ships a
 * new enum value, on every device that skipped an intermediate version. The rule
 * from `docs/02-architecture/data-migrations.md` §Unknown enum values is that
 * reading must return a documented value and never throw, because the user is
 * looking at a list at the time.
 *
 * The fallback is never the value that proceeds. It is the value that asks.
 */
class UnknownEnumTest {

    private val converters = TypeConverters()

    private val future = "A_VALUE_FROM_A_LATER_VERSION"

    @Test
    fun `a value this version does not know reads as the documented fallback`() {
        assertEquals(AutonomyLevel.ASK_EVERYTHING, converters.stringToAutonomyLevel(future))
        assertEquals(ProjectKind.LOCAL, converters.stringToProjectKind(future))
        assertEquals(ProviderKind.CUSTOM, converters.stringToProviderKind(future))
        assertEquals(RemoteTargetKind.SSH, converters.stringToRemoteTargetKind(future))
        assertEquals(RemoteTargetStatus.UNKNOWN, converters.stringToRemoteTargetStatus(future))
        assertEquals(RunState.INTERRUPTED, converters.stringToRunState(future))
        assertEquals(VerificationState.UNVERIFIED, converters.stringToVerificationState(future))
        assertEquals(ToolInvocationStatus.PENDING, converters.stringToToolInvocationStatus(future))
        assertEquals(PlanStepState.SKIPPED, converters.stringToPlanStepState(future))
        assertEquals(FileChangeType.MODIFIED, converters.stringToFileChangeType(future))
        assertEquals(DiffDecision.PENDING, converters.stringToDiffDecision(future))
        assertEquals(NotificationChannel.RUNNER, converters.stringToNotificationChannel(future))
        assertEquals(PermissionMode.DEFAULT, converters.stringToPermissionMode(future))
        assertEquals(MessageRole.SYSTEM, converters.stringToMessageRole(future))
        assertEquals(MessagePartKind.TEXT, converters.stringToMessagePartKind(future))
        assertEquals(OffloadPolicy.NEVER, converters.stringToOffloadPolicy(future))
        assertEquals(SkillSourceKind.BUILTIN, converters.stringToSkillSourceKind(future))
        assertEquals(SkillInstallScope.GLOBAL, converters.stringToSkillInstallScope(future))
        assertEquals(SessionLogCategory.SYSTEM, converters.stringToSessionLogCategory(future))
        assertEquals(SessionLogSeverity.INFO, converters.stringToSessionLogSeverity(future))
        assertEquals(TestParserType.GENERIC, converters.stringToTestParserType(future))
    }

    @Test
    fun `no fallback ever grants more than the user gave`() {
        assertEquals(
            "A level the app cannot read must ask, not act.",
            AutonomyLevel.entries.first(),
            converters.stringToAutonomyLevel(future),
        )
        assertEquals(
            "A permission mode the app cannot read must not be a bypass.",
            PermissionMode.DEFAULT,
            converters.stringToPermissionMode(future),
        )
        assertEquals(
            "A diff decision the app cannot read must still be the user's to make.",
            DiffDecision.entries.first(),
            converters.stringToDiffDecision(future),
        )
        assertEquals(
            "A run the app cannot reason about must not read as passing.",
            VerificationState.UNVERIFIED,
            converters.stringToVerificationState(future),
        )
    }

    @Test
    fun `a value this version does know is read as itself`() {
        assertEquals(RunState.RUNNING, converters.stringToRunState("RUNNING"))
        assertEquals(ProjectKind.REMOTE, converters.stringToProjectKind("REMOTE"))
        assertEquals(SessionLogSeverity.ERROR, converters.stringToSessionLogSeverity("ERROR"))
    }

    @Test
    fun `a stored null is still null, so a nullable column stays nullable`() {
        assertEquals(null, converters.stringToRunState(null))
        assertEquals(null, converters.stringToProjectKind(null))
    }

    @Test
    fun `a row written by a later version does not crash the read`() = runTest {
        val file = SchemaFixtures.databaseFile("unknown-enum-e2e")
        withDatabase(file) { db ->
            db.projectDao().insertProject(
                ProjectEntity(
                    id = "proj_01",
                    name = "Werk",
                    kind = ProjectKind.LOCAL,
                    path = "/tmp/werk",
                    createdAt = 1000L,
                    updatedAt = 1000L,
                ),
            )
        }

        // Written by raw SQL, the way a database from a later version would hold it.
        open(file).use { connection ->
            connection.execSQL("UPDATE projects SET kind = '$future' WHERE id = 'proj_01'")
        }

        withDatabase(file) { db ->
            val project = db.projectDao().getProjectById("proj_01")
            assertNotNull("A future enum value must not take the row down with it.", project)
            assertEquals(ProjectKind.LOCAL, project?.kind)
            assertEquals("Werk", project?.name)
        }
    }

    @Test
    fun `a list of projects is not taken down by one future value`() = runTest {
        val file = SchemaFixtures.databaseFile("unknown-enum-list")
        withDatabase(file) { db ->
            db.projectDao().insertProject(
                ProjectEntity(id = "a", name = "A", kind = ProjectKind.LOCAL, path = "/a", createdAt = 1, updatedAt = 1),
            )
            db.projectDao().insertProject(
                ProjectEntity(id = "b", name = "B", kind = ProjectKind.REMOTE, path = "/b", createdAt = 2, updatedAt = 2),
            )
        }
        open(file).use { connection ->
            connection.execSQL("UPDATE projects SET kind = '$future' WHERE id = 'a'")
        }

        withDatabase(file) { db ->
            val projects = db.projectDao().observeAllProjects().first()
            assertEquals("One bad row must not lose the good ones.", 2, projects.size)
            assertEquals(ProjectKind.LOCAL, projects.first { it.id == "a" }.kind)
            assertEquals(ProjectKind.REMOTE, projects.first { it.id == "b" }.kind)
        }
    }

    private suspend fun withDatabase(file: File, block: suspend (AppDatabase) -> Unit) {
        val db = Room.databaseBuilder<AppDatabase>(file.absolutePath, factory = { AppDatabase_Impl() })
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*Migrations.roomMigrations())
            .build()
        try {
            block(db)
        } finally {
            db.close()
        }
    }
}
