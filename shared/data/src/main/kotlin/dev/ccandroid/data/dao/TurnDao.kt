package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.ccandroid.data.entity.TurnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TurnDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTurn(turn: TurnEntity)

    @Update
    suspend fun updateTurn(turn: TurnEntity)

    @Query("SELECT * FROM turns WHERE id = :id LIMIT 1")
    suspend fun getTurnById(id: String): TurnEntity?

    @Query("SELECT * FROM turns WHERE conversationId = :conversationId ORDER BY `index` ASC")
    fun observeTurnsForConversation(conversationId: String): Flow<List<TurnEntity>>

    @Query("SELECT * FROM turns WHERE conversationId = :conversationId ORDER BY `index` DESC LIMIT 1")
    suspend fun getLatestTurnForConversation(conversationId: String): TurnEntity?

    @Query("SELECT * FROM turns WHERE runId = :runId LIMIT 1")
    suspend fun getTurnByRunId(runId: String): TurnEntity?

    @Query("SELECT * FROM turns WHERE conversationId = :conversationId ORDER BY `index` ASC")
    suspend fun getTurnsForConversation(conversationId: String): List<TurnEntity>
}
