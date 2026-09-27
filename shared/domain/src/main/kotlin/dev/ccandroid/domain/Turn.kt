package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public data class Turn(
    val id: String,
    val conversationId: String,
    val turnNumber: Int,
    val createdAtMillis: Long = System.currentTimeMillis()
)
