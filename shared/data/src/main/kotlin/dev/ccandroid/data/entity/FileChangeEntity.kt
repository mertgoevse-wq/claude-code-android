package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.FileChange
import dev.ccandroid.domain.FileChangeType

@Entity(
    tableName = "file_changes",
    indices = [
        Index(value = ["runId"]),
        Index(value = ["runId", "path"], unique = true),
    ]
)
data class FileChangeEntity(
    @PrimaryKey val id: String,
    val runId: String,
    val path: String,
    val changeType: FileChangeType,
    val oldPath: String? = null,
    val linesAdded: Int,
    val linesRemoved: Int,
    val binary: Boolean = false,
    val hunksJson: String,
) {
    companion object {
        fun fromDomain(change: FileChange): FileChangeEntity = FileChangeEntity(
            id = change.id,
            runId = change.runId,
            path = change.path,
            changeType = change.changeType,
            oldPath = change.oldPath,
            linesAdded = change.linesAdded,
            linesRemoved = change.linesRemoved,
            binary = change.binary,
            hunksJson = change.hunksJson,
        )
    }

    fun toDomain(): FileChange = FileChange(
        id = id,
        runId = runId,
        path = path,
        changeType = changeType,
        oldPath = oldPath,
        linesAdded = linesAdded,
        linesRemoved = linesRemoved,
        binary = binary,
        hunksJson = hunksJson,
    )
}
