package dev.ccandroid.core

import kotlinx.serialization.Serializable

@Serializable
public data class BuildInfo(
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val gitSha: String,
    val isDebug: Boolean
)
