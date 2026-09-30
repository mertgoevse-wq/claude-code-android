package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.ccandroid.data.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun observeConversationById(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE projectId = :projectId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun observeConversationsForProject(projectId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeAllConversationsForProject(projectId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentConversations(limit: Int = 20): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getConversationBySessionId(sessionId: String): ConversationEntity?

    @Query("UPDATE conversations SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveConversation(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM conversations WHERE projectId = :projectId ORDER BY updatedAt DESC")
    suspend fun getConversationsForProject(projectId: String): List<ConversationEntity>
}
