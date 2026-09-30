package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.ccandroid.data.entity.RunEntity
import dev.ccandroid.domain.RunState
import kotlinx.coroutines.flow.Flow

/**
 * The terminal-state list in [observeActiveRuns] is hand-written SQL and must
 * match `RunState.isTerminal` in the domain. That is the price of a constant
 * query string; the domain owns the answer, this query repeats it.
 */
@Dao
interface RunDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRun(run: RunEntity)

    @Update
    suspend fun updateRun(run: RunEntity)

    @Query("SELECT * FROM runs WHERE id = :id LIMIT 1")
    suspend fun getRunById(id: String): RunEntity?

    @Query("SELECT * FROM runs WHERE id = :id LIMIT 1")
    fun observeRunById(id: String): Flow<RunEntity?>

    @Query("SELECT * FROM runs WHERE projectId = :projectId ORDER BY startedAt DESC")
    fun observeRunsForProject(projectId: String): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE projectId = :projectId ORDER BY startedAt DESC")
    suspend fun getRunsForProject(projectId: String): List<RunEntity>

    @Query("SELECT * FROM runs WHERE state = :state ORDER BY startedAt DESC")
    suspend fun getRunsByState(state: RunState): List<RunEntity>

    @Query("SELECT * FROM runs WHERE state NOT IN ('DONE', 'FAILED', 'CANCELLED', 'INTERRUPTED') ORDER BY startedAt DESC")
    fun observeActiveRuns(): Flow<List<RunEntity>>

    @Query("UPDATE runs SET state = :state, endedAt = :endedAt, durationMs = :durationMs WHERE id = :id")
    suspend fun updateRunState(id: String, state: RunState, endedAt: Long? = System.currentTimeMillis(), durationMs: Long? = null)

    @Query("UPDATE runs SET commitSha = :sha WHERE id = :id")
    suspend fun updateCommitSha(id: String, sha: String)

    @Query("UPDATE runs SET pullRequestUrl = :url WHERE id = :id")
    suspend fun updatePullRequestUrl(id: String, url: String)
}
