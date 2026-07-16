package page.chat.screen

import page.chat.biz.ChatMessage
import page.chat.biz.MessageAuthor
import kotlin.test.Test
import kotlin.test.assertNotEquals

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
}
