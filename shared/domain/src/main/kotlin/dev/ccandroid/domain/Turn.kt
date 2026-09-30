package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class TurnState {
    PLANNING,
    RUNNING,
    AWAITING_PERMISSION,
    VERIFYING,
    DONE,
    FAILED,
    INTERRUPTED,
    CANCELLED,
}

@Serializable
public data class Turn(
    val id: String,
    val conversationId: String,
    val index: Int,
    val userMessageId: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val state: TurnState = TurnState.PLANNING,
    val runId: String? = null,
    val costUsd: Long = 0L,
    val attemptCount: Int = 1,
)
