package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.MessagePartKind

@Entity(
    tableName = "message_parts",
    indices = [Index(value = ["messageId", "ordinal"], unique = true)]
)
data class MessagePartEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val kind: MessagePartKind,
    val ordinal: Int,
    val payloadJson: String,
    val collapsed: Boolean = false,
) {
    companion object {
        fun fromDomain(part: dev.ccandroid.domain.MessagePart): MessagePartEntity = MessagePartEntity(
            id = part.id,
            messageId = part.messageId,
            kind = part.kind,
            ordinal = part.ordinal,
            payloadJson = part.payloadJson,
            collapsed = part.collapsed,
        )
    }

    fun toDomain(): dev.ccandroid.domain.MessagePart = dev.ccandroid.domain.MessagePart(
        id = id,
        messageId = messageId,
        kind = kind,
        ordinal = ordinal,
        payloadJson = payloadJson,
        collapsed = collapsed,
    )
}