package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.ccandroid.data.entity.ToolInvocationEntity
import dev.ccandroid.data.entity.ToolResultEntity
import dev.ccandroid.domain.ToolInvocationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolInvocationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertToolInvocation(invocation: ToolInvocationEntity)

    @Update
    suspend fun updateToolInvocation(invocation: ToolInvocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToolResult(result: ToolResultEntity)

    @Query("SELECT * FROM tool_invocations WHERE id = :id LIMIT 1")
    suspend fun getToolInvocationById(id: String): ToolInvocationEntity?

    @Query("SELECT * FROM tool_invocations WHERE turnId = :turnId AND toolUseId = :toolUseId LIMIT 1")
    suspend fun getToolInvocationByUseId(turnId: String, toolUseId: String): ToolInvocationEntity?

    @Query("SELECT * FROM tool_invocations WHERE turnId = :turnId ORDER BY startedAt ASC")
    fun observeToolInvocationsForTurn(turnId: String): Flow<List<ToolInvocationEntity>>

    @Query("SELECT * FROM tool_results WHERE invocationId = :invocationId LIMIT 1")
    suspend fun getToolResultForInvocation(invocationId: String): ToolResultEntity?

    @Query("UPDATE tool_invocations SET status = :status, endedAt = :endedAt, outputPreview = :outputPreview, outputRef = :outputRef WHERE id = :id")
    suspend fun updateToolInvocationStatus(
        id: String,
        status: ToolInvocationStatus,
        endedAt: Long? = System.currentTimeMillis(),
        outputPreview: String? = null,
        outputRef: String? = null,
    )

    @Transaction
    suspend fun completeToolInvocation(
        id: String,
        status: ToolInvocationStatus,
        result: ToolResultEntity,
        outputPreview: String? = null,
        outputRef: String? = null,
        endedAt: Long = System.currentTimeMillis(),
    ) {
        updateToolInvocationStatus(id, status, endedAt, outputPreview, outputRef)
        insertToolResult(result)
    }
}
