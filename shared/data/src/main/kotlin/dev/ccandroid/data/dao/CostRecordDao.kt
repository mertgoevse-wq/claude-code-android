package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.ccandroid.data.entity.CostRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CostRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCostRecord(record: CostRecordEntity)

    @Query("SELECT * FROM cost_records WHERE id = :id LIMIT 1")
    suspend fun getCostRecordById(id: String): CostRecordEntity?

    @Query("SELECT * FROM cost_records WHERE runId = :runId ORDER BY id ASC")
    fun observeCostRecordsForRun(runId: String): Flow<List<CostRecordEntity>>

    @Query("SELECT * FROM cost_records WHERE projectId = :projectId ORDER BY id ASC")
    fun observeCostRecordsForProject(projectId: String): Flow<List<CostRecordEntity>>

    @Query("SELECT * FROM cost_records WHERE runId = :runId ORDER BY id ASC")
    suspend fun getCostRecordsForRun(runId: String): List<CostRecordEntity>

    @Query("SELECT * FROM cost_records WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun getCostRecordsForProject(projectId: String): List<CostRecordEntity>

    @Query("SELECT SUM(costUsdMicros) FROM cost_records WHERE runId = :runId")
    fun observeTotalCostForRun(runId: String): Flow<Long?>

    @Query("SELECT SUM(costUsdMicros) FROM cost_records WHERE projectId = :projectId")
    suspend fun getTotalCostForProject(projectId: String): Long?

    @Query("SELECT SUM(costUsdMicros) FROM cost_records WHERE conversationId = :conversationId")
    suspend fun getTotalCostForConversation(conversationId: String): Long?

    @Query("SELECT SUM(costUsdMicros) FROM cost_records WHERE projectId = :projectId")
    fun observeTotalCostForProject(projectId: String): Flow<Long?>
}
