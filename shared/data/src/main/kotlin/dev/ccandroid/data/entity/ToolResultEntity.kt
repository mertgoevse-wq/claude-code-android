package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.ToolResult

@Entity(
    tableName = "tool_results",
    foreignKeys = [
        ForeignKey(
            entity = ToolInvocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["invocationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["invocationId"], unique = true),
        Index(value = ["toolUseId"]),
    ]
)
data class ToolResultEntity(
    @PrimaryKey val invocationId: String,
    val toolUseId: String,
    val output: String,
    val isError: Boolean = false,
    val exitCode: Int? = null,
    val durationMs: Long? = null,
) {
    companion object {
        fun fromDomain(result: ToolResult): ToolResultEntity = ToolResultEntity(
            invocationId = result.invocationId,
            toolUseId = result.toolUseId,
            output = result.output,
            isError = result.isError,
            exitCode = result.exitCode,
            durationMs = result.durationMs,
        )
    }

    fun toDomain(): ToolResult = ToolResult(
        invocationId = invocationId,
        toolUseId = toolUseId,
        output = output,
        isError = isError,
        exitCode = exitCode,
        durationMs = durationMs,
    )
}
