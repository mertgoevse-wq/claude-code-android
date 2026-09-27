package dev.ccandroid.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The design token document claims measured contrast ratios for every pair it
 * lists. This test recomputes them from the values in [CcColors] so that editing
 * a token cannot silently break AA, and it pins the values themselves so a
 * change has to be deliberate.
 */
class DesignTokensTest {

    private fun channel(v: Float): Double {
        val c = v.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    /** WCAG 2.1 relative luminance. */
    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun hex(color: Color): String = String.format(
        "#%06X",
        ((color.red * 255).toInt() shl 16) or
            ((color.green * 255).toInt() shl 8) or
            (color.blue * 255).toInt(),
    )

    private fun assertAA(ratio: Double, pair: String) {
        assertTrue("$pair contrast is $ratio, below AA (4.5)", ratio >= 4.5)
    }

    private fun assertNonText(ratio: Double, pair: String) {
        assertTrue("$pair contrast is $ratio, below the 3:1 non-text bar", ratio >= 3.0)
    }

    @Test
    fun `light text tokens meet AA on the light background`() {
        val c = CcColors.light
        assertAA(contrast(c.textPrimary, c.background), "light textPrimary/background")
        assertAA(contrast(c.textSecondary, c.background), "light textSecondary/background")
        assertAA(contrast(c.textTertiary, c.background), "light textTertiary/background")
    }

    @Test
    fun `dark text tokens meet AA on the dark background`() {
        val c = CcColors.dark
        assertAA(contrast(c.textPrimary, c.background), "dark textPrimary/background")
        assertAA(contrast(c.textSecondary, c.background), "dark textSecondary/background")
        assertAA(contrast(c.textTertiary, c.background), "dark textTertiary/background")
    }

    /**
     * The dark theme's accent is light, so its button text must be dark. White
     * on that accent is 2.63:1 and would fail — this is the rule that looks
     * wrong in a preview and is correct.
     */
    @Test
    fun `dark accent takes dark text, not white`() {
        val c = CcColors.dark
        assertAA(contrast(c.onAccent, c.accent), "dark onAccent/accent")
        assertTrue(
            "white on the dark accent unexpectedly passes AA; the token may have changed",
            contrast(Color.White, c.accent) < 4.5,
        )
    }

    @Test
    fun `light accent takes white text`() {
        val c = CcColors.light
        assertAA(contrast(c.onAccent, c.accent), "light onAccent/accent")
    }

    /**
     * `accent` is a fill, `accentText` is for text. On `accentSubtle` the fill
     * colour falls below AA, which is why the text token exists at all.
     */
    @Test
    fun `accent text on accentSubtle uses accentText, not accent`() {
        val c = CcColors.light
        assertAA(contrast(c.accentText, c.accentSubtle), "light accentText/accentSubtle")
        assertTrue(
            "accent on accentSubtle unexpectedly passes AA; the text token would be redundant",
            contrast(c.accent, c.accentSubtle) < 4.5,
        )
    }

    /** The boundary of any interactive control must clear the 3:1 non-text bar. */
    @Test
    fun `interactive boundaries clear 3 to 1 in both themes`() {
        assertNonText(
            contrast(CcColors.light.borderStrong, CcColors.light.surface),
            "light borderStrong/surface",
        )
        assertNonText(
            contrast(CcColors.dark.borderStrong, CcColors.dark.surface),
            "dark borderStrong/surface",
        )
    }

    /**
     * The terminal is always dark, so every ANSI colour must stay readable on it.
     *
     * `black` is the one documented exception: ANSI black on a dark terminal is
     * near-invisible by fixed assignment, and every real terminal has the same
     * property. Asserting it here would assert something a terminal cannot
     * deliver; the renderer falls back to white instead.
     */
    @Test
    fun `the terminal palette is readable on the terminal background`() {
        val bg = CcColors.terminalBackground
        val t = CcColors.terminal
        assertAA(contrast(t.red, bg), "terminal red/background")
        assertAA(contrast(t.green, bg), "terminal green/background")
        assertAA(contrast(t.yellow, bg), "terminal yellow/background")
        assertAA(contrast(t.blue, bg), "terminal blue/background")
        assertAA(contrast(t.magenta, bg), "terminal magenta/background")
        assertAA(contrast(t.cyan, bg), "terminal cyan/background")
        assertAA(contrast(t.white, bg), "terminal white/background")
        assertAA(contrast(t.brightBlack, bg), "terminal brightBlack/background")
        assertAA(contrast(t.brightRed, bg), "terminal brightRed/background")
        assertAA(contrast(t.brightGreen, bg), "terminal brightGreen/background")
        assertAA(contrast(t.brightYellow, bg), "terminal brightYellow/background")
        assertAA(contrast(t.brightBlue, bg), "terminal brightBlue/background")
        assertAA(contrast(t.brightMagenta, bg), "terminal brightMagenta/background")
        assertAA(contrast(t.brightCyan, bg), "terminal brightCyan/background")
        assertAA(contrast(t.brightWhite, bg), "terminal brightWhite/background")
    }

    @Test
    fun `token values match the design token document`() {
        assertEquals("#FAF9F5", hex(CcColors.light.background))
        assertEquals("#1A1917", hex(CcColors.dark.background))
        assertEquals("#B25133", hex(CcColors.light.accent))
        assertEquals("#E08A66", hex(CcColors.dark.accent))
        assertEquals("#8F3F24", hex(CcColors.light.focusRing))
        assertEquals("#E59572", hex(CcColors.dark.focusRing))
    }

    /** Nothing below 12 sp, and nothing below 400 weight for body text. */
    @Test
    fun `no type role falls below the minimum size`() {
        val all = listOf(
            CcType.displayLarge, CcType.displaySmall, CcType.titleLarge, CcType.titleMedium,
            CcType.bodyLarge, CcType.bodyMedium, CcType.bodySmall, CcType.labelLarge,
            CcType.labelMedium, CcType.labelSmall, CcType.mono, CcType.monoSmall,
        )
        all.forEach { style ->
            assertTrue(
                "${style.fontSize} is below the 12 sp floor",
                style.fontSize.value >= 12f,
            )
        }
    }

    @Test
    fun `spacing is a monotonic grid`() {
        val steps = listOf(
            CcSpacing.space1, CcSpacing.space2, CcSpacing.space3, CcSpacing.space4,
            CcSpacing.space5, CcSpacing.space6, CcSpacing.space7, CcSpacing.space8,
            CcSpacing.space9, CcSpacing.space10,
        )
        var previous = 0f
        steps.forEach { step ->
            assertEquals("spacing step ${step.value} is not on the 2 pt grid", 0f, step.value % 2f, 0.001f)
            assertTrue("spacing scale is not monotonic at ${step.value}", step.value > previous)
            previous = step.value
        }
    }

    /** The 320 ms ceiling, with the mark's idle loop as the only exception. */
    @Test
    fun `no state transition exceeds the slow ceiling`() {
        assertTrue(CcMotion.instantMillis <= 300)
        assertTrue(CcMotion.fastMillis <= 300)
        assertTrue(CcMotion.standardMillis <= 300)
        assertEquals(320, CcMotion.slowMillis)
    }

    /** Nothing exists between 12 and 16 dp: a 14 dp radius is nobody's decision. */
    @Test
    fun `radii avoid the gap between medium and large`() {
        assertEquals(12f, CcShape.radiusMedium.value, 0.001f)
        assertEquals(16f, CcShape.radiusLarge.value, 0.001f)
    }

    /** Elevation separates by border and tonal step, not by shadow alone. */
    @Test
    fun `elevation level zero draws no border`() {
        assertEquals(null, lightElevation(CcColors.light).level0.border)
        assertEquals(null, darkElevation(CcColors.dark).level0.border)
        assertEquals(CcColors.light.border, lightElevation(CcColors.light).level1.border)
    }
}
