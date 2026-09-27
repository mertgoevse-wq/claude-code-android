package dev.ccandroid.domain

import kotlinx.serialization.Serializable

public enum class ProjectKind {
    LOCAL, CLONED, REMOTE
}

@Serializable
public data class Project(
    val id: String,
    val name: String,
    val kind: ProjectKind,
    val path: String,
    val backendId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
