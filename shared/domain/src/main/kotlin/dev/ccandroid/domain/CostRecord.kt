package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public data class CostRecord(
    val id: String,
    val runId: String,
    val projectId: String,
    val conversationId: String,
    val inputTokens: Long = 0L,
    val outputTokens: Long = 0L,
    val cacheReadTokens: Long = 0L,
    val cacheCreationTokens: Long = 0L,
    val costUsdMicros: Long = 0L,
    val isEstimated: Boolean = true,
    val modelId: String,
    val providerId: String,
    val inputPricePerMtok: Long = 0L,
    val outputPricePerMtok: Long = 0L,
)
