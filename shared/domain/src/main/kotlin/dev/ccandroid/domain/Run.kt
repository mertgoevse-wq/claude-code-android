package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class RunState {
    CREATED,
    PREPARING,
    PLANNING,
    RUNNING,
    AWAITING_PERMISSION,
    VERIFYING,
    RETRYING,
    COMMITTING,
    PUSHING,
    DONE,
    FAILED,
    INTERRUPTED,
    CANCELLED,
    OFFLOADED,
}

/**
 * True once the run has ended and will not move again.
 *
 * Declared here so the one place that decides it is the domain, not a SQL
 * string copied into each query that needs it. `RunDao.observeActiveRuns` is the
 * only other copy and it must agree.
 */
public val RunState.isTerminal: Boolean
    get() = this in RunStateTerminal

private val RunStateTerminal: Set<RunState> = setOf(
    RunState.DONE,
    RunState.FAILED,
    RunState.CANCELLED,
    RunState.INTERRUPTED,
    RunState.OFFLOADED,
)

@Serializable
public data class Run(
    val id: String,
    val projectId: String,
    val conversationId: String,
    val turnId: String,
    val backendId: String,
    val backendProfile: String,
    val state: RunState = RunState.CREATED,
    val taskText: String,
    val branchName: String? = null,
    val commitSha: String? = null,
    val pullRequestUrl: String? = null,
    val attemptCount: Int = 1,
    val maxAttempts: Int = 10,
    val isOffloaded: Boolean = false,
    val errorId: String? = null,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationMs: Long? = null,
)
