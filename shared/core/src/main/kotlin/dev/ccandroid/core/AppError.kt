package dev.ccandroid.core

import kotlinx.serialization.Serializable

@Serializable
public enum class ErrorCode {
    // Provider and network
    PROVIDER_AUTH_FAILED,
    PROVIDER_RATE_LIMITED,
    PROVIDER_OVERLOADED,
    PROVIDER_BILLING,
    PROVIDER_MODEL_NOT_FOUND,
    PROVIDER_BAD_REQUEST,
    PROVIDER_TIMEOUT,
    PROVIDER_UNREACHABLE,
    PROVIDER_MALFORMED_RESPONSE,
    NO_PROVIDER_CONFIGURED,

    // Engine
    ENGINE_NOT_INSTALLED,
    ENGINE_LAUNCH_FAILED,
    ENGINE_UNEXPECTED_VERSION,
    ENGINE_EXIT_NONZERO,
    ENGINE_KILLED,
    ENGINE_MALFORMED_OUTPUT,
    ENGINE_SESSION_NOT_FOUND,
    ENGINE_OUTPUT_TOO_LARGE,

    // Runtime and environment
    RUNTIME_PROFILE_MISSING,
    RUNTIME_CHECKSUM_MISMATCH,
    RUNTIME_CHECKSUM_UNREACHABLE,
    RUNTIME_INCOMPATIBLE,
    RUNTIME_INSUFFICIENT_STORAGE,
    RUNTIME_PTY_FAILED,
    RUNTIME_BINARY_PROBE_FAILED,

    // Project and version control
    PROJECT_PATH_MISSING,
    PROJECT_NOT_A_REPOSITORY,
    GIT_COMMAND_FAILED,
    GIT_DIRTY_WORKTREE,
    GIT_DETACHED_HEAD,
    GIT_BRANCH_EXISTS,
    GIT_PUSH_REJECTED,
    GIT_REMOTE_NOT_FOUND,
    GIT_PUSH_TO_DEFAULT_BLOCKED,

    // GitHub API
    GITHUB_NOT_AUTHENTICATED,
    GITHUB_RATE_LIMITED,
    GITHUB_PR_EXISTS,
    GITHUB_REPO_NOT_FOUND,
    GITHUB_SECONDARY_LIMIT,

    // Verification
    VERIFY_NO_COMMANDS,
    VERIFY_COMMAND_NOT_FOUND,
    VERIFY_FAILED,
    VERIFY_TIMEOUT,
    VERIFY_UNPARSABLE,

    // Permissions and policy
    POLICY_BLOCK_DELETE,
    POLICY_BLOCK_SPEND,
    POLICY_BLOCK_PUBLISH,
    POLICY_BLOCK_DEFAULT_BRANCH,
    POLICY_NEEDS_APPROVAL,
    POLICY_RATE_LIMITED_ATTEMPTS,

    // Skills
    SKILL_PARSE_FAILED,
    SKILL_VALIDATION_FAILED,
    SKILL_ALREADY_INSTALLED,
    SKILL_SOURCE_UNREACHABLE,
    SKILL_NAME_CONFLICT,

    // Remote runners
    RUNNER_UNREACHABLE,
    RUNNER_OFFLINE,
    RUNNER_QUOTA_EXCEEDED,
    RUNNER_MISSING_CAPABILITY,
    RUNNER_AUTH_FAILED,

    // Local and system
    STORAGE_FULL,
    NETWORK_OFFLINE,
    NETWORK_METERED,
    BATTERY_LOW,
    PERMISSION_DENIED_ANDROID,
    BIOMETRIC_UNAVAILABLE,

    // Generic
    UNKNOWN_ERROR
}

@Serializable
public sealed interface AppError {
    public val code: ErrorCode
    public val messageDe: String
    public val messageEn: String
    public val retryable: Boolean

    @Serializable
    public data class Simple(
        override val code: ErrorCode,
        override val messageDe: String,
        override val messageEn: String,
        override val retryable: Boolean = false,
        val details: String? = null
    ) : AppError

    @Serializable
    public data class PolicyRefusal(
        override val code: ErrorCode,
        override val messageDe: String,
        override val messageEn: String,
        val command: String? = null
    ) : AppError {
        override val retryable: Boolean get() = false
    }

    @Serializable
    public data class VerificationFailure(
        override val code: ErrorCode = ErrorCode.VERIFY_FAILED,
        val failedCount: Int,
        val totalCount: Int,
        val rawOutput: String? = null
    ) : AppError {
        override val messageDe: String get() = "$failedCount von $totalCount Prüfungen fehlgeschlagen."
        override val messageEn: String get() = "$failedCount of $totalCount checks failed."
        override val retryable: Boolean get() = true
    }
}
