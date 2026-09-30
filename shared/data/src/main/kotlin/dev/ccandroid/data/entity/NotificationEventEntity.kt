package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.NotificationChannel
import dev.ccandroid.domain.NotificationEvent

@Entity(
    tableName = "notification_events",
    indices = [
        Index(value = ["runId"]),
        Index(value = ["deliveredAt"]),
        Index(value = ["readAt"]),
    ]
)
data class NotificationEventEntity(
    @PrimaryKey val id: String,
    val runId: String? = null,
    val channel: NotificationChannel,
    val titleDe: String,
    val titleEn: String,
    val bodyDe: String,
    val bodyEn: String,
    val actionJson: String? = null,
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
) {
    companion object {
        fun fromDomain(event: NotificationEvent): NotificationEventEntity = NotificationEventEntity(
            id = event.id,
            runId = event.runId,
            channel = event.channel,
            titleDe = event.titleDe,
            titleEn = event.titleEn,
            bodyDe = event.bodyDe,
            bodyEn = event.bodyEn,
            actionJson = event.actionJson,
            deliveredAt = event.deliveredAt,
            readAt = event.readAt,
        )
    }

    fun toDomain(): NotificationEvent = NotificationEvent(
        id = id,
        runId = runId,
        channel = channel,
        titleDe = titleDe,
        titleEn = titleEn,
        bodyDe = bodyDe,
        bodyEn = bodyEn,
        actionJson = actionJson,
        deliveredAt = deliveredAt,
        readAt = readAt,
    )
}
