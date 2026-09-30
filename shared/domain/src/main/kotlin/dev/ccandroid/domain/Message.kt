package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
}

@Serializable
public enum class MessagePartKind {
    TEXT,
    CODE,
    THINKING,
    TOOL_CARD,
    DIFF_SUMMARY,
    ERROR,
}

@Serializable
public data class Message(
    val id: String,
    val turnId: String,
    val role: MessageRole,
    val createdAt: Long,
    val renderedMarkdown: String,
    val parentToolUseId: String? = null,
    val isRedacted: Boolean = false,
)

@Serializable
public data class MessagePart(
    val id: String,
    val messageId: String,
    val kind: MessagePartKind,
    val ordinal: Int,
    val payloadJson: String,
    val collapsed: Boolean = false,
)
