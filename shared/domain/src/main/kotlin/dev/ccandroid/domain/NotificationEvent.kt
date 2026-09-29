package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class NotificationChannel {
    RUN_DONE,
    RUN_FAILED,
    PERMISSION,
    GITHUB,
    RUNNER,
    UPDATE,
}

@Serializable
public data class NotificationEvent(
    val id: String,
    val runId: String? = null,
    val channel: NotificationChannel,
    val titleDe: String,
    val titleEn: String,
    val bodyDe: String,
    val bodyEn: String,
    val actionJson: String? = null,
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
)
