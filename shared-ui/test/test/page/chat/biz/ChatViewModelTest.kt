package page.chat.biz

import page.chat.ChatStrings
import repository.agent.CookConversationMessage
import repository.agent.CookMessageRole
import repository.agent.CookModel
import repository.agent.CookRepo
import repository.agent.CookResponseEvent
import repository.agent.CookStartupIssue
import repository.agent.GlmCookModel
import repository.agent.OpenRouterCookModel
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
import kotlin.test.assertNull
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
            cookRepository = FakeCookRepo(
                startupIssue = CookStartupIssue.MissingApiKey("GLM_API_KEY"),
            ),
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
        val cook = FakeCookRepo(response = textResponse("A saved answer"))
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
        val cook = FakeCookRepo(response = textResponse("A saved answer"))
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
    fun `selected model is used for the request and persisted with the completed turn`() = runBlocking {
        val history = RecordingConversationHistoryRepo()
        val cook = FakeCookRepo(response = textResponse("OpenRouter answer"))
        val viewModel = ChatViewModel(cook, history, testChatStrings)
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("Use the selected model")
        viewModel.sendMessage(OpenRouterCookModel)
        withTimeout(1_000) { history.saveCompleted.await() }

        assertEquals(OpenRouterCookModel, cook.lastModel)
        assertEquals(OpenRouterCookModel.id, history.savedTurns.single().modelId)
    }

    @Test
    fun `switching model during a response keeps the request model snapshot`() = runBlocking {
        val releaseResponse = CompletableDeferred<Unit>()
        val history = RecordingConversationHistoryRepo()
        val cook = FakeCookRepo(
            response = flow {
                emit(CookResponseEvent.TextDelta("GLM "))
                releaseResponse.await()
                emit(CookResponseEvent.TextDelta("answer"))
            },
        )
        val viewModel = ChatViewModel(cook, history, testChatStrings)
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("Keep the original request model")
        viewModel.sendMessage(GlmCookModel)
        withTimeout(1_000) { cook.requestStarted.await() }
        viewModel.onModelChanged(OpenRouterCookModel)
        releaseResponse.complete(Unit)
        withTimeout(1_000) { history.saveCompleted.await() }

        assertEquals(GlmCookModel, cook.lastModel)
        assertEquals(GlmCookModel.id, history.savedTurns.single().modelId)
    }

    @Test
    fun `missing selected model key blocks sending and preserves the draft`() = runBlocking {
        val history = RecordingConversationHistoryRepo()
        val cook = FakeCookRepo(
            startupIssue = CookStartupIssue.MissingApiKey("OPENROUTER_API_KEY"),
        )
        val viewModel = ChatViewModel(cook, history, testChatStrings)
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("Keep this draft")
        viewModel.sendMessage(OpenRouterCookModel)

        assertEquals("Keep this draft", viewModel.draftUiState.value.draft)
        assertEquals(
            "Set the OPENROUTER_API_KEY environment variable before starting Cook.",
            viewModel.requestUiState.value.errorMessage,
        )
        assertTrue(cook.lastConversation.isEmpty())
    }

    @Test
    fun `switching model refreshes model specific startup guidance`() {
        val cook = FakeCookRepo(
            startupIssues = mapOf(
                OpenRouterCookModel.id to CookStartupIssue.MissingApiKey("OPENROUTER_API_KEY"),
            ),
        )
        val viewModel = ChatViewModel(cook, FakeConversationHistoryRepo(), testChatStrings)

        viewModel.onModelChanged(OpenRouterCookModel)

        assertEquals(
            "Set the OPENROUTER_API_KEY environment variable before starting Cook.",
            viewModel.conversationUiState.value.messages.single().text,
        )
    }

    @Test
    fun `failed response retains partial agent output and marks it failed`() = runBlocking {
        val history = RecordingConversationHistoryRepo()
        val cook = FakeCookRepo(
            response = flow {
                emit(CookResponseEvent.TextDelta("Partial answer"))
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
            listOf(MessageAuthor.Agent, MessageAuthor.User, MessageAuthor.Agent),
            viewModel.conversationUiState.value.messages.map(ChatMessage::author),
        )
        val failedMessage = viewModel.conversationUiState.value.messages.last()
        assertEquals("Partial answer", failedMessage.text)
        assertEquals(AgentStatus.Failed, failedMessage.agentStatus)
        assertEquals("Connection unavailable", viewModel.requestUiState.value.errorMessage)

        assertTrue(viewModel.navigateDraftHistory(ChatDraftHistoryDirection.Previous))
        assertEquals("Do not save this", viewModel.draftUiState.value.draft)
        assertEquals("Connection unavailable", viewModel.requestUiState.value.errorMessage)
    }

    @Test
    fun `preparing events show status without visible response content`() = runBlocking {
        val blankChunksEmitted = CompletableDeferred<Unit>()
        val releaseAnswer = CompletableDeferred<Unit>()
        val history = RecordingConversationHistoryRepo()
        val cook = FakeCookRepo(
            response = flow {
                emit(CookResponseEvent.Preparing)
                blankChunksEmitted.complete(Unit)
                releaseAnswer.await()
                emit(CookResponseEvent.TextDelta("\nVisible answer"))
            },
        )
        val viewModel = ChatViewModel(cook, history, testChatStrings)
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("Wait for visible content")
        viewModel.sendMessage()
        withTimeout(1_000) { blankChunksEmitted.await() }

        val pendingMessage = viewModel.conversationUiState.value.messages.last()
        assertEquals("", pendingMessage.text)
        assertTrue(pendingMessage.isPending)
        assertEquals(AgentStatus.Preparing, pendingMessage.agentStatus)

        releaseAnswer.complete(Unit)
        withTimeout(1_000) { history.saveCompleted.await() }
        assertEquals("\nVisible answer", history.savedTurns.single().assistantContent)
    }

    @Test
    fun `agent status follows preparing tool and responding events`() = runBlocking {
        val preparingEmitted = CompletableDeferred<Unit>()
        val releaseTool = CompletableDeferred<Unit>()
        val toolStarted = CompletableDeferred<Unit>()
        val releaseToolFinished = CompletableDeferred<Unit>()
        val toolFinished = CompletableDeferred<Unit>()
        val releaseText = CompletableDeferred<Unit>()
        val textEmitted = CompletableDeferred<Unit>()
        val releaseCompletion = CompletableDeferred<Unit>()
        val history = RecordingConversationHistoryRepo()
        val cook = FakeCookRepo(
            response = flow {
                emit(CookResponseEvent.Preparing)
                preparingEmitted.complete(Unit)
                releaseTool.await()
                emit(CookResponseEvent.ToolStarted("lookup_english_word"))
                toolStarted.complete(Unit)
                releaseToolFinished.await()
                emit(CookResponseEvent.ToolFinished("lookup_english_word"))
                toolFinished.complete(Unit)
                releaseText.await()
                emit(CookResponseEvent.TextDelta("Definition"))
                textEmitted.complete(Unit)
                releaseCompletion.await()
            },
        )
        val viewModel = ChatViewModel(cook, history, testChatStrings)
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("Define a word")
        viewModel.sendMessage()
        withTimeout(1_000) { preparingEmitted.await() }
        assertEquals(AgentStatus.Preparing, viewModel.conversationUiState.value.messages.last().agentStatus)

        releaseTool.complete(Unit)
        withTimeout(1_000) { toolStarted.await() }
        assertEquals(
            AgentStatus.UsingTool("lookup_english_word"),
            viewModel.conversationUiState.value.messages.last().agentStatus,
        )

        releaseToolFinished.complete(Unit)
        withTimeout(1_000) { toolFinished.await() }
        assertEquals(AgentStatus.Preparing, viewModel.conversationUiState.value.messages.last().agentStatus)

        releaseText.complete(Unit)
        withTimeout(1_000) { textEmitted.await() }
        assertEquals(AgentStatus.Responding, viewModel.conversationUiState.value.messages.last().agentStatus)
        assertEquals("Definition", viewModel.conversationUiState.value.messages.last().text)

        releaseCompletion.complete(Unit)
        withTimeout(1_000) { viewModel.requestUiState.first { state -> !state.isSending } }
        assertEquals(AgentStatus.Done, viewModel.conversationUiState.value.messages.last().agentStatus)
    }

    @Test
    fun `done is visible before conversation persistence completes`() = runBlocking {
        val history = BlockingSaveConversationHistoryRepo()
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(response = textResponse("Complete answer")),
            historyRepository = history,
            strings = testChatStrings,
        )
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("Complete this")
        viewModel.sendMessage()
        withTimeout(1_000) { history.saveStarted.await() }

        assertFalse(viewModel.requestUiState.value.isSending)
        assertEquals(AgentStatus.Done, viewModel.conversationUiState.value.messages.last().agentStatus)

        history.releaseSave.complete(Unit)
        Unit
    }

    @Test
    fun `clearing history waits for an active save and deletes the saved conversation`() = runBlocking {
        val history = BlockingSaveConversationHistoryRepo()
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(response = textResponse("Saved before clear")),
            historyRepository = history,
            strings = testChatStrings,
        )
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("Clear this turn")
        viewModel.sendMessage()
        withTimeout(1_000) { history.saveStarted.await() }
        viewModel.requestClearHistory()
        assertTrue(viewModel.historyUiState.value.isClearConfirmationVisible)
        viewModel.confirmClearHistory()
        assertTrue(viewModel.historyUiState.value.isClearing)

        history.releaseSave.complete(Unit)
        withTimeout(1_000) { viewModel.historyUiState.first { state -> !state.isClearing } }

        assertEquals(listOf(1L), history.deletedConversationIds)
        assertEquals(1, viewModel.conversationUiState.value.messages.size)
    }

    @Test
    fun `overlapping responses serialize pending conversation persistence`() = runBlocking {
        val history = SequencedSaveConversationHistoryRepo()
        val cook = FakeCookRepo(response = textResponse("First answer"))
        val viewModel = ChatViewModel(cook, history, testChatStrings)
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("First question")
        viewModel.sendMessage()
        withTimeout(1_000) { history.firstSaveStarted.await() }

        cook.response = textResponse("Second answer")
        viewModel.onDraftChanged("Second question")
        viewModel.sendMessage()
        withTimeout(1_000) { viewModel.requestUiState.first { state -> !state.isSending } }
        assertEquals(1, history.savedBatches.size)

        history.releaseFirstSave.complete(Unit)
        withTimeout(1_000) { history.secondSaveCompleted.await() }
        assertEquals(
            listOf(listOf("First question"), listOf("Second question")),
            history.savedBatches.map { batch -> batch.map(ConversationHistoryTurn::userContent) },
        )
    }

    @Test
    fun `empty response marks the status-only agent message failed`() = runBlocking {
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
            listOf(MessageAuthor.Agent, MessageAuthor.User, MessageAuthor.Agent),
            viewModel.conversationUiState.value.messages.map(ChatMessage::author),
        )
        assertEquals(AgentStatus.Failed, viewModel.conversationUiState.value.messages.last().agentStatus)
        assertEquals("", viewModel.conversationUiState.value.messages.last().text)
        assertTrue(history.savedTurns.isEmpty())
    }

    @Test
    fun `status-only failed message is removed after its terminal animation window`() = runBlocking {
        val history = RecordingConversationHistoryRepo()
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(response = emptyFlow()),
            historyRepository = history,
            strings = testChatStrings,
            terminalStatusTiming = ChatTerminalStatusTiming(holdMillis = 1),
        )
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("No answer")
        viewModel.sendMessage()
        withTimeout(1_000) {
            viewModel.requestUiState.first { state -> !state.isSending && state.errorMessage.isNotEmpty() }
        }
        withTimeout(1_000) {
            viewModel.conversationUiState.first { state ->
                state.messages.last().agentStatus == AgentStatus.Failed &&
                    !state.messages.last().isAgentStatusVisible
            }
        }
        assertEquals(3, viewModel.conversationUiState.value.messages.size)
        withTimeout(2_000) {
            viewModel.conversationUiState.first { state -> state.messages.size == 2 }
        }

        assertEquals(
            listOf(MessageAuthor.Agent, MessageAuthor.User),
            viewModel.conversationUiState.value.messages.map(ChatMessage::author),
        )
    }

    @Test
    fun `new request immediately clears the previous terminal status`() = runBlocking {
        val history = RecordingConversationHistoryRepo()
        val cook = FakeCookRepo(response = textResponse("First answer"))
        val releaseSecondResponse = CompletableDeferred<Unit>()
        val viewModel = ChatViewModel(cook, history, testChatStrings)
        withTimeout(1_000) { history.loadCompleted.await() }

        viewModel.onDraftChanged("First question")
        viewModel.sendMessage()
        withTimeout(1_000) { history.saveCompleted.await() }
        assertEquals(AgentStatus.Done, viewModel.conversationUiState.value.messages.last().agentStatus)

        cook.response = flow {
            emit(CookResponseEvent.Preparing)
            releaseSecondResponse.await()
            emit(CookResponseEvent.TextDelta("Second answer"))
        }
        viewModel.onDraftChanged("Second question")
        viewModel.sendMessage()

        val agentMessages = viewModel.conversationUiState.value.messages.filter {
            it.author == MessageAuthor.Agent
        }
        assertNull(agentMessages[1].agentStatus)
        releaseSecondResponse.complete(Unit)
        Unit
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

        cook.response = textResponse("Successful answer")
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
    private val startupIssue: CookStartupIssue? = null,
    private val startupIssues: Map<String, CookStartupIssue> = emptyMap(),
    var response: Flow<CookResponseEvent> = emptyFlow(),
) : CookRepo {
    var lastModel: CookModel? = null
    var lastConversation: List<CookConversationMessage> = emptyList()
    val requestStarted = CompletableDeferred<Unit>()

    override fun startupIssue(model: CookModel): CookStartupIssue? =
        startupIssues[model.id] ?: startupIssue

    /** Verifies that send message. */
    override fun sendMessage(
        model: CookModel,
        conversation: List<CookConversationMessage>,
    ): Flow<CookResponseEvent> {
        lastModel = model
        lastConversation = conversation
        requestStarted.complete(Unit)
        return response
    }
}

private fun textResponse(vararg chunks: String): Flow<CookResponseEvent> =
    flowOf(*chunks.map(CookResponseEvent::TextDelta).toTypedArray())

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

private class BlockingSaveConversationHistoryRepo : FakeConversationHistoryRepo() {
    val loadCompleted = CompletableDeferred<Unit>()
    val saveStarted = CompletableDeferred<Unit>()
    val releaseSave = CompletableDeferred<Unit>()
    val deletedConversationIds = mutableListOf<Long>()

    override suspend fun loadLatestConversation(): ConversationHistory? {
        loadCompleted.complete(Unit)
        return null
    }

    override suspend fun saveSuccessfulTurns(
        conversationId: Long?,
        turns: List<ConversationHistoryTurn>,
    ): Long {
        saveStarted.complete(Unit)
        releaseSave.await()
        return conversationId ?: 1L
    }

    override suspend fun deleteConversation(conversationId: Long) {
        deletedConversationIds += conversationId
    }
}

private class SequencedSaveConversationHistoryRepo : FakeConversationHistoryRepo() {
    val loadCompleted = CompletableDeferred<Unit>()
    val firstSaveStarted = CompletableDeferred<Unit>()
    val releaseFirstSave = CompletableDeferred<Unit>()
    val secondSaveCompleted = CompletableDeferred<Unit>()
    val savedBatches = mutableListOf<List<ConversationHistoryTurn>>()

    override suspend fun loadLatestConversation(): ConversationHistory? {
        loadCompleted.complete(Unit)
        return null
    }

    override suspend fun saveSuccessfulTurns(
        conversationId: Long?,
        turns: List<ConversationHistoryTurn>,
    ): Long {
        savedBatches += turns
        if (savedBatches.size == 1) {
            firstSaveStarted.complete(Unit)
            releaseFirstSave.await()
        } else {
            secondSaveCompleted.complete(Unit)
        }
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
    missingApiKey = "Set the {environment_variable} environment variable before starting Cook.",
    unsupportedPlatform = "Cook's AI agent is currently available on Desktop only.",
    historyLoadFailed = "Couldn't load conversation history.",
    historySaveFailed = "Couldn't save conversation history.",
    historyClearFailed = "Couldn't clear conversation history.",
)
