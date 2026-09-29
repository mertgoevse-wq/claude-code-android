package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.ProjectKind

@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["isArchived", "lastRunAt"]),
        Index(value = ["remoteOwner", "remoteName"], unique = true),
    ]
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: ProjectKind,
    val path: String,
    val backendId: String? = null,
    val vcsProvider: String? = null,
    val remoteOwner: String? = null,
    val remoteName: String? = null,
    val isPrivate: Boolean = true,
    val defaultBranch: String? = null,
    val isArchived: Boolean = false,
    val lastRunAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        fun fromDomain(project: dev.ccandroid.domain.Project): ProjectEntity = ProjectEntity(
            id = project.id,
            name = project.name,
            kind = project.kind,
            path = project.path,
            backendId = project.backendId,
            vcsProvider = project.vcsProvider,
            remoteOwner = project.remoteOwner,
            remoteName = project.remoteName,
            isPrivate = project.isPrivate,
            defaultBranch = project.defaultBranch,
            isArchived = project.isArchived,
            lastRunAt = project.lastRunAt,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt,
        )
    }

    fun toDomain(): dev.ccandroid.domain.Project = dev.ccandroid.domain.Project(
        id = id,
        name = name,
        kind = kind,
        path = path,
        backendId = backendId,
        vcsProvider = vcsProvider,
        remoteOwner = remoteOwner,
        remoteName = remoteName,
        isPrivate = isPrivate,
        defaultBranch = defaultBranch,
        isArchived = isArchived,
        lastRunAt = lastRunAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}