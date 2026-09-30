package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class ToolInvocationStatus {
    PENDING,
    RUNNING,
    DONE,
    ERROR,
    DENIED,
}

@Serializable
public data class ToolInvocation(
    val id: String,
    val turnId: String,
    val toolUseId: String,
    val parentToolUseId: String? = null,
    val name: String,
    val titleDe: String,
    val titleEn: String,
    val targetPath: String? = null,
    val inputJson: String,
    val status: ToolInvocationStatus = ToolInvocationStatus.PENDING,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val outputPreview: String? = null,
    val outputRef: String? = null,
    val isDestructive: Boolean = false,
)

@Serializable
public data class ToolResult(
    val invocationId: String,
    val toolUseId: String,
    val output: String,
    val isError: Boolean = false,
    val exitCode: Int? = null,
    val durationMs: Long? = null,
)
