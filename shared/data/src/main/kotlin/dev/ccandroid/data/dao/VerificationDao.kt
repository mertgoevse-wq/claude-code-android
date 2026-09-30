package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.ccandroid.data.entity.TestResultEntity
import dev.ccandroid.data.entity.VerificationRunEntity
import dev.ccandroid.domain.VerificationState
import kotlinx.coroutines.flow.Flow

@Dao
interface VerificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerificationRun(run: VerificationRunEntity)

    @Update
    suspend fun updateVerificationRun(run: VerificationRunEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestResults(results: List<TestResultEntity>)

    @Query("SELECT * FROM verification_runs WHERE id = :id LIMIT 1")
    suspend fun getVerificationRunById(id: String): VerificationRunEntity?

    @Query("SELECT * FROM verification_runs WHERE runId = :runId ORDER BY attempt DESC")
    fun observeVerificationRunsForRun(runId: String): Flow<List<VerificationRunEntity>>

    @Query("SELECT * FROM verification_runs WHERE runId = :runId ORDER BY attempt DESC")
    suspend fun getVerificationRunsForRun(runId: String): List<VerificationRunEntity>

    @Query("SELECT * FROM verification_runs WHERE runId = :runId ORDER BY attempt DESC LIMIT 1")
    suspend fun getLatestVerificationRun(runId: String): VerificationRunEntity?

    @Query("SELECT * FROM test_results WHERE verificationRunId = :verificationRunId ORDER BY ordinal ASC")
    fun observeTestResults(verificationRunId: String): Flow<List<TestResultEntity>>

    @Query("SELECT * FROM test_results WHERE verificationRunId = :verificationRunId ORDER BY ordinal ASC")
    suspend fun getTestResults(verificationRunId: String): List<TestResultEntity>

    @Query("UPDATE verification_runs SET state = :state, judgedAt = :judgedAt WHERE id = :id")
    suspend fun updateVerificationState(id: String, state: VerificationState, judgedAt: Long? = System.currentTimeMillis())

    @Transaction
    suspend fun completeVerificationRun(run: VerificationRunEntity, results: List<TestResultEntity>) {
        insertVerificationRun(run)
        if (results.isNotEmpty()) {
            insertTestResults(results)
        }
    }
}
