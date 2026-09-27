package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class StepStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED
}

@Serializable
public data class PlanStep(
    val id: String,
    val index: Int,
    val description: String,
    val status: StepStatus = StepStatus.PENDING
)

@Serializable
public data class Plan(
    val id: String,
    val runId: String,
    val steps: List<PlanStep> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
)
