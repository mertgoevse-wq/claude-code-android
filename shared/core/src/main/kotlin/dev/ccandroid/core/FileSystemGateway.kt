package dev.ccandroid.core

public data class FileMetadata(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedMillis: Long
)

public interface FileSystemGateway {
    public suspend fun listFiles(path: String): List<FileMetadata>
    public suspend fun readFile(path: String): ByteArray
    public suspend fun readText(path: String): String
    public suspend fun writeFile(path: String, content: ByteArray): Boolean
    public suspend fun writeText(path: String, content: String): Boolean
    public suspend fun exists(path: String): Boolean
    public suspend fun createDirectories(path: String): Boolean
}
