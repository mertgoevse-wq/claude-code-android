package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.ToolInvocationStatus

@Entity(
    tableName = "tool_invocations",
    indices = [
        Index(value = ["turnId", "startedAt"]),
        Index(value = ["turnId", "toolUseId"], unique = true),
    ]
)
data class ToolInvocationEntity(
    @PrimaryKey val id: String,
    val turnId: String,
    val toolUseId: String,
    val parentToolUseId: String? = null,
    val name: String,
    val titleDe: String,
    val titleEn: String,
    val targetPath: String? = null,
    val inputJson: String,
    val status: ToolInvocationStatus,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val outputPreview: String? = null,
    val outputRef: String? = null,
    val isDestructive: Boolean,
) {
    companion object {
        fun fromDomain(tool: dev.ccandroid.domain.ToolInvocation): ToolInvocationEntity = ToolInvocationEntity(
            id = tool.id,
            turnId = tool.turnId,
            toolUseId = tool.toolUseId,
            parentToolUseId = tool.parentToolUseId,
            name = tool.name,
            titleDe = tool.titleDe,
            titleEn = tool.titleEn,
            targetPath = tool.targetPath,
            inputJson = tool.inputJson,
            status = tool.status,
            startedAt = tool.startedAt,
            endedAt = tool.endedAt,
            outputPreview = tool.outputPreview,
            outputRef = tool.outputRef,
            isDestructive = tool.isDestructive,
        )
    }

    fun toDomain(): dev.ccandroid.domain.ToolInvocation = dev.ccandroid.domain.ToolInvocation(
        id = id,
        turnId = turnId,
        toolUseId = toolUseId,
        parentToolUseId = parentToolUseId,
        name = name,
        titleDe = titleDe,
        titleEn = titleEn,
        targetPath = targetPath,
        inputJson = inputJson,
        status = status,
        startedAt = startedAt,
        endedAt = endedAt,
        outputPreview = outputPreview,
        outputRef = outputRef,
        isDestructive = isDestructive,
    )
}