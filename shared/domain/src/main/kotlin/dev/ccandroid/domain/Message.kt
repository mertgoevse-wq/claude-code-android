package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

@Serializable
public data class Message(
    val id: String,
    val turnId: String,
    val role: MessageRole,
    val content: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
