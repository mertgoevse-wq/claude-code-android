package dev.ccandroid.domain

import kotlinx.serialization.Serializable

@Serializable
public enum class AutonomyLevel {
    MANUAL,
    INTERACTIVE,
    AUTONOMOUS,
    FULL_AUTONOMY
}
