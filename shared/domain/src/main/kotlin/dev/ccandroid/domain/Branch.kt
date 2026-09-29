package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public data class Branch(
    val projectId: String,
    val name: String,
    val baseBranch: String = "main",
    val headCommitSha: String,
    val isMerged: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
