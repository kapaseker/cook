package page.chat.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MarkdownParserTest {
    @Test
    fun `parses strict ATX headings and limits their levels`() {
        val document = parseMarkdown("# One\n## Two ##\n###### Six\n####### Seven\n#Missing space")

        assertEquals(4, document.blocks.size)
        assertEquals(1, assertIs<MarkdownBlock.Heading>(document.blocks[0]).level)
        assertEquals("One", document.blocks[0].inlines.plainText())
        assertEquals(2, assertIs<MarkdownBlock.Heading>(document.blocks[1]).level)
        assertEquals("Two", document.blocks[1].inlines.plainText())
        assertEquals(6, assertIs<MarkdownBlock.Heading>(document.blocks[2]).level)
        assertEquals("Six", document.blocks[2].inlines.plainText())
        assertEquals("####### Seven #Missing space", assertIs<MarkdownBlock.Text>(document.blocks[3]).inlines.plainText())
    }

    @Test
    fun `splits paragraphs and applies Markdown line break rules`() {
        val document = parseMarkdown("first\nsecond  \nthird\\\nfourth\n\nfifth")

        assertEquals(2, document.blocks.size)
        assertEquals("first second\nthird\nfourth", document.blocks[0].inlines.plainText())
        assertEquals("fifth", document.blocks[1].inlines.plainText())
    }

    @Test
    fun `parses bold italic and bold italic text`() {
        val inlines = assertIs<MarkdownBlock.Text>(
            parseMarkdown("plain **bold** *italic* ***both*** __strong__ _em_").blocks.single(),
        ).inlines

        assertEquals("plain bold italic both strong em", inlines.plainText())
        assertEquals(
            listOf(
                MarkdownInlineStyle.Bold,
                MarkdownInlineStyle.Italic,
                MarkdownInlineStyle.BoldItalic,
                MarkdownInlineStyle.Bold,
                MarkdownInlineStyle.Italic,
            ),
            inlines.filterIsInstance<MarkdownInline.Styled>().map { it.style },
        )
    }

    @Test
    fun `keeps unsupported and incomplete syntax as literal text`() {
        val inlines = assertIs<MarkdownBlock.Text>(
            parseMarkdown("[link](https://example.com) and **unfinished and \\*escaped\\*").blocks.single(),
        ).inlines

        assertEquals("[link](https://example.com) and **unfinished and \\*escaped\\*", inlines.plainText())
        assertEquals(emptyList(), inlines.filterIsInstance<MarkdownInline.Styled>())
    }

    @Test
    fun `does not treat intraword underscores as emphasis`() {
        val inlines = assertIs<MarkdownBlock.Text>(parseMarkdown("file_name and _emphasis_").blocks.single()).inlines

        assertEquals("file_name and emphasis", inlines.plainText())
        assertEquals(1, inlines.filterIsInstance<MarkdownInline.Styled>().size)
    }
}

private fun List<MarkdownInline>.plainText(): String = joinToString(separator = "") { inline ->
    when (inline) {
        is MarkdownInline.Text -> inline.value
        is MarkdownInline.Styled -> inline.children.plainText()
    }
}
