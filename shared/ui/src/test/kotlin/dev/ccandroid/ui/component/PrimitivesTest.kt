package dev.ccandroid.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests asserting the contract of [Primitives.kt] per `docs/03-design/component-library.md`.
 */
class PrimitivesTest {

    @Test
    fun `button variants exist for all four specified design roles`() {
        val variants = ButtonVariant.values().map { it.name }.toSet()
        assertEquals(
            setOf("Primary", "Secondary", "Tertiary", "Danger"),
            variants,
        )
    }

    @Test
    fun `button sizes have specified heights`() {
        assertEquals(36f, ButtonSize.Small.height.value, 0.001f)
        assertEquals(48f, ButtonSize.Medium.height.value, 0.001f)
        assertEquals(56f, ButtonSize.Large.height.value, 0.001f)
    }

    @Test
    fun `chip types exist for filter, status, and tag`() {
        val types = ChipType.values().map { it.name }.toSet()
        assertEquals(
            setOf("Filter", "Status", "Tag"),
            types,
        )
    }

    @Test
    fun `all nine status badge states are specified with glyph and bilingual labels`() {
        val states = StatusState.values()
        assertEquals(9, states.size)

        states.forEach { state ->
            assertTrue("Glyph for ${state.name} must not be empty", state.glyph.isNotEmpty())
            assertTrue("English label for ${state.name} must not be empty", state.labelEn.isNotEmpty())
            assertTrue("German label for ${state.name} must not be empty", state.labelDe.isNotEmpty())
        }
    }

    @Test
    fun `every status state has distinct glyph and label for accessibility in greyscale`() {
        val glyphs = StatusState.values().map { it.glyph }
        // Each glyph must be distinct so colour is never the sole indicator
        assertEquals(StatusState.values().size, glyphs.toSet().size)

        val labelsEn = StatusState.values().map { it.labelEn }
        assertEquals(StatusState.values().size, labelsEn.toSet().size)

        val labelsDe = StatusState.values().map { it.labelDe }
        assertEquals(StatusState.values().size, labelsDe.toSet().size)
    }
}
