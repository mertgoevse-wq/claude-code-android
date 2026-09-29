package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.ccandroid.data.entity.DiffEntryEntity
import dev.ccandroid.data.entity.FileChangeEntity
import dev.ccandroid.domain.DiffDecision
import kotlinx.coroutines.flow.Flow

@Dao
interface FileChangeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileChange(change: FileChangeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileChanges(changes: List<FileChangeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiffEntries(entries: List<DiffEntryEntity>)

    @Query("SELECT * FROM file_changes WHERE id = :id LIMIT 1")
    suspend fun getFileChangeById(id: String): FileChangeEntity?

    @Query("SELECT * FROM file_changes WHERE runId = :runId ORDER BY path ASC")
    fun observeFileChangesForRun(runId: String): Flow<List<FileChangeEntity>>

    @Query("SELECT * FROM diff_entries WHERE fileChangeId = :fileChangeId ORDER BY ordinal ASC")
    fun observeDiffEntriesForFileChange(fileChangeId: String): Flow<List<DiffEntryEntity>>

    @Query("SELECT * FROM diff_entries WHERE fileChangeId = :fileChangeId ORDER BY ordinal ASC")
    suspend fun getDiffEntriesForFileChange(fileChangeId: String): List<DiffEntryEntity>

    @Query("UPDATE diff_entries SET decision = :decision, revertCommitId = :revertCommitId WHERE id = :diffEntryId")
    suspend fun updateDiffDecision(diffEntryId: String, decision: DiffDecision, revertCommitId: String? = null)

    @Transaction
    suspend fun insertFileChangeWithEntries(change: FileChangeEntity, entries: List<DiffEntryEntity>) {
        insertFileChange(change)
        if (entries.isNotEmpty()) {
            insertDiffEntries(entries)
        }
    }
}
