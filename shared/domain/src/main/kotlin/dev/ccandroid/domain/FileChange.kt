package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class FileChangeType {
    ADDED,
    MODIFIED,
    DELETED,
    RENAMED,
}

@Serializable
public enum class DiffDecision {
    PENDING,
    ACCEPTED,
    REVERTED,
}

@Serializable
public data class FileChange(
    val id: String,
    val runId: String,
    val path: String,
    val changeType: FileChangeType,
    val oldPath: String? = null,
    val linesAdded: Int,
    val linesRemoved: Int,
    val binary: Boolean = false,
    val hunksJson: String,
) {
    init {
        if (changeType == FileChangeType.RENAMED) {
            requireNotNull(oldPath) { "RENAMED change must have an oldPath" }
        }
    }
}

@Serializable
public data class DiffEntry(
    val id: String,
    val fileChangeId: String,
    val ordinal: Int,
    val oldStart: Int,
    val oldLines: Int,
    val newStart: Int,
    val newLines: Int,
    val linesJson: String,
    val decision: DiffDecision = DiffDecision.PENDING,
    val revertCommitId: String? = null,
)
