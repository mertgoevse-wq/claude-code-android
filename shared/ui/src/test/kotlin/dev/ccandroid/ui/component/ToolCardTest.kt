package dev.ccandroid.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [ToolCard] contracts per `docs/03-design/component-library.md`.
 */
class ToolCardTest {

    @Test
    fun `all tool card states exist with dedicated glyphs`() {
        val states = ToolCardState.values()
        assertEquals(5, states.size)
        states.forEach { state ->
            assertTrue("Glyph for ${state.name} must not be empty", state.glyph.isNotEmpty())
        }
        val glyphs = states.map { it.glyph }.toSet()
        assertEquals("Each tool card state must have a distinct glyph", states.size, glyphs.size)
    }

    @Test
    fun `tool card target truncation limits to 52 characters`() {
        val shortPath = "shared/ui/component/Primitives.kt"
        assertEquals(shortPath, ToolCardUtils.truncateTarget(shortPath))

        val longPath = "very/deeply/nested/directory/structure/that/exceeds/the/maximum/allowed/length/of/fifty/two/characters/file.kt"
        val truncated = ToolCardUtils.truncateTarget(longPath)
        assertTrue("Truncated length must be <= 52, was ${truncated.length}", truncated.length <= 52)
        assertTrue("Truncated target must contain ellipsis", truncated.contains("…"))
    }

    @Test
    fun `tool card output truncation caps at 2048 bytes with marker`() {
        val smallOutput = "Normal output under 2KB"
        val (processedOutput, isTruncated) = ToolCardUtils.truncateOutput(smallOutput)
        assertEquals(smallOutput, processedOutput)
        assertFalse("Small output must not be marked as truncated", isTruncated)

        val largeOutput = "A".repeat(3000)
        val (truncatedOutput, truncatedFlag) = ToolCardUtils.truncateOutput(largeOutput)
        assertTrue("Large output must be marked as truncated", truncatedFlag)
        assertTrue("Truncated output size must be <= 2048 characters", truncatedOutput.length <= 2048)
    }
}
