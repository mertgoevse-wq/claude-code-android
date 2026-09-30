package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import dev.ccandroid.domain.Branch

@Entity(
    tableName = "branches",
    primaryKeys = ["projectId", "name"],
    indices = [
        Index(value = ["projectId"]),
    ]
)
data class BranchEntity(
    val projectId: String,
    val name: String,
    val baseBranch: String = "main",
    val headCommitSha: String,
    val isMerged: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        fun fromDomain(branch: Branch): BranchEntity = BranchEntity(
            projectId = branch.projectId,
            name = branch.name,
            baseBranch = branch.baseBranch,
            headCommitSha = branch.headCommitSha,
            isMerged = branch.isMerged,
            createdAt = branch.createdAt,
            updatedAt = branch.updatedAt,
        )
    }

    fun toDomain(): Branch = Branch(
        projectId = projectId,
        name = name,
        baseBranch = baseBranch,
        headCommitSha = headCommitSha,
        isMerged = isMerged,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
