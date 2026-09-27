package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class ToolStatus {
    PENDING, RUNNING, COMPLETED, FAILED, REFUSED
}

@Serializable
public data class ToolInvocation(
    val id: String,
    val turnId: String,
    val toolName: String,
    val inputJson: String,
    val status: ToolStatus = ToolStatus.PENDING,
    val output: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
