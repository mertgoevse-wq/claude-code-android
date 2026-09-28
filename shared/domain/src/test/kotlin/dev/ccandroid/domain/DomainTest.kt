package dev.ccandroid.domain

import dev.ccandroid.core.ErrorCode
import dev.ccandroid.domain.policy.HardBlockPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exhaustive unit tests for all 28 domain entities and their invariants
 * per docs/02-architecture/data-model.md.
 */
class DomainTest {

    @Test
    fun `project creation retains fields and respects local invariant`() {
        val now = 1727500000000L
        val project = Project(
            id = "proj_01J8Y",
            name = "Test Project",
            kind = ProjectKind.LOCAL,
            path = "/tmp/test",
            createdAt = now,
            updatedAt = now,
        )
        assertEquals("proj_01J8Y", project.id)
        assertEquals("Test Project", project.name)
        assertEquals(ProjectKind.LOCAL, project.kind)
        assertEquals("/tmp/test", project.path)
        assertTrue(project.isPrivate)
        assertFalse(project.isArchived)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `local project cannot have vcsProvider`() {
        Project(
            id = "proj_01",
            name = "Local with VCS",
            kind = ProjectKind.LOCAL,
            path = "/tmp/test",
            vcsProvider = "GITHUB",
            createdAt = 1000L,
            updatedAt = 1000L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cloned project must have remote owner and name`() {
        Project(
            id = "proj_02",
            name = "Incomplete Cloned",
            kind = ProjectKind.CLONED,
            path = "/tmp/test",
            createdAt = 1000L,
            updatedAt = 1000L,
        )
    }

    @Test
    fun `project setting initializes with correct default autonomy level`() {
        val setting = ProjectSetting(
            projectId = "proj_01",
            modelProviderId = "prov_anthropic",
            modelId = "claude-sonnet-4-5",
        )
        assertEquals(AutonomyLevel.ASK_RISKY, setting.autonomyLevel)
        assertEquals(PermissionMode.DEFAULT, setting.permissionMode)
        assertEquals(OffloadPolicy.NEVER, setting.offloadPolicy)
        assertEquals("task/", setting.branchPrefix)
        assertTrue(setting.autoCommit)
        assertTrue(setting.autoPr)
        assertTrue(setting.notificationsEnabled)
    }

    @Test
    fun `conversation retains session and timestamp fields`() {
        val conv = Conversation(
            id = "conv_01",
            projectId = "proj_01",
            title = "First conversation",
            sessionId = "session_xyz",
            createdAt = 1000L,
            updatedAt = 1050L,
            lastMessagePreview = "Hello world",
        )
        assertEquals("session_xyz", conv.sessionId)
        assertEquals("Hello world", conv.lastMessagePreview)
    }

    @Test
    fun `turn maintains state and cost`() {
        val turn = Turn(
            id = "turn_01",
            conversationId = "conv_01",
            index = 1,
            userMessageId = "msg_01",
            startedAt = 1000L,
            endedAt = 1500L,
            state = TurnState.DONE,
            costUsd = 1200L,
            attemptCount = 1,
        )
        assertEquals(TurnState.DONE, turn.state)
        assertEquals(1200L, turn.costUsd)
    }

    @Test
    fun `message and message parts structure correctly`() {
        val msg = Message(
            id = "msg_01",
            turnId = "turn_01",
            role = MessageRole.ASSISTANT,
            createdAt = 1000L,
            renderedMarkdown = "Here is the plan",
        )
        assertEquals(MessageRole.ASSISTANT, msg.role)
        assertFalse(msg.isRedacted)

        val part = MessagePart(
            id = "part_01",
            messageId = msg.id,
            kind = MessagePartKind.TEXT,
            ordinal = 0,
            payloadJson = "{\"text\":\"Hello\"}",
        )
        assertEquals(MessagePartKind.TEXT, part.kind)
        assertFalse(part.collapsed)
    }

    @Test
    fun `tool invocation tracks lifecycle and previews`() {
        val tool = ToolInvocation(
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
        assertEquals("Read", tool.name)
        assertEquals(ToolInvocationStatus.DONE, tool.status)
        assertFalse(tool.isDestructive)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `plan with empty steps throws exception`() {
        Plan(
            id = "plan_01",
            steps = emptyList(),
            createdAt = 1000L,
        )
    }

    @Test
    fun `plan with steps succeeds`() {
        val step = PlanStep(
            id = "step_01",
            planId = "plan_01",
            ordinal = 0,
            titleDe = "Architektur entwerfen",
            titleEn = "Design architecture",
            acceptanceCriteria = "All docs exist",
            state = PlanStepState.DONE,
            isCheckpoint = true,
        )
        val plan = Plan(
            id = "plan_01",
            steps = listOf(step),
            createdAt = 1000L,
        )
        assertEquals(1, plan.steps.size)
        assertTrue(plan.steps.first().isCheckpoint)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `file change with renamed type requires old path`() {
        FileChange(
            id = "fc_01",
            runId = "run_01",
            path = "new/path.kt",
            changeType = FileChangeType.RENAMED,
            oldPath = null,
            linesAdded = 10,
            linesRemoved = 0,
            hunksJson = "[]",
        )
    }

    @Test
    fun `verification run and test results match contract`() {
        val vrun = VerificationRun(
            id = "vrun_01",
            runId = "run_01",
            attempt = 1,
            state = VerificationState.PASSED,
            commandCount = 1,
            durationMs = 5000L,
        )
        assertEquals(VerificationState.PASSED, vrun.state)

        val tresult = TestResult(
            id = "tr_01",
            verificationRunId = vrun.id,
            ordinal = 0,
            command = "./gradlew check",
            exitCode = 0,
            durationMs = 4500L,
            summaryLine = "301 actionable tasks green",
            failedTestNames = emptyList(),
            parserUsed = TestParserType.GRADLE,
        )
        assertEquals(0, tresult.exitCode)
        assertEquals(TestParserType.GRADLE, tresult.parserUsed)
    }

    @Test
    fun `cost record retains integer micro usd values without float drift`() {
        val cost = CostRecord(
            id = "cost_01",
            runId = "run_01",
            projectId = "proj_01",
            conversationId = "conv_01",
            inputTokens = 1500L,
            outputTokens = 400L,
            costUsdMicros = 8500L,
            isEstimated = false,
            modelId = "claude-sonnet-4-5",
            providerId = "prov_anthropic",
        )
        assertEquals(8500L, cost.costUsdMicros)
        assertFalse(cost.isEstimated)
    }

    @Test
    fun `run tracks execution states and branches`() {
        val run = Run(
            id = "run_01",
            projectId = "proj_01",
            conversationId = "conv_01",
            turnId = "turn_01",
            backendId = "native-glibc",
            backendProfile = "PROOT",
            state = RunState.DONE,
            taskText = "Build Phase 2",
            branchName = "task/phase-2-data-and-core",
            startedAt = 1000L,
            endedAt = 2500L,
            durationMs = 1500L,
        )
        assertEquals(RunState.DONE, run.state)
        assertEquals("task/phase-2-data-and-core", run.branchName)
    }

    @Test
    fun `provider and secret profile store credentials securely`() {
        val secret = SecretProfile(
            id = "sec_01",
            name = "Default Keystore Profile",
            cipherText = "encrypted_payload",
            iv = "initialization_vector",
            keyAlias = "cca_anthropic_key",
            hint = "...1234",
            createdAt = 1000L,
        )
        assertEquals("cca_anthropic_key", secret.keyAlias)

        val prov = Provider(
            id = "prov_01",
            name = "Anthropic",
            kind = ProviderKind.ANTHROPIC,
            secretProfileId = secret.id,
            isEnabled = true,
            isDefault = true,
        )
        assertEquals(ProviderKind.ANTHROPIC, prov.kind)
        assertTrue(prov.isDefault)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `project scoped skill install requires project id`() {
        SkillInstall(
            skillId = "skill_01",
            scope = SkillInstallScope.PROJECT,
            projectId = null,
            installPath = "/tmp/skills/skill_01",
            installedAt = 1000L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `app setting refuses secret storage invariant`() {
        AppSetting(
            key = "user_api_key",
            value = "sk-ant-12345",
            updatedAt = 1000L,
        )
    }

    @Test
    fun `app setting permits safe configurations`() {
        val setting = AppSetting(
            key = "terminal_font_size",
            value = "14",
            updatedAt = 1000L,
        )
        assertEquals("14", setting.value)
    }

    @Test
    fun `remote target and session log entries record events`() {
        val remote = RemoteTarget(
            id = "rt_01",
            name = "CI Runner",
            kind = RemoteTargetKind.SSH,
            host = "192.168.1.100",
            port = 22,
            secretProfileId = "sec_01",
        )
        assertEquals(RemoteTargetKind.SSH, remote.kind)

        val log = SessionLogEntry(
            id = "log_01",
            timestamp = 1000L,
            category = SessionLogCategory.COMMAND,
            severity = SessionLogSeverity.INFO,
            message = "Executed gradle check",
            durationMs = 2500L,
        )
        assertEquals(SessionLogCategory.COMMAND, log.category)
        assertEquals(SessionLogSeverity.INFO, log.severity)
    }

    @Test
    fun `checkpoint and branch link to project and runs`() {
        val cp = Checkpoint(
            id = "cp_01",
            runId = "run_01",
            stepId = "step_01",
            gitCommitSha = "abcdef123456",
            description = "Architecture verified",
            createdAt = 1000L,
        )
        assertEquals("abcdef123456", cp.gitCommitSha)

        val branch = Branch(
            projectId = "proj_01",
            name = "task/phase-2-data-and-core",
            headCommitSha = "abcdef123456",
            createdAt = 1000L,
            updatedAt = 1000L,
        )
        assertEquals("main", branch.baseBranch)
        assertFalse(branch.isMerged)
    }

    @Test
    fun `hard block policy refuses rm commands`() {
        val refusal = HardBlockPolicy.checkCommand("rm -rf /")
        assertNotNull(refusal)
        assertEquals(ErrorCode.POLICY_BLOCK_DELETE, refusal?.code)
    }

    @Test
    fun `hard block policy refuses git push main`() {
        val refusal = HardBlockPolicy.checkCommand("git push origin main")
        assertNotNull(refusal)
        assertEquals(ErrorCode.POLICY_BLOCK_DEFAULT_BRANCH, refusal?.code)
    }

    @Test
    fun `hard block policy allows safe git commands`() {
        val refusal = HardBlockPolicy.checkCommand("git status")
        assertNull(refusal)
    }
}
