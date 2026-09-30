package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.ccandroid.data.entity.ProjectSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSetting(setting: ProjectSettingEntity)

    @Update
    suspend fun updateSetting(setting: ProjectSettingEntity)

    @Query("SELECT * FROM project_settings WHERE projectId = :projectId LIMIT 1")
    suspend fun getSettingForProject(projectId: String): ProjectSettingEntity?

    @Query("SELECT * FROM project_settings WHERE projectId = :projectId LIMIT 1")
    fun observeSettingForProject(projectId: String): Flow<ProjectSettingEntity?>
}
