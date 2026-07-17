package page.chat.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Renders the supported Markdown subset for an agent chat message. */
@Composable
internal fun AgentMarkdownText(
    markdown: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val document = remember(markdown) { parseMarkdown(markdown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        document.blocks.forEach { block ->
            Text(
                text = block.inlines.toAnnotatedString(),
                style = when (block) {
                    is MarkdownBlock.Text -> MaterialTheme.typography.bodyLarge
                    is MarkdownBlock.Heading -> when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                },
                color = color,
            )
        }
    }
}

private fun List<MarkdownInline>.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    appendInlines(this@toAnnotatedString)
}

private fun AnnotatedString.Builder.appendInlines(inlines: List<MarkdownInline>) {
    inlines.forEach { inline ->
        when (inline) {
            is MarkdownInline.Text -> append(inline.value)
            is MarkdownInline.Styled -> pushStyle(inline.style.toSpanStyle()).also {
                appendInlines(inline.children)
                pop()
            }
        }
    }
}

private fun MarkdownInlineStyle.toSpanStyle(): SpanStyle = when (this) {
    MarkdownInlineStyle.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
    MarkdownInlineStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
    MarkdownInlineStyle.BoldItalic -> SpanStyle(
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
    )
}
