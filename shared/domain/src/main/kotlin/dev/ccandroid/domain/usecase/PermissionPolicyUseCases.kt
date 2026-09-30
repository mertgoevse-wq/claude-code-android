package dev.ccandroid.domain.usecase

import dev.ccandroid.core.AppError
import dev.ccandroid.core.Outcome
import dev.ccandroid.core.OutcomeException
import dev.ccandroid.core.tryCatch
import dev.ccandroid.domain.AutonomyLevel
import dev.ccandroid.domain.PermissionMode
import dev.ccandroid.domain.ProjectSetting
import dev.ccandroid.domain.policy.HardBlockPolicy

/**
 * Use cases for permission policy and autonomy levels.
 *
 * Per docs/02-architecture/data-model.md §ProjectSetting and task P2-4:
 * - Get effective permission mode for an autonomy level
 * - Check if a tool is allowed/denied for a project
 * - Compute effective tool list (defaults + allowed - denied - hard blocks)
 * - Validate settings changes don't violate hard blocks
 * - Map autonomy level to CLI permission mode
 */

/**
 * Default tool sets per autonomy level.
 * Per docs/08-orchestration/permissions.md:
 * - ASK_EVERYTHING (default): Runs free: Nothing
 * - ASK_RISKY (acceptEdits): Runs free: Reads, greps, verification commands, subagents
 * - AUTO_WITH_CHECKPOINTS (bypassPermissions): Runs free: Everything permitted (at checkpoints)
 * - FULL_AUTO (bypassPermissions): Runs free: Everything permitted
 */
public object DefaultToolSets {
    public val ASK_EVERYTHING: List<String> = listOf(
        "Read", "Grep", "Glob", "LS",
    )
    public val ASK_RISKY: List<String> = listOf(
        "Read", "Grep", "Glob", "LS",
        "Task",  // subagent
    )
    public val AUTO_WITH_CHECKPOINTS: List<String> = listOf(
        "Read", "Grep", "Glob", "LS",
        "Bash", "Edit", "Write", "NotebookEdit",
        "Task", "TodoWrite", "TodoRead",
    )
    public val FULL_AUTO: List<String> = listOf(
        "Read", "Grep", "Glob", "LS",
        "Bash", "Edit", "Write", "NotebookEdit",
        "Task", "TodoWrite", "TodoRead",
        "WebFetch", "WebSearch",
    )

    public fun getDefaultTools(autonomyLevel: AutonomyLevel): List<String> = when (autonomyLevel) {
        AutonomyLevel.ASK_EVERYTHING -> ASK_EVERYTHING
        AutonomyLevel.ASK_RISKY -> ASK_RISKY
        AutonomyLevel.AUTO_WITH_CHECKPOINTS -> AUTO_WITH_CHECKPOINTS
        AutonomyLevel.FULL_AUTO -> FULL_AUTO
    }
}

/**
 * Hard block tools that are never allowed regardless of autonomy level.
 */
public object HardBlockTools {
    private val hardBlocked = setOf(
        "rm", "rm -rf", "git push --delete", "git branch -D",
        "git tag -d", "git reset --hard", "git clean -f",
        "git checkout main", "git checkout master",
    )

    public fun isHardBlocked(toolName: String): Boolean {
        return hardBlocked.any { toolName.startsWith(it) }
    }

    public fun getHardBlockedTools(): Set<String> = hardBlocked
}

/**
 * Gets the effective permission mode for an autonomy level.
 */
public class GetPermissionModeUseCase {
    public operator fun invoke(autonomyLevel: AutonomyLevel): PermissionMode = when (autonomyLevel) {
        AutonomyLevel.ASK_EVERYTHING -> PermissionMode.DEFAULT
        AutonomyLevel.ASK_RISKY -> PermissionMode.ACCEPT_NON_DESTRUCTIVE
        AutonomyLevel.AUTO_WITH_CHECKPOINTS -> PermissionMode.ACCEPT_NON_DESTRUCTIVE
        AutonomyLevel.FULL_AUTO -> PermissionMode.BYPASS_ALL
    }
}

/**
 * Checks if a tool call requires approval for the given project settings.
 */
public class CheckToolApprovalUseCase(
    private val getPermissionMode: GetPermissionModeUseCase = GetPermissionModeUseCase(),
) {
    // All known tools across all autonomy levels
    private val allKnownTools = setOf(
        "Read", "Grep", "Glob", "LS",
        "Bash", "Edit", "Write", "NotebookEdit", "Task",
        "TodoWrite", "TodoRead",
        "WebFetch", "WebSearch",
    )

    public operator fun invoke(
        settings: ProjectSetting,
        toolName: String,
        command: String? = null,
    ): Outcome<Boolean> {
        // Unknown tools always require approval at every level
        if (toolName !in allKnownTools) {
            return Outcome.Success(true)
        }

        // Denied tools always require approval at every level
        if (toolName in settings.deniedTools) {
            return Outcome.Success(true)
        }

        // Hard blocks always require approval (and are refused)
        if (HardBlockTools.isHardBlocked(toolName)) {
            return Outcome.Success(true)
        }
        if (command != null) {
            val hardBlockRefusal = HardBlockPolicy.checkCommand(command)
            if (hardBlockRefusal != null) {
                return Outcome.Success(true)
            }
        }

        val permissionMode = getPermissionMode(settings.autonomyLevel)

        return when (permissionMode) {
            PermissionMode.DEFAULT -> Outcome.Success(true) // Always ask
            PermissionMode.ACCEPT_NON_DESTRUCTIVE -> Outcome.Success(
                !isAllowedTool(settings, toolName)
            )
            PermissionMode.BYPASS_ALL -> Outcome.Success(false) // Never ask (except hard blocks, denied, unknown)
        }
    }

    private fun isAllowedTool(settings: ProjectSetting, toolName: String): Boolean {
        // Check explicit allow list
        if (settings.allowedTools.isNotEmpty()) {
            return toolName in settings.allowedTools
        }
        // Use default tool set for autonomy level
        return toolName in DefaultToolSets.getDefaultTools(settings.autonomyLevel)
    }
}

/**
 * Computes the effective tool list for a project (defaults + allowed - denied - hard blocks).
 */
public class ComputeEffectiveToolsUseCase {
    public operator fun invoke(settings: ProjectSetting): Outcome<List<String>> {
        val defaultTools = DefaultToolSets.getDefaultTools(settings.autonomyLevel)
        val allowed = settings.allowedTools
        val denied = settings.deniedTools.toSet()
        val hardBlocked = HardBlockTools.getHardBlockedTools()

        val baseTools = if (allowed.isEmpty()) defaultTools else allowed
        val effective = baseTools.filter { it !in denied && it !in hardBlocked }
        return Outcome.Success(effective.distinct())
    }
}

/**
 * Validates that project settings don't violate hard blocks.
 * Invariant: autonomyLevel = FULL_AUTO does not imply a relaxed deny list.
 */
public class ValidateProjectSettingsUseCase {
    public operator fun invoke(settings: ProjectSetting): Outcome<ProjectSetting> {
        // Always ensure hard block tools are in deniedTools
        val hardBlocked = HardBlockTools.getHardBlockedTools()
        val updatedDenied = settings.deniedTools.toMutableSet().apply {
            addAll(hardBlocked)
        }

        // Verify that hard blocks are never removed from deniedTools
        require(hardBlocked.all { it in updatedDenied }) {
            "Hard block tools must always be in deniedTools"
        }

        return Outcome.Success(settings.copy(deniedTools = updatedDenied.toList()))
    }
}

/**
 * Updates project autonomy level with validation.
 */
public class UpdateAutonomyLevelUseCase(
    private val settingRepo: ProjectSettingRepository,
    private val validateSettings: ValidateProjectSettingsUseCase = ValidateProjectSettingsUseCase(),
) {
    public suspend operator fun invoke(
        projectId: String,
        newAutonomyLevel: AutonomyLevel,
    ): Outcome<ProjectSetting> = tryCatch {
        val current = settingRepo.getSetting(projectId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Projekt nicht gefunden.",
                messageEn = "Project not found.",
            ))

        val updated = current.copy(
            autonomyLevel = newAutonomyLevel,
            permissionMode = GetPermissionModeUseCase()(newAutonomyLevel),
            updatedAt = System.currentTimeMillis(),
        )

        val validated = validateSettings(updated).getOrThrow()
        settingRepo.updateSetting(validated).getOrThrow()
        validated
    }
}

/**
 * Updates project denied tools list with hard block enforcement.
 */
public class UpdateDeniedToolsUseCase(
    private val settingRepo: ProjectSettingRepository,
    private val validateSettings: ValidateProjectSettingsUseCase = ValidateProjectSettingsUseCase(),
) {
    public suspend operator fun invoke(
        projectId: String,
        newDeniedTools: List<String>,
    ): Outcome<ProjectSetting> = tryCatch {
        val current = settingRepo.getSetting(projectId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Projekt nicht gefunden.",
                messageEn = "Project not found.",
            ))

        val updated = current.copy(
            deniedTools = newDeniedTools,
            updatedAt = System.currentTimeMillis(),
        )

        val validated = validateSettings(updated).getOrThrow()
        settingRepo.updateSetting(validated).getOrThrow()
        validated
    }
}

/**
 * Updates project allowed tools list.
 */
public class UpdateAllowedToolsUseCase(
    private val settingRepo: ProjectSettingRepository,
) {
    public suspend operator fun invoke(
        projectId: String,
        newAllowedTools: List<String>,
    ): Outcome<ProjectSetting> = tryCatch {
        val current = settingRepo.getSetting(projectId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Projekt nicht gefunden.",
                messageEn = "Project not found.",
            ))

        val updated = current.copy(
            allowedTools = newAllowedTools,
            updatedAt = System.currentTimeMillis(),
        )

        settingRepo.updateSetting(updated).getOrThrow()
        updated
    }
}

/**
 * Gets the CLI permission mode string for a project.
 */
public class GetCliPermissionModeUseCase(
    private val getPermissionMode: GetPermissionModeUseCase = GetPermissionModeUseCase(),
) {
    public operator fun invoke(settings: ProjectSetting): String = when (getPermissionMode(settings.autonomyLevel)) {
        PermissionMode.DEFAULT -> "default"
        PermissionMode.ACCEPT_NON_DESTRUCTIVE -> "acceptEdits"
        PermissionMode.BYPASS_ALL -> "bypassPermissions"
    }
}

/**
 * Checks if a project can run a tool without approval.
 */
public class CanRunToolWithoutApprovalUseCase(
    private val checkToolApproval: CheckToolApprovalUseCase = CheckToolApprovalUseCase(),
) {
    public suspend operator fun invoke(
        settings: ProjectSetting,
        toolName: String,
        command: String? = null,
    ): Outcome<Boolean> = checkToolApproval(settings, toolName, command).map { !it }
}