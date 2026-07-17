package repository.history

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomConversationHistoryRepoTest {

    @Test
    fun `saves ordered completed turns with database generated ids`() = runBlocking {
        withRepository { repository ->
            val conversationId = repository.saveSuccessfulTurns(
                conversationId = null,
                turns = listOf(turn(sequence = 0, user = "Hello", assistant = "Hi there")),
            )

            assertTrue(conversationId > 0)
            repository.saveSuccessfulTurns(
                conversationId = conversationId,
                turns = listOf(turn(sequence = 1, user = "How are you?", assistant = "很好。\n\n- Ready")),
            )

            assertEquals(
                listOf("Hello", "How are you?"),
                repository.loadLatestConversation()?.turns?.map(ConversationHistoryTurn::userContent),
            )
            assertEquals(
                "很好。\n\n- Ready",
                repository.loadLatestConversation()?.turns?.last()?.assistantContent,
            )
        }
    }

    @Test
    fun `replaying a turn sequence does not duplicate history`() = runBlocking {
        withRepository { repository ->
            val turn = turn(sequence = 0, user = "Repeat", assistant = "Saved once")
            val conversationId = repository.saveSuccessfulTurns(null, listOf(turn))

            repository.saveSuccessfulTurns(conversationId, listOf(turn))

            assertEquals(1, repository.loadLatestConversation()?.turns?.size)
        }
    }

    @Test
    fun `loads the most recently updated conversation and cascades deletion`() = runBlocking {
        withRepository { repository ->
            val firstConversationId = repository.saveSuccessfulTurns(
                null,
                listOf(turn(sequence = 0, user = "First", assistant = "One", completedAt = 1)),
            )
            val secondConversationId = repository.saveSuccessfulTurns(
                null,
                listOf(turn(sequence = 0, user = "Second", assistant = "Two", completedAt = 2)),
            )

            assertEquals(secondConversationId, repository.loadLatestConversation()?.id)
            repository.deleteConversation(secondConversationId)

            assertEquals(firstConversationId, repository.loadLatestConversation()?.id)
            repository.deleteConversation(firstConversationId)
            assertNull(repository.loadLatestConversation())
        }
    }

    private suspend fun withRepository(block: suspend (RoomConversationHistoryRepo) -> Unit) {
        val database = Room.inMemoryDatabaseBuilder<ConversationHistoryDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        try {
            block(RoomConversationHistoryRepo(database.conversationHistoryDao()))
        } finally {
            database.close()
        }
    }

    private fun turn(
        sequence: Long,
        user: String,
        assistant: String,
        completedAt: Long = 10L + sequence,
    ) = ConversationHistoryTurn(
        sequence = sequence,
        userContent = user,
        assistantContent = assistant,
        modelId = "test-model",
        completedAtEpochMillis = completedAt,
    )
}
