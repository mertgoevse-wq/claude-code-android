package dev.ccandroid.data.repository

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.room.Room
import dev.ccandroid.data.db.AppDatabase
import dev.ccandroid.core.ErrorCode
import dev.ccandroid.domain.CostRecord
import dev.ccandroid.domain.Message
import dev.ccandroid.domain.MessagePart
import dev.ccandroid.domain.MessagePartKind
import dev.ccandroid.domain.MessageRole
import dev.ccandroid.domain.Plan
import dev.ccandroid.domain.PlanStep
import dev.ccandroid.domain.PlanStepState
import dev.ccandroid.domain.Project
import dev.ccandroid.domain.ProjectKind
import dev.ccandroid.domain.ProjectSetting
import dev.ccandroid.domain.Run
import dev.ccandroid.domain.RunState
import dev.ccandroid.domain.TestParserType
import dev.ccandroid.domain.TestResult
import dev.ccandroid.domain.Turn
import dev.ccandroid.domain.TurnState
import dev.ccandroid.domain.VerificationRun
import dev.ccandroid.domain.VerificationState
import dev.ccandroid.domain.usecase.ConversationRepository
import dev.ccandroid.domain.usecase.CostRepository
import dev.ccandroid.domain.usecase.MessageRepository
import dev.ccandroid.domain.usecase.PlanRepository
import dev.ccandroid.domain.usecase.ProjectRepository
import dev.ccandroid.domain.usecase.RunRepository
import dev.ccandroid.domain.usecase.TurnRepository
import dev.ccandroid.domain.usecase.VerificationRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The repositories against a real SQLite engine (in-memory), per the test plan:
 * ≥ 80 % coverage on `shared/data`, boundaries, error paths, and the empty case.
 *
 * One behaviour per test; every test names the invariant it holds.
 */
class RoomRepositoriesTest {

    private lateinit var database: androidx.room.RoomDatabase
    private lateinit var repos: RoomRepositories

    @Before
    fun setUp() {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        database = db
        repos = RoomRepositories(db)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- fixtures -----------------------------------------------------

    private fun project(
        id: String = "proj_01",
        remoteOwner: String? = null,
        remoteName: String? = null,
    ) = Project(
        id = id,
        name = "Test project",
        kind = if (remoteOwner == null) ProjectKind.LOCAL else ProjectKind.CLONED,
        path = "/data/projects/$id",
        vcsProvider = remoteOwner?.let { "GITHUB" },
        remoteOwner = remoteOwner,
        remoteName = remoteName,
        createdAt = 1_000,
        updatedAt = 1_000,
    )

    private fun setting(projectId: String = "proj_01") = ProjectSetting(
        projectId = projectId,
        modelProviderId = "anthropic",
        modelId = "claude-sonnet-4-5",
        verifyCommands = listOf("./gradlew check"),
    )

    private fun conversation(id: String = "conv_01", projectId: String = "proj_01", archived: Boolean = false) =
        dev.ccandroid.domain.Conversation(
            id = id,
            projectId = projectId,
            title = "Chat $id",
            sessionId = "sess_$id",
            isArchived = archived,
            createdAt = 1_000,
            updatedAt = 1_000,
        )

    private fun turn(id: String, conversationId: String, index: Int) = Turn(
        id = id,
        conversationId = conversationId,
        index = index,
        userMessageId = "msg_$id",
        startedAt = 1_000,
        state = TurnState.PLANNING,
    )

    private fun message(id: String, turnId: String) = Message(
        id = id,
        turnId = turnId,
        role = MessageRole.USER,
        createdAt = 1_000,
        renderedMarkdown = "hello",
    )

    private fun run(id: String, projectId: String, turnId: String, state: RunState = RunState.CREATED) = Run(
        id = id,
        projectId = projectId,
        conversationId = "conv_01",
        turnId = turnId,
        backendId = "local",
        backendProfile = "native",
        state = state,
        taskText = "fix the tests",
        startedAt = 1_000,
    )

    private fun costRecord(id: String, runId: String, micros: Long, conversationId: String = "conv_01") = CostRecord(
        id = id,
        runId = runId,
        projectId = "proj_01",
        conversationId = conversationId,
        costUsdMicros = micros,
        modelId = "claude-sonnet-4-5",
        providerId = "anthropic",
    )

    private fun plan(id: String, runId: String?) = Plan(
        id = id,
        runId = runId,
        title = "Plan $id",
        steps = listOf(
            PlanStep(
                id = "${id}_s1",
                planId = id,
                ordinal = 0,
                titleDe = "Schritt eins",
                titleEn = "Step one",
                acceptanceCriteria = "it works",
                state = PlanStepState.PENDING,
            ),
        ),
        createdAt = 1_000,
    )

    private fun verification(id: String, runId: String, attempt: Int = 1) = VerificationRun(
        id = id,
        runId = runId,
        attempt = attempt,
        state = VerificationState.PASSED,
        commandCount = 1,
        createdAt = 1_000,
    )

    private fun testResult(id: String, verificationRunId: String, ordinal: Int) = TestResult(
        id = id,
        verificationRunId = verificationRunId,
        ordinal = ordinal,
        command = "./gradlew check",
        exitCode = 0,
        durationMs = 42,
        summaryLine = "all green",
        parserUsed = TestParserType.GENERIC,
    )

    // --- project repository -------------------------------------------

    @Test
    fun `a project round trips through the database`() = runTest {
        val repo: ProjectRepository = repos.project

        repo.insert(project()).getOrThrow()
        val loaded = repo.getById("proj_01").getOrThrow()

        assertNotNull(loaded)
        assertEquals("Test project", loaded?.name)
        assertEquals(ProjectKind.LOCAL, loaded?.kind)
    }

    @Test
    fun `an unknown project id returns null rather than throwing`() = runTest {
        val repo: ProjectRepository = repos.project

        assertNull(repo.getById("proj_missing").getOrThrow())
    }

    @Test
    fun `a duplicate project id is a named conflict, not a driver exception`() = runTest {
        val repo: ProjectRepository = repos.project
        repo.insert(project()).getOrThrow()

        val outcome = repo.insert(project())

        assertTrue(outcome.isFailure)
        assertEquals(ErrorCode.PROJECT_ALREADY_EXISTS, outcome.errorOrNull()?.code)
    }

    @Test
    fun `a cloned project whose remote is already cloned is refused`() = runTest {
        val repo: ProjectRepository = repos.project
        repo.insert(project(remoteOwner = "acme", remoteName = "widgets")).getOrThrow()

        val outcome = repo.insert(project(id = "proj_02", remoteOwner = "acme", remoteName = "widgets"))

        assertTrue(outcome.isFailure)
        // The unique index fires; the caller still gets a typed Outcome.
        assertTrue(outcome.errorOrNull() is dev.ccandroid.core.AppError.Simple)
    }

    @Test
    fun `getAll returns every project including archived ones`() = runTest {
        val repo: ProjectRepository = repos.project
        repo.insert(project()).getOrThrow()
        repo.insert(project(id = "proj_02")).getOrThrow()
        repo.update(project().copy(id = "proj_02", isArchived = true)).getOrThrow()

        val all = repo.getAll().getOrThrow()

        assertEquals(2, all.size)
        assertTrue(all.any { it.isArchived })
    }

    @Test
    fun `settings round trip through the same repository`() = runTest {
        val repo: ProjectRepository = repos.project
        repo.insert(project()).getOrThrow()
        repo.insertSetting(setting()).getOrThrow()

        val loaded = repo.getSetting("proj_01").getOrThrow()

        assertEquals(listOf("./gradlew check"), loaded?.verifyCommands)
        assertEquals("claude-sonnet-4-5", loaded?.modelId)
    }

    @Test
    fun `existsByRemote finds the project it was inserted for`() = runTest {
        val repo: ProjectRepository = repos.project
        repo.insert(project(remoteOwner = "acme", remoteName = "widgets")).getOrThrow()

        val exists = repo.existsByRemote("acme", "widgets").getOrThrow()
        val missing = repo.existsByRemote("acme", "other").getOrThrow()

        assertTrue(exists)
        assertFalse(missing)
    }

    // --- conversation repository ----------------------------------------

    @Test
    fun `a conversation round trips and archives without ever being deleted`() = runTest {
        val repo: ConversationRepository = repos.conversation
        repo.insert(conversation()).getOrThrow()

        repo.update(conversation().copy(isArchived = true)).getOrThrow()
        val loaded = repo.getById("conv_01").getOrThrow()

        assertNotNull("Archive is the only way out; the row must still be there.", loaded)
        assertTrue(loaded?.isArchived == true)
    }

    @Test
    fun `getByProject excludes archived conversations unless asked`() = runTest {
        val repo: ConversationRepository = repos.conversation
        repo.insert(conversation()).getOrThrow()
        repo.insert(conversation(id = "conv_02", archived = true)).getOrThrow()

        val active = repo.getByProject("proj_01", includeArchived = false).getOrThrow()
        val everything = repo.getByProject("proj_01", includeArchived = true).getOrThrow()

        assertEquals(1, active.size)
        assertEquals(2, everything.size)
    }

    @Test
    fun `deleting a conversation is refused, never performed`() = runTest {
        val repo: ConversationRepository = repos.conversation
        repo.insert(conversation()).getOrThrow()

        val outcome = repo.delete("conv_01")

        assertTrue(outcome.isFailure)
        assertEquals(ErrorCode.POLICY_BLOCK_DELETE, outcome.errorOrNull()?.code)
        // The row is still there: the refusal is real, not cosmetic.
        assertNotNull(repo.getById("conv_01").getOrThrow())
    }

    // --- turn repository ------------------------------------------------

    @Test
    fun `a turn whose conversation is missing is a named not-found`() = runTest {
        val repo: TurnRepository = repos.turn

        val outcome = repo.insert(turn("turn_01", conversationId = "conv_missing", index = 0))

        assertTrue(outcome.isFailure)
        assertEquals(ErrorCode.PROJECT_NOT_FOUND, outcome.errorOrNull()?.code)
    }

    @Test
    fun `turns come back ordered by index and the last turn is the newest`() = runTest {
        val repo: TurnRepository = repos.turn
        val convRepo: ConversationRepository = repos.conversation
        convRepo.insert(conversation()).getOrThrow()
        repo.insert(turn("turn_02", "conv_01", index = 1)).getOrThrow()
        repo.insert(turn("turn_01", "conv_01", index = 0)).getOrThrow()

        val turns = repo.getByConversation("conv_01").getOrThrow()

        assertEquals(listOf("turn_01", "turn_02"), turns.map { it.id })
        assertEquals("turn_02", repo.getLastTurn("conv_01").getOrThrow()?.id)
    }

    @Test
    fun `updating a turn persists its changes`() = runTest {
        val repo: TurnRepository = repos.turn
        val convRepo: ConversationRepository = repos.conversation
        convRepo.insert(conversation()).getOrThrow()

        val originalTurn = turn("turn_01", "conv_01", 0)
        repo.insert(originalTurn).getOrThrow()

        val updatedTurn = originalTurn.copy(
            state = TurnState.RUNNING,
            endedAt = 2000L
        )
        repo.update(updatedTurn).getOrThrow()

        val loaded = repo.getById("turn_01").getOrThrow()
        assertNotNull(loaded)
        assertEquals(TurnState.RUNNING, loaded?.state)
        assertEquals(2000L, loaded?.endedAt)
    }

    @Test
    fun `getById returns the correct turn`() = runTest {
        val repo: TurnRepository = repos.turn
        val convRepo: ConversationRepository = repos.conversation
        convRepo.insert(conversation()).getOrThrow()

        val turn1 = turn("turn_01", "conv_01", 0)
        val turn2 = turn("turn_02", "conv_01", 1)
        repo.insert(turn1).getOrThrow()
        repo.insert(turn2).getOrThrow()

        val loaded = repo.getById("turn_02").getOrThrow()
        assertNotNull(loaded)
        assertEquals("turn_02", loaded?.id)
        assertEquals(1, loaded?.index)
    }

    // --- message repository ----------------------------------------------

    @Test
    fun `a message and its parts round trip in order`() = runTest {
        val msgRepo: MessageRepository = repos.message
        msgRepo.insert(message("msg_01", "turn_01")).getOrThrow()
        msgRepo.insertPart(
            MessagePart(
                id = "part_01",
                messageId = "msg_01",
                kind = MessagePartKind.TEXT,
                ordinal = 0,
                payloadJson = "{}",
            ),
        ).getOrThrow()
        msgRepo.insertPart(
            MessagePart(
                id = "part_02",
                messageId = "msg_01",
                kind = MessagePartKind.CODE,
                ordinal = 1,
                payloadJson = "{}",
            ),
        ).getOrThrow()

        val parts = msgRepo.getParts("msg_01").getOrThrow()

        assertEquals(listOf("part_01", "part_02"), parts.map { it.id })
    }

    @Test
    fun `messages for a turn come back ordered by creation`() = runTest {
        val msgRepo: MessageRepository = repos.message
        msgRepo.insert(message("msg_02", "turn_01").copy(createdAt = 2_000)).getOrThrow()
        msgRepo.insert(message("msg_01", "turn_01").copy(createdAt = 1_000)).getOrThrow()

        val messages = msgRepo.getByTurn("turn_01").getOrThrow()

        assertEquals(listOf("msg_01", "msg_02"), messages.map { it.id })
    }

    @Test
    fun `updating a message persists its changes`() = runTest {
        val msgRepo: MessageRepository = repos.message
        val turnRepo: TurnRepository = repos.turn
        val convRepo: ConversationRepository = repos.conversation
        convRepo.insert(conversation()).getOrThrow()
        turnRepo.insert(turn("turn_01", "conv_01", 0)).getOrThrow()

        val originalMessage = message("msg_01", "turn_01")
        msgRepo.insert(originalMessage).getOrThrow()

        val updatedMessage = originalMessage.copy(
            renderedMarkdown = "updated content",
            role = MessageRole.ASSISTANT
        )
        msgRepo.update(updatedMessage).getOrThrow()

        val loaded = msgRepo.getByTurn("turn_01").getOrThrow().firstOrNull { it.id == "msg_01" }
        assertNotNull(loaded)
        assertEquals("updated content", loaded?.renderedMarkdown)
        assertEquals(MessageRole.ASSISTANT, loaded?.role)
    }

    @Test
    fun `updating a message part persists its changes`() = runTest {
        val msgRepo: MessageRepository = repos.message
        val turnRepo: TurnRepository = repos.turn
        val convRepo: ConversationRepository = repos.conversation
        convRepo.insert(conversation()).getOrThrow()
        turnRepo.insert(turn("turn_01", "conv_01", 0)).getOrThrow()

        val originalMessage = message("msg_01", "turn_01")
        msgRepo.insert(originalMessage).getOrThrow()
        val originalPart = MessagePart(
            id = "part_01",
            messageId = "msg_01",
            kind = MessagePartKind.TEXT,
            ordinal = 0,
            payloadJson = "{\"original\": \"data\"}"
        )
        msgRepo.insertPart(originalPart).getOrThrow()

        val updatedPart = originalPart.copy(
            kind = MessagePartKind.CODE,
            payloadJson = "{\"code\": \"print('hello')\"}"
        )
        msgRepo.updatePart(updatedPart).getOrThrow()

        val loaded = msgRepo.getParts("msg_01").getOrThrow().firstOrNull { it.id == "part_01" }
        assertNotNull(loaded)
        assertEquals(MessagePartKind.CODE, loaded?.kind)
        assertEquals("{\"code\": \"print('hello')\"}", loaded?.payloadJson)
    }

    // --- run repository ----------------------------------------------------

    @Test
    fun `runs are listed per project and filterable by state`() = runTest {
        val repo: RunRepository = repos.run
        val convRepo: ConversationRepository = repos.conversation
        convRepo.insert(conversation()).getOrThrow()
        repo.insert(run("run_01", "proj_01", "turn_01", state = RunState.RUNNING)).getOrThrow()
        repo.insert(run("run_02", "proj_01", "turn_01", state = RunState.DONE)).getOrThrow()

        val all = repo.getByProject("proj_01").getOrThrow()
        val running = repo.getByState(RunState.RUNNING).getOrThrow()

        assertEquals(2, all.size)
        assertEquals(listOf("run_01"), running.map { it.id })
    }

    @Test
    fun `updating a run persists its terminal state`() = runTest {
        val repo: RunRepository = repos.run
        val convRepo: ConversationRepository = repos.conversation
        convRepo.insert(conversation()).getOrThrow()
        repo.insert(run("run_01", "proj_01", "turn_01")).getOrThrow()

        repo.update(run("run_01", "proj_01", "turn_01", state = RunState.FAILED)).getOrThrow()

        assertEquals(RunState.FAILED, repo.getById("run_01").getOrThrow()?.state)
    }

    // --- cost repository ---------------------------------------------------

    @Test
    fun `cost sums to zero when nothing was recorded`() = runTest {
        val repo: CostRepository = repos.cost

        assertEquals(0L, repo.getTotalCost("proj_01").getOrThrow())
        assertEquals(0L, repo.getTotalCostByConversation("conv_missing").getOrThrow())
    }

    @Test
    fun `cost sums across runs for a project and per conversation`() = runTest {
        val repo: CostRepository = repos.cost
        repo.insert(costRecord("cost_01", "run_01", micros = 150)).getOrThrow()
        repo.insert(costRecord("cost_02", "run_01", micros = 250)).getOrThrow()
        repo.insert(costRecord("cost_03", "run_02", micros = 1_000, conversationId = "conv_02")).getOrThrow()

        assertEquals(1_400L, repo.getTotalCost("proj_01").getOrThrow())
        assertEquals(400L, repo.getTotalCostByConversation("conv_01").getOrThrow())
        assertEquals(1_000L, repo.getTotalCostByConversation("conv").getOrThrow())
        assertEquals(2, repo.getByRunId("run_01").getOrThrow().size)
    }

    @Test
    fun `getByProjectId returns cost records for the correct project`() = runTest {
        val repo: CostRepository = repos.cost
        // Insert cost records for two different projects
        repo.insert(costRecord("cost_01", "run_01", micros = 100, projectId = "proj_a")).getOrThrow()
        repo.insert(costRecord("cost_02", "run_01", micros = 200, projectId = "proj_a")).getOrThrow()
        repo.insert(costRecord("cost_03", "run_02", micros = 300, projectId = "proj_b")).getOrThrow()

        val projACosts = repo.getByProjectId("proj_a").getOrThrow()
        val projBCosts = repo.getByProjectId("proj_b").getOrThrow()
        val emptyProjCosts = repo.getByProjectId("empty").getOrThrow()

        assertEquals(2, projACosts.size)
        assertEquals(100L + 200L, projACosts.sumOf { it.costUsdMicros })
        assertEquals(1, projBCosts.size)
        assertEquals(300L, projBCosts.firstOrNull()?.costUsdMicros)
        assertTrue(emptyProjCosts.isEmptyOrNull())
    }

    // --- plan repository --------------------------------------------------------

    @Test
    fun `a plan carries its steps through insert and load`() = runTest {
        val repo: PlanRepository = repos.plan

        repo.insert(plan("plan_01", runId = "run_01")).getOrThrow()
        val loaded = repo.getByRunId("run_01").getOrThrow()

        assertNotNull(loaded)
        assertEquals(1, loaded?.steps?.size)
        assertEquals("Step one", loaded?.steps?.first()?.titleEn)
    }

    @Test
    fun `updating a plan replaces its steps and cannot duplicate them`() = runTest {
        val repo: PlanRepository = repos.plan
        repo.insert(plan("plan_01", runId = null)).getOrThrow()

        val changed = plan("plan_01", runId = null).copy(
            steps = plan("plan_01", runId = null).steps.map { it.copy(state = PlanStepState.DONE) },
        )
        repo.update(changed).getOrThrow()
        val loaded = repo.getByRunId("run_01").getOrThrow()
        assertNull("A plan with no run id must not appear under another run's id.", loaded)

        val byId = repos.plan.getById("plan_01").getOrThrow()
        assertEquals(PlanStepState.DONE, byId?.steps?.single()?.state)
    }

    @Test
    fun `an unknown plan id returns null`() = runTest {
        assertNull(repos.plan.getById("plan_missing").getOrThrow())
    }

    // --- verification repository ---------------------------------------------------

    @Test
    fun `verification runs list newest attempt first`() = runTest {
        val repo: VerificationRepository = repos.verification
        repo.insert(verification("ver_01", "run_01", attempt = 1)).getOrThrow()
        repo.insert(verification("ver_02", "run_01", attempt = 2)).getOrThrow()

        val runs = repo.getByRunId("run_01").getOrThrow()

        assertEquals(listOf("ver_02", "ver_01"), runs.map { it.id })
    }

    @Test
    fun `complete writes a verification run and its results as one fact`() = runTest {
        val repo = repos.verification
        repo.complete(
            verification("ver_01", "run_01"),
            listOf(testResult("tr_01", "ver_01", ordinal = 0)),
        ).getOrThrow()

        val loaded = repo.getById("ver_01").getOrThrow()
        assertNotNull(loaded)
        assertEquals(VerificationState.PASSED, loaded?.state)
    }

    @Test
    fun `updating a verification run persists its changes`() = runTest {
        val repo: VerificationRepository = repos.verification
        val originalRun = verification("ver_01", "run_01")
        repo.insert(originalRun).getOrThrow()

        val updatedRun = originalRun.copy(
            state = VerificationState.FAILED,
            durationMs = 5000
        )
        repo.update(updatedRun).getOrThrow()

        val loaded = repo.getById("ver_01").getOrThrow()
        assertNotNull(loaded)
        assertEquals(VerificationState.FAILED, loaded?.state)
        assertEquals(5000, loaded?.durationMs)
    }

    // --- project settings repository ------------------------------------------------

    @Test
    fun `settings update in place through the settings repository`() = runTest {
        val repo = repos.settings
        repo.updateSetting(setting()).getOrThrow()

        repo.updateSetting(setting().copy(autonomyLevel = dev.ccandroid.domain.AutonomyLevel.FULL_AUTO)).getOrThrow()

        val loaded = repo.getSetting("proj_01").getOrThrow()
        assertEquals(dev.ccandroid.domain.AutonomyLevel.FULL_AUTO, loaded?.autonomyLevel)
        assertEquals(3, loaded?.retryBudget)
    }

    @Test
    fun `a project's own setting method agrees with the settings repository`() = runTest {
        val projectRepo: ProjectRepository = repos.project
        projectRepo.insert(project()).getOrThrow()
        projectRepo.insertSetting(setting()).getOrThrow()

        val viaProject = projectRepo.getSetting("proj_01").getOrThrow()
        val viaSettings = repos.settings.getSetting("proj_01").getOrThrow()

        assertEquals(viaSettings, viaProject)
    }

    // --- the boundary rule: no bare exception crosses --------------------------------

    @Test
    fun `every repository method returns an Outcome, never a bare exception`() = runTest {
        // Structural check across the classes in this file: any signature that
        // is not Outcome-returning is the bare-exception boundary code rule 4
        // forbids.
        val repositories = listOf(
            ProjectRepository::class,
            ConversationRepository::class,
            TurnRepository::class,
            MessageRepository::class,
            RunRepository::class,
            CostRepository::class,
            PlanRepository::class,
            VerificationRepository::class,
            dev.ccandroid.domain.usecase.ProjectSettingRepository::class,
        )
        val violations = repositories.flatMap { iface ->
            iface.members
                .filterNot { it.name in setOf("equals", "hashCode", "toString") }
                .filter { "Outcome" !in it.returnType.toString() }
                .map { "${iface.simpleName}.${it.name}: ${it.returnType}" }
        }

        assertTrue(
            "Every repository method must return Outcome<T>, was: $violations",
            violations.isEmpty(),
        )
    }
}
