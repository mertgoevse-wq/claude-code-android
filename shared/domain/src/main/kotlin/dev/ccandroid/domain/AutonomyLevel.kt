package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class AutonomyLevel {
    ASK_EVERYTHING,
    ASK_RISKY,
    AUTO_WITH_CHECKPOINTS,
    FULL_AUTO,
}
