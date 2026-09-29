package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.ccandroid.data.entity.CheckpointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckpointDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCheckpoint(checkpoint: CheckpointEntity)

    @Query("SELECT * FROM checkpoints WHERE id = :id LIMIT 1")
    suspend fun getCheckpointById(id: String): CheckpointEntity?

    @Query("SELECT * FROM checkpoints WHERE runId = :runId ORDER BY createdAt ASC")
    fun observeCheckpointsForRun(runId: String): Flow<List<CheckpointEntity>>
}
