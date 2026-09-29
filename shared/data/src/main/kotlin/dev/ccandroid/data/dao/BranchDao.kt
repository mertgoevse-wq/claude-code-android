package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.ccandroid.data.entity.BranchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchEntity)

    @Update
    suspend fun updateBranch(branch: BranchEntity)

    @Query("SELECT * FROM branches WHERE projectId = :projectId AND name = :name LIMIT 1")
    suspend fun getBranch(projectId: String, name: String): BranchEntity?

    @Query("SELECT * FROM branches WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeBranchesForProject(projectId: String): Flow<List<BranchEntity>>

    @Query("UPDATE branches SET isMerged = 1, updatedAt = :updatedAt WHERE projectId = :projectId AND name = :name")
    suspend fun markAsMerged(projectId: String, name: String, updatedAt: Long = System.currentTimeMillis())
}
