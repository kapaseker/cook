package page.chat.screen

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import page.chat.biz.ChatMessage
import page.chat.biz.MessageAuthor
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatConversationScreenTest {

    /** Verifies that scroll trigger changes when the latest message streams more text. */
    @Test
    fun `scroll trigger changes when the latest message streams more text`() {
        val pending = ChatMessage(
            id = 2L,
            author = MessageAuthor.Agent,
            text = "Thinking...",
            isPending = true,
        )
        val messages = listOf(
            ChatMessage(id = 1L, author = MessageAuthor.User, text = "Hello"),
            pending,
        )

        val beforeChunk = latestMessageScrollKey(messages)
        val afterChunk = latestMessageScrollKey(
            messages.dropLast(1) + pending.copy(text = "Hello! How can I help?"),
        )

        assertNotEquals(beforeChunk, afterChunk)
    }

    @Test
    fun `history navigation only activates at the relevant multiline boundary`() {
        val middleLine = TextFieldValue("First\nSecond\nThird", selection = TextRange(8))
        val firstLine = TextFieldValue("First\nSecond\nThird", selection = TextRange(2))
        val lastLine = TextFieldValue("First\nSecond\nThird", selection = TextRange(16))

        assertTrue(isCursorOnFirstLine(firstLine))
        assertFalse(isCursorOnLastLine(firstLine))
        assertFalse(isCursorOnFirstLine(middleLine))
        assertFalse(isCursorOnLastLine(middleLine))
        assertTrue(isCursorOnLastLine(lastLine))
    }
}
