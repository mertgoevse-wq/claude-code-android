package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.VerificationRun
import dev.ccandroid.domain.VerificationState

@Entity(
    tableName = "verification_runs",
    indices = [
        Index(value = ["runId"]),
        Index(value = ["runId", "attempt"], unique = true),
    ]
)
data class VerificationRunEntity(
    @PrimaryKey val id: String,
    val runId: String,
    val attempt: Int = 1,
    val state: VerificationState = VerificationState.NOT_STARTED,
    val commandCount: Int = 0,
    val durationMs: Long = 0L,
    val logRef: String? = null,
    val judgedAt: Long? = null,
    val createdAt: Long,
) {
    companion object {
        fun fromDomain(run: VerificationRun): VerificationRunEntity = VerificationRunEntity(
            id = run.id,
            runId = run.runId,
            attempt = run.attempt,
            state = run.state,
            commandCount = run.commandCount,
            durationMs = run.durationMs,
            logRef = run.logRef,
            judgedAt = run.judgedAt,
            createdAt = run.createdAt,
        )
    }

    fun toDomain(): VerificationRun = VerificationRun(
        id = id,
        runId = runId,
        attempt = attempt,
        state = state,
        commandCount = commandCount,
        durationMs = durationMs,
        logRef = logRef,
        judgedAt = judgedAt,
        createdAt = createdAt,
    )
}
