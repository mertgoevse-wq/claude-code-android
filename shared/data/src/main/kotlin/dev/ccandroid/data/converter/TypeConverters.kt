package dev.ccandroid.data.converter

import androidx.room.TypeConverter
import dev.ccandroid.domain.AutonomyLevel
import dev.ccandroid.domain.DiffDecision
import dev.ccandroid.domain.FileChangeType
import dev.ccandroid.domain.MessagePartKind
import dev.ccandroid.domain.MessageRole
import dev.ccandroid.domain.NotificationChannel
import dev.ccandroid.domain.OffloadPolicy
import dev.ccandroid.domain.PermissionMode
import dev.ccandroid.domain.PlanStepState
import dev.ccandroid.domain.ProjectKind
import dev.ccandroid.domain.ProviderKind
import dev.ccandroid.domain.RemoteTargetKind
import dev.ccandroid.domain.RemoteTargetStatus
import dev.ccandroid.domain.RunState
import dev.ccandroid.domain.SessionLogCategory
import dev.ccandroid.domain.SessionLogSeverity
import dev.ccandroid.domain.SkillInstallScope
import dev.ccandroid.domain.SkillSourceKind
import dev.ccandroid.domain.TestParserType
import dev.ccandroid.domain.ToolInvocationStatus
import dev.ccandroid.domain.VerificationState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class TypeConverters {

    @TypeConverter
    fun autonomyLevelToString(level: AutonomyLevel?): String? = level?.name

    @TypeConverter
    fun stringToAutonomyLevel(name: String?): AutonomyLevel? = name?.let { AutonomyLevel.valueOf(it) }

    @TypeConverter
    fun projectKindToString(kind: ProjectKind?): String? = kind?.name

    @TypeConverter
    fun stringToProjectKind(name: String?): ProjectKind? = name?.let { ProjectKind.valueOf(it) }

    @TypeConverter
    fun providerKindToString(kind: ProviderKind?): String? = kind?.name

    @TypeConverter
    fun stringToProviderKind(name: String?): ProviderKind? = name?.let { ProviderKind.valueOf(it) }

    @TypeConverter
    fun remoteTargetKindToString(kind: RemoteTargetKind?): String? = kind?.name

    @TypeConverter
    fun stringToRemoteTargetKind(name: String?): RemoteTargetKind? = name?.let { RemoteTargetKind.valueOf(it) }

    @TypeConverter
    fun remoteTargetStatusToString(status: RemoteTargetStatus?): String? = status?.name

    @TypeConverter
    fun stringToRemoteTargetStatus(name: String?): RemoteTargetStatus? = name?.let { RemoteTargetStatus.valueOf(it) }

    @TypeConverter
    fun runStateToString(state: RunState?): String? = state?.name

    @TypeConverter
    fun stringToRunState(name: String?): RunState? = name?.let { RunState.valueOf(it) }

    @TypeConverter
    fun verificationStateToString(state: VerificationState?): String? = state?.name

    @TypeConverter
    fun stringToVerificationState(name: String?): VerificationState? = name?.let { VerificationState.valueOf(it) }

    @TypeConverter
    fun toolInvocationStatusToString(status: ToolInvocationStatus?): String? = status?.name

    @TypeConverter
    fun stringToToolInvocationStatus(name: String?): ToolInvocationStatus? = name?.let { ToolInvocationStatus.valueOf(it) }

    @TypeConverter
    fun planStepStateToString(state: PlanStepState?): String? = state?.name

    @TypeConverter
    fun stringToPlanStepState(name: String?): PlanStepState? = name?.let { PlanStepState.valueOf(it) }

    @TypeConverter
    fun fileChangeTypeToString(type: FileChangeType?): String? = type?.name

    @TypeConverter
    fun stringToFileChangeType(name: String?): FileChangeType? = name?.let { FileChangeType.valueOf(it) }

    @TypeConverter
    fun diffDecisionToString(decision: DiffDecision?): String? = decision?.name

    @TypeConverter
    fun stringToDiffDecision(name: String?): DiffDecision? = name?.let { DiffDecision.valueOf(it) }

    @TypeConverter
    fun notificationChannelToString(channel: NotificationChannel?): String? = channel?.name

    @TypeConverter
    fun stringToNotificationChannel(name: String?): NotificationChannel? = name?.let { NotificationChannel.valueOf(it) }

    @TypeConverter
    fun permissionModeToString(mode: PermissionMode?): String? = mode?.name

    @TypeConverter
    fun stringToPermissionMode(name: String?): PermissionMode? = name?.let { PermissionMode.valueOf(it) }

    @TypeConverter
    fun messageRoleToString(role: MessageRole?): String? = role?.name

    @TypeConverter
    fun stringToMessageRole(name: String?): MessageRole? = name?.let { MessageRole.valueOf(it) }

    @TypeConverter
    fun messagePartKindToString(kind: MessagePartKind?): String? = kind?.name

    @TypeConverter
    fun stringToMessagePartKind(name: String?): MessagePartKind? = name?.let { MessagePartKind.valueOf(it) }

    @TypeConverter
    fun offloadPolicyToString(policy: OffloadPolicy?): String? = policy?.name

    @TypeConverter
    fun stringToOffloadPolicy(name: String?): OffloadPolicy? = name?.let { OffloadPolicy.valueOf(it) }

    @TypeConverter
    fun skillSourceKindToString(kind: SkillSourceKind?): String? = kind?.name

    @TypeConverter
    fun stringToSkillSourceKind(name: String?): SkillSourceKind? = name?.let { SkillSourceKind.valueOf(it) }

    @TypeConverter
    fun skillInstallScopeToString(scope: SkillInstallScope?): String? = scope?.name

    @TypeConverter
    fun stringToSkillInstallScope(name: String?): SkillInstallScope? = name?.let { SkillInstallScope.valueOf(it) }

    @TypeConverter
    fun sessionLogCategoryToString(category: SessionLogCategory?): String? = category?.name

    @TypeConverter
    fun stringToSessionLogCategory(name: String?): SessionLogCategory? = name?.let { SessionLogCategory.valueOf(it) }

    @TypeConverter
    fun sessionLogSeverityToString(severity: SessionLogSeverity?): String? = severity?.name

    @TypeConverter
    fun stringToSessionLogSeverity(name: String?): SessionLogSeverity? = name?.let { SessionLogSeverity.valueOf(it) }

    @TypeConverter
    fun testParserTypeToString(type: TestParserType?): String? = type?.name

    @TypeConverter
    fun stringToTestParserType(name: String?): TestParserType? = name?.let { TestParserType.valueOf(it) }

    @TypeConverter
    fun listToJson(list: List<String>?): String? = list?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToList(jsonStr: String?): List<String>? = jsonStr?.let { json.decodeFromString<List<String>>(it) }

    @TypeConverter
    fun mapToJson(map: Map<String, Long>?): String? = map?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToMap(jsonStr: String?): Map<String, Long>? = jsonStr?.let { json.decodeFromString<Map<String, Long>>(it) }
}