package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.SkillInstall
import dev.ccandroid.domain.SkillInstallScope

@Entity(
    tableName = "skill_installs",
    foreignKeys = [
        ForeignKey(
            entity = SkillEntity::class,
            parentColumns = ["id"],
            childColumns = ["skillId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["skillId", "scope", "projectId"], unique = true),
        Index(value = ["projectId"]),
    ]
)
data class SkillInstallEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val skillId: String,
    val scope: SkillInstallScope,
    val projectId: String? = null,
    val installPath: String,
    val isEnabled: Boolean = true,
    val installedAt: Long,
) {
    companion object {
        fun fromDomain(install: SkillInstall): SkillInstallEntity = SkillInstallEntity(
            skillId = install.skillId,
            scope = install.scope,
            projectId = install.projectId,
            installPath = install.installPath,
            isEnabled = install.isEnabled,
            installedAt = install.installedAt,
        )
    }

    fun toDomain(): SkillInstall = SkillInstall(
        skillId = skillId,
        scope = scope,
        projectId = projectId,
        installPath = installPath,
        isEnabled = isEnabled,
        installedAt = installedAt,
    )
}
