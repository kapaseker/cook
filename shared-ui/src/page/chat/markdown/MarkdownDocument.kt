package page.chat.markdown

/** The supported Markdown block types used by an agent chat message. */
internal data class MarkdownDocument(
    val blocks: List<MarkdownBlock>,
)

internal sealed interface MarkdownBlock {
    val inlines: List<MarkdownInline>

    data class Text(override val inlines: List<MarkdownInline>) : MarkdownBlock

    data class Heading(
        val level: Int,
        override val inlines: List<MarkdownInline>,
    ) : MarkdownBlock
}

internal sealed interface MarkdownInline {
    data class Text(val value: String) : MarkdownInline

    data class Styled(
        val style: MarkdownInlineStyle,
        val children: List<MarkdownInline>,
    ) : MarkdownInline
}

internal enum class MarkdownInlineStyle {
    Bold,
    Italic,
    BoldItalic,
}
