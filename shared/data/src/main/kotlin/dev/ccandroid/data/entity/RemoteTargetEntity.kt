package dev.ccandroid.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ccandroid.domain.RemoteTarget
import dev.ccandroid.domain.RemoteTargetKind
import dev.ccandroid.domain.RemoteTargetStatus

@Entity(
    tableName = "remote_targets",
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["secretProfileId"]),
    ]
)
data class RemoteTargetEntity(
    @PrimaryKey val id: String,
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
) {
    companion object {
        fun fromDomain(target: RemoteTarget): RemoteTargetEntity = RemoteTargetEntity(
            id = target.id,
            projectId = target.projectId,
            name = target.name,
            kind = target.kind,
            host = target.host,
            port = target.port,
            user = target.user,
            secretProfileId = target.secretProfileId,
            status = target.status,
            lastProbeAt = target.lastProbeAt,
            lastProbeMessage = target.lastProbeMessage,
            capabilitiesJson = target.capabilitiesJson,
            isEnabled = target.isEnabled,
        )
    }

    fun toDomain(): RemoteTarget = RemoteTarget(
        id = id,
        projectId = projectId,
        name = name,
        kind = kind,
        host = host,
        port = port,
        user = user,
        secretProfileId = secretProfileId,
        status = status,
        lastProbeAt = lastProbeAt,
        lastProbeMessage = lastProbeMessage,
        capabilitiesJson = capabilitiesJson,
        isEnabled = isEnabled,
    )
}
