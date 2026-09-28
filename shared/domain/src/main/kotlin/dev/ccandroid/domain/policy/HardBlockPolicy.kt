package dev.ccandroid.domain.policy

import dev.ccandroid.core.AppError
import dev.ccandroid.core.ErrorCode

public object HardBlockPolicy {
    private val destructiveGitRegex = Regex(
        "^git\\s+(push\\s+.*--delete|branch\\s+-D|tag\\s+-d|reset\\s+--hard|clean\\s+-f|checkout\\s+main|checkout\\s+master|rm\\s+)",
        RegexOption.IGNORE_CASE
    )

    private val deletionCommandsRegex = Regex(
        "^(rm\\s+|rmdir\\s+|unlink\\s+|shred\\s+|truncate\\s+-s\\s+0\\s+|>\\s*\\S+)",
        RegexOption.IGNORE_CASE
    )

    public fun checkCommand(command: String): AppError.PolicyRefusal? {
        val trimmed = command.trim()

        // Check for deletion commands
        if (deletionCommandsRegex.containsMatchIn(trimmed)) {
            return AppError.PolicyRefusal(
                code = ErrorCode.POLICY_BLOCK_DELETE,
                messageDe = "Diese Aktion würde etwas löschen. Das ist in dieser App nicht möglich.",
                messageEn = "This action would delete something. Hard Block 1 forbids deletion.",
                command = command
            )
        }

        // Check for destructive git commands
        if (destructiveGitRegex.containsMatchIn(trimmed)) {
            return AppError.PolicyRefusal(
                code = ErrorCode.POLICY_BLOCK_DELETE,
                messageDe = "Destruktive Git-Aktionen sind blockiert.",
                messageEn = "Destructive git actions are forbidden by Hard Block 1.",
                command = command
            )
        }

        // Check for push to default branch
        if (Regex("^git\\s+push\\s+.*\\b(main|master)\\b").containsMatchIn(trimmed)) {
            return AppError.PolicyRefusal(
                code = ErrorCode.POLICY_BLOCK_DEFAULT_BRANCH,
                messageDe = "Auf den Hauptzweig wird nie hochgeladen.",
                messageEn = "Pushes to default branches (main/master) are forbidden by Hard Block 4.",
                command = command
            )
        }

        // Check for push --all and --mirror
        if (Regex("^git\\s+push\\s+.*(--all|--mirror)\\b").containsMatchIn(trimmed)) {
            return AppError.PolicyRefusal(
                code = ErrorCode.POLICY_BLOCK_DEFAULT_BRANCH,
                messageDe = "Auf den Hauptzweig wird nie hochgeladen.",
                messageEn = "Push with --all or --mirror is forbidden by Hard Block 4.",
                command = command
            )
        }
        return null
    }
}
