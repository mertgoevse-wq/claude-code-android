package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.ccandroid.data.entity.RemoteTargetEntity
import dev.ccandroid.domain.RemoteTargetStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteTargetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRemoteTarget(target: RemoteTargetEntity)

    @Update
    suspend fun updateRemoteTarget(target: RemoteTargetEntity)

    @Query("SELECT * FROM remote_targets WHERE id = :id LIMIT 1")
    suspend fun getRemoteTargetById(id: String): RemoteTargetEntity?

    @Query("SELECT * FROM remote_targets ORDER BY name ASC")
    fun observeAllRemoteTargets(): Flow<List<RemoteTargetEntity>>

    @Query("SELECT * FROM remote_targets WHERE (projectId IS NULL OR projectId = :projectId) AND isEnabled = 1 ORDER BY name ASC")
    fun observeAvailableTargets(projectId: String): Flow<List<RemoteTargetEntity>>

    @Query("UPDATE remote_targets SET status = :status, lastProbeAt = :probeAt, lastProbeMessage = :probeMessage, capabilitiesJson = :capabilities WHERE id = :id")
    suspend fun updateProbeResult(
        id: String,
        status: RemoteTargetStatus,
        probeAt: Long = System.currentTimeMillis(),
        probeMessage: String? = null,
        capabilities: String? = null,
    )
}
