package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.Run
import dev.ccandroid.domain.RunState

@Entity(
    tableName = "runs",
    indices = [
        Index(value = ["projectId", "startedAt"]),
        Index(value = ["state"]),
        Index(value = ["conversationId"]),
        Index(value = ["turnId"]),
    ]
)
data class RunEntity(
    @PrimaryKey val id: String,
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
) {
    companion object {
        fun fromDomain(run: Run): RunEntity = RunEntity(
            id = run.id,
            projectId = run.projectId,
            conversationId = run.conversationId,
            turnId = run.turnId,
            backendId = run.backendId,
            backendProfile = run.backendProfile,
            state = run.state,
            taskText = run.taskText,
            branchName = run.branchName,
            commitSha = run.commitSha,
            pullRequestUrl = run.pullRequestUrl,
            attemptCount = run.attemptCount,
            maxAttempts = run.maxAttempts,
            isOffloaded = run.isOffloaded,
            errorId = run.errorId,
            startedAt = run.startedAt,
            endedAt = run.endedAt,
            durationMs = run.durationMs,
        )
    }

    fun toDomain(): Run = Run(
        id = id,
        projectId = projectId,
        conversationId = conversationId,
        turnId = turnId,
        backendId = backendId,
        backendProfile = backendProfile,
        state = state,
        taskText = taskText,
        branchName = branchName,
        commitSha = commitSha,
        pullRequestUrl = pullRequestUrl,
        attemptCount = attemptCount,
        maxAttempts = maxAttempts,
        isOffloaded = isOffloaded,
        errorId = errorId,
        startedAt = startedAt,
        endedAt = endedAt,
        durationMs = durationMs,
    )
}
