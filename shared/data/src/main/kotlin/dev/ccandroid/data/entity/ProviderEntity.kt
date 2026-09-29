package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.Provider
import dev.ccandroid.domain.ProviderKind

@Entity(
    tableName = "providers",
    indices = [
        Index(value = ["isDefault"]),
        Index(value = ["secretProfileId"]),
    ]
)
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: ProviderKind,
    val baseUrl: String? = null,
    val pathTemplate: String? = null,
    val secretProfileId: String,
    val headersJson: String? = null,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val lastTestedAt: Long? = null,
    val lastTestResult: String? = null,
    val discoveredModels: List<String> = emptyList(),
) {
    companion object {
        fun fromDomain(provider: Provider): ProviderEntity = ProviderEntity(
            id = provider.id,
            name = provider.name,
            kind = provider.kind,
            baseUrl = provider.baseUrl,
            pathTemplate = provider.pathTemplate,
            secretProfileId = provider.secretProfileId,
            headersJson = provider.headersJson,
            isEnabled = provider.isEnabled,
            isDefault = provider.isDefault,
            lastTestedAt = provider.lastTestedAt,
            lastTestResult = provider.lastTestResult,
            discoveredModels = provider.discoveredModels,
        )
    }

    fun toDomain(): Provider = Provider(
        id = id,
        name = name,
        kind = kind,
        baseUrl = baseUrl,
        pathTemplate = pathTemplate,
        secretProfileId = secretProfileId,
        headersJson = headersJson,
        isEnabled = isEnabled,
        isDefault = isDefault,
        lastTestedAt = lastTestedAt,
        lastTestResult = lastTestResult,
        discoveredModels = discoveredModels,
    )
}
