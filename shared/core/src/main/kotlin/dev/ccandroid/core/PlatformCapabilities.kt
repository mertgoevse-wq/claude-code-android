package dev.ccandroid.core

import kotlinx.serialization.Serializable

@Serializable
public data class PlatformCapabilities(
    val hasTerminalSupport: Boolean,
    val hasPtySupport: Boolean,
    val hasAvfSupport: Boolean,
    val hasProotSupport: Boolean,
    val is64Bit: Boolean,
    val abi: String
)
