package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public data class Conversation(
    val id: String,
    val projectId: String,
    val title: String,
    val sessionId: String,
    val backendId: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMessagePreview: String? = null,
)
