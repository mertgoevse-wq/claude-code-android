package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public data class CostRecord(
    val id: String,
    val runId: String,
    val promptTokens: Long,
    val completionTokens: Long,
    val totalCostMicroUsd: Long,
    val timestampMillis: Long = System.currentTimeMillis()
)
