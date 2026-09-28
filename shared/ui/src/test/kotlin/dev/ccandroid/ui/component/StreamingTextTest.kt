package dev.ccandroid.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [StreamingText] markdown chunk parsing and streaming caret behavior.
 */
class StreamingTextTest {

    @Test
    fun `markdown parser parses headings paragraphs and lists`() {
        val markdown = """
            # Heading 1
            A regular paragraph with text.
            - Bullet 1
            - Bullet 2
            1. Numbered 1
            2. Numbered 2
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown, isStreaming = false)
        assertTrue("Blocks should not be empty", blocks.isNotEmpty())

        val headingBlock = blocks.filterIsInstance<MarkdownBlock.Heading>().firstOrNull()
        assertEquals(1, headingBlock?.level)
        assertEquals("Heading 1", headingBlock?.text)

        val bulletItems = blocks.filterIsInstance<MarkdownBlock.BulletList>().firstOrNull()?.items
        assertEquals(2, bulletItems?.size)

        val numberedItems = blocks.filterIsInstance<MarkdownBlock.NumberedList>().firstOrNull()?.items
        assertEquals(2, numberedItems?.size)
    }

    @Test
    fun `markdown parser parses closed code blocks`() {
        val markdown = """
            Here is some code:
            ```kotlin
            val answer = 42
            ```
            Done.
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown, isStreaming = false)
        val codeBlocks = blocks.filterIsInstance<MarkdownBlock.Code>()
        assertEquals(1, codeBlocks.size)
        assertEquals("kotlin", codeBlocks.first().language)
        assertEquals("val answer = 42", codeBlocks.first().code.trim())
        assertFalse("Block should be closed", codeBlocks.first().isOpen)
    }

    @Test
    fun `markdown parser detects unclosed code block during active streaming`() {
        val markdown = """
            Streaming answer:
            ```python
            def compute():
                return True
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown, isStreaming = true)
        val codeBlocks = blocks.filterIsInstance<MarkdownBlock.Code>()
        assertEquals(1, codeBlocks.size)
        assertEquals("python", codeBlocks.first().language)
        assertTrue("Block should be marked open when streaming", codeBlocks.first().isOpen)
    }

    @Test
    fun `inline parser parses bold italic inline code and links`() {
        val text = "This is **bold**, *italic*, `code`, and [link](https://example.com)"
        val inlines = MarkdownParser.parseInline(text)

        val hasBold = inlines.any { it is InlineSegment.Bold && it.text == "bold" }
        val hasItalic = inlines.any { it is InlineSegment.Italic && it.text == "italic" }
        val hasCode = inlines.any { it is InlineSegment.InlineCode && it.code == "code" }
        val hasLink = inlines.any { it is InlineSegment.Link && it.label == "link" && it.url == "https://example.com" }

        assertTrue("Should detect bold", hasBold)
        assertTrue("Should detect italic", hasItalic)
        assertTrue("Should detect inline code", hasCode)
        assertTrue("Should detect link", hasLink)
    }
}
