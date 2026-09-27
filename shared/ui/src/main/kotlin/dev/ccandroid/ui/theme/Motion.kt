package dev.ccandroid.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec

/**
 * Motion tokens. Nothing exceeds [slowMillis] except the mark's idle loop: a user
 * waiting for a result reads a long transition as a hang.
 *
 * Full spec in `docs/03-design/motion.md`.
 */
public object CcMotion {
    public const val instantMillis: Int = 100 // press feedback, ripple
    public const val fastMillis: Int = 150 // chip toggle, checkbox
    public const val standardMillis: Int = 220 // card appearance, sheet present, list insert
    public const val slowMillis: Int = 320 // screen transition, mark state change
    public const val ambientMillis: Int = 2_800 // the mark at idle, the only loop

    /** Anything entering. */
    public val easeOut: Easing = FastOutSlowInEasing

    /**
     * Anything leaving — the accelerate curve, starting slow and speeding up.
     *
     * The token document names this `FastInSlowOutEasing`, which is the Android
     * Material Components name and does not exist in Compose. Compose spells the
     * same curve [LinearOutSlowInEasing]; the curve is identical, only the name
     * differs. Recorded as a doc amendment rather than silently substituted.
     */
    public val easeIn: Easing = LinearOutSlowInEasing

    /** Anything moving within. */
    public val easeInOut: Easing = FastOutSlowInEasing

    public const val emphasizedDampingRatio: Float = 0.7f
    public const val emphasizedStiffness: Float = 380f

    /** The mark's expression changes. */
    public fun <T> emphasized(visibilityThreshold: T? = null): SpringSpec<T> = SpringSpec(
        dampingRatio = emphasizedDampingRatio,
        stiffness = emphasizedStiffness,
        visibilityThreshold = visibilityThreshold,
    )
}

/**
 * Haptic patterns. Each reinforces a visual or textual state that already
 * exists — haptics are never the only signal for something — and all of them
 * respect the system haptic setting.
 */
public enum class CcHaptic {
    /** Button press, chip toggle. */
    TAP,

    /** Verification passed, run done: double tap, 40 ms apart. */
    SUCCESS,

    /** Unverified, retried: single long. */
    WARNING,

    /** Failed, refused: triple short. */
    ERROR,

    /** A permission sheet appears: single medium. */
    PERMISSION,
}
