package dev.ccandroid.domain.policy

import dev.ccandroid.core.AppError
import dev.ccandroid.core.ErrorCode

public object HardBlockPolicy {
    private val destructiveGitRegex = Regex(
        "^git\\s+(push\\s+.*--delete|branch\\s+-D|tag\\s+-d|reset\\s+--hard|clean\\s+-f|checkout\\s+main|checkout\\s+master)",
        RegexOption.IGNORE_CASE
    )

    public fun checkCommand(command: String): AppError.PolicyRefusal? {
        val trimmed = command.trim()
        if (trimmed.startsWith("rm ") || trimmed.startsWith("rm -")) {
            return AppError.PolicyRefusal(
                code = ErrorCode.POLICY_BLOCK_DELETE,
                messageDe = "Diese Aktion würde etwas löschen. Das ist in dieser App nicht möglich.",
                messageEn = "This action would delete something. Hard block 1 forbids deletion.",
                command = command
            )
        }
        if (destructiveGitRegex.containsMatchIn(trimmed)) {
            return AppError.PolicyRefusal(
                code = ErrorCode.POLICY_BLOCK_DELETE,
                messageDe = "Destruktive Git-Aktionen sind blockiert.",
                messageEn = "Destructive git actions are forbidden by Hard Block 1.",
                command = command
            )
        }
        if (Regex("^git\\s+push\\s+.*\\b(main|master)\\b").containsMatchIn(trimmed)) {
            return AppError.PolicyRefusal(
                code = ErrorCode.POLICY_BLOCK_DEFAULT_BRANCH,
                messageDe = "Auf den Hauptzweig wird nie hochgeladen.",
                messageEn = "Pushes to default branches (main/master) are forbidden by Hard Block 4.",
                command = command
            )
        }
        return null
    }
}
