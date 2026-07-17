package page.chat.markdown

/** The supported Markdown block types used by an agent chat message. */
internal data class MarkdownDocument(
    val blocks: kotlin.collections.List<MarkdownBlock>,
)

internal sealed interface MarkdownBlock {
    val inlines: kotlin.collections.List<MarkdownInline>

    data class Text(override val inlines: kotlin.collections.List<MarkdownInline>) : MarkdownBlock

    data class Heading(
        val level: Int,
        override val inlines: kotlin.collections.List<MarkdownInline>,
    ) : MarkdownBlock

    data class List(
        val ordered: Boolean,
        val startNumber: Int,
        val delimiter: Char,
        val items: kotlin.collections.List<MarkdownListItem>,
        val isLoose: Boolean,
    ) : MarkdownBlock {
        override val inlines: kotlin.collections.List<MarkdownInline> = emptyList()
    }
}

internal data class MarkdownListItem(
    val blocks: kotlin.collections.List<MarkdownBlock>,
    val hasBlankLine: Boolean,
)

internal sealed interface MarkdownInline {
    data class Text(val value: String) : MarkdownInline

    data class Styled(
        val style: MarkdownInlineStyle,
        val children: kotlin.collections.List<MarkdownInline>,
    ) : MarkdownInline
}

internal enum class MarkdownInlineStyle {
    Bold,
    Italic,
    BoldItalic,
}
