package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class SkillSourceKind {
    BUILTIN,
    GITHUB,
    ARCHIVE,
    AI_GENERATED,
}

@Serializable
public enum class SkillInstallScope {
    GLOBAL,
    PROJECT,
}

@Serializable
public data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val sourceKind: SkillSourceKind = SkillSourceKind.BUILTIN,
    val sourceUrl: String? = null,
    val sourceRef: String? = null,
    val contentHash: String,
    val isValid: Boolean = true,
    val validationError: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
public data class SkillInstall(
    val skillId: String,
    val scope: SkillInstallScope,
    val projectId: String? = null,
    val installPath: String,
    val isEnabled: Boolean = true,
    val installedAt: Long,
) {
    init {
        if (scope == SkillInstallScope.PROJECT) {
            requireNotNull(projectId) { "PROJECT scope install must have a projectId" }
        }
    }
}
