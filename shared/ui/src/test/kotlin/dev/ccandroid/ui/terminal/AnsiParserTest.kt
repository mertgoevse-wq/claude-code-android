package dev.ccandroid.ui.terminal

import androidx.compose.ui.graphics.Color
import dev.ccandroid.ui.theme.CcColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The raw control characters, written as escapes.
 *
 * A literal ESC or BEL in a source file is invisible in an editor and in a
 * diff, so a sequence that lost its ESC would still read as a test of
 * escape parsing. Spelling them out is the difference between a test and a
 * mystery.
 */
private const val ESC: String = "\u001B"
private const val BEL: String = "\u0007"
private const val CR: String = "\u000D"

/**
 * The parser's contract is that no input can lose text. A shell emits escape
 * sequences this parser has never seen, truncates streams mid-sequence, and
 * occasionally emits something malformed; in every one of those cases the
 * correct behaviour is to render what was there rather than to throw or to
 * swallow.
 *
 * These tests therefore assert on the rendered text as much as on the styling:
 * a parser that produced beautiful colours while eating a byte would pass a
 * colour-only test and lose a build log.
 */
class AnsiParserTest {

    private fun render(input: String): String =
        AnsiParser.parse(input).joinToString("") { it.text }

    /**
     * The last span, not the only one. `[0m` separates text from text, so a
     * sequence like `...mx[0my` is two spans by construction; asserting on
     * "the span" means the one carrying the text under test, which is the last.
     */
    private fun single(input: String): AnsiSpan = AnsiParser.parse(input).last()

    private fun hex(argb: Int): String = String.format("#%06X", argb and 0xFFFFFF)

    private fun hex(color: Color): String = hex(
        ((color.red * 255).toInt() shl 16) or
            ((color.green * 255).toInt() shl 8) or
            (color.blue * 255).toInt(),
    )

    @Test
    fun `empty input parses to no spans`() {
        assertEquals(emptyList<AnsiSpan>(), AnsiParser.parse(""))
    }

    @Test
    fun `plain text is one span carrying the whole string`() {
        val spans = AnsiParser.parse("claude --version")
        assertEquals(1, spans.size)
        assertEquals("claude --version", spans[0].text)
        assertEquals(AnsiColor.Ansi(7), spans[0].style.foreground)
        assertNull(spans[0].style.background)
        assertTrue(!spans[0].style.bold)
    }

    @Test
    fun `the sixteen ANSI colours are the identity mapping of indices 0 to 15`() {
        val palette = CcColors.terminal
        val expected = listOf(
            palette.black, palette.red, palette.green, palette.yellow,
            palette.blue, palette.magenta, palette.cyan, palette.white,
            palette.brightBlack, palette.brightRed, palette.brightGreen,
            palette.brightYellow, palette.brightBlue, palette.brightMagenta,
            palette.brightCyan, palette.brightWhite,
        )
        expected.forEachIndexed { index, color ->
            assertEquals(
                "256 index $index is not the token's ANSI $index",
                hex(color),
                hex(AnsiPalette.foreground(AnsiColor.Ansi(index))),
            )
        }
    }

    @Test
    fun `SGR 30 to 37 select the eight normal foreground colours`() {
        val normal = listOf(
            CcColors.terminal.black, CcColors.terminal.red, CcColors.terminal.green,
            CcColors.terminal.yellow, CcColors.terminal.blue, CcColors.terminal.magenta,
            CcColors.terminal.cyan, CcColors.terminal.white,
        )
        normal.forEachIndexed { offset, color ->
            val span = single("${ESC}[${30 + offset}mx")
            assertEquals(AnsiColor.Ansi(offset), span.style.foreground)
            assertNull(span.style.background)
            assertEquals(hex(color), hex(AnsiPalette.foreground(AnsiColor.Ansi(offset))))
        }
    }

    @Test
    fun `SGR 90 to 97 select the eight bright foreground colours`() {
        for (offset in 0..7) {
            assertEquals(
                AnsiColor.Ansi(offset + 8),
                single("${ESC}[${90 + offset}mx").style.foreground,
            )
        }
    }

    @Test
    fun `SGR 40 to 47 and 100 to 107 set the background, not the foreground`() {
        assertEquals(AnsiColor.Ansi(1), single("${ESC}[41mx").style.background)
        assertEquals(AnsiColor.Ansi(14), single("${ESC}[106mx").style.background)
        // A background-only sequence must leave the foreground unset, or every
        // "red background" run would also recolour its text.
        assertNull(single("${ESC}[41mx").style.foreground)
    }

    @Test
    fun `256 colour foreground is the six by six by six cube`() {
        // 196 is pure red, 46 pure green, 21 pure blue: the cube's axis extremes.
        assertEquals("#FF0000", hex(Ansi256.argb(196)))
        assertEquals("#00FF00", hex(Ansi256.argb(46)))
        assertEquals("#0000FF", hex(Ansi256.argb(21)))
    }

    @Test
    fun `256 colour boundaries map the way xterm does`() {
        // 16 and 231 are the cube's first and last cells, both black and white.
        assertEquals("#000000", hex(Ansi256.argb(16)))
        assertEquals("#FFFFFF", hex(Ansi256.argb(231)))
        // 232 and 255 are the greyscale ramp's first and last steps.
        assertEquals("#080808", hex(Ansi256.argb(232)))
        assertEquals("#EEEEEE", hex(Ansi256.argb(255)))
    }

    @Test
    fun `a 256 colour cell's channels are the standard xterm levels`() {
        // 196 decomposes to 5,0,0 and 21 to 0,0,5 on the 0/95/135/175/215/255
        // axis, which is the one detail a hand-rolled cube usually gets wrong.
        //
        // The decomposition subtracts the 16-cell ANSI block first, so the cell
        // index is offset before it is split. Decomposing the raw index instead
        // is the classic off-by-16: for 21 it yields 3,3 rather than 0,0,5, and
        // green/blue come out wrong while the sum of the two cubes still looks
        // plausible.
        val red = 196 - 16
        assertEquals(255, Ansi256.cubeLevel(red / 36))
        assertEquals(0, Ansi256.cubeLevel(red % 36 / 6))
        assertEquals(0, Ansi256.cubeLevel(red % 6))
        val blue = 21 - 16
        assertEquals(0, Ansi256.cubeLevel(blue / 36))
        assertEquals(0, Ansi256.cubeLevel(blue % 36 / 6))
        assertEquals(255, Ansi256.cubeLevel(blue % 6))
    }

    @Test
    fun `the greyscale ramp is eight plus ten times the step`() {
        assertEquals(8, Ansi256.greyLevel(232))
        assertEquals(238, Ansi256.greyLevel(255))
        assertEquals(128, Ansi256.greyLevel(244))
    }

    @Test
    fun `an index outside the 256 colour space is rejected rather than clamped`() {
        assertNull(Ansi256.argbOrNull(256))
        assertNull(Ansi256.argbOrNull(-1))
        assertNotEquals(Ansi256.argbOrNull(0), Ansi256.argbOrNull(256))
    }

    @Test
    fun `a 256 colour sequence is decoded to the indexed colour`() {
        val span = single("${ESC}[38;5;196mx")
        assertEquals(AnsiColor.Indexed(196), span.style.foreground)
        assertEquals("#FF0000", hex(AnsiPalette.foreground(AnsiColor.Indexed(196))))
    }

    @Test
    fun `a 256 colour background is decoded like the foreground`() {
        val span = single("${ESC}[48;5;196mx")
        assertEquals(AnsiColor.Indexed(196), span.style.background)
        assertNull(span.style.foreground)
    }

    @Test
    fun `truecolour is taken verbatim`() {
        val span = single("${ESC}[38;2;18;52;86mx")
        assertEquals(AnsiColor.Rgb(18, 52, 86), span.style.foreground)
        assertEquals("#123456", hex(AnsiPalette.foreground(AnsiColor.Rgb(18, 52, 86))))
    }

    @Test
    fun `truecolour backgrounds are taken verbatim`() {
        val span = single("${ESC}[48;2;1;2;3;38;2;4;5;6mx")
        assertEquals(AnsiColor.Rgb(1, 2, 3), span.style.background)
        assertEquals(AnsiColor.Rgb(4, 5, 6), span.style.foreground)
    }

    @Test
    fun `every attribute is parsed`() {
        val style = single("${ESC}[1;2;3;4;7;9mx").style
        assertTrue(style.bold)
        assertTrue(style.dim)
        assertTrue(style.italic)
        assertTrue(style.underline)
        assertTrue(style.inverse)
        assertTrue(style.strikethrough)
    }

    @Test
    fun `a later sequence clears only the attributes it names`() {
        // 24 is "underline off". 22 is "intensity off" and would clear bold too,
        // which is the opposite of what this test asserts.
        val style = single("${ESC}[1;3;4m${ESC}[24mx").style
        assertTrue(style.bold)
        assertTrue(style.italic)
        assertTrue(!style.underline)
    }

    @Test
    fun `reset clears every attribute, colour, and background`() {
        val style = single("${ESC}[31;42;1;3;4mx${ESC}[0my").style
        assertNull(style.foreground)
        assertNull(style.background)
        assertTrue(!style.bold)
        assertTrue(!style.dim)
        assertTrue(!style.italic)
        assertTrue(!style.underline)
        assertTrue(!style.inverse)
        assertTrue(!style.strikethrough)
    }

    @Test
    fun `bold then reset then plain keeps the ordering`() {
        val spans = AnsiParser.parse("${ESC}[1mA${ESC}[0mB")
        assertEquals("AB", spans.joinToString("") { it.text })
        assertTrue(spans.first { it.text == "A" }.style.bold)
        assertTrue(!spans.first { it.text == "B" }.style.bold)
    }

    @Test
    fun `styles bleed forward until something changes them`() {
        val spans = AnsiParser.parse("${ESC}[32mgreen forever")
        assertEquals(1, spans.size)
        assertEquals("green forever", spans[0].text)
        assertEquals(AnsiColor.Ansi(2), spans[0].style.foreground)
    }

    @Test
    fun `consecutive same-style sequences do not split the text`() {
        val spans = AnsiParser.parse("${ESC}[32mone${ESC}[32mtwo")
        assertEquals(1, spans.size)
        assertEquals("onetwo", spans[0].text)
    }

    @Test
    fun `inverse swaps the two channels at render time`() {
        val on = CcColors.terminalBackground
        val span = single("${ESC}[7;31mx")
        assertTrue(span.style.inverse)
        assertEquals(hex(on), hex(span.foregroundColor(on)))
        assertEquals(hex(CcColors.terminal.red), hex(span.backgroundColor()))
    }

    @Test
    fun `inverse with an explicit background fills with the foreground colour`() {
        val span = single("${ESC}[7;31;42mx")
        assertEquals(hex(CcColors.terminal.green), hex(span.backgroundColor()))
    }

    @Test
    fun `a malformed parameter list is ignored and the text survives`() {
        val input = "before${ESC}[38;2;mnot;an;argafter"
        assertEquals("beforenot;an;argafter", render(input))
    }

    @Test
    fun `an out of range colour index is ignored and the style is unchanged`() {
        assertEquals(AnsiColor.Ansi(1), single("${ESC}[31m${ESC}[38;5;999mx").style.foreground)
        assertEquals(AnsiColor.Ansi(1), single("${ESC}[31m${ESC}[38;5;300mx").style.foreground)
    }

    @Test
    fun `a truecolour channel above 255 is ignored and the style is unchanged`() {
        val span = single("${ESC}[31m${ESC}[38;2;300;0;0mx")
        assertEquals(AnsiColor.Ansi(1), span.style.foreground)
    }

    @Test
    fun `an unknown SGR parameter changes nothing but keeps the text`() {
        val input = "${ESC}[32mtextcolored${ESC}[73;9999mother"
        val spans = AnsiParser.parse(input)
        assertEquals("textcoloredother", render(input))
        assertEquals(AnsiColor.Ansi(2), spans.last().style.foreground)
    }

    @Test
    fun `an unterminated sequence at end of input is rendered as it arrived`() {
        val input = "${ESC}[38;5;20"
        assertEquals(input, render(input))
    }

    @Test
    fun `an unterminated sequence in the middle of a line keeps the rest of the line`() {
        val input = "before${ESC}[31"
        assertEquals(input, render(input))
    }

    @Test
    fun `an unknown final byte is rendered rather than swallowed`() {
        // "G" is a cursor-position report a program may have echoed. It is not an
        // SGR sequence, so it must not be eaten.
        val input = "a${ESC}Gb"
        assertEquals(input, render(input))
    }

    @Test
    fun `control characters other than newline and tab are rendered visibly`() {
        val bell = "before${BEL}after"
        assertEquals(bell, render(bell))
    }

    @Test
    fun `a carriage return stays visible rather than being applied as a move`() {
        // This is a rendering block, not a terminal emulator: a CR that
        // overwrote the line would hide output, and hard block 6 requires
        // output to be seen.
        val input = "old\rnew"
        assertEquals(input, render(input))
    }

    @Test
    fun `newlines and tabs are preserved`() {
        assertEquals("a\nb\tc", render("a\nb\tc"))
    }

    @Test
    fun `parsing is a pure function of its input`() {
        val input = "${ESC}[32mok${ESC}[0m${ESC}[1mbold"
        assertEquals(AnsiParser.parse(input), AnsiParser.parse(input))
    }

    @Test
    fun `a large mixed stream loses no characters`() {
        val actual = StringBuilder()
        val expected = StringBuilder()
        for (i in 0 until 2_000) {
            actual.append("${ESC}[${30 + (i % 8)}m")
            val word = "line$i\n"
            actual.append(word)
            expected.append(word)
        }
        assertEquals(expected.toString(), render(actual.toString()))
    }
}
