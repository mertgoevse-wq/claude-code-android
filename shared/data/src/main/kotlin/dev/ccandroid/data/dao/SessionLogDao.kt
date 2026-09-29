package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.ccandroid.data.entity.SessionLogEntryEntity
import dev.ccandroid.domain.SessionLogCategory
import dev.ccandroid.domain.SessionLogSeverity
import kotlinx.coroutines.flow.Flow

/**
 * Append-only data access object for transparency and session logging.
 * Invariant from docs/02-architecture/data-model.md §SessionLogEntry:
 * This DAO intentionally exposes NO update and NO delete functions.
 */
@Dao
interface SessionLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLogEntry(entry: SessionLogEntryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLogEntries(entries: List<SessionLogEntryEntity>)

    @Query("SELECT * FROM session_log_entries WHERE id = :id LIMIT 1")
    suspend fun getLogEntryById(id: String): SessionLogEntryEntity?

    @Query("SELECT * FROM session_log_entries WHERE runId = :runId ORDER BY timestamp ASC")
    fun observeLogsForRun(runId: String): Flow<List<SessionLogEntryEntity>>

    @Query("SELECT * FROM session_log_entries WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun observeLogsForProject(projectId: String): Flow<List<SessionLogEntryEntity>>

    @Query("SELECT * FROM session_log_entries WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeLogsForConversation(conversationId: String): Flow<List<SessionLogEntryEntity>>

    @Query("SELECT * FROM session_log_entries ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentLogs(limit: Int = 100): Flow<List<SessionLogEntryEntity>>

    @Query("SELECT * FROM session_log_entries WHERE category = :category AND severity = :severity ORDER BY timestamp DESC")
    fun observeLogsByCategoryAndSeverity(category: SessionLogCategory, severity: SessionLogSeverity): Flow<List<SessionLogEntryEntity>>
}
