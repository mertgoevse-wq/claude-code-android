package dev.ccandroid.core

import kotlinx.coroutines.flow.Flow

public data class NetworkStatus(
    val isConnected: Boolean,
    val isMetered: Boolean,
    val isWifi: Boolean
)

public interface NetworkMonitor {
    public val status: Flow<NetworkStatus>
    public fun currentStatus(): NetworkStatus
}
