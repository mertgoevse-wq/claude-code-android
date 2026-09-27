package dev.ccandroid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import dev.ccandroid.ui.R

/**
 * Typography. Full scale in `docs/03-design/typography.md`.
 *
 * Inter and JetBrains Mono are bundled in the APK: no runtime download, no FOUT,
 * and no network dependency for rendering text. This app is the one thing that
 * must work offline, so a font that arrives over the network is a font that will
 * be missing when it matters. Both are SIL Open Font License 1.1.
 */
public object CcType {

    /**
     * The variable font, addressed through [FontVariation] so that weight
     * selection resolves against the same file rather than needing one file per
     * weight. The `opsz` axis is left at its default; the UI roles are small
     * enough that optical sizing has nothing useful to do.
     */
    @OptIn(ExperimentalTextApi::class)
    public val Inter: FontFamily = FontFamily(
        Font(
            resId = R.font.inter_variable,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
        Font(
            resId = R.font.inter_variable,
            weight = FontWeight.SemiBold,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(600)),
        ),
    )

    /** The terminal's face, and the face for any code a person reads. */
    @OptIn(ExperimentalTextApi::class)
    public val JetBrainsMono: FontFamily = FontFamily(
        Font(
            resId = R.font.jetbrains_mono_variable,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
    )

    /**
     * Numerals are tabular wherever a number changes — cost counters, token
     * counts, elapsed time, file counts. Proportional figures make a number
     * jitter as it updates, which during a live run is genuinely distracting.
     */
    private const val TABULAR = "tnum"

    /**
     * Large text needs its letters pulled closer or it looks loose; small text
     * needs a hair of space or it looks cramped and blurry. This is the single
     * most visible difference between a designed interface and a default one.
     */
    private fun spec(
        size: Int,
        lineHeight: Int,
        weight: FontWeight,
        tracking: Double,
        family: FontFamily = Inter,
        tabular: Boolean = false,
    ): TextStyle = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
        fontFeatureSettings = if (tabular) TABULAR else null,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    public val displayLarge: TextStyle = spec(34, 42, FontWeight.Normal, -0.5)
    public val displaySmall: TextStyle = spec(28, 36, FontWeight.Normal, -0.25)
    public val titleLarge: TextStyle = spec(22, 30, FontWeight.SemiBold, -0.2)
    public val titleMedium: TextStyle = spec(17, 24, FontWeight.SemiBold, -0.1)
    public val bodyLarge: TextStyle = spec(16, 25, FontWeight.Normal, 0.0)
    public val bodyMedium: TextStyle = spec(15, 23, FontWeight.Normal, 0.0)
    public val bodySmall: TextStyle = spec(14, 21, FontWeight.Normal, 0.1)
    public val labelLarge: TextStyle = spec(15, 20, FontWeight.SemiBold, 0.0)
    public val labelMedium: TextStyle = spec(13, 18, FontWeight.SemiBold, 0.1)
    public val labelSmall: TextStyle = spec(12, 16, FontWeight.Medium, 0.2)
    public val mono: TextStyle = spec(14, 21, FontWeight.Normal, 0.0, JetBrainsMono, tabular = true)
    public val monoSmall: TextStyle = spec(12, 18, FontWeight.Normal, 0.0, JetBrainsMono, tabular = true)

    /**
     * The same roles, exposed through Material 3 so that the components which
     * read `MaterialTheme.typography` get our scale rather than the default one.
     * A role absent from Material's slots stays here as a named token.
     */
    public val material: Typography = Typography(
        displayLarge = displayLarge,
        displaySmall = displaySmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
    )
}
