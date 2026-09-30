package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public data class Checkpoint(
    val id: String,
    val runId: String,
    val stepId: String,
    val gitCommitSha: String,
    val description: String,
    val createdAt: Long,
)
