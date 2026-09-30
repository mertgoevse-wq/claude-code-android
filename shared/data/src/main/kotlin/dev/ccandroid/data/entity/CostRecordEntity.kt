package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.CostRecord

@Entity(
    tableName = "cost_records",
    indices = [
        Index(value = ["runId"]),
        Index(value = ["projectId"]),
        Index(value = ["conversationId"]),
    ]
)
data class CostRecordEntity(
    @PrimaryKey val id: String,
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
) {
    companion object {
        fun fromDomain(record: CostRecord): CostRecordEntity = CostRecordEntity(
            id = record.id,
            runId = record.runId,
            projectId = record.projectId,
            conversationId = record.conversationId,
            inputTokens = record.inputTokens,
            outputTokens = record.outputTokens,
            cacheReadTokens = record.cacheReadTokens,
            cacheCreationTokens = record.cacheCreationTokens,
            costUsdMicros = record.costUsdMicros,
            isEstimated = record.isEstimated,
            modelId = record.modelId,
            providerId = record.providerId,
            inputPricePerMtok = record.inputPricePerMtok,
            outputPricePerMtok = record.outputPricePerMtok,
        )
    }

    fun toDomain(): CostRecord = CostRecord(
        id = id,
        runId = runId,
        projectId = projectId,
        conversationId = conversationId,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        cacheReadTokens = cacheReadTokens,
        cacheCreationTokens = cacheCreationTokens,
        costUsdMicros = costUsdMicros,
        isEstimated = isEstimated,
        modelId = modelId,
        providerId = providerId,
        inputPricePerMtok = inputPricePerMtok,
        outputPricePerMtok = outputPricePerMtok,
    )
}
