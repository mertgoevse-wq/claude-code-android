package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.Plan
import dev.ccandroid.domain.PlanStep

@Entity(
    tableName = "plans",
    indices = [Index(value = ["runId"])]
)
data class PlanEntity(
    @PrimaryKey val id: String,
    val runId: String? = null,
    val title: String? = null,
    val createdAt: Long,
) {
    companion object {
        fun fromDomain(plan: Plan): PlanEntity = PlanEntity(
            id = plan.id,
            runId = plan.runId,
            title = plan.title,
            createdAt = plan.createdAt,
        )
    }

    fun toDomain(steps: List<PlanStep>): Plan = Plan(
        id = id,
        runId = runId,
        title = title,
        steps = steps,
        createdAt = createdAt,
    )
}
