package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.ModelSpec

@Entity(
    tableName = "model_specs",
    foreignKeys = [
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["providerId"])
    ]
)
data class ModelSpecEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val displayName: String,
    val contextWindow: Int,
    val maxOutputTokens: Int,
    val inputPricePerMtok: Long,
    val outputPricePerMtok: Long,
    val supportsVision: Boolean = false,
    val supportsTools: Boolean = true,
) {
    companion object {
        fun fromDomain(spec: ModelSpec): ModelSpecEntity = ModelSpecEntity(
            id = spec.id,
            providerId = spec.providerId,
            displayName = spec.displayName,
            contextWindow = spec.contextWindow,
            maxOutputTokens = spec.maxOutputTokens,
            inputPricePerMtok = spec.inputPricePerMtok,
            outputPricePerMtok = spec.outputPricePerMtok,
            supportsVision = spec.supportsVision,
            supportsTools = spec.supportsTools,
        )
    }

    fun toDomain(): ModelSpec = ModelSpec(
        id = id,
        providerId = providerId,
        displayName = displayName,
        contextWindow = contextWindow,
        maxOutputTokens = maxOutputTokens,
        inputPricePerMtok = inputPricePerMtok,
        outputPricePerMtok = outputPricePerMtok,
        supportsVision = supportsVision,
        supportsTools = supportsTools,
    )
}
