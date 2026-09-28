package dev.ccandroid.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ccandroid.ui.theme.CcMotion
import dev.ccandroid.ui.theme.CcTheme

/**
 * All eleven states for the mark per `docs/03-design/logo-animation.md`.
 * State is a pure function of the engine run state and active tool.
 */
public enum class MarkState {
    IDLE,
    THINKING,
    READING,
    WRITING,
    RUNNING,
    WAITING,
    VERIFYING,
    PAUSED,
    ERROR,
    DONE,
    UNVERIFIED,
}

/**
 * Geometric parameters for constructing the mark conforming to `docs/03-design/brand-assets.md`
 * and `docs/03-design/logo-animation.md`.
 */
public object ClaudeMarkGeometry {
    public const val INNER_RADIUS_RATIO: Float = 0.30f
    public const val VALLEY_RADIUS_RATIO: Float = 0.34f
    public const val OUTER_RADIUS_RATIO: Float = 0.46f
    public const val EYE_DIAMETER_RATIO: Float = 0.11f
    public const val EYE_SEPARATION_RATIO: Float = 0.24f
    public const val PUPIL_RATIO: Float = 0.40f
    public const val MAX_GAZE_RADIUS_DP: Float = 6f
    public val ROTATION_ANGLES: List<Float> = listOf(0f, 45f, 90f, 135f)

    /**
     * Builds a single teardrop lobe (a 90° arc pair) centered at (0, 0).
     */
    public fun createTeardropLobe(size: Float): Path {
        val rOuter = OUTER_RADIUS_RATIO * size
        val rArc = (rOuter * kotlin.math.sqrt(2.0)).toFloat()
        val path = Path()
        val topCenterY = -rOuter
        val topRect = Rect(
            left = -rArc,
            top = topCenterY - rArc,
            right = rArc,
            bottom = topCenterY + rArc,
        )
        path.arcTo(topRect, startAngleDegrees = 135f, sweepAngleDegrees = -90f, forceMoveTo = true)

        val bottomCenterY = rOuter
        val bottomRect = Rect(
            left = -rArc,
            top = bottomCenterY - rArc,
            right = rArc,
            bottom = bottomCenterY + rArc,
        )
        path.arcTo(bottomRect, startAngleDegrees = 315f, sweepAngleDegrees = -90f, forceMoveTo = false)
        path.close()
        return path
    }

    /**
     * Returns the four teardrop paths rotated 0, 45, 90, 135 deg around the origin.
     */
    public fun createTeardropPaths(size: Float): List<Path> {
        val baseLobe = createTeardropLobe(size)
        return ROTATION_ANGLES.map { angle ->
            val rotatedPath = Path()
            val matrix = Matrix()
            matrix.rotateZ(angle)
            rotatedPath.addPath(baseLobe)
            rotatedPath.transform(matrix)
            rotatedPath
        }
    }
}

/**
 * Character facial expression parameters for a given [MarkState].
 */
public data class ClaudeMarkExpression(
    val eyesVisible: Boolean,
    val lidClosureLeft: Float = 0f,
    val lidClosureRight: Float = 0f,
    val eyeScale: Float = 1.0f,
    val leftEyeExtraWidth: Float = 0f,
    val defaultGaze: Offset = Offset.Zero,
    val eyebrowLift: Float = 0f,
)

/**
 * Pure expression mapping for all 11 mark states per `docs/03-design/logo-animation.md`.
 */
public fun expressionFor(state: MarkState): ClaudeMarkExpression = when (state) {
    MarkState.IDLE -> ClaudeMarkExpression(
        eyesVisible = false,
    )
    MarkState.THINKING -> ClaudeMarkExpression(
        eyesVisible = true,
        lidClosureLeft = 0.12f,
        lidClosureRight = 0.12f,
        defaultGaze = Offset(-0.7f, -0.7f),
    )
    MarkState.READING -> ClaudeMarkExpression(
        eyesVisible = true,
        lidClosureLeft = 0.0f,
        lidClosureRight = 0.0f,
        defaultGaze = Offset(0.3f, 0.5f),
    )
    MarkState.WRITING -> ClaudeMarkExpression(
        eyesVisible = true,
        lidClosureLeft = 0.0f,
        lidClosureRight = 0.0f,
        defaultGaze = Offset(0.3f, 0.5f),
    )
    MarkState.RUNNING -> ClaudeMarkExpression(
        eyesVisible = true,
        lidClosureLeft = 0.18f,
        lidClosureRight = 0.18f,
        leftEyeExtraWidth = 1f,
        defaultGaze = Offset(0f, 1f),
    )
    MarkState.WAITING -> ClaudeMarkExpression(
        eyesVisible = true,
        lidClosureLeft = -0.20f,
        lidClosureRight = -0.20f,
        eyeScale = 1.20f,
        defaultGaze = Offset.Zero,
    )
    MarkState.VERIFYING -> ClaudeMarkExpression(
        eyesVisible = true,
        lidClosureLeft = 0.0f,
        lidClosureRight = 0.0f,
        eyebrowLift = 1f,
        defaultGaze = Offset(0f, 0.8f),
    )
    MarkState.PAUSED -> ClaudeMarkExpression(
        eyesVisible = true,
        lidClosureLeft = 0.50f,
        lidClosureRight = 0.50f,
        defaultGaze = Offset.Zero,
    )
    MarkState.ERROR -> ClaudeMarkExpression(
        eyesVisible = true,
        lidClosureLeft = 0.24f,
        lidClosureRight = 0.24f,
        defaultGaze = Offset(0f, 0.6f),
    )
    MarkState.DONE -> ClaudeMarkExpression(
        eyesVisible = true,
        lidClosureLeft = -0.10f,
        lidClosureRight = -0.10f,
        eyeScale = 1.06f,
        defaultGaze = Offset.Zero,
    )
    MarkState.UNVERIFIED -> ClaudeMarkExpression(
        eyesVisible = true,
        lidClosureLeft = 0.50f,
        lidClosureRight = 0.50f,
        defaultGaze = Offset.Zero,
    )
}

/**
 * Maps run state and active tool to [MarkState].
 * Pure function independent of Android runtime.
 */
public fun mapEventToMarkState(runState: String?, toolName: String?): MarkState {
    val cleanTool = toolName?.trim()
    val cleanRun = runState?.trim()?.uppercase()

    if (cleanTool != null && cleanTool.isNotEmpty()) {
        when {
            cleanTool.equals("Read", ignoreCase = true) ||
                cleanTool.equals("Grep", ignoreCase = true) ||
                cleanTool.equals("Glob", ignoreCase = true) ||
                cleanTool.equals("readFile", ignoreCase = true) ||
                cleanTool.equals("grepSearch", ignoreCase = true) -> return MarkState.READING

            cleanTool.equals("Edit", ignoreCase = true) ||
                cleanTool.equals("Write", ignoreCase = true) ||
                cleanTool.equals("NotebookEdit", ignoreCase = true) ||
                cleanTool.equals("editFile", ignoreCase = true) ||
                cleanTool.equals("writeFile", ignoreCase = true) -> return MarkState.WRITING

            cleanTool.equals("Bash", ignoreCase = true) ||
                cleanTool.equals("terminal", ignoreCase = true) ||
                cleanTool.equals("executeCommand", ignoreCase = true) -> return MarkState.RUNNING
        }
    }

    if (cleanRun != null && cleanRun.isNotEmpty()) {
        when {
            cleanRun == "IDLE" -> return MarkState.IDLE
            cleanRun == "THINKING" -> return MarkState.THINKING
            cleanRun == "READING" -> return MarkState.READING
            cleanRun == "WRITING" -> return MarkState.WRITING
            cleanRun == "RUNNING" -> return MarkState.RUNNING
            cleanRun == "WAITING" || cleanRun == "PERMISSION_REQUESTED" || cleanRun == "PERMISSION_REQUIRED" -> return MarkState.WAITING
            cleanRun == "VERIFYING" || cleanRun == "VERIFICATION" -> return MarkState.VERIFYING
            cleanRun == "PAUSED" || cleanRun == "SUSPENDED" -> return MarkState.PAUSED
            cleanRun == "ERROR" || cleanRun == "FAILED" || cleanRun.startsWith("ERROR") || cleanRun.startsWith("FAIL") -> return MarkState.ERROR
            cleanRun == "DONE" || cleanRun == "COMPLETED" || cleanRun == "FINISHED_VERIFIED" || cleanRun == "VERIFIED" -> return MarkState.DONE
            cleanRun == "UNVERIFIED" || cleanRun == "FINISHED_UNVERIFIED" -> return MarkState.UNVERIFIED
        }
    }

    return if (cleanTool != null && cleanTool.isNotEmpty()) {
        MarkState.RUNNING
    } else {
        MarkState.IDLE
    }
}

/**
 * Animated Claude Mark conforming strictly to `docs/03-design/logo-animation.md`.
 *
 * An eight-pointed form built from four rotated teardrops arranged around a centre.
 * Calm breath when idle, expressive character when working.
 */
@Composable
public fun AnimatedClaudeMark(
    state: MarkState,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    gazeOffset: Offset = Offset.Zero,
    reducedMotion: Boolean = false,
) {
    val tokens = CcTheme.tokens
    val density = LocalDensity.current
    val expression = remember(state) { expressionFor(state) }

    // Idle breathing animation: 2800 ms cycle, 1.00 -> 1.04 -> 1.00 (or 1.00 -> 1.02 -> 1.00 for reduced motion)
    // Continues at 60% amplitude when working.
    val targetBreathScale = remember(state, reducedMotion) {
        if (reducedMotion) {
            if (state == MarkState.IDLE) 1.02f else 1.012f
        } else {
            if (state == MarkState.IDLE) 1.04f else 1.024f
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ClaudeMarkBreath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1.00f,
        targetValue = targetBreathScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = CcMotion.ambientMillis / 2,
                easing = CcMotion.easeInOut,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ClaudeMarkBreathScale",
    )

    // Eye entry: 320 ms easeOut with 1.12 overshoot (or 150 ms fade in if reduced motion)
    val eyeAlpha by animateFloatAsState(
        targetValue = if (expression.eyesVisible) 1f else 0f,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = CcMotion.fastMillis)
        } else {
            tween(durationMillis = CcMotion.slowMillis, easing = CcMotion.easeOut)
        },
        label = "EyeAlpha",
    )

    val eyeEntryScale by animateFloatAsState(
        targetValue = if (expression.eyesVisible) expression.eyeScale else 0f,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = CcMotion.fastMillis)
        } else {
            spring(
                dampingRatio = 0.6f,
                stiffness = CcMotion.emphasizedStiffness,
            )
        },
        label = "EyeEntryScale",
    )

    // Expression transitions: stiffness 380f, damping 0.7f
    val animatedLidLeft by animateFloatAsState(
        targetValue = expression.lidClosureLeft,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = CcMotion.fastMillis)
        } else {
            CcMotion.emphasized()
        },
        label = "LidClosureLeft",
    )

    val animatedLidRight by animateFloatAsState(
        targetValue = expression.lidClosureRight,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = CcMotion.fastMillis)
        } else {
            CcMotion.emphasized()
        },
        label = "LidClosureRight",
    )

    val maxGazePx = with(density) { ClaudeMarkGeometry.MAX_GAZE_RADIUS_DP.dp.toPx() }
    val rawTargetGaze = if (gazeOffset != Offset.Zero) {
        val dist = gazeOffset.getDistance()
        if (dist > maxGazePx) gazeOffset * (maxGazePx / dist) else gazeOffset
    } else {
        expression.defaultGaze * maxGazePx
    }

    val animatedGaze by animateOffsetAsState(
        targetValue = rawTargetGaze,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(durationMillis = CcMotion.slowMillis, easing = CcMotion.easeInOut)
        },
        label = "GazeOffset",
    )

    val contentDesc = "Claude mark, ${state.name.lowercase()}"
    val markAccent = tokens.colors.accent
    val eyeBackground = tokens.colors.background

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = contentDesc }
            .graphicsLayer {
                scaleX = breathScale
                scaleY = breathScale
            },
    ) {
        val canvasSize = this.size.minDimension
        val cx = canvasSize / 2f
        val cy = canvasSize / 2f

        val baseLobe = ClaudeMarkGeometry.createTeardropLobe(canvasSize)
        ClaudeMarkGeometry.ROTATION_ANGLES.forEach { angle ->
            rotate(degrees = angle, pivot = Offset(cx, cy)) {
                translate(left = cx, top = cy) {
                    drawPath(path = baseLobe, color = markAccent)
                }
            }
        }

        if (eyeAlpha > 0.01f && eyeEntryScale > 0.01f) {
            val baseEyeDiameter = canvasSize * ClaudeMarkGeometry.EYE_DIAMETER_RATIO
            val eyeDiameter = baseEyeDiameter * eyeEntryScale
            val eyeRadius = eyeDiameter / 2f
            val separation = canvasSize * ClaudeMarkGeometry.EYE_SEPARATION_RATIO

            val pupilDiameter = eyeDiameter * ClaudeMarkGeometry.PUPIL_RATIO
            val pupilRadius = pupilDiameter / 2f
            val maxPupilDisplacement = (eyeRadius - pupilRadius).coerceAtLeast(0f)

            val gazeNormalized = if (maxGazePx > 0f) animatedGaze / maxGazePx else Offset.Zero
            val pupilOffset = Offset(
                x = (gazeNormalized.x * maxPupilDisplacement).coerceIn(-maxPupilDisplacement, maxPupilDisplacement),
                y = (gazeNormalized.y * maxPupilDisplacement).coerceIn(-maxPupilDisplacement, maxPupilDisplacement),
            )

            val leftCenter = Offset(cx - separation / 2f, cy)
            val rightCenter = Offset(cx + separation / 2f, cy)

            val extraLeftWidth = expression.leftEyeExtraWidth

            drawOval(
                color = eyeBackground,
                topLeft = Offset(leftCenter.x - eyeRadius - extraLeftWidth / 2f, leftCenter.y - eyeRadius),
                size = Size(eyeDiameter + extraLeftWidth, eyeDiameter),
                alpha = eyeAlpha,
            )
            drawCircle(
                color = markAccent,
                radius = pupilRadius,
                center = leftCenter + pupilOffset,
                alpha = eyeAlpha,
            )
            if (animatedLidLeft > 0f) {
                val lidHeight = eyeDiameter * animatedLidLeft.coerceIn(0f, 1f)
                drawRect(
                    color = markAccent,
                    topLeft = Offset(leftCenter.x - eyeRadius - extraLeftWidth / 2f, leftCenter.y - eyeRadius),
                    size = Size(eyeDiameter + extraLeftWidth, lidHeight),
                    alpha = eyeAlpha,
                )
            }

            drawOval(
                color = eyeBackground,
                topLeft = Offset(rightCenter.x - eyeRadius, rightCenter.y - eyeRadius),
                size = Size(eyeDiameter, eyeDiameter),
                alpha = eyeAlpha,
            )
            drawCircle(
                color = markAccent,
                radius = pupilRadius,
                center = rightCenter + pupilOffset,
                alpha = eyeAlpha,
            )
            if (animatedLidRight > 0f) {
                val lidHeight = eyeDiameter * animatedLidRight.coerceIn(0f, 1f)
                drawRect(
                    color = markAccent,
                    topLeft = Offset(rightCenter.x - eyeRadius, rightCenter.y - eyeRadius),
                    size = Size(eyeDiameter, lidHeight),
                    alpha = eyeAlpha,
                )
            }

            if (expression.eyebrowLift > 0f) {
                val browY = cy - eyeRadius - 2f
                drawLine(
                    color = eyeBackground,
                    start = Offset(leftCenter.x - eyeRadius, browY),
                    end = Offset(leftCenter.x + eyeRadius, browY),
                    strokeWidth = expression.eyebrowLift,
                    alpha = eyeAlpha,
                )
                drawLine(
                    color = eyeBackground,
                    start = Offset(rightCenter.x - eyeRadius, browY),
                    end = Offset(rightCenter.x + eyeRadius, browY),
                    strokeWidth = expression.eyebrowLift,
                    alpha = eyeAlpha,
                )
            }
        }
    }
}
