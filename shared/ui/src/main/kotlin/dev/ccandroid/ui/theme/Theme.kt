package dev.ccandroid.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Every design token, resolved for the active theme. A component reads these
 * through [CcTheme] and never names a colour, spacing, radius, or duration
 * literal — that is what makes "a token resolves in one place only" true.
 *
 * The terminal palette is deliberately *not* themed: see [CcTerminalPalette].
 */
public data class CcThemeTokens(
    val colors: CcColorScheme,
    val typography: CcTypographyTokens,
    val elevation: CcElevation,
    val syntax: CcSyntaxPalette,
    val diff: CcDiffPalette,
) {
    val terminal: CcTerminalPalette get() = CcColors.terminal
    val terminalBackground: Color get() = CcColors.terminalBackground
}

/** The type scale, carried as a named holder so it can be passed as one value. */
public data class CcTypographyTokens(
    val displayLarge: TextStyle = CcType.displayLarge,
    val displaySmall: TextStyle = CcType.displaySmall,
    val titleLarge: TextStyle = CcType.titleLarge,
    val titleMedium: TextStyle = CcType.titleMedium,
    val bodyLarge: TextStyle = CcType.bodyLarge,
    val bodyMedium: TextStyle = CcType.bodyMedium,
    val bodySmall: TextStyle = CcType.bodySmall,
    val labelLarge: TextStyle = CcType.labelLarge,
    val labelMedium: TextStyle = CcType.labelMedium,
    val labelSmall: TextStyle = CcType.labelSmall,
    val mono: TextStyle = CcType.mono,
    val monoSmall: TextStyle = CcType.monoSmall,
)

public val LocalCcTokens = staticCompositionLocalOf<CcThemeTokens> {
    error("CcTheme tokens were read before CcTheme was applied.")
}

public object CcTheme {
    public val tokens: CcThemeTokens
        @Composable @ReadOnlyComposable get() = LocalCcTokens.current
}

private fun colorSchemeOf(c: CcColorScheme, base: ColorScheme): ColorScheme = base.copy(
    primary = c.accent,
    onPrimary = c.onAccent,
    primaryContainer = c.accentSubtle,
    onPrimaryContainer = c.accentText,
    secondary = c.info,
    onSecondary = c.onAccent,
    background = c.background,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.surfaceSubtle,
    onSurfaceVariant = c.textSecondary,
    outline = c.border,
    outlineVariant = c.borderStrong,
    error = c.danger,
    onError = c.onAccent,
    scrim = c.textPrimary,
)

/**
 * Applies the design system.
 *
 * Both themes are declared here and nowhere else, so a screen cannot disagree
 * with the token document about what a colour means.
 */
@Composable
public fun CcTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) CcColors.dark else CcColors.light
    val tokens = CcThemeTokens(
        colors = colors,
        typography = CcTypographyTokens(),
        elevation = if (darkTheme) darkElevation(colors) else lightElevation(colors),
        syntax = if (darkTheme) CcColors.syntaxDark else CcColors.syntaxLight,
        diff = if (darkTheme) CcColors.diffDark else CcColors.diffLight,
    )

    CompositionLocalProvider(LocalCcTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorSchemeOf(
                c = colors,
                base = if (darkTheme) darkColorScheme() else lightColorScheme(),
            ),
            typography = CcType.material,
            shapes = Shapes(
                extraSmall = CcShape.small,
                small = CcShape.small,
                medium = CcShape.medium,
                large = CcShape.large,
                extraLarge = CcShape.large,
            ),
            content = content,
        )
    }
}
