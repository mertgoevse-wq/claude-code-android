package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.Skill
import dev.ccandroid.domain.SkillSourceKind

@Entity(
    tableName = "skills",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class SkillEntity(
    @PrimaryKey val id: String,
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
) {
    companion object {
        fun fromDomain(skill: Skill): SkillEntity = SkillEntity(
            id = skill.id,
            name = skill.name,
            description = skill.description,
            sourceKind = skill.sourceKind,
            sourceUrl = skill.sourceUrl,
            sourceRef = skill.sourceRef,
            contentHash = skill.contentHash,
            isValid = skill.isValid,
            validationError = skill.validationError,
            createdAt = skill.createdAt,
            updatedAt = skill.updatedAt,
        )
    }

    fun toDomain(): Skill = Skill(
        id = id,
        name = name,
        description = description,
        sourceKind = sourceKind,
        sourceUrl = sourceUrl,
        sourceRef = sourceRef,
        contentHash = contentHash,
        isValid = isValid,
        validationError = validationError,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
