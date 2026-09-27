package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public data class Conversation(
    val id: String,
    val projectId: String,
    val title: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
