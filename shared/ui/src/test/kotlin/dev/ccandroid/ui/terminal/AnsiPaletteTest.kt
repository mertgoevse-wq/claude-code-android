package dev.ccandroid.ui.terminal

import androidx.compose.ui.graphics.Color
import dev.ccandroid.ui.theme.CcColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The ANSI palette is the one place a component owns colours rather than only
 * reading tokens, and it is the one place a contrast failure is invisible until
 * somebody is reading a build log in bright sunlight. The token document
 * quotes a ratio for every entry; this recomputes them the way
 * `DesignTokensTest` does for the semantic colours.
 *
 * The rules, from `docs/03-design/color-and-contrast.md`: 4.5:1 for body text
 * at 14 sp and up. The terminal's default text is 14 sp, so the bar is 4.5.
 */
class AnsiPaletteTest {

    private fun channel(v: Float): Double {
        val c = v.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

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

    private fun contrastReport(): String = (0..15).joinToString("\n") { index ->
        val name = AnsiPalette.name(index)
        val ratio = contrast(AnsiPalette.foreground(AnsiColor.Ansi(index)), CcColors.terminalBackground)
        String.format("%-13s %s  %5.2f:1  %s", name, hex(AnsiPalette.foreground(AnsiColor.Ansi(index))), ratio, if (ratio >= 4.5) "AA" else "BELOW AA")
    }

    /**
     * The palette the token document publishes, indexed by ANSI number. This is
     * the identity mapping asserted in [AnsiParserTest]; here it is asserted
     * against the values themselves so an edit to a token is a deliberate act.
     */
    private val documented: Map<Int, String> = mapOf(
        0 to "#1F1E1B", 1 to "#E8796B", 2 to "#6FBE8C", 3 to "#D9A94A",
        4 to "#79AEE0", 5 to "#D79AD2", 6 to "#7FD4D8", 7 to "#D8D5CD",
        8 to "#9C9891", 9 to "#F29A8E", 10 to "#8FD3A6", 11 to "#E8C46B",
        12 to "#9CC3E8", 13 to "#E6B6E2", 14 to "#9FE2E6", 15 to "#F2F0EA",
    )

    @Test
    fun `all sixteen palette entries match the design token document`() {
        documented.forEach { (index, value) ->
            assertEquals(
                "ANSI $index (${AnsiPalette.name(index)}) does not match the token document",
                value,
                hex(AnsiPalette.foreground(AnsiColor.Ansi(index))),
            )
        }
    }

    /**
     * Every entry except `black` must clear 4.5:1 on the terminal background.
     *
     * `black` is the documented exception: ANSI assignments are fixed, so ANSI
     * black on a dark terminal is near-invisible, and [AnsiPalette.readableOn]
     * substitutes the white entry at render time rather than shipping 1:1 text.
     */
    @Test
    fun `every ANSI colour except black clears AA on the terminal background`() {
        for (index in 1..15) {
            val color = AnsiPalette.foreground(AnsiColor.Ansi(index))
            val ratio = contrast(color, CcColors.terminalBackground)
            assertTrue(
                "ANSI $index (${AnsiPalette.name(index)}) is ${"%.2f".format(ratio)}:1 on the " +
                    "terminal background, below AA\n${contrastReport()}",
                ratio >= 4.5,
            )
        }
    }

    /** The exception is real, and it is the only one. */
    @Test
    fun `black is the single documented exception and is replaced at render time`() {
        val black = AnsiPalette.foreground(AnsiColor.Ansi(0))
        assertTrue(
            "ANSI black unexpectedly clears AA; the documented fallback is now dead code",
            contrast(black, CcColors.terminalBackground) < 4.5,
        )
        assertEquals(
            AnsiPalette.foreground(AnsiColor.Ansi(7)),
            AnsiPalette.readableOn(black, CcColors.terminalBackground),
        )
    }

    /** Backgrounds are the same colours, so the same bar applies to them. */
    @Test
    fun `backgrounds use the same palette as foregrounds`() {
        documented.forEach { (index, value) ->
            assertEquals(value, hex(AnsiPalette.background(AnsiColor.Ansi(index))))
        }
    }

    /** A background is only useful if text on it stays readable. */
    @Test
    fun `the default text colour clears AA on every readable background`() {
        val defaultText = AnsiPalette.foreground(AnsiColor.Ansi(7))
        for (index in 0..15) {
            val background = AnsiPalette.background(AnsiColor.Ansi(index))
            val text = AnsiPalette.readableOn(defaultText, background)
            val ratio = contrast(text, background)
            assertTrue(
                "readable text on ${AnsiPalette.name(index)} is ${"%.2f".format(ratio)}:1, below AA",
                ratio >= 4.5,
            )
        }
    }

    @Test
    fun `a 256 colour index is resolved through the same two branches`() {
        // 0-15 is the ANSI block, so it must be identical to the SGR mapping.
        assertEquals(
            AnsiPalette.foreground(AnsiColor.Ansi(3)),
            AnsiPalette.foreground(AnsiColor.Indexed(3)),
        )
        // 16 and up is the xterm cube, which is not the app's palette: the raw
        // cell is black, and [AnsiPaletteTest.an unreadable 256 colour is
        // replaced rather than shipped] is what asserts the substitution. The
        // two cannot be asserted on the same call, because the substitution is
        // exactly the difference between them.
        assertEquals(Color(0xFF000000), Color(Ansi256.argb(16)))
    }

    @Test
    fun `an unreadable 256 colour is replaced rather than shipped`() {
        // xterm 16 is the cube's black cell: unreadable on this background, so
        // the same fallback the ANSI black gets applies to computed colours too.
        assertEquals(
            AnsiPalette.foreground(AnsiColor.Ansi(7)),
            AnsiPalette.foreground(AnsiColor.Indexed(16)),
        )
    }
}
