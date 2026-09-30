package dev.ccandroid.domain.usecase

import dev.ccandroid.core.AppError
import dev.ccandroid.core.IdGenerator
import dev.ccandroid.core.Outcome
import dev.ccandroid.core.OutcomeException
import dev.ccandroid.core.tryCatch
import dev.ccandroid.domain.CostRecord
import dev.ccandroid.domain.Plan
import dev.ccandroid.domain.PlanStep
import dev.ccandroid.domain.PlanStepState
import dev.ccandroid.domain.ProjectSetting
import dev.ccandroid.domain.TestParserType
import dev.ccandroid.domain.VerificationRun
import dev.ccandroid.domain.VerificationState

/**
 * Use cases for budget, plans, and verification.
 *
 * Per docs/02-architecture/data-model.md and tasks P2-5:
 * - CostRecord: record and query cost
 * - Plan/PlanStep: create, update, approve, reorder
 * - VerificationRun: run, detect commands, judge
 */

public interface CostRepository {
    suspend fun insert(record: CostRecord): Outcome<Unit>
    suspend fun getByRunId(runId: String): Outcome<List<CostRecord>>
    suspend fun getByProjectId(projectId: String): Outcome<List<CostRecord>>
    suspend fun getTotalCost(projectId: String): Outcome<Long>
    suspend fun getTotalCostByConversation(conversationId: String): Outcome<Long>
}

public interface PlanRepository {
    suspend fun insert(plan: Plan): Outcome<Unit>
    suspend fun getById(id: String): Outcome<Plan?>
    suspend fun getByRunId(runId: String): Outcome<Plan?>
    suspend fun update(plan: Plan): Outcome<Unit>
}

public interface VerificationRepository {
    suspend fun insert(verificationRun: VerificationRun): Outcome<Unit>
    suspend fun getById(id: String): Outcome<VerificationRun?>
    suspend fun getByRunId(runId: String): Outcome<List<VerificationRun>>
    suspend fun update(verificationRun: VerificationRun): Outcome<Unit>
}

public interface ProjectSettingRepository {
    suspend fun getSetting(projectId: String): Outcome<ProjectSetting?>
    suspend fun updateSetting(setting: ProjectSetting): Outcome<Unit>
}

/**
 * Records a cost entry for a run.
 */
public class RecordCostUseCase(
    private val costRepo: CostRepository,
    private val idGenerator: IdGenerator,
) {
    public suspend operator fun invoke(
        runId: String,
        projectId: String,
        conversationId: String,
        inputTokens: Long,
        outputTokens: Long,
        cacheReadTokens: Long,
        cacheCreationTokens: Long,
        costUsdMicros: Long,
        isEstimated: Boolean,
        modelId: String,
        providerId: String,
        inputPricePerMtok: Long,
        outputPricePerMtok: Long,
    ): Outcome<CostRecord> = tryCatch {
        val record = CostRecord(
            id = idGenerator.newId("cost"),
            runId = runId,
            projectId = projectId,
            conversationId = conversationId,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            cacheReadTokens = cacheReadTokens,
            cacheCreationTokens = cacheCreationTokens,
            costUsdMicros = costUsdMicros,
            isEstimated = isEstimated,
            modelId = modelId,
            providerId = providerId,
            inputPricePerMtok = inputPricePerMtok,
            outputPricePerMtok = outputPricePerMtok,
        )
        costRepo.insert(record).getOrThrow()
        record
    }
}

/**
 * Gets the total cost for a run.
 */
public class GetRunCostUseCase(
    private val costRepo: CostRepository,
) {
    public suspend operator fun invoke(runId: String): Outcome<Long> = tryCatch {
        val records = costRepo.getByRunId(runId).getOrThrow()
        records.sumOf { it.costUsdMicros }
    }
}

/**
 * Gets the total cost for a project.
 */
public class GetProjectCostUseCase(
    private val costRepo: CostRepository,
) {
    public suspend operator fun invoke(projectId: String): Outcome<Long> = costRepo.getTotalCost(projectId)
}

/**
 * Gets the total cost for a conversation.
 */
public class GetConversationCostUseCase(
    private val costRepo: CostRepository,
) {
    public suspend operator fun invoke(conversationId: String): Outcome<Long> = tryCatch {
        costRepo.getTotalCostByConversation(conversationId).getOrThrow()
    }
}

/**
 * Checks if a project's cost advisory threshold has been exceeded.
 */
public class CheckCostAdvisoryThresholdUseCase(
    private val getProjectCost: GetProjectCostUseCase,
) {
    public suspend operator fun invoke(
        projectId: String,
        settings: ProjectSetting,
    ): Outcome<Boolean> = tryCatch {
        val threshold = settings.costAdvisoryThresholdUsd
        if (threshold == null) {
            false
        } else {
            val currentCost = getProjectCost(projectId).getOrThrow()
            currentCost >= threshold
        }
    }
}

/**
 * Creates a new plan for a run.
 */
public class CreatePlanUseCase(
    private val idGenerator: IdGenerator,
) {
    public operator fun invoke(
        runId: String,
        steps: List<PlanStep>,
    ): Outcome<Plan> {
        if (steps.isEmpty()) {
            return Outcome.Failure(AppError.Simple(
                code = dev.ccandroid.core.ErrorCode.VERIFY_NO_COMMANDS,
                messageDe = "Der Plan ist leer.",
                messageEn = "The plan is empty.",
                retryable = false,
            ))
        }
        val plan = Plan(
            id = idGenerator.newId("plan"),
            runId = runId,
            steps = steps,
            createdAt = System.currentTimeMillis(),
        )
        return Outcome.Success(plan)
    }
}

/**
 * Gets a plan by ID.
 */
public class GetPlanUseCase(
    private val planRepo: PlanRepository,
) {
    public suspend operator fun invoke(planId: String): Outcome<Plan?> = planRepo.getById(planId)
}

/**
 * Gets a plan by run ID.
 */
public class GetPlanByRunIdUseCase(
    private val planRepo: PlanRepository,
) {
    public suspend operator fun invoke(runId: String): Outcome<Plan?> = planRepo.getByRunId(runId)
}

/**
 * Updates a plan step's state.
 */
public class UpdatePlanStepUseCase(
    private val planRepo: PlanRepository,
) {
    public suspend operator fun invoke(
        planId: String,
        stepId: String,
        newState: PlanStepState,
        startedAt: Long? = null,
        endedAt: Long? = null,
        attemptCount: Int? = null,
    ): Outcome<Plan> = tryCatch {
        val plan = planRepo.getById(planId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Plan nicht gefunden.",
                messageEn = "Plan not found.",
            ))

        val updatedSteps = plan.steps.map { step ->
            if (step.id == stepId) {
                step.copy(
                    state = newState,
                    startedAt = startedAt?.takeIf { it != 0L } ?: step.startedAt,
                    endedAt = endedAt?.takeIf { it != 0L } ?: step.endedAt,
                    attemptCount = attemptCount?.takeIf { it >= 0 } ?: step.attemptCount,
                )
            } else {
                step
            }
        } as List<PlanStep>
        val updatedPlan = plan.copy(steps = updatedSteps)
        planRepo.update(updatedPlan).getOrThrow()
        updatedPlan
    }
}

/**
 * Adds a step to a plan at a specific position.
 */
public class AddPlanStepUseCase(
    private val planRepo: PlanRepository,
) {
    public suspend operator fun invoke(
        planId: String,
        step: PlanStep,
        position: Int,
    ): Outcome<Plan> = tryCatch {
        val plan = planRepo.getById(planId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Plan nicht gefunden.",
                messageEn = "Plan not found.",
            ))

        val updatedSteps = plan.steps.toMutableList().apply {
            add(position, step)
        }
        // Re-ordinal all steps
        val reindexed = updatedSteps.mapIndexed { index, s ->
            s.copy(ordinal = index)
        } as List<PlanStep>
        val updatedPlan = plan.copy(steps = reindexed)
        planRepo.update(updatedPlan).getOrThrow()
        updatedPlan
    }
}

/**
 * Removes a step from a plan (only pending steps can be removed).
 */
public class RemovePlanStepUseCase(
    private val planRepo: PlanRepository,
) {
    public suspend operator fun invoke(
        planId: String,
        stepId: String,
    ): Outcome<Plan> = tryCatch {
        val plan = planRepo.getById(planId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Plan nicht gefunden.",
                messageEn = "Plan not found.",
            ))

        val step = plan.steps.find { it.id == stepId }
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Schritt nicht gefunden.",
                messageEn = "Step not found.",
            ))

        if (step.state != PlanStepState.PENDING) {
            throw OutcomeException(AppError.Simple(
                code = dev.ccandroid.core.ErrorCode.VERIFY_FAILED,
                messageDe = "Nur wartende Schritte können entfernt werden.",
                messageEn = "Only pending steps can be removed.",
                retryable = false,
            ))
        }

        val updatedSteps = plan.steps.filter { it.id != stepId }
            .mapIndexed { index, s -> s.copy(ordinal = index) } as List<PlanStep>

        val updatedPlan = plan.copy(steps = updatedSteps)
        planRepo.update(updatedPlan).getOrThrow()
        updatedPlan
    }
}

/**
 * Reorders steps in a plan.
 */
public class ReorderPlanStepsUseCase(
    private val planRepo: PlanRepository,
) {
    public suspend operator fun invoke(
        planId: String,
        newOrder: List<String>, // step IDs in new order
    ): Outcome<Plan> = tryCatch {
        val plan = planRepo.getById(planId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Plan nicht gefunden.",
                messageEn = "Plan not found.",
            ))

        val stepMap = plan.steps.associateBy { it.id }
        val reordered = newOrder.mapNotNull { stepMap[it] }
            .mapIndexed { index, s -> s.copy(ordinal = index) } as List<PlanStep>

        if (reordered.size != plan.steps.size) {
            throw OutcomeException(AppError.Simple(
                code = dev.ccandroid.core.ErrorCode.VERIFY_FAILED,
                messageDe = "Ungültige Reihenfolge.",
                messageEn = "Invalid order.",
                retryable = false,
            ))
        }

        val updatedPlan = plan.copy(steps = reordered)
        planRepo.update(updatedPlan).getOrThrow()
        updatedPlan
    }
}

/**
 * Approves a plan (user confirms the plan).
 */
public class ApprovePlanUseCase(
    private val planRepo: PlanRepository,
) {
    public suspend operator fun invoke(planId: String): Outcome<Plan> = tryCatch {
        val plan = planRepo.getById(planId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Plan nicht gefunden.",
                messageEn = "Plan not found.",
            ))

        // Mark first pending step as active if any
        var firstPendingFound = false
        val updatedSteps = plan.steps.map { step ->
            if (step.state == PlanStepState.PENDING && !firstPendingFound) {
                firstPendingFound = true
                step.copy(state = PlanStepState.ACTIVE, startedAt = System.currentTimeMillis())
            } else step
        }
        val updatedPlan = plan.copy(steps = updatedSteps)
        planRepo.update(updatedPlan).getOrThrow()
        updatedPlan
    }
}

/**
 * Proposes a plan change during a run (at AUTO_WITH_CHECKPOINTS and FULL_AUTO).
 */
public class ProposePlanChangeUseCase(
    private val planRepo: PlanRepository,
) {
    public suspend operator fun invoke(
        planId: String,
        newStep: PlanStep,
        position: Int,
        reason: String,
    ): Outcome<Plan> = tryCatch {
        val plan = planRepo.getById(planId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Plan nicht gefunden.",
                messageEn = "Plan not found.",
            ))

        val proposedStep = newStep.copy(
            titleDe = "${newStep.titleDe} (vorgeschlagen: $reason)",
            titleEn = "${newStep.titleEn} (proposed: $reason)",
        )

        val updatedSteps = plan.steps.toMutableList().apply {
            add(position, proposedStep)
        }
        val reindexed = updatedSteps.mapIndexed { index, s -> s.copy(ordinal = index) }
        val updatedPlan = plan.copy(steps = reindexed)
        planRepo.update(updatedPlan).getOrThrow()
        updatedPlan
    }
}

/**
 * Detects verification commands for a project.
 */
public class DetectVerificationCommandsUseCase {
    public operator fun invoke(projectPath: String): Outcome<List<String>> {
        val commands = mutableListOf<String>()

        // Gradle
        val gradleFiles = listOf("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts")
        if (gradleFiles.any { java.io.File(projectPath, it).exists() }) {
            commands.add("./gradlew assembleDebug")
            commands.add("./gradlew test")
            commands.add("./gradlew lint")
        }

        // npm
        val packageJson = java.io.File(projectPath, "package.json")
        if (packageJson.exists()) {
            val content = packageJson.readText()
            if (content.contains("\"test\"")) commands.add("npm test")
            if (content.contains("\"build\"")) commands.add("npm run build")
            if (content.contains("\"lint\"")) commands.add("npm run lint")
        }

        // Python/pytest
        if (java.io.File(projectPath, "pyproject.toml").exists() || java.io.File(projectPath, "pytest.ini").exists()) {
            commands.add("pytest")
        } else if (java.io.File(projectPath, "requirements.txt").exists() &&
                   java.io.File(projectPath, "tests").exists()) {
            commands.add("pytest -q")
        }

        // Cargo (Rust)
        if (java.io.File(projectPath, "Cargo.toml").exists()) {
            commands.add("cargo build")
            commands.add("cargo test")
            commands.add("cargo clippy")
        }

        // Go
        if (java.io.File(projectPath, "go.mod").exists()) {
            commands.add("go build ./...")
            commands.add("go test ./...")
        }

        // Maven
        if (java.io.File(projectPath, "pom.xml").exists()) {
            commands.add("mvn -q test")
        }

        // CMake
        if (java.io.File(projectPath, "CMakeLists.txt").exists()) {
            commands.add("cmake --build .")
            commands.add("ctest")
        }

        return Outcome.Success(commands)
    }
}

/**
 * Runs verification commands for a project.
 */
public class RunVerificationUseCase(
    private val verificationRepo: VerificationRepository,
    private val idGenerator: IdGenerator,
) {
    public suspend operator fun invoke(
        runId: String,
        attempt: Int,
        commands: List<String>,
    ): Outcome<VerificationRun> = tryCatch {
        val verificationRun = VerificationRun(
            id = idGenerator.newId("vrun"),
            runId = runId,
            attempt = attempt,
            state = VerificationState.RUNNING,
            commandCount = commands.size,
            durationMs = 0,
            createdAt = System.currentTimeMillis(),
        )
        verificationRepo.insert(verificationRun).getOrThrow()
        verificationRun
    }
}

/**
 * Updates a verification run with results.
 */
public class UpdateVerificationRunUseCase(
    private val verificationRepo: VerificationRepository,
) {
    public suspend operator fun invoke(
        verificationRunId: String,
        newState: VerificationState,
        durationMs: Long,
        logRef: String?,
    ): Outcome<VerificationRun> = tryCatch {
        val run = verificationRepo.getById(verificationRunId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.VERIFY_FAILED,
                messageDe = "Verifizierungslauf nicht gefunden.",
                messageEn = "Verification run not found.",
            ))

        val updated = run.copy(
            state = newState,
            durationMs = durationMs,
            logRef = logRef,
        )
        verificationRepo.update(updated).getOrThrow()
        updated
    }
}

/**
 * Judges a verification run based on exit codes.
 */
public class JudgeVerificationRunUseCase {
    public operator fun invoke(
        verificationRun: VerificationRun,
        results: List<VerificationRunResult>,
    ): Outcome<VerificationState> {
        if (results.isEmpty()) {
            return Outcome.Success(VerificationState.UNVERIFIED)
        }

        val hasError = results.any { it.exitCode != 0 && it.exitCode != 1 } // 1 = test failure, >1 = error
        val hasFailure = results.any { it.exitCode == 1 }

        return when {
            hasError -> Outcome.Success(VerificationState.ERROR)
            hasFailure -> Outcome.Success(VerificationState.FAILED)
            else -> Outcome.Success(VerificationState.PASSED)
        }
    }
}

public data class VerificationRunResult(
    val command: String,
    val exitCode: Int,
    val durationMs: Long,
    val summaryLine: String,
    val failedTestNames: List<String>,
    val parserUsed: TestParserType,
)

/**
 * Gets the retry budget for a project.
 */
public class GetRetryBudgetUseCase(
    private val settingRepo: ProjectSettingRepository,
) {
    public suspend operator fun invoke(projectId: String): Outcome<Int> = tryCatch {
        val setting = settingRepo.getSetting(projectId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Projekt nicht gefunden.",
                messageEn = "Project not found.",
            ))
        setting.retryBudget
    }
}

/**
 * Updates the retry budget for a project.
 */
public class UpdateRetryBudgetUseCase(
    private val settingRepo: ProjectSettingRepository,
) {
    public suspend operator fun invoke(
        projectId: String,
        newBudget: Int,
    ): Outcome<ProjectSetting> = tryCatch {
        val setting = settingRepo.getSetting(projectId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Projekt nicht gefunden.",
                messageEn = "Project not found.",
            ))

        val allowedBudgets = setOf(3, 10, 50, -1) // -1 = unlimited
        if (newBudget !in allowedBudgets) {
            throw OutcomeException(AppError.Simple(
                code = dev.ccandroid.core.ErrorCode.VERIFY_FAILED,
                messageDe = "Ungültiges Budget. Erlaubt: 3, 10, 50, unbegrenzt.",
                messageEn = "Invalid budget. Allowed: 3, 10, 50, unlimited.",
                retryable = false,
            ))
        }

        val updated = setting.copy(
            retryBudget = newBudget,
            updatedAt = System.currentTimeMillis(),
        )
        settingRepo.updateSetting(updated).getOrThrow()
        updated
    }
}

/**
 * Gets the cost breakdown for a run (estimated vs actual).
 */
public class GetCostBreakdownUseCase(
    private val costRepo: CostRepository,
) {
    public suspend operator fun invoke(runId: String): Outcome<CostBreakdown> = tryCatch {
        val records = costRepo.getByRunId(runId).getOrThrow()
        val estimated = records.filter { it.isEstimated }.sumOf { it.costUsdMicros }
        val actual = records.filter { !it.isEstimated }.sumOf { it.costUsdMicros }
        val total = records.sumOf { it.costUsdMicros }
        val byModel = records.groupBy { it.modelId }.mapValues { (_, v) -> v.sumOf { it.costUsdMicros } }
        val byProvider = records.groupBy { it.providerId }.mapValues { (_, v) -> v.sumOf { it.costUsdMicros } }

        CostBreakdown(
            estimatedUsdMicros = estimated,
            actualUsdMicros = actual,
            totalUsdMicros = total,
            byModel = byModel,
            byProvider = byProvider,
            recordCount = records.size,
        )
    }
}

public data class CostBreakdown(
    val estimatedUsdMicros: Long,
    val actualUsdMicros: Long,
    val totalUsdMicros: Long,
    val byModel: Map<String, Long>,
    val byProvider: Map<String, Long>,
    val recordCount: Int,
)