package dev.ccandroid.domain

import kotlinx.serialization.Serializable

public enum class ProjectKind {
    LOCAL,
    CLONED,
    REMOTE,
}

@Serializable
public data class Project(
    val id: String,
    val name: String,
    val kind: ProjectKind,
    val path: String,
    val backendId: String? = null,
    val vcsProvider: String? = null,
    val remoteOwner: String? = null,
    val remoteName: String? = null,
    val isPrivate: Boolean = true,
    val defaultBranch: String? = null,
    val description: String? = null,
    val isArchived: Boolean = false,
    val lastRunAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        // Enforce invariants from docs/02-architecture/data-model.md
        if (kind == ProjectKind.LOCAL) {
            require(vcsProvider == null) { "LOCAL project must not have a vcsProvider" }
        }
        if (kind == ProjectKind.CLONED) {
            requireNotNull(remoteOwner) { "CLONED project must have remoteOwner" }
            requireNotNull(remoteName) { "CLONED project must have remoteName" }
        }
        require(isPrivate) { "isPrivate must always be true for projects in this repository" }
    }
}
