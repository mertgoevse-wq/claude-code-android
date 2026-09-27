package dev.ccandroid.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner radii. Nothing exists between 12 and 16 dp — a 14 dp radius is a
 * decision nobody made. Values are binding; see
 * `docs/03-design/design-tokens.md`.
 */
public object CcShape {
    public val radiusSmall: Dp = 8.dp // chips, badges, small inline elements
    public val radiusMedium: Dp = 12.dp // buttons, inputs
    public val radiusLarge: Dp = 16.dp // cards
    /** Avatars, the mark's container, the send button. A percentage, not a dp. */
    public const val radiusFullPercent: Int = 50

    /** Bottom sheets are rounded at the top and square at the bottom. */
    public val radiusSheetTop: Dp = 24.dp

    public val small: RoundedCornerShape = RoundedCornerShape(radiusSmall)
    public val medium: RoundedCornerShape = RoundedCornerShape(radiusMedium)
    public val large: RoundedCornerShape = RoundedCornerShape(radiusLarge)
    public val full: RoundedCornerShape = RoundedCornerShape(percent = radiusFullPercent)
    public val sheet: RoundedCornerShape =
        RoundedCornerShape(topStart = radiusSheetTop, topEnd = radiusSheetTop)
}

/**
 * Elevation is a border and a tonal step, not a drop shadow — this is why the
 * interface reads as flat rather than cheap.
 *
 * Dark mode needs *more* shadow to read as elevation, because a dark surface on
 * a dark background is distinguished by luminance rather than occlusion. These
 * are the only two shadows permitted in the codebase.
 */
/**
 * One elevation level: the surface it sits on, the border that separates it, and
 * an optional shadow.
 *
 * [shadowDp] and [shadowAlpha] are the two permitted shadows in the codebase,
 * both defined in the token document. Everything else separates by border and
 * tonal step alone.
 */
public data class CcElevationLevel(
    val surface: Color,
    /** Null at level 0, which sits directly on the background with no border. */
    val border: Color?,
    val shadowDp: Dp = 0.dp,
    val shadowAlpha: Float = 0f,
)

/** The four levels, for one theme. */
public data class CcElevation(
    val level0: CcElevationLevel,
    val level1: CcElevationLevel,
    val level2: CcElevationLevel,
    val level3: CcElevationLevel,
)

internal fun lightElevation(c: CcColorScheme): CcElevation = CcElevation(
    level0 = CcElevationLevel(surface = c.background, border = null),
    level1 = CcElevationLevel(surface = c.surface, border = c.border),
    level2 = CcElevationLevel(
        surface = c.surface,
        border = c.border,
        shadowDp = 2.dp,
        shadowAlpha = 0.06f,
    ),
    level3 = CcElevationLevel(
        surface = c.surface,
        border = c.border,
        shadowDp = 8.dp,
        shadowAlpha = 0.10f,
    ),
)

internal fun darkElevation(c: CcColorScheme): CcElevation = CcElevation(
    level0 = CcElevationLevel(surface = c.background, border = null),
    level1 = CcElevationLevel(surface = c.surface, border = c.border),
    level2 = CcElevationLevel(surface = c.surfaceSubtle, border = c.border),
    // Dark needs more shadow to read as elevation, not less.
    level3 = CcElevationLevel(
        surface = c.surfaceSubtle,
        border = c.borderStrong,
        shadowDp = 12.dp,
        shadowAlpha = 0.30f,
    ),
)
