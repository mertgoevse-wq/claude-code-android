package dev.ccandroid.core

public interface CryptoGateway {
    public suspend fun encrypt(alias: String, plainText: ByteArray): ByteArray
    public suspend fun decrypt(alias: String, cipherText: ByteArray): ByteArray
    public suspend fun deleteKey(alias: String): Boolean
    public suspend fun hasKey(alias: String): Boolean
}
