package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.PlanStep
import dev.ccandroid.domain.PlanStepState

@Entity(
    tableName = "plan_steps",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["planId", "ordinal"], unique = true)
    ]
)
data class PlanStepEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val ordinal: Int,
    val titleDe: String,
    val titleEn: String,
    val acceptanceCriteria: String,
    val state: PlanStepState = PlanStepState.PENDING,
    val isCheckpoint: Boolean = false,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val attemptCount: Int = 0,
) {
    companion object {
        fun fromDomain(step: PlanStep): PlanStepEntity = PlanStepEntity(
            id = step.id,
            planId = step.planId,
            ordinal = step.ordinal,
            titleDe = step.titleDe,
            titleEn = step.titleEn,
            acceptanceCriteria = step.acceptanceCriteria,
            state = step.state,
            isCheckpoint = step.isCheckpoint,
            startedAt = step.startedAt,
            endedAt = step.endedAt,
            attemptCount = step.attemptCount,
        )
    }

    fun toDomain(): PlanStep = PlanStep(
        id = id,
        planId = planId,
        ordinal = ordinal,
        titleDe = titleDe,
        titleEn = titleEn,
        acceptanceCriteria = acceptanceCriteria,
        state = state,
        isCheckpoint = isCheckpoint,
        startedAt = startedAt,
        endedAt = endedAt,
        attemptCount = attemptCount,
    )
}
