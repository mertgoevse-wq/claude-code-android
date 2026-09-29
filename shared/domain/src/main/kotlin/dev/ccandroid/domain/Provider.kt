package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class ProviderKind {
    ANTHROPIC,
    OPENAI_CHAT,
    OPENAI_RESPONSES,
    CUSTOM,
}

@Serializable
public data class ModelSpec(
    val id: String,
    val providerId: String,
    val displayName: String,
    val contextWindow: Int,
    val maxOutputTokens: Int,
    val inputPricePerMtok: Long,
    val outputPricePerMtok: Long,
    val supportsVision: Boolean = false,
    val supportsTools: Boolean = true,
)

@Serializable
public data class Provider(
    val id: String,
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
)
