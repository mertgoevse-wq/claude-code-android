package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.TestParserType
import dev.ccandroid.domain.TestResult

@Entity(
    tableName = "test_results",
    foreignKeys = [
        ForeignKey(
            entity = VerificationRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["verificationRunId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["verificationRunId", "ordinal"], unique = true)
    ]
)
data class TestResultEntity(
    @PrimaryKey val id: String,
    val verificationRunId: String,
    val ordinal: Int,
    val command: String,
    val exitCode: Int,
    val durationMs: Long,
    val summaryLine: String,
    val failedTestNames: List<String> = emptyList(),
    val parserUsed: TestParserType = TestParserType.GENERIC,
) {
    companion object {
        fun fromDomain(result: TestResult): TestResultEntity = TestResultEntity(
            id = result.id,
            verificationRunId = result.verificationRunId,
            ordinal = result.ordinal,
            command = result.command,
            exitCode = result.exitCode,
            durationMs = result.durationMs,
            summaryLine = result.summaryLine,
            failedTestNames = result.failedTestNames,
            parserUsed = result.parserUsed,
        )
    }

    fun toDomain(): TestResult = TestResult(
        id = id,
        verificationRunId = verificationRunId,
        ordinal = ordinal,
        command = command,
        exitCode = exitCode,
        durationMs = durationMs,
        summaryLine = summaryLine,
        failedTestNames = failedTestNames,
        parserUsed = parserUsed,
    )
}
