package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.SessionLogCategory
import dev.ccandroid.domain.SessionLogEntry
import dev.ccandroid.domain.SessionLogSeverity

@Entity(
    tableName = "session_log_entries",
    indices = [
        Index(value = ["runId"]),
        Index(value = ["projectId"]),
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"]),
        Index(value = ["category", "severity"]),
    ]
)
data class SessionLogEntryEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val runId: String? = null,
    val projectId: String? = null,
    val conversationId: String? = null,
    val category: SessionLogCategory,
    val severity: SessionLogSeverity = SessionLogSeverity.INFO,
    val message: String,
    val detailJson: String? = null,
    val durationMs: Long? = null,
) {
    companion object {
        fun fromDomain(entry: SessionLogEntry): SessionLogEntryEntity = SessionLogEntryEntity(
            id = entry.id,
            timestamp = entry.timestamp,
            runId = entry.runId,
            projectId = entry.projectId,
            conversationId = entry.conversationId,
            category = entry.category,
            severity = entry.severity,
            message = entry.message,
            detailJson = entry.detailJson,
            durationMs = entry.durationMs,
        )
    }

    fun toDomain(): SessionLogEntry = SessionLogEntry(
        id = id,
        timestamp = timestamp,
        runId = runId,
        projectId = projectId,
        conversationId = conversationId,
        category = category,
        severity = severity,
        message = message,
        detailJson = detailJson,
        durationMs = durationMs,
    )
}
