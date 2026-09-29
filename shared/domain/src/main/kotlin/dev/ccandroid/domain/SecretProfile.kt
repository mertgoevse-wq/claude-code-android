package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public data class SecretProfile(
    val id: String,
    val name: String,
    val cipherText: String,
    val iv: String,
    val keyAlias: String,
    val hint: String,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
)
