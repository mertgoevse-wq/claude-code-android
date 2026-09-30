package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.MessageRole

@Entity(
    tableName = "messages",
    indices = [Index(value = ["turnId", "createdAt"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val turnId: String,
    val role: MessageRole,
    val createdAt: Long,
    val renderedMarkdown: String,
    val parentToolUseId: String? = null,
    val isRedacted: Boolean = false,
) {
    companion object {
        fun fromDomain(msg: dev.ccandroid.domain.Message): MessageEntity = MessageEntity(
            id = msg.id,
            turnId = msg.turnId,
            role = msg.role,
            createdAt = msg.createdAt,
            renderedMarkdown = msg.renderedMarkdown,
            parentToolUseId = msg.parentToolUseId,
            isRedacted = msg.isRedacted,
        )
    }

    fun toDomain(): dev.ccandroid.domain.Message = dev.ccandroid.domain.Message(
        id = id,
        turnId = turnId,
        role = role,
        createdAt = createdAt,
        renderedMarkdown = renderedMarkdown,
        parentToolUseId = parentToolUseId,
        isRedacted = isRedacted,
    )
}