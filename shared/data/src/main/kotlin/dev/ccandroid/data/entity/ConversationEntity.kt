package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["projectId", "updatedAt"])]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val sessionId: String,
    val backendId: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMessagePreview: String? = null,
) {
    companion object {
        fun fromDomain(conv: dev.ccandroid.domain.Conversation): ConversationEntity = ConversationEntity(
            id = conv.id,
            projectId = conv.projectId,
            title = conv.title,
            sessionId = conv.sessionId,
            backendId = conv.backendId,
            isArchived = conv.isArchived,
            createdAt = conv.createdAt,
            updatedAt = conv.updatedAt,
            lastMessagePreview = conv.lastMessagePreview,
        )
    }

    fun toDomain(): dev.ccandroid.domain.Conversation = dev.ccandroid.domain.Conversation(
        id = id,
        projectId = projectId,
        title = title,
        sessionId = sessionId,
        backendId = backendId,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastMessagePreview = lastMessagePreview,
    )
}