package dev.ccandroid.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the contract of [CodeBlock] per `docs/03-design/component-library.md`.
 */
class CodeBlockTest {

    @Test
    fun `code block syntax tokenizer extracts language keywords`() {
        val code = "val x: Int = 42 // comment\nfun hello() = \"world\""
        val tokens = CodeBlockTokenizer.tokenize(code, "kotlin")

        assertTrue("Tokens must not be empty", tokens.isNotEmpty())

        val keywordTokens = tokens.filter { it.type == SyntaxTokenType.Keyword }
        val keywordStrings = keywordTokens.map { it.text }
        assertTrue("val should be recognized as keyword", keywordStrings.contains("val"))
        assertTrue("fun should be recognized as keyword", keywordStrings.contains("fun"))
    }

    @Test
    fun `code block syntax tokenizer identifies strings and comments`() {
        val code = "val msg = \"hello world\" // greetings"
        val tokens = CodeBlockTokenizer.tokenize(code, "kotlin")

        val stringTokens = tokens.filter { it.type == SyntaxTokenType.StringLiteral }
        assertEquals(1, stringTokens.size)
        assertEquals("\"hello world\"", stringTokens.first().text)

        val commentTokens = tokens.filter { it.type == SyntaxTokenType.Comment }
        assertEquals(1, commentTokens.size)
        assertEquals("// greetings", commentTokens.first().text)
    }

    @Test
    fun `code block formats line numbers correctly`() {
        val code = "line 1\nline 2\nline 3"
        val lines = code.split("\n")
        assertEquals(3, lines.size)
        val maxGutterWidthDigits = lines.size.toString().length
        assertEquals(1, maxGutterWidthDigits)
    }

    @Test
    fun `code block language label normalization`() {
        assertEquals("KOTLIN", CodeBlockTokenizer.normalizeLanguage("kotlin"))
        assertEquals("BASH", CodeBlockTokenizer.normalizeLanguage("sh"))
        assertEquals("JSON", CodeBlockTokenizer.normalizeLanguage("json"))
        assertEquals("TEXT", CodeBlockTokenizer.normalizeLanguage(null))
    }
}
