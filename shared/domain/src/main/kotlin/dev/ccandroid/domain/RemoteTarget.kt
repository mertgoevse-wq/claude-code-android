package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class RemoteTargetKind {
    SSH,
    ORACLE,
    GITHUB_ACTIONS,
}

@Serializable
public enum class RemoteTargetStatus {
    UNKNOWN,
    ONLINE,
    OFFLINE,
    UNREACHABLE,
    QUOTA_EXCEEDED,
}

@Serializable
public data class RemoteTarget(
    val id: String,
    val projectId: String? = null,
    val name: String,
    val kind: RemoteTargetKind = RemoteTargetKind.SSH,
    val host: String? = null,
    val port: Int? = null,
    val user: String? = null,
    val secretProfileId: String,
    val status: RemoteTargetStatus = RemoteTargetStatus.UNKNOWN,
    val lastProbeAt: Long? = null,
    val lastProbeMessage: String? = null,
    val capabilitiesJson: String? = null,
    val isEnabled: Boolean = true,
)
