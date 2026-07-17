package page.chat.markdown

private val atxHeadingRegex = Regex("""^ {0,3}(#{1,6})(?:[\t ]+(.*)|[\t ]*)$""")
private val closingHeadingSequenceRegex = Regex("""[\t ]+#+[\t ]*$""")

/** Parses the Markdown subset supported in agent responses. */
internal fun parseMarkdown(raw: String): MarkdownDocument {
    if (raw.isEmpty()) return MarkdownDocument(emptyList())

    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphLines = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphLines.isEmpty()) return
        val text = paragraphLines.toMarkdownParagraph()
        if (text.isNotEmpty()) {
            blocks += MarkdownBlock.Text(parseMarkdownInlines(text))
        }
        paragraphLines.clear()
    }

    raw.replace("\r\n", "\n").replace('\r', '\n').split('\n').forEach { line ->
        val heading = atxHeadingRegex.matchEntire(line)
        when {
            line.isBlank() -> flushParagraph()
            heading != null -> {
                flushParagraph()
                val content = heading.groupValues[2].replace(closingHeadingSequenceRegex, "")
                blocks += MarkdownBlock.Heading(
                    level = heading.groupValues[1].length,
                    inlines = parseMarkdownInlines(content),
                )
            }
            else -> paragraphLines += line
        }
    }
    flushParagraph()
    return MarkdownDocument(blocks)
}

private fun List<String>.toMarkdownParagraph(): String = buildString {
    this@toMarkdownParagraph.forEachIndexed { index, paragraphLine ->
        if (index > 0) {
            val previousLine = this@toMarkdownParagraph[index - 1]
            if (previousLine.hasHardLineBreak()) {
                deleteRange(length - previousLine.trailingHardBreakLength(), length)
                append('\n')
            } else {
                append(' ')
            }
        }
        append(paragraphLine.trimStart())
    }
}.trim()

private fun String.hasHardLineBreak(): Boolean =
    trailingBackslashCount() % 2 == 1 || trailingSpaceCount() >= 2

private fun String.trailingHardBreakLength(): Int = when {
    trailingBackslashCount() % 2 == 1 -> 1
    else -> trailingSpaceCount()
}

private fun String.trailingBackslashCount(): Int = takeLastWhile { it == '\\' }.length

private fun String.trailingSpaceCount(): Int = takeLastWhile { it == ' ' }.length

private fun parseMarkdownInlines(text: String): List<MarkdownInline> = InlineParser(text).parse()

private class InlineParser(private val source: String) {
    private var index = 0

    fun parse(): List<MarkdownInline> = parseUntil(null).inlines

    private fun parseUntil(closing: String?): ParsedInlines {
        val result = mutableListOf<MarkdownInline>()
        val plain = StringBuilder()

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                result += MarkdownInline.Text(plain.toString())
                plain.clear()
            }
        }

        while (index < source.length) {
            if (closing != null && source.startsWith(closing, index) && canCloseDelimiter(index, closing)) {
                flushPlain()
                index += closing.length
                return ParsedInlines(result, closed = true)
            }

            val character = source[index]
            when {
                character == '\\' && index + 1 < source.length && source[index + 1].isMarkdownEscapable() -> {
                    plain.append(source[index + 1])
                    index += 2
                }
                character == '*' || character == '_' -> {
                    val delimiter = openingDelimiterAt(index)
                    if (delimiter == null) {
                        plain.append(character)
                        index += 1
                    } else {
                        val start = index
                        index += delimiter.length
                        val parsedChildren = parseUntil(delimiter)
                        if (parsedChildren.closed && parsedChildren.inlines.isNotEmpty()) {
                            flushPlain()
                            result += MarkdownInline.Styled(delimiter.style(), parsedChildren.inlines)
                        } else {
                            plain.append(source.substring(start, index))
                        }
                    }
                }
                else -> {
                    plain.append(character)
                    index += 1
                }
            }
        }
        flushPlain()
        return ParsedInlines(result, closed = false)
    }

    private fun openingDelimiterAt(position: Int): String? {
        val delimiterCharacter = source[position]
        val runLength = source.substring(position).takeWhile { it == delimiterCharacter }.length
        val next = source.getOrNull(position + runLength)
        val previous = source.getOrNull(position - 1)
        if (!isLeftFlanking(previous, next)) return null
        if (delimiterCharacter == '_' && previous.isWordCharacter() && next.isWordCharacter()) return null
        return when {
            runLength >= 3 -> "${delimiterCharacter}${delimiterCharacter}${delimiterCharacter}"
            runLength >= 2 -> "${delimiterCharacter}${delimiterCharacter}"
            else -> delimiterCharacter.toString()
        }
    }

    private fun canCloseDelimiter(position: Int, delimiter: String): Boolean {
        val previous = source.getOrNull(position - 1)
        val next = source.getOrNull(position + delimiter.length)
        if (!isRightFlanking(previous, next)) return false
        return delimiter[0] != '_' || !previous.isWordCharacter() || !next.isWordCharacter()
    }
}

private data class ParsedInlines(
    val inlines: List<MarkdownInline>,
    val closed: Boolean,
)

private fun isLeftFlanking(previous: Char?, next: Char?): Boolean =
    next != null && !next.isWhitespace() && (!next.isPunctuation() || previous == null || previous.isWhitespace() || previous.isPunctuation())

private fun isRightFlanking(previous: Char?, next: Char?): Boolean =
    previous != null && !previous.isWhitespace() && (!previous.isPunctuation() || next == null || next.isWhitespace() || next.isPunctuation())

private fun Char?.isWordCharacter(): Boolean = this?.isLetterOrDigit() == true || this == '_'

private fun Char.isPunctuation(): Boolean = !isLetterOrDigit() && !isWhitespace()

private fun Char.isMarkdownEscapable(): Boolean = this in "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

private fun String.style(): MarkdownInlineStyle = when (length) {
    1 -> MarkdownInlineStyle.Italic
    2 -> MarkdownInlineStyle.Bold
    else -> MarkdownInlineStyle.BoldItalic
}
