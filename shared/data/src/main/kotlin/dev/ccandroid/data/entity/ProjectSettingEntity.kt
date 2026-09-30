package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.ccandroid.domain.AutonomyLevel
import dev.ccandroid.domain.OffloadPolicy
import dev.ccandroid.domain.PermissionMode
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val jsonSerializer = Json { ignoreUnknownKeys = true }

@Entity(tableName = "project_settings", primaryKeys = ["projectId"])
data class ProjectSettingEntity(
    val projectId: String,
    val autonomyLevel: AutonomyLevel = AutonomyLevel.ASK_RISKY,
    val modelProviderId: String,
    val modelId: String,
    val verifyCommands: String = "", // JSON list
    val retryBudget: Int = 3,
    val allowedTools: String = "", // JSON list
    val deniedTools: String = "", // JSON list
    val permissionMode: PermissionMode = PermissionMode.DEFAULT,
    val offloadPolicy: OffloadPolicy = OffloadPolicy.NEVER,
    val branchPrefix: String = "task/",
    val costAdvisoryThresholdUsd: Long? = null,
    val autoCommit: Boolean = true,
    val autoPr: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        fun fromDomain(setting: dev.ccandroid.domain.ProjectSetting): ProjectSettingEntity = ProjectSettingEntity(
            projectId = setting.projectId,
            autonomyLevel = setting.autonomyLevel,
            modelProviderId = setting.modelProviderId,
            modelId = setting.modelId,
            verifyCommands = setting.verifyCommands.toJsonString(),
            retryBudget = setting.retryBudget,
            allowedTools = setting.allowedTools.toJsonString(),
            deniedTools = setting.deniedTools.toJsonString(),
            permissionMode = setting.permissionMode,
            offloadPolicy = setting.offloadPolicy,
            branchPrefix = setting.branchPrefix,
            costAdvisoryThresholdUsd = setting.costAdvisoryThresholdUsd,
            autoCommit = setting.autoCommit,
            autoPr = setting.autoPr,
            notificationsEnabled = setting.notificationsEnabled,
            updatedAt = setting.updatedAt,
        )
    }

    fun toDomain(): dev.ccandroid.domain.ProjectSetting = dev.ccandroid.domain.ProjectSetting(
        projectId = projectId,
        autonomyLevel = autonomyLevel,
        modelProviderId = modelProviderId,
        modelId = modelId,
        verifyCommands = verifyCommands.fromJsonList(),
        retryBudget = retryBudget,
        allowedTools = allowedTools.fromJsonList(),
        deniedTools = deniedTools.fromJsonList(),
        permissionMode = permissionMode,
        offloadPolicy = offloadPolicy,
        branchPrefix = branchPrefix,
        costAdvisoryThresholdUsd = costAdvisoryThresholdUsd,
        autoCommit = autoCommit,
        autoPr = autoPr,
        notificationsEnabled = notificationsEnabled,
        updatedAt = updatedAt,
    )
}

private fun List<String>.toJsonString(): String = jsonSerializer.encodeToString(this)
private fun String.fromJsonList(): List<String> = if (isBlank()) emptyList() else jsonSerializer.decodeFromString<List<String>>(this)