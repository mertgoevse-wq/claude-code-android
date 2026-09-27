package dev.ccandroid.core

import kotlinx.coroutines.flow.Flow

public data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)

public interface ProcessGateway {
    public suspend fun execute(
        command: List<String>,
        workingDir: String? = null,
        environment: Map<String, String> = emptyMap()
    ): ProcessResult

    public fun stream(
        command: List<String>,
        workingDir: String? = null,
        environment: Map<String, String> = emptyMap()
    ): Flow<String>

    public suspend fun killProcessTree(pid: Long): Boolean
}
