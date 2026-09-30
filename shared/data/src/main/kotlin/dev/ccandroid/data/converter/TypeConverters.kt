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

/**
 * Reads a stored enum name, falling back to [fallback] when the name is not one
 * this version knows.
 *
 * A value from a newer app version reaches a phone that skipped it on every
 * update that ships a new enum value, so `valueOf` throwing is a crash on a list
 * the user is looking at. The fallback is never the value that proceeds: it is
 * the value that asks the user. Every choice is in the table in
 * `docs/02-architecture/data-migrations.md`.
 */
private inline fun <reified T : Enum<T>> String?.fromStorage(fallback: T): T? =
    if (this == null) null else runCatching { enumValueOf<T>(this) }.getOrDefault(fallback)


class TypeConverters {

    @TypeConverter
    fun autonomyLevelToString(level: AutonomyLevel?): String? = level?.name

    @TypeConverter
    fun stringToAutonomyLevel(name: String?): AutonomyLevel? = name.fromStorage(AutonomyLevel.ASK_EVERYTHING)

    @TypeConverter
    fun projectKindToString(kind: ProjectKind?): String? = kind?.name

    @TypeConverter
    fun stringToProjectKind(name: String?): ProjectKind? = name.fromStorage(ProjectKind.LOCAL)

    @TypeConverter
    fun providerKindToString(kind: ProviderKind?): String? = kind?.name

    @TypeConverter
    fun stringToProviderKind(name: String?): ProviderKind? = name.fromStorage(ProviderKind.CUSTOM)

    @TypeConverter
    fun remoteTargetKindToString(kind: RemoteTargetKind?): String? = kind?.name

    @TypeConverter
    fun stringToRemoteTargetKind(name: String?): RemoteTargetKind? = name.fromStorage(RemoteTargetKind.SSH)

    @TypeConverter
    fun remoteTargetStatusToString(status: RemoteTargetStatus?): String? = status?.name

    @TypeConverter
    fun stringToRemoteTargetStatus(name: String?): RemoteTargetStatus? = name.fromStorage(RemoteTargetStatus.UNKNOWN)

    @TypeConverter
    fun runStateToString(state: RunState?): String? = state?.name

    @TypeConverter
    fun stringToRunState(name: String?): RunState? = name.fromStorage(RunState.INTERRUPTED)

    @TypeConverter
    fun verificationStateToString(state: VerificationState?): String? = state?.name

    @TypeConverter
    fun stringToVerificationState(name: String?): VerificationState? = name.fromStorage(VerificationState.UNVERIFIED)

    @TypeConverter
    fun toolInvocationStatusToString(status: ToolInvocationStatus?): String? = status?.name

    @TypeConverter
    fun stringToToolInvocationStatus(name: String?): ToolInvocationStatus? = name.fromStorage(ToolInvocationStatus.PENDING)

    @TypeConverter
    fun planStepStateToString(state: PlanStepState?): String? = state?.name

    @TypeConverter
    fun stringToPlanStepState(name: String?): PlanStepState? = name.fromStorage(PlanStepState.SKIPPED)

    @TypeConverter
    fun fileChangeTypeToString(type: FileChangeType?): String? = type?.name

    @TypeConverter
    fun stringToFileChangeType(name: String?): FileChangeType? = name.fromStorage(FileChangeType.MODIFIED)

    @TypeConverter
    fun diffDecisionToString(decision: DiffDecision?): String? = decision?.name

    @TypeConverter
    fun stringToDiffDecision(name: String?): DiffDecision? = name.fromStorage(DiffDecision.PENDING)

    @TypeConverter
    fun notificationChannelToString(channel: NotificationChannel?): String? = channel?.name

    @TypeConverter
    fun stringToNotificationChannel(name: String?): NotificationChannel? = name.fromStorage(NotificationChannel.RUNNER)

    @TypeConverter
    fun permissionModeToString(mode: PermissionMode?): String? = mode?.name

    @TypeConverter
    fun stringToPermissionMode(name: String?): PermissionMode? = name.fromStorage(PermissionMode.DEFAULT)

    @TypeConverter
    fun messageRoleToString(role: MessageRole?): String? = role?.name

    @TypeConverter
    fun stringToMessageRole(name: String?): MessageRole? = name.fromStorage(MessageRole.SYSTEM)

    @TypeConverter
    fun messagePartKindToString(kind: MessagePartKind?): String? = kind?.name

    @TypeConverter
    fun stringToMessagePartKind(name: String?): MessagePartKind? = name.fromStorage(MessagePartKind.TEXT)

    @TypeConverter
    fun offloadPolicyToString(policy: OffloadPolicy?): String? = policy?.name

    @TypeConverter
    fun stringToOffloadPolicy(name: String?): OffloadPolicy? = name.fromStorage(OffloadPolicy.NEVER)

    @TypeConverter
    fun skillSourceKindToString(kind: SkillSourceKind?): String? = kind?.name

    @TypeConverter
    fun stringToSkillSourceKind(name: String?): SkillSourceKind? = name.fromStorage(SkillSourceKind.BUILTIN)

    @TypeConverter
    fun skillInstallScopeToString(scope: SkillInstallScope?): String? = scope?.name

    @TypeConverter
    fun stringToSkillInstallScope(name: String?): SkillInstallScope? = name.fromStorage(SkillInstallScope.GLOBAL)

    @TypeConverter
    fun sessionLogCategoryToString(category: SessionLogCategory?): String? = category?.name

    @TypeConverter
    fun stringToSessionLogCategory(name: String?): SessionLogCategory? = name.fromStorage(SessionLogCategory.SYSTEM)

    @TypeConverter
    fun sessionLogSeverityToString(severity: SessionLogSeverity?): String? = severity?.name

    @TypeConverter
    fun stringToSessionLogSeverity(name: String?): SessionLogSeverity? = name.fromStorage(SessionLogSeverity.INFO)

    @TypeConverter
    fun testParserTypeToString(type: TestParserType?): String? = type?.name

    @TypeConverter
    fun stringToTestParserType(name: String?): TestParserType? = name.fromStorage(TestParserType.GENERIC)

    @TypeConverter
    fun listToJson(list: List<String>?): String? = list?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToList(jsonStr: String?): List<String>? = jsonStr?.let { json.decodeFromString<List<String>>(it) }

    @TypeConverter
    fun mapToJson(map: Map<String, Long>?): String? = map?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToMap(jsonStr: String?): Map<String, Long>? = jsonStr?.let { json.decodeFromString<Map<String, Long>>(it) }
}