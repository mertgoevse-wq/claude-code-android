package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.ccandroid.data.entity.NotificationEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(event: NotificationEventEntity)

    @Update
    suspend fun updateNotification(event: NotificationEventEntity)

    @Query("SELECT * FROM notification_events WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: String): NotificationEventEntity?

    @Query("SELECT * FROM notification_events WHERE readAt IS NULL ORDER BY deliveredAt DESC")
    fun observeUnreadNotifications(): Flow<List<NotificationEventEntity>>

    @Query("SELECT * FROM notification_events ORDER BY deliveredAt DESC")
    fun observeAllNotifications(): Flow<List<NotificationEventEntity>>

    @Query("UPDATE notification_events SET readAt = :readAt WHERE id = :id")
    suspend fun markAsRead(id: String, readAt: Long = System.currentTimeMillis())

    @Query("UPDATE notification_events SET readAt = :readAt WHERE readAt IS NULL")
    suspend fun markAllAsRead(readAt: Long = System.currentTimeMillis())
}
