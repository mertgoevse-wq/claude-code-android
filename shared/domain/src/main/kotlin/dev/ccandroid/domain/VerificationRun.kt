package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class VerificationState {
    NOT_STARTED,
    RUNNING,
    PASSED,
    FAILED,
    ERROR,
    UNVERIFIED,
}

@Serializable
public enum class TestParserType {
    GRADLE,
    NPM,
    PYTEST,
    CARGO,
    GO,
    GENERIC,
    NONE,
}

@Serializable
public data class VerificationRun(
    val id: String,
    val runId: String,
    val attempt: Int = 1,
    val state: VerificationState = VerificationState.NOT_STARTED,
    val commandCount: Int = 0,
    val durationMs: Long = 0L,
    val logRef: String? = null,
    val judgedAt: Long? = null,
    val createdAt: Long,
)

@Serializable
public data class TestResult(
    val id: String,
    val verificationRunId: String,
    val ordinal: Int,
    val command: String,
    val exitCode: Int,
    val durationMs: Long,
    val summaryLine: String,
    val failedTestNames: List<String> = emptyList(),
    val parserUsed: TestParserType = TestParserType.GENERIC,
)
