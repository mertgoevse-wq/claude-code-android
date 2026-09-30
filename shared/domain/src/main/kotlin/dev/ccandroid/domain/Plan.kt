package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class PlanStepState {
    PENDING,
    ACTIVE,
    DONE,
    FAILED,
    SKIPPED,
}

@Serializable
public data class PlanStep(
    val id: String,
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
)

@Serializable
public data class Plan(
    val id: String,
    val runId: String? = null,
    val title: String? = null,
    val steps: List<PlanStep>,
    val createdAt: Long,
) {
    init {
        // docs/02-architecture/data-model.md §Plan: an empty plan is invalid
        require(steps.isNotEmpty()) { "Plan must contain at least one step" }
    }
}
