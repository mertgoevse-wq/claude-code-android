package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.ccandroid.data.entity.ModelSpecEntity
import dev.ccandroid.data.entity.ProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ProviderEntity)

    @Update
    suspend fun updateProvider(provider: ProviderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModelSpecs(specs: List<ModelSpecEntity>)

    @Query("SELECT * FROM providers ORDER BY name ASC")
    fun observeAllProviders(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE isEnabled = 1 ORDER BY isDefault DESC, name ASC")
    fun observeEnabledProviders(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE id = :id LIMIT 1")
    suspend fun getProviderById(id: String): ProviderEntity?

    @Query("SELECT * FROM providers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProvider(): ProviderEntity?

    @Query("SELECT * FROM model_specs WHERE providerId = :providerId ORDER BY displayName ASC")
    fun observeModelSpecsForProvider(providerId: String): Flow<List<ModelSpecEntity>>

    @Query("SELECT * FROM model_specs ORDER BY displayName ASC")
    fun observeAllModelSpecs(): Flow<List<ModelSpecEntity>>

    @Query("SELECT * FROM model_specs WHERE id = :modelId LIMIT 1")
    suspend fun getModelSpecById(modelId: String): ModelSpecEntity?

    @Query("UPDATE providers SET isDefault = 0")
    suspend fun clearDefaultProvider()

    @Query("UPDATE providers SET isDefault = 1 WHERE id = :id")
    suspend fun setAsDefaultProvider(id: String)

    @Transaction
    suspend fun setDefaultProvider(id: String) {
        clearDefaultProvider()
        setAsDefaultProvider(id)
    }

    @Query("UPDATE providers SET lastTestedAt = :testedAt, lastTestResult = :testResult WHERE id = :id")
    suspend fun updateTestResult(id: String, testedAt: Long, testResult: String)

    @Transaction
    suspend fun updateProviderWithModels(provider: ProviderEntity, models: List<ModelSpecEntity>) {
        insertProvider(provider)
        if (models.isNotEmpty()) {
            insertModelSpecs(models)
        }
    }
}
