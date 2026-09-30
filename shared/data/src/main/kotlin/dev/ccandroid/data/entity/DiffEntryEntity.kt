package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.DiffDecision
import dev.ccandroid.domain.DiffEntry

@Entity(
    tableName = "diff_entries",
    foreignKeys = [
        ForeignKey(
            entity = FileChangeEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileChangeId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["fileChangeId", "ordinal"], unique = true)
    ]
)
data class DiffEntryEntity(
    @PrimaryKey val id: String,
    val fileChangeId: String,
    val ordinal: Int,
    val oldStart: Int,
    val oldLines: Int,
    val newStart: Int,
    val newLines: Int,
    val linesJson: String,
    val decision: DiffDecision = DiffDecision.PENDING,
    val revertCommitId: String? = null,
) {
    companion object {
        fun fromDomain(entry: DiffEntry): DiffEntryEntity = DiffEntryEntity(
            id = entry.id,
            fileChangeId = entry.fileChangeId,
            ordinal = entry.ordinal,
            oldStart = entry.oldStart,
            oldLines = entry.oldLines,
            newStart = entry.newStart,
            newLines = entry.newLines,
            linesJson = entry.linesJson,
            decision = entry.decision,
            revertCommitId = entry.revertCommitId,
        )
    }

    fun toDomain(): DiffEntry = DiffEntry(
        id = id,
        fileChangeId = fileChangeId,
        ordinal = ordinal,
        oldStart = oldStart,
        oldLines = oldLines,
        newStart = newStart,
        newLines = newLines,
        linesJson = linesJson,
        decision = decision,
        revertCommitId = revertCommitId,
    )
}
