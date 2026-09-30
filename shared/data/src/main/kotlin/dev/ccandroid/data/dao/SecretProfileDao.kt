package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.ccandroid.data.entity.SecretProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecretProfileDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSecretProfile(profile: SecretProfileEntity)

    @Update
    suspend fun updateSecretProfile(profile: SecretProfileEntity)

    @Query("SELECT * FROM secret_profiles WHERE id = :id LIMIT 1")
    suspend fun getSecretProfileById(id: String): SecretProfileEntity?

    @Query("SELECT * FROM secret_profiles ORDER BY name ASC")
    fun observeAllSecretProfiles(): Flow<List<SecretProfileEntity>>

    @Query("UPDATE secret_profiles SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun markUsed(id: String, timestamp: Long = System.currentTimeMillis())
}
