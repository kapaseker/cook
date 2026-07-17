package page.chat.markdown

private val atxHeadingRegex = Regex("""^ {0,3}(#{1,6})(?:[\t ]+(.*)|[\t ]*)$""")
private val closingHeadingSequenceRegex = Regex("""[\t ]+#+[\t ]*$""")
private val unorderedListRegex = Regex("""^( *)([-+*])(?:[\t ]+(.*)|[\t ]*)$""")
private val orderedListRegex = Regex("""^( *)(\d{1,9})([.)])(?:[\t ]+(.*)|[\t ]*)$""")

/** Parses the Markdown subset supported in agent responses. */
internal fun parseMarkdown(raw: String): MarkdownDocument {
    if (raw.isEmpty()) return MarkdownDocument(emptyList())

    val lines = raw.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    return MarkdownDocument(MarkdownBlockParser(lines).parse())
}

private class MarkdownBlockParser(private val lines: List<String>) {
    private var index = 0

    fun parse(): List<MarkdownBlock> = parseBlocks()

    private fun parseBlocks(): List<MarkdownBlock> {
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

        while (index < lines.size) {
            val line = lines[index]
            val heading = atxHeadingRegex.matchEntire(line)
            val listMarker = line.toListMarker()?.takeIf { it.indent <= 3 }
            when {
                line.isBlank() -> {
                    flushParagraph()
                    index += 1
                }
                heading != null -> {
                    flushParagraph()
                    val content = heading.groupValues[2].replace(closingHeadingSequenceRegex, "")
                    blocks += MarkdownBlock.Heading(
                        level = heading.groupValues[1].length,
                        inlines = parseMarkdownInlines(content),
                    )
                    index += 1
                }
                listMarker != null && (paragraphLines.isEmpty() || !listMarker.ordered || listMarker.number == 1) -> {
                    flushParagraph()
                    blocks += parseList(listMarker)
                }
                else -> {
                    paragraphLines += line
                    index += 1
                }
            }
        }
        flushParagraph()
        return blocks
    }

    private fun parseList(firstMarker: ListMarker): MarkdownBlock.List {
        val items = mutableListOf<MarkdownListItem>()
        val baseIndent = firstMarker.indent
        val ordered = firstMarker.ordered
        val startNumber = firstMarker.number
        val delimiter = firstMarker.delimiter

        while (index < lines.size) {
            val marker = lines[index].toListMarker() ?: break
            if (marker.indent != baseIndent || marker.ordered != ordered) break

            index += 1
            val itemBlocks = mutableListOf<MarkdownBlock>()
            val itemLines = mutableListOf(marker.content)
            var hasBlankLine = false
            var followsBlankLine = false

            fun flushItemLines() {
                if (itemLines.isEmpty()) return
                itemBlocks += MarkdownBlockParser(itemLines.toList()).parse()
                itemLines.clear()
            }

            while (index < lines.size) {
                val line = lines[index]
                val candidate = line.toListMarker()
                when {
                    candidate != null && candidate.indent < baseIndent -> break
                    candidate != null && candidate.indent == baseIndent -> break
                    candidate != null && candidate.indent > baseIndent -> {
                        flushItemLines()
                        itemBlocks += parseList(candidate)
                        followsBlankLine = false
                    }
                    line.isBlank() -> {
                        itemLines += ""
                        hasBlankLine = true
                        followsBlankLine = true
                        index += 1
                    }
                    line.leadingSpaceCount() >= baseIndent + 2 -> {
                        itemLines += line.drop(baseIndent + 2)
                        followsBlankLine = false
                        index += 1
                    }
                    followsBlankLine -> break
                    else -> {
                        itemLines += line
                        index += 1
                    }
                }

                if (candidate != null && candidate.indent <= baseIndent) break
            }
            flushItemLines()
            items += MarkdownListItem(
                blocks = itemBlocks,
                hasBlankLine = hasBlankLine,
            )
        }

        return MarkdownBlock.List(
            ordered = ordered,
            startNumber = startNumber,
            delimiter = delimiter,
            items = items,
            isLoose = items.any(MarkdownListItem::hasBlankLine),
        )
    }
}

private data class ListMarker(
    val indent: Int,
    val ordered: Boolean,
    val number: Int,
    val delimiter: Char,
    val content: String,
)

private fun String.toListMarker(): ListMarker? {
    unorderedListRegex.matchEntire(this)?.let { match ->
        return ListMarker(
            indent = match.groupValues[1].length,
            ordered = false,
            number = 0,
            delimiter = match.groupValues[2].single(),
            content = match.groupValues[3],
        )
    }
    orderedListRegex.matchEntire(this)?.let { match ->
        return ListMarker(
            indent = match.groupValues[1].length,
            ordered = true,
            number = match.groupValues[2].toInt(),
            delimiter = match.groupValues[3].single(),
            content = match.groupValues[4],
        )
    }
    return null
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

private fun String.leadingSpaceCount(): Int = takeWhile { it == ' ' }.length

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
