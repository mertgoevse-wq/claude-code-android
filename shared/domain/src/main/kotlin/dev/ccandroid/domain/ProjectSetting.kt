package dev.ccandroid.domain

import kotlinx.serialization.Serializable

public enum class PermissionMode {
    DEFAULT,
    ACCEPT_NON_DESTRUCTIVE,
    BYPASS_ALL,
}

public enum class OffloadPolicy {
    NEVER,
    WHEN_HEAVY,
    ALWAYS,
}

@Serializable
public data class ProjectSetting(
    val projectId: String,
    val autonomyLevel: AutonomyLevel = AutonomyLevel.ASK_RISKY,
    val modelProviderId: String,
    val modelId: String,
    val verifyCommands: List<String> = emptyList(),
    val retryBudget: Int = 3,
    val allowedTools: List<String> = emptyList(),
    val deniedTools: List<String> = emptyList(),
    val permissionMode: PermissionMode = PermissionMode.DEFAULT,
    val offloadPolicy: OffloadPolicy = OffloadPolicy.NEVER,
    val branchPrefix: String = "task/",
    val costAdvisoryThresholdUsd: Long? = null,
    val autoCommit: Boolean = true,
    val autoPr: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)
