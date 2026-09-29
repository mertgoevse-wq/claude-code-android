package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.ccandroid.data.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun observeProjectById(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY lastRunAt DESC, createdAt DESC")
    fun observeActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY lastRunAt DESC, createdAt DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("UPDATE projects SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveProject(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET isArchived = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun unarchiveProject(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM projects WHERE remoteOwner = :owner AND remoteName = :name LIMIT 1")
    suspend fun getProjectByRemote(owner: String, name: String): ProjectEntity?
}
