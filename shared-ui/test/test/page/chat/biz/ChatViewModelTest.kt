package page.chat.biz

import page.chat.ChatStrings
import repository.agent.CookConversationMessage
import repository.agent.CookMessageRole
import repository.agent.CookModel
import repository.agent.CookRepo
import repository.agent.CookStartupIssue
import repository.history.ConversationHistory
import repository.history.ConversationHistoryRepo
import repository.history.ConversationHistoryTurn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatViewModelTest {

    /** Verifies that shows the welcome message when cook starts normally. */
    @Test
    fun `shows the welcome message when Cook starts normally`() {
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(),
            historyRepository = FakeConversationHistoryRepo(),
            strings = testChatStrings,
        )

        assertEquals(
            "\nHi, I'm Cook. 👋\n\n" +
                "Talk to me in English or Chinese, and I'll help you improve your English along the way.\n",
            viewModel.conversationUiState.value.messages.single().text,
        )
    }

    /** Verifies that shows the startup error instead of the welcome message. */
    @Test
    fun `shows the startup error instead of the welcome message`() {
        val startupError = "Set the GLM_API_KEY environment variable before starting Cook."
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(startupIssue = CookStartupIssue.MissingApiKey),
            historyRepository = FakeConversationHistoryRepo(),
            strings = testChatStrings,
        )

        assertEquals(startupError, viewModel.conversationUiState.value.messages.single().text)
    }

    /** Verifies that shows the unsupported platform message for non desktop implementations. */
    @Test
    fun `shows the unsupported platform message for non desktop implementations`() {
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(
                startupIssue = CookStartupIssue.UnsupportedPlatform,
            ),
            historyRepository = FakeConversationHistoryRepo(),
            strings = testChatStrings,
        )

        assertEquals(
            "Cook's AI agent is currently available on Desktop only.",
            viewModel.conversationUiState.value.messages.single().text,
        )
    }

    /** Verifies that drafting updates only draft state. */
    @Test
    fun `draft updates input state without changing conversation or request`() {
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(),
            historyRepository = FakeConversationHistoryRepo(),
            strings = testChatStrings,
        )
        val originalConversation = viewModel.conversationUiState.value
        val originalRequest = viewModel.requestUiState.value

        viewModel.onDraftChanged("Hello")

        assertEquals("Hello", viewModel.draftUiState.value.draft)
        assertEquals(originalConversation, viewModel.conversationUiState.value)
        assertEquals(originalRequest, viewModel.requestUiState.value)
        assertEquals("", viewModel.requestUiState.value.errorMessage)
    }

    @Test
    fun `persists a completed response and uses only successful turns as context`() = runBlocking {
        val history = RecordingConversationHistoryRepo()
        val cook = FakeCookRepo(response = flowOf("A saved answer"))
        val viewModel = ChatViewModel(cook, history, testChatStrings)
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("A saved question")
        viewModel.sendMessage()
        withTimeout(1_000) { history.saveCompleted.await() }

        assertEquals(
            listOf("A saved question"),
            history.savedTurns.map(ConversationHistoryTurn::userContent),
        )
        assertEquals("A saved answer", history.savedTurns.single().assistantContent)
        assertEquals(
            listOf(CookMessageRole.User),
            cook.lastConversation.map(CookConversationMessage::role),
        )
    }

    @Test
    fun `removes every empty line before sending`() = runBlocking {
        val history = RecordingConversationHistoryRepo()
        val cook = FakeCookRepo(response = flowOf("A saved answer"))
        val viewModel = ChatViewModel(cook, history, testChatStrings)
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("\n\nFirst question\n\n   \nSecond question\n\n")
        viewModel.sendMessage()
        withTimeout(1_000) { history.saveCompleted.await() }

        assertEquals(
            listOf("First question\nSecond question"),
            history.savedTurns.map(ConversationHistoryTurn::userContent),
        )
    }

    @Test
    fun `failed response removes partial agent output and remains available to draft history`() = runBlocking {
        val history = RecordingConversationHistoryRepo()
        val cook = FakeCookRepo(
            response = flow {
                emit("Partial answer")
                throw IllegalStateException("Connection unavailable")
            },
        )
        val viewModel = ChatViewModel(cook, history, testChatStrings)
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("Do not save this")
        viewModel.sendMessage()
        withTimeout(1_000) {
            viewModel.requestUiState.first { state -> !state.isSending && state.errorMessage.isNotEmpty() }
        }

        assertTrue(history.savedTurns.isEmpty())
        assertEquals(
            listOf(MessageAuthor.Agent, MessageAuthor.User),
            viewModel.conversationUiState.value.messages.map(ChatMessage::author),
        )
        assertEquals("Do not save this", viewModel.conversationUiState.value.messages.last().text)
        assertEquals("Connection unavailable", viewModel.requestUiState.value.errorMessage)

        assertTrue(viewModel.navigateDraftHistory(ChatDraftHistoryDirection.Previous))
        assertEquals("Do not save this", viewModel.draftUiState.value.draft)
        assertEquals("Connection unavailable", viewModel.requestUiState.value.errorMessage)
    }

    @Test
    fun `empty response removes the pending agent message and reports the composer error`() = runBlocking {
        val history = RecordingConversationHistoryRepo()
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(response = emptyFlow()),
            historyRepository = history,
            strings = testChatStrings,
        )
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("Question with an empty answer")
        viewModel.sendMessage()
        withTimeout(1_000) {
            viewModel.requestUiState.first { state -> !state.isSending && state.errorMessage.isNotEmpty() }
        }

        assertEquals(testChatStrings.emptyResponse, viewModel.requestUiState.value.errorMessage)
        assertEquals(
            listOf(MessageAuthor.Agent, MessageAuthor.User),
            viewModel.conversationUiState.value.messages.map(ChatMessage::author),
        )
        assertTrue(history.savedTurns.isEmpty())
    }

    @Test
    fun `failed user message is excluded from later model context`() = runBlocking {
        val history = RecordingConversationHistoryRepo()
        val cook = FakeCookRepo(
            response = flow { throw IllegalStateException("Connection unavailable") },
        )
        val viewModel = ChatViewModel(cook, history, testChatStrings)
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("Failed question")
        viewModel.sendMessage()
        withTimeout(1_000) { viewModel.requestUiState.first { state -> !state.isSending } }

        cook.response = flowOf("Successful answer")
        viewModel.onDraftChanged("Successful question")
        viewModel.sendMessage()
        withTimeout(1_000) { history.saveCompleted.await() }

        assertEquals(
            listOf("Successful question"),
            cook.lastConversation.map(CookConversationMessage::content),
        )
    }

    @Test
    fun `clearing history removes transient failed messages from draft navigation`() = runBlocking {
        val history = RecordingConversationHistoryRepo()
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(
                response = flow { throw IllegalStateException("Connection unavailable") },
            ),
            historyRepository = history,
            strings = testChatStrings,
        )
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("Transient failed question")
        viewModel.sendMessage()
        withTimeout(1_000) { viewModel.requestUiState.first { state -> !state.isSending } }

        viewModel.requestClearHistory()
        assertTrue(viewModel.historyUiState.value.isClearConfirmationVisible)
        viewModel.confirmClearHistory()
        withTimeout(1_000) {
            viewModel.historyUiState.first { state -> !state.isClearing }
        }

        assertEquals(1, viewModel.conversationUiState.value.messages.size)
        assertEquals("", viewModel.requestUiState.value.errorMessage)
        assertFalse(viewModel.navigateDraftHistory(ChatDraftHistoryDirection.Previous))
    }

    @Test
    fun `browses successful user messages and restores the interrupted draft`() = runBlocking {
        val history = ConversationHistory(
            id = 7L,
            turns = listOf(
                successfulTurn(sequence = 0, question = "First question"),
                successfulTurn(sequence = 1, question = "Second question"),
            ),
        )
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(),
            historyRepository = FakeConversationHistoryRepo(history),
            strings = testChatStrings,
        )
        withTimeout(1_000) { viewModel.historyUiState.first { it.isLoaded } }

        viewModel.onDraftChanged("Unsent draft")

        assertTrue(viewModel.navigateDraftHistory(ChatDraftHistoryDirection.Previous))
        assertEquals("Second question", viewModel.draftUiState.value.draft)
        assertTrue(viewModel.navigateDraftHistory(ChatDraftHistoryDirection.Previous))
        assertEquals("First question", viewModel.draftUiState.value.draft)
        assertTrue(viewModel.navigateDraftHistory(ChatDraftHistoryDirection.Previous))
        assertEquals("First question", viewModel.draftUiState.value.draft)

        assertTrue(viewModel.navigateDraftHistory(ChatDraftHistoryDirection.Next))
        assertEquals("Second question", viewModel.draftUiState.value.draft)
        assertTrue(viewModel.navigateDraftHistory(ChatDraftHistoryDirection.Next))
        assertEquals("Unsent draft", viewModel.draftUiState.value.draft)
        assertTrue(!viewModel.navigateDraftHistory(ChatDraftHistoryDirection.Next))
    }
}

private class FakeCookRepo(
    override val startupIssue: CookStartupIssue? = null,
    var response: Flow<String> = emptyFlow(),
) : CookRepo {
    override val model = CookModel(id = "test", displayName = "Test model")
    var lastConversation: List<CookConversationMessage> = emptyList()

    /** Verifies that send message. */
    override fun sendMessage(conversation: List<CookConversationMessage>): Flow<String> {
        lastConversation = conversation
        return response
    }
}

private open class FakeConversationHistoryRepo(
    private val initialHistory: ConversationHistory? = null,
) : ConversationHistoryRepo {
    override suspend fun loadLatestConversation(): ConversationHistory? = initialHistory

    override suspend fun saveSuccessfulTurns(
        conversationId: Long?,
        turns: List<ConversationHistoryTurn>,
    ): Long = conversationId ?: 1L

    override suspend fun deleteConversation(conversationId: Long) = Unit
}

private fun successfulTurn(sequence: Long, question: String) = ConversationHistoryTurn(
    sequence = sequence,
    userContent = question,
    assistantContent = "Answer $sequence",
    modelId = "test",
    completedAtEpochMillis = sequence,
)

private class RecordingConversationHistoryRepo : FakeConversationHistoryRepo() {
    val loadCompleted = CompletableDeferred<Unit>()
    val saveCompleted = CompletableDeferred<Unit>()
    val savedTurns = mutableListOf<ConversationHistoryTurn>()

    override suspend fun loadLatestConversation(): ConversationHistory? {
        loadCompleted.complete(Unit)
        return null
    }

    override suspend fun saveSuccessfulTurns(
        conversationId: Long?,
        turns: List<ConversationHistoryTurn>,
    ): Long {
        savedTurns += turns
        saveCompleted.complete(Unit)
        return conversationId ?: 1L
    }
}

private val testChatStrings = ChatStrings(
    welcomeMessage = "\nHi, I'm Cook. 👋\n\n" +
        "Talk to me in English or Chinese, and I'll help you improve your English along the way.\n",
    thinking = "Thinking...",
    emptyResponse = "The agent returned an empty response.",
    agentRequestFailed = "The agent request failed.",
    couldNotAnswer = "I could not answer that request.",
    missingApiKey = "Set the GLM_API_KEY environment variable before starting Cook.",
    unsupportedPlatform = "Cook's AI agent is currently available on Desktop only.",
    historyLoadFailed = "Couldn't load conversation history.",
    historySaveFailed = "Couldn't save conversation history.",
    historyClearFailed = "Couldn't clear conversation history.",
)
