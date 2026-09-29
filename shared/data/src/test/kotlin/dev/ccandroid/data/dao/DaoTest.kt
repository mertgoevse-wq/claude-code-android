package dev.ccandroid.data.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.ccandroid.data.db.AppDatabase
import dev.ccandroid.data.entity.*
import dev.ccandroid.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Room DAO tests against an in-memory database.
 * Tests all DAO operations against the schema defined in data-model.md.
 * Coverage target: ≥ 80% for shared/data module.
 *
 * These run as plain JVM tests on Room's KMP path with the BundledSQLiteDriver,
 * the setup the Room documentation recommends for local database tests.
 * Robolectric was tried first and is unusable here: its native runtime ships
 * no linux/aarch64 build, and this repo's build host is ARM64
 * (robolectric/robolectric#9166; progress log, P2-6 entry).
 */
class DaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `project DAO - insert and get by id`() = runTest {
        val project = ProjectEntity(
            id = "proj_01",
            name = "Test Project",
            kind = ProjectKind.LOCAL,
            path = "/tmp/test",
            createdAt = 1000L,
            updatedAt = 1000L,
        )
        database.projectDao().insertProject(project)
        val retrieved = database.projectDao().getProjectById("proj_01")
        assertNotNull(retrieved)
        assertEquals("Test Project", retrieved?.name)
        assertEquals(ProjectKind.LOCAL, retrieved?.kind)
    }

    @Test
    fun `project DAO - observe active projects`() = runTest {
        val project1 = ProjectEntity(id = "p1", name = "Active", kind = ProjectKind.LOCAL, path = "/p1", createdAt = 1000L, updatedAt = 1000L, isArchived = false, lastRunAt = 2000L)
        val project2 = ProjectEntity(id = "p2", name = "Archived", kind = ProjectKind.LOCAL, path = "/p2", createdAt = 1000L, updatedAt = 1000L, isArchived = true)
        database.projectDao().insertProject(project1)
        database.projectDao().insertProject(project2)

        val active = database.projectDao().observeActiveProjects().first()
        assertEquals(1, active.size)
        assertEquals("Active", active.first().name)
    }

    @Test
    fun `project DAO - archive and unarchive`() = runTest {
        val project = ProjectEntity(id = "p1", name = "Test", kind = ProjectKind.LOCAL, path = "/p1", createdAt = 1000L, updatedAt = 1000L)
        database.projectDao().insertProject(project)

        database.projectDao().archiveProject("p1", 2000L)
        var retrieved = database.projectDao().getProjectById("p1")
        assertTrue(retrieved?.isArchived == true)

        database.projectDao().unarchiveProject("p1", 3000L)
        retrieved = database.projectDao().getProjectById("p1")
        assertFalse(retrieved?.isArchived == true)
    }

    @Test
    fun `project DAO - unique constraint on remote owner and name`() = runTest {
        val project1 = ProjectEntity(
            id = "p1", name = "Repo", kind = ProjectKind.CLONED, path = "/p1",
            vcsProvider = "GITHUB", remoteOwner = "owner", remoteName = "repo",
            createdAt = 1000L, updatedAt = 1000L
        )
        val project2 = ProjectEntity(
            id = "p2", name = "Repo2", kind = ProjectKind.CLONED, path = "/p2",
            vcsProvider = "GITHUB", remoteOwner = "owner", remoteName = "repo",
            createdAt = 1000L, updatedAt = 1000L
        )
        database.projectDao().insertProject(project1)
        try {
            database.projectDao().insertProject(project2)
            fail("Expected exception for duplicate remote")
        } catch (e: Exception) {
            // Expected - unique constraint violation
        }
    }

    @Test
    fun `project setting DAO - insert and get`() = runTest {
        val setting = ProjectSettingEntity(
            projectId = "proj_01",
            autonomyLevel = AutonomyLevel.ASK_RISKY,
            modelProviderId = "prov_anthropic",
            modelId = "claude-sonnet-4-5",
            verifyCommands = "[]",
            retryBudget = 10,
            allowedTools = "[]",
            deniedTools = "[]",
            permissionMode = PermissionMode.ACCEPT_NON_DESTRUCTIVE,
            offloadPolicy = OffloadPolicy.NEVER,
            branchPrefix = "task/",
            costAdvisoryThresholdUsd = null,
            autoCommit = true,
            autoPr = true,
            notificationsEnabled = true,
        )
        database.projectSettingDao().insertOrUpdateSetting(setting)
        val retrieved = database.projectSettingDao().getSettingForProject("proj_01")
        assertNotNull(retrieved)
        assertEquals(AutonomyLevel.ASK_RISKY, retrieved?.autonomyLevel)
        assertEquals("claude-sonnet-4-5", retrieved?.modelId)
    }

    @Test
    fun `conversation DAO - insert and observe for project`() = runTest {
        val conv = ConversationEntity(
            id = "conv_01",
            projectId = "proj_01",
            title = "Test Conversation",
            sessionId = "session_123",
            createdAt = 1000L,
            updatedAt = 1000L,
        )
        database.conversationDao().insertConversation(conv)

        val conversations = database.conversationDao().observeConversationsForProject("proj_01").first()
        assertEquals(1, conversations.size)
        assertEquals("Test Conversation", conversations.first().title)
    }

    @Test
    fun `conversation DAO - archive conversation`() = runTest {
        val conv = ConversationEntity(id = "conv_01", projectId = "proj_01", title = "Test", sessionId = "s1", createdAt = 1000L, updatedAt = 1000L)
        database.conversationDao().insertConversation(conv)

        database.conversationDao().archiveConversation("conv_01", 2000L)
        val retrieved = database.conversationDao().getConversationById("conv_01")
        assertTrue(retrieved?.isArchived == true)
    }

    @Test
    fun `turn DAO - insert and observe for conversation`() = runTest {
        val turn = TurnEntity(
            id = "turn_01",
            conversationId = "conv_01",
            index = 1,
            userMessageId = "msg_01",
            startedAt = 1000L,
            endedAt = 2000L,
            state = TurnState.DONE,
            costUsd = 1000L,
            attemptCount = 1,
        )
        database.turnDao().insertTurn(turn)

        val turns = database.turnDao().observeTurnsForConversation("conv_01").first()
        assertEquals(1, turns.size)
        assertEquals(TurnState.DONE, turns.first().state)
    }

    @Test
    fun `message DAO - insert and observe for turn`() = runTest {
        val message = MessageEntity(
            id = "msg_01",
            turnId = "turn_01",
            role = MessageRole.ASSISTANT,
            createdAt = 1000L,
            renderedMarkdown = "Hello world",
            isRedacted = false,
        )
        database.messageDao().insertMessage(message)

        val messages = database.messageDao().observeMessagesForTurn("turn_01").first()
        assertEquals(1, messages.size)
        assertEquals(MessageRole.ASSISTANT, messages.first().role)
    }

    @Test
    fun `message part DAO - insert and observe for message`() = runTest {
        val part = MessagePartEntity(
            id = "part_01",
            messageId = "msg_01",
            kind = MessagePartKind.TEXT,
            ordinal = 0,
            payloadJson = "{\"text\":\"Hello\"}",
            collapsed = false,
        )
        database.messageDao().insertMessagePart(part)

        val parts = database.messageDao().observeMessagePartsForMessage("msg_01").first()
        assertEquals(1, parts.size)
        assertEquals(MessagePartKind.TEXT, parts.first().kind)
    }

    @Test
    fun `tool invocation DAO - insert and observe for turn`() = runTest {
        val tool = ToolInvocationEntity(
            id = "tool_01",
            turnId = "turn_01",
            toolUseId = "tuse_123",
            name = "Read",
            titleDe = "Datei gelesen",
            titleEn = "Read file",
            targetPath = "build.gradle.kts",
            inputJson = "{\"file\":\"build.gradle.kts\"}",
            status = ToolInvocationStatus.DONE,
            startedAt = 1000L,
            endedAt = 1200L,
            outputPreview = "plugins { ... }",
            isDestructive = false,
        )
        database.toolInvocationDao().insertToolInvocation(tool)

        val tools = database.toolInvocationDao().observeToolInvocationsForTurn("turn_01").first()
        assertEquals(1, tools.size)
        assertEquals("Read", tools.first().name)
    }

    @Test
    fun `tool invocation DAO - complete tool invocation transaction`() = runTest {
        val tool = ToolInvocationEntity(
            id = "tool_01",
            turnId = "turn_01",
            toolUseId = "tuse_123",
            name = "Read",
            titleDe = "Datei gelesen",
            titleEn = "Read file",
            targetPath = "build.gradle.kts",
            inputJson = "{\"file\":\"build.gradle.kts\"}",
            status = ToolInvocationStatus.RUNNING,
            startedAt = 1000L,
            isDestructive = false,
        )
        database.toolInvocationDao().insertToolInvocation(tool)

        val result = ToolResultEntity(
            invocationId = "tool_01",
            toolUseId = "tuse_123",
            output = "{\"content\":\"plugins { ... }\"}",
            exitCode = 0,
        )

        database.toolInvocationDao().completeToolInvocation(
            id = "tool_01",
            status = ToolInvocationStatus.DONE,
            result = result,
            outputPreview = "plugins { ... }",
            outputRef = "logs/tool_01.log",
            endedAt = 1200L,
        )

        val updatedTool = database.toolInvocationDao().getToolInvocationById("tool_01")
        assertEquals(ToolInvocationStatus.DONE, updatedTool?.status)
        assertEquals(1200L, updatedTool?.endedAt)

        val toolResult = database.toolInvocationDao().getToolResultForInvocation("tool_01")
        assertNotNull(toolResult)
        assertEquals(0, toolResult?.exitCode)
    }

    @Test
    fun `plan DAO - insert and observe for run`() = runTest {
        val plan = PlanEntity(
            id = "plan_01",
            runId = "run_01",
            title = "Test Plan",
            createdAt = 1000L,
        )
        database.planDao().insertPlan(plan)

        val retrieved = database.planDao().getPlanByRunId("run_01")
        assertNotNull(retrieved)
        assertEquals("plan_01", retrieved?.id)
    }

    @Test
    fun `plan step DAO - insert and get`() = runTest {
        database.planDao().insertPlan(
            PlanEntity(id = "plan_01", runId = "run_01", title = "Test Plan", createdAt = 1000L)
        )
        val step = PlanStepEntity(
            id = "step_01",
            planId = "plan_01",
            ordinal = 0,
            titleDe = "Schritt 1",
            titleEn = "Step 1",
            acceptanceCriteria = "AC1",
            state = PlanStepState.PENDING,
            isCheckpoint = false,
        )
        database.planDao().insertPlanSteps(listOf(step))

        val retrieved = database.planDao().getPlanSteps("plan_01")
        assertEquals(1, retrieved.size)
        assertEquals("Schritt 1", retrieved.first().titleDe)
    }

    @Test
    fun `file change DAO - insert and observe for run`() = runTest {
        val change = FileChangeEntity(
            id = "fc_01",
            runId = "run_01",
            path = "src/main.kt",
            changeType = FileChangeType.MODIFIED,
            oldPath = null,
            linesAdded = 10,
            linesRemoved = 5,
            binary = false,
            hunksJson = "[{\"oldStart\":1,\"oldLines\":5,\"newStart\":1,\"newLines\":10}]",
        )
        database.fileChangeDao().insertFileChange(change)

        val changes = database.fileChangeDao().observeFileChangesForRun("run_01").first()
        assertEquals(1, changes.size)
        assertEquals(FileChangeType.MODIFIED, changes.first().changeType)
    }

    @Test
    fun `diff entry DAO - insert and observe for file change`() = runTest {
        database.fileChangeDao().insertFileChange(
            FileChangeEntity(
                id = "fc_01", runId = "run_01", path = "src/main.kt",
                changeType = FileChangeType.MODIFIED,
                linesAdded = 1, linesRemoved = 0, binary = false, hunksJson = "[]",
            )
        )
        val entry = DiffEntryEntity(
            id = "de_01",
            fileChangeId = "fc_01",
            ordinal = 0,
            oldStart = 1,
            oldLines = 5,
            newStart = 1,
            newLines = 10,
            linesJson = "[{\"kind\":\"CONTEXT\",\"text\":\"line1\"},{\"kind\":\"ADD\",\"text\":\"new line\"}]",
            decision = DiffDecision.PENDING,
            revertCommitId = null,
        )
        database.fileChangeDao().insertDiffEntries(listOf(entry))

        val entries = database.fileChangeDao().observeDiffEntriesForFileChange("fc_01").first()
        assertEquals(1, entries.size)
        assertEquals(DiffDecision.PENDING, entries.first().decision)
    }

    @Test
    fun `verification run DAO - insert and get by run id`() = runTest {
        val vrun = VerificationRunEntity(
            id = "vrun_01",
            runId = "run_01",
            attempt = 1,
            state = VerificationState.RUNNING,
            commandCount = 2,
            durationMs = 0,
            logRef = "logs/vrun_01.log",
            judgedAt = null,
            createdAt = 1000L,
        )
        database.verificationDao().insertVerificationRun(vrun)

        val runs = database.verificationDao().observeVerificationRunsForRun("run_01").first()
        assertEquals(1, runs.size)
        assertEquals(VerificationState.RUNNING, runs.first().state)
    }

    @Test
    fun `test result DAO - insert and observe for verification run`() = runTest {
        database.verificationDao().insertVerificationRun(
            VerificationRunEntity(
                id = "vrun_01", runId = "run_01", attempt = 1,
                state = VerificationState.RUNNING, commandCount = 1, durationMs = 0,
                logRef = "logs/vrun_01.log", judgedAt = null, createdAt = 1000L,
            )
        )
        val result = TestResultEntity(
            id = "tr_01",
            verificationRunId = "vrun_01",
            ordinal = 0,
            command = "./gradlew test",
            exitCode = 0,
            durationMs = 5000L,
            summaryLine = "All tests passed",
            failedTestNames = emptyList(),
            parserUsed = TestParserType.GRADLE,
        )
        database.verificationDao().insertTestResults(listOf(result))

        val results = database.verificationDao().observeTestResults("vrun_01").first()
        assertEquals(1, results.size)
        assertEquals(0, results.first().exitCode)
        assertEquals(TestParserType.GRADLE, results.first().parserUsed)
    }

    @Test
    fun `cost record DAO - insert and observe for run`() = runTest {
        val cost = CostRecordEntity(
            id = "cost_01",
            runId = "run_01",
            projectId = "proj_01",
            conversationId = "conv_01",
            inputTokens = 1500L,
            outputTokens = 400L,
            cacheReadTokens = 100L,
            cacheCreationTokens = 50L,
            costUsdMicros = 8500L,
            isEstimated = true,
            modelId = "claude-sonnet-4-5",
            providerId = "prov_anthropic",
            inputPricePerMtok = 3000L,
            outputPricePerMtok = 15000L,
        )
        database.costRecordDao().insertCostRecord(cost)

        val costs = database.costRecordDao().observeCostRecordsForRun("run_01").first()
        assertEquals(1, costs.size)
        assertEquals(8500L, costs.first().costUsdMicros)
    }

    @Test
    fun `cost record DAO - observe total cost for project`() = runTest {
        val cost1 = CostRecordEntity(
            id = "c1", runId = "r1", projectId = "p1", conversationId = "c1",
            inputTokens = 1000L, outputTokens = 200L, cacheReadTokens = 0, cacheCreationTokens = 0,
            costUsdMicros = 5000L, isEstimated = false, modelId = "m1", providerId = "prov1",
            inputPricePerMtok = 3000L, outputPricePerMtok = 15000L,
        )
        val cost2 = CostRecordEntity(
            id = "c2", runId = "r2", projectId = "p1", conversationId = "c1",
            inputTokens = 500L, outputTokens = 100L, cacheReadTokens = 0, cacheCreationTokens = 0,
            costUsdMicros = 3000L, isEstimated = true, modelId = "m1", providerId = "prov1",
            inputPricePerMtok = 3000L, outputPricePerMtok = 15000L,
        )
        database.costRecordDao().insertCostRecord(cost1)
        database.costRecordDao().insertCostRecord(cost2)

        val total = database.costRecordDao().observeTotalCostForProject("p1").first()
        assertEquals(8000L, total ?: 0L)
    }

    @Test
    fun `run DAO - insert and observe active runs`() = runTest {
        val run1 = RunEntity(
            id = "run_01", projectId = "p1", conversationId = "c1", turnId = "t1",
            backendId = "native", backendProfile = "PROOT", state = RunState.RUNNING,
            taskText = "Test", startedAt = 1000L,
        )
        val run2 = RunEntity(
            id = "run_02", projectId = "p1", conversationId = "c1", turnId = "t2",
            backendId = "native", backendProfile = "PROOT", state = RunState.DONE,
            taskText = "Test", startedAt = 1000L,
        )
        database.runDao().insertRun(run1)
        database.runDao().insertRun(run2)

        val activeRuns = database.runDao().observeActiveRuns().first()
        assertEquals(1, activeRuns.size)
        assertEquals(RunState.RUNNING, activeRuns.first().state)
    }

    @Test
    fun `provider DAO - insert and observe enabled`() = runTest {
        val provider = ProviderEntity(
            id = "prov_01",
            name = "Anthropic",
            kind = ProviderKind.ANTHROPIC,
            baseUrl = "https://api.anthropic.com",
            pathTemplate = "/v1/messages",
            secretProfileId = "sec_01",
            headersJson = null,
            isEnabled = true,
            isDefault = true,
            lastTestedAt = 1000L,
            lastTestResult = "OK",
            discoveredModels = listOf("claude-sonnet-4-5"),
        )
        database.providerDao().insertProvider(provider)

        val providers = database.providerDao().observeEnabledProviders().first()
        assertEquals(1, providers.size)
        assertTrue(providers.first().isEnabled)
    }

    @Test
    fun `provider DAO - default provider`() = runTest {
        val provider1 = ProviderEntity(
            id = "p1", name = "P1", kind = ProviderKind.ANTHROPIC,
            secretProfileId = "sec1", isEnabled = true, isDefault = true,
            lastTestedAt = 1000L, lastTestResult = "OK", discoveredModels = emptyList(),
        )
        val provider2 = ProviderEntity(
            id = "p2", name = "P2", kind = ProviderKind.OPENAI_CHAT,
            secretProfileId = "sec2", isEnabled = true, isDefault = false,
            lastTestedAt = 1000L, lastTestResult = "OK", discoveredModels = emptyList(),
        )
        database.providerDao().insertProvider(provider1)
        database.providerDao().insertProvider(provider2)

        val default = database.providerDao().getDefaultProvider()
        assertNotNull(default)
        assertEquals("p1", default?.id)
    }

    @Test
    fun `secret profile DAO - insert and get`() = runTest {
        val secret = SecretProfileEntity(
            id = "sec_01",
            name = "Default",
            cipherText = "encrypted",
            iv = "iv",
            keyAlias = "cca_key",
            hint = "...1234",
            createdAt = 1000L,
            lastUsedAt = 1000L,
        )
        database.secretProfileDao().insertSecretProfile(secret)

        val retrieved = database.secretProfileDao().getSecretProfileById("sec_01")
        assertNotNull(retrieved)
        assertEquals("cca_key", retrieved?.keyAlias)
    }

    @Test
    fun `skill DAO - insert and observe global installs`() = runTest {
        val skill = SkillEntity(
            id = "skill_01", name = "test-skill", description = "Test skill",
            sourceKind = SkillSourceKind.BUILTIN, sourceUrl = null, sourceRef = null,
            contentHash = "hash123", isValid = true, validationError = null,
            createdAt = 1000L, updatedAt = 1000L,
        )
        val install = SkillInstallEntity(
            skillId = "skill_01", scope = SkillInstallScope.GLOBAL,
            projectId = null, installPath = "/skills/test-skill",
            isEnabled = true, installedAt = 1000L,
        )
        database.skillDao().insertSkill(skill)
        database.skillDao().insertSkillInstall(install)

        val globalInstalls = database.skillDao().observeGlobalInstalls().first()
        assertEquals(1, globalInstalls.size)
        assertEquals(SkillInstallScope.GLOBAL, globalInstalls.first().scope)
    }

    @Test
    fun `remote target DAO - insert and observe all`() = runTest {
        val target = RemoteTargetEntity(
            id = "rt_01", projectId = null, name = "My Server",
            kind = RemoteTargetKind.SSH, host = "192.168.1.100", port = 22,
            user = "user", secretProfileId = "sec_01",
            status = RemoteTargetStatus.ONLINE, lastProbeAt = 1000L,
            lastProbeMessage = "OK", capabilitiesJson = "{}",
            isEnabled = true,
        )
        database.remoteTargetDao().insertRemoteTarget(target)

        val enabled = database.remoteTargetDao().observeAllRemoteTargets().first()
        assertEquals(1, enabled.size)
        assertTrue(enabled.first().isEnabled)
    }

    @Test
    fun `session log DAO - insert and observe for run`() = runTest {
        val log = SessionLogEntryEntity(
            id = "log_01",
            timestamp = 1000L,
            runId = "run_01",
            projectId = "proj_01",
            conversationId = "conv_01",
            category = SessionLogCategory.COMMAND,
            severity = SessionLogSeverity.INFO,
            message = "Executed gradle check",
            detailJson = "{}",
            durationMs = 2500L,
        )
        database.sessionLogDao().insertLogEntry(log)

        val logs = database.sessionLogDao().observeLogsForRun("run_01").first()
        assertEquals(1, logs.size)
        assertEquals(SessionLogCategory.COMMAND, logs.first().category)
    }

    @Test
    fun `notification event DAO - insert and observe unread`() = runTest {
        val notification = NotificationEventEntity(
            id = "notif_01",
            runId = "run_01",
            channel = NotificationChannel.RUN_DONE,
            titleDe = "Lauf abgeschlossen",
            titleEn = "Run completed",
            bodyDe = "Der Lauf ist fertig",
            bodyEn = "The run is done",
            actionJson = null,
            deliveredAt = null,
            readAt = null,
        )
        database.notificationEventDao().insertNotification(notification)

        val unread = database.notificationEventDao().observeUnreadNotifications().first()
        assertEquals(1, unread.size)
        assertNull(unread.first().readAt)
    }

    @Test
    fun `app setting DAO - insert and get`() = runTest {
        val setting = AppSettingEntity(
            key = "terminal_font_size",
            value = "14",
            updatedAt = 1000L,
        )
        database.appSettingDao().insertOrUpdateSetting(setting)

        val retrieved = database.appSettingDao().getSetting("terminal_font_size")
        assertNotNull(retrieved)
        assertEquals("14", retrieved?.value)
    }

    @Test
    fun `branch DAO - insert and observe for project`() = runTest {
        val branch = BranchEntity(
            projectId = "proj_01",
            name = "task/feature",
            baseBranch = "main",
            headCommitSha = "abc123",
            isMerged = false,
            createdAt = 1000L,
            updatedAt = 1000L,
        )
        database.branchDao().insertBranch(branch)

        val branches = database.branchDao().observeBranchesForProject("proj_01").first()
        assertEquals(1, branches.size)
        assertEquals("task/feature", branches.first().name)
    }

    @Test
    fun `checkpoint DAO - insert and observe by run`() = runTest {
        val checkpoint = CheckpointEntity(
            id = "cp_01",
            runId = "run_01",
            stepId = "step_01",
            gitCommitSha = "abc123",
            description = "Architecture verified",
            createdAt = 1000L,
        )
        database.checkpointDao().insertCheckpoint(checkpoint)

        val checkpoints = database.checkpointDao().observeCheckpointsForRun("run_01").first()
        assertEquals(1, checkpoints.size)
        assertEquals("abc123", checkpoints.first().gitCommitSha)
    }
}
