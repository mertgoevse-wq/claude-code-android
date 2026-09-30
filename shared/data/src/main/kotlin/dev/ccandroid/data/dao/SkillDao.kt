package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.ccandroid.data.entity.SkillEntity
import dev.ccandroid.data.entity.SkillInstallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity)

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkillInstall(install: SkillInstallEntity)

    @Query("SELECT * FROM skills WHERE id = :id LIMIT 1")
    suspend fun getSkillById(id: String): SkillEntity?

    @Query("SELECT * FROM skills WHERE name = :name LIMIT 1")
    suspend fun getSkillByName(name: String): SkillEntity?

    @Query("SELECT * FROM skills ORDER BY name ASC")
    fun observeAllSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skill_installs WHERE skillId = :skillId")
    fun observeInstallsForSkill(skillId: String): Flow<List<SkillInstallEntity>>

    @Query("SELECT * FROM skill_installs WHERE scope = 'GLOBAL' AND isEnabled = 1")
    fun observeGlobalInstalls(): Flow<List<SkillInstallEntity>>

    @Query("SELECT * FROM skill_installs WHERE projectId = :projectId AND isEnabled = 1")
    fun observeProjectInstalls(projectId: String): Flow<List<SkillInstallEntity>>

    @Query("UPDATE skill_installs SET isEnabled = :enabled WHERE localId = :localId")
    suspend fun updateInstallState(localId: Long, enabled: Boolean)
}
