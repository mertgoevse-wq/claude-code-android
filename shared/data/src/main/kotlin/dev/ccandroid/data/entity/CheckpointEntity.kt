package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.Checkpoint

@Entity(
    tableName = "checkpoints",
    indices = [
        Index(value = ["runId"]),
        Index(value = ["runId", "stepId"]),
    ]
)
data class CheckpointEntity(
    @PrimaryKey val id: String,
    val runId: String,
    val stepId: String,
    val gitCommitSha: String,
    val description: String,
    val createdAt: Long,
) {
    companion object {
        fun fromDomain(checkpoint: Checkpoint): CheckpointEntity = CheckpointEntity(
            id = checkpoint.id,
            runId = checkpoint.runId,
            stepId = checkpoint.stepId,
            gitCommitSha = checkpoint.gitCommitSha,
            description = checkpoint.description,
            createdAt = checkpoint.createdAt,
        )
    }

    fun toDomain(): Checkpoint = Checkpoint(
        id = id,
        runId = runId,
        stepId = stepId,
        gitCommitSha = gitCommitSha,
        description = description,
        createdAt = createdAt,
    )
}
