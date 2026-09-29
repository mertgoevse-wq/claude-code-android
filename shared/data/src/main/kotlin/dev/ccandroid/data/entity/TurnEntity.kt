package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.TurnState

@Entity(
    tableName = "turns",
    indices = [Index(value = ["conversationId", "index"], unique = true)]
)
data class TurnEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val index: Int,
    val userMessageId: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val state: TurnState,
    val runId: String? = null,
    val costUsd: Long = 0,
    val attemptCount: Int = 1,
) {
    companion object {
        fun fromDomain(turn: dev.ccandroid.domain.Turn): TurnEntity = TurnEntity(
            id = turn.id,
            conversationId = turn.conversationId,
            index = turn.index,
            userMessageId = turn.userMessageId,
            startedAt = turn.startedAt,
            endedAt = turn.endedAt,
            state = turn.state,
            runId = turn.runId,
            costUsd = turn.costUsd,
            attemptCount = turn.attemptCount,
        )
    }

    fun toDomain(): dev.ccandroid.domain.Turn = dev.ccandroid.domain.Turn(
        id = id,
        conversationId = conversationId,
        index = index,
        userMessageId = userMessageId,
        startedAt = startedAt,
        endedAt = endedAt,
        state = state,
        runId = runId,
        costUsd = costUsd,
        attemptCount = attemptCount,
    )
}