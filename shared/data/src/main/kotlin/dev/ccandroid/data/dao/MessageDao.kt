package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.ccandroid.data.entity.MessageEntity
import dev.ccandroid.data.entity.MessagePartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessagePart(part: MessagePartEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageParts(parts: List<MessagePartEntity>)

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE turnId = :turnId ORDER BY createdAt ASC")
    fun observeMessagesForTurn(turnId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM message_parts WHERE messageId = :messageId ORDER BY ordinal ASC")
    fun observeMessagePartsForMessage(messageId: String): Flow<List<MessagePartEntity>>

    @Query("SELECT * FROM message_parts WHERE messageId = :messageId ORDER BY ordinal ASC")
    suspend fun getMessagePartsForMessage(messageId: String): List<MessagePartEntity>

    @Query("UPDATE message_parts SET collapsed = :collapsed WHERE id = :partId")
    suspend fun updateMessagePartCollapse(partId: String, collapsed: Boolean)

    @Transaction
    suspend fun insertMessageWithParts(message: MessageEntity, parts: List<MessagePartEntity>) {
        insertMessage(message)
        if (parts.isNotEmpty()) {
            insertMessageParts(parts)
        }
    }
}
