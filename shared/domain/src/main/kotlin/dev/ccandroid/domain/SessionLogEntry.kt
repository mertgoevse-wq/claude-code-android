package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class SessionLogCategory {
    COMMAND,
    DIFF,
    PERMISSION,
    ERROR,
    COST,
    STATE,
    SYSTEM,
    NETWORK,
}

@Serializable
public enum class SessionLogSeverity {
    INFO,
    WARN,
    ERROR,
}

@Serializable
public data class SessionLogEntry(
    val id: String,
    val timestamp: Long,
    val runId: String? = null,
    val projectId: String? = null,
    val conversationId: String? = null,
    val category: SessionLogCategory,
    val severity: SessionLogSeverity = SessionLogSeverity.INFO,
    val message: String,
    val detailJson: String? = null,
    val durationMs: Long? = null,
)
