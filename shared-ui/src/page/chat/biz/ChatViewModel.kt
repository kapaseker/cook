package page.chat.biz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import page.chat.ChatStrings
import page.chat.missingApiKey
import repository.agent.CookConversationMessage
import repository.agent.CookMessageRole
import repository.agent.CookModel
import repository.agent.CookRepo
import repository.agent.CookResponseEvent
import repository.agent.CookStartupException
import repository.agent.CookStartupIssue
import repository.agent.GlmCookModel
import repository.history.ConversationHistoryRepo
import repository.history.ConversationHistoryTurn
import java.util.concurrent.ConcurrentLinkedQueue

class ChatViewModel(
    private val cookRepository: CookRepo,
    private val historyRepository: ConversationHistoryRepo,
    private val strings: ChatStrings,
    initialModel: CookModel = GlmCookModel,
    private val terminalStatusTiming: ChatTerminalStatusTiming = ChatTerminalStatusTiming(),
) : ViewModel() {

    private var nextMessageId = 0L
    private var selectedModel = initialModel
    private var currentConversationId: Long? = null
    private val successfulTurns = mutableListOf<ConversationHistoryTurn>()
    private val sentUserMessages = mutableListOf<String>()
    private val pendingPersistenceTurns = ConcurrentLinkedQueue<ConversationHistoryTurn>()
    private var draftHistoryIndex: Int? = null
    private var draftBeforeHistoryNavigation: String? = null
    private var terminalStatusJob: Job? = null
    private val persistenceMutex = Mutex()

    private val _conversationUiState = MutableStateFlow(
        ChatConversationUiState(messages = listOf(initialAgentMessage())),
    )
    val conversationUiState: StateFlow<ChatConversationUiState> =
        _conversationUiState.asStateFlow()

    private val _draftUiState = MutableStateFlow(ChatDraftUiState())
    val draftUiState: StateFlow<ChatDraftUiState> = _draftUiState.asStateFlow()

    private val _requestUiState = MutableStateFlow(ChatRequestUiState())
    val requestUiState: StateFlow<ChatRequestUiState> = _requestUiState.asStateFlow()

    private val _historyUiState = MutableStateFlow(ChatHistoryUiState())
    val historyUiState: StateFlow<ChatHistoryUiState> = _historyUiState.asStateFlow()

    init {
        loadHistory()
    }

    /** Updates the draft and clears the request error. */
    fun onDraftChanged(value: String) {
        resetDraftHistoryNavigation()
        _draftUiState.update { state -> state.copy(draft = value) }
        _requestUiState.update { state -> state.copy(errorMessage = "") }
    }

    /** Updates the selected model and any model-specific startup guidance. */
    fun onModelChanged(model: CookModel) {
        selectedModel = model
        _conversationUiState.update { state ->
            if (state.messages.size == 1 && state.messages.single().author == MessageAuthor.Agent) {
                state.copy(messages = listOf(initialAgentMessage(state.messages.single().id)))
            } else {
                state
            }
        }
    }

    /** Browses user messages sent during this session without discarding the current draft. */
    fun navigateDraftHistory(direction: ChatDraftHistoryDirection): Boolean {
        if (sentUserMessages.isEmpty()) return false

        when (direction) {
            ChatDraftHistoryDirection.Previous -> {
                val nextIndex = draftHistoryIndex?.let { (it - 1).coerceAtLeast(0) }
                    ?: sentUserMessages.lastIndex.also {
                        draftBeforeHistoryNavigation = _draftUiState.value.draft
                    }
                draftHistoryIndex = nextIndex
                updateDraftFromHistory(sentUserMessages[nextIndex])
            }

            ChatDraftHistoryDirection.Next -> {
                val currentIndex = draftHistoryIndex ?: return false
                if (currentIndex == sentUserMessages.lastIndex) {
                    draftHistoryIndex = null
                    updateDraftFromHistory(draftBeforeHistoryNavigation.orEmpty())
                    draftBeforeHistoryNavigation = null
                } else {
                    val nextIndex = currentIndex + 1
                    draftHistoryIndex = nextIndex
                    updateDraftFromHistory(sentUserMessages[nextIndex])
                }
            }
        }
        return true
    }

    /** Streams the assistant response and persists only a completed, non-empty response. */
    fun sendMessage(model: CookModel = GlmCookModel) {
        selectedModel = model
        val question = removeEmptyLines(_draftUiState.value.draft)
        if (
            question.isEmpty() ||
            _requestUiState.value.isSending ||
            !_historyUiState.value.isLoaded ||
            _historyUiState.value.isClearing
        ) {
            return
        }
        cookRepository.startupIssue(model)?.let { issue ->
            _requestUiState.update { state ->
                state.copy(errorMessage = startupIssueMessage(issue))
            }
            return
        }

        clearTerminalStatus()
        val pendingMessageId = nextId()
        _conversationUiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    id = nextId(),
                    author = MessageAuthor.User,
                    text = question,
                ) + ChatMessage(
                    id = pendingMessageId,
                    author = MessageAuthor.Agent,
                    text = "",
                    isPending = true,
                    agentStatus = AgentStatus.Thinking,
                    isAgentStatusVisible = true,
                ),
            )
        }
        _draftUiState.update { state -> state.copy(draft = "") }
        sentUserMessages += question
        resetDraftHistoryNavigation()
        _requestUiState.update { ChatRequestUiState(isSending = true) }

        viewModelScope.launch(Dispatchers.Default) {
            val collected = StringBuilder()
            val result = runCatching {
                cookRepository.sendMessage(model, modelConversation(question)).collect { event ->
                    when (event) {
                        CookResponseEvent.Preparing -> updateAgentStatus(
                            pendingMessageId,
                            AgentStatus.Preparing,
                        )
                        is CookResponseEvent.TextDelta -> {
                            collected.append(event.text)
                            if (collected.isNotBlank()) {
                                updatePendingMessage(
                                    id = pendingMessageId,
                                    text = collected.toString(),
                                    status = AgentStatus.Responding,
                                )
                            }
                        }
                        is CookResponseEvent.ToolStarted -> updateAgentStatus(
                            pendingMessageId,
                            AgentStatus.UsingTool(event.name),
                        )
                        is CookResponseEvent.ToolFinished -> updateAgentStatus(
                            pendingMessageId,
                            AgentStatus.Preparing,
                        )
                    }
                }
            }
            val answer = collected.toString().takeIf(String::isNotBlank)

            if (result.isSuccess && answer != null) {
                updatePendingMessage(
                    id = pendingMessageId,
                    text = answer,
                    isPending = false,
                    status = AgentStatus.Done,
                )
                val turn = ConversationHistoryTurn(
                    sequence = successfulTurns.size.toLong(),
                    userContent = question,
                    assistantContent = answer,
                    modelId = model.id,
                    completedAtEpochMillis = System.currentTimeMillis(),
                )
                successfulTurns += turn
                pendingPersistenceTurns += turn
                scheduleTerminalStatusCleanup(pendingMessageId)
                _requestUiState.update { ChatRequestUiState(isSending = false) }
                persistPendingTurns()
            } else {
                val requestError = if (result.isSuccess) {
                    strings.emptyResponse
                } else {
                    result.exceptionOrNull()?.let(::errorMessage) ?: strings.agentRequestFailed
                }
                updatePendingMessage(
                    id = pendingMessageId,
                    text = answer.orEmpty(),
                    isPending = false,
                    status = AgentStatus.Failed,
                )
                scheduleTerminalStatusCleanup(pendingMessageId)
                _requestUiState.update {
                    ChatRequestUiState(isSending = false, errorMessage = requestError)
                }
            }
        }
    }

    /** Opens the destructive clear-history confirmation when a conversation is visible. */
    fun requestClearHistory() {
        if (
            !_historyUiState.value.isLoaded ||
            _historyUiState.value.isClearing ||
            _requestUiState.value.isSending ||
            _conversationUiState.value.messages.none { it.author == MessageAuthor.User }
        ) {
            return
        }
        _historyUiState.update { state -> state.copy(isClearConfirmationVisible = true) }
    }

    /** Hides the clear-history confirmation without changing data. */
    fun dismissClearHistoryConfirmation() {
        _historyUiState.update { state -> state.copy(isClearConfirmationVisible = false) }
    }

    /** Deletes the persisted conversation and resets the live conversation after success. */
    fun confirmClearHistory() {
        if (!_historyUiState.value.isClearConfirmationVisible) return

        _historyUiState.update {
            it.copy(isClearing = true, errorMessage = "", isClearConfirmationVisible = false)
        }
        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching {
                persistenceMutex.withLock {
                    currentConversationId?.let { conversationId ->
                        historyRepository.deleteConversation(conversationId)
                    }
                    pendingPersistenceTurns.clear()
                }
            }
            if (result.isSuccess) {
                terminalStatusJob?.cancel()
                terminalStatusJob = null
                currentConversationId = null
                successfulTurns.clear()
                sentUserMessages.clear()
                pendingPersistenceTurns.clear()
                resetDraftHistoryNavigation()
                _conversationUiState.update { ChatConversationUiState(listOf(initialAgentMessage())) }
                _requestUiState.update { ChatRequestUiState() }
                _historyUiState.update { ChatHistoryUiState(isLoaded = true) }
            } else {
                _historyUiState.update {
                    it.copy(isClearing = false, errorMessage = strings.historyClearFailed)
                }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { historyRepository.loadLatestConversation() }
                .onSuccess { history ->
                    currentConversationId = history?.id
                    successfulTurns += history?.turns.orEmpty()
                    sentUserMessages += history?.turns.orEmpty().map(ConversationHistoryTurn::userContent)
                    val messages = history?.turns.orEmpty().flatMap { turn ->
                        listOf(
                            ChatMessage(nextId(), MessageAuthor.User, turn.userContent),
                            ChatMessage(nextId(), MessageAuthor.Agent, turn.assistantContent),
                        )
                    }
                    if (messages.isNotEmpty()) {
                        _conversationUiState.update { ChatConversationUiState(messages) }
                    }
                    _historyUiState.update { ChatHistoryUiState(isLoaded = true) }
                }
                .onFailure {
                    _historyUiState.update {
                        ChatHistoryUiState(isLoaded = true, errorMessage = strings.historyLoadFailed)
                    }
                }
        }
    }

    private suspend fun persistPendingTurns() {
        persistenceMutex.withLock {
            val turns = pendingPersistenceTurns.toList()
            if (turns.isEmpty()) return@withLock

            runCatching {
                historyRepository.saveSuccessfulTurns(currentConversationId, turns)
            }.onSuccess { conversationId ->
                currentConversationId = conversationId
                pendingPersistenceTurns.removeAll(turns.toSet())
                _historyUiState.update { state -> state.copy(errorMessage = "") }
            }.onFailure {
                _historyUiState.update { state -> state.copy(errorMessage = strings.historySaveFailed) }
            }
        }
    }

    private fun modelConversation(question: String): List<CookConversationMessage> = successfulTurns.flatMap { turn ->
        listOf(
            CookConversationMessage(CookMessageRole.User, turn.userContent),
            CookConversationMessage(CookMessageRole.Assistant, turn.assistantContent),
        )
    } + CookConversationMessage(CookMessageRole.User, question)

    private fun updatePendingMessage(
        id: Long,
        text: String,
        isPending: Boolean = true,
        status: AgentStatus = AgentStatus.Responding,
    ) {
        _conversationUiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == id) {
                        message.copy(
                            text = text,
                            isPending = isPending,
                            agentStatus = status,
                            isAgentStatusVisible = true,
                        )
                    } else {
                        message
                    }
                },
            )
        }
    }

    private fun updateAgentStatus(id: Long, status: AgentStatus) {
        _conversationUiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == id) {
                        message.copy(agentStatus = status, isAgentStatusVisible = true)
                    } else {
                        message
                    }
                },
            )
        }
    }

    private fun scheduleTerminalStatusCleanup(id: Long) {
        terminalStatusJob?.cancel()
        terminalStatusJob = viewModelScope.launch(Dispatchers.Default) {
            delay(terminalStatusTiming.holdMillis)
            _conversationUiState.update { state ->
                state.copy(
                    messages = state.messages.map { message ->
                        if (message.id == id) message.copy(isAgentStatusVisible = false) else message
                    },
                )
            }
            delay(AgentStatusFadeDurationMillis.toLong())
            clearTerminalStatus(id)
        }
    }

    private fun clearTerminalStatus(id: Long? = null) {
        if (id == null) {
            terminalStatusJob?.cancel()
            terminalStatusJob = null
        }
        _conversationUiState.update { state ->
            state.copy(
                messages = state.messages.mapNotNull { message ->
                    val isTarget = (id == null || message.id == id) && message.agentStatus.isTerminal
                    when {
                        !isTarget -> message
                        message.text.isBlank() -> null
                        else -> message.copy(agentStatus = null, isAgentStatusVisible = false)
                    }
                },
            )
        }
        if (id != null) terminalStatusJob = null
    }

    private fun updateDraftFromHistory(value: String) {
        _draftUiState.update { state -> state.copy(draft = value) }
    }

    private fun resetDraftHistoryNavigation() {
        draftHistoryIndex = null
        draftBeforeHistoryNavigation = null
    }

    private fun initialAgentMessage(id: Long = nextId()) = ChatMessage(
        id = id,
        author = MessageAuthor.Agent,
        text = when (val issue = cookRepository.startupIssue(selectedModel)) {
            null -> strings.welcomeMessage
            else -> startupIssueMessage(issue)
        },
    )

    private fun nextId(): Long {
        nextMessageId += 1
        return nextMessageId
    }

    private fun errorMessage(throwable: Throwable): String = when (throwable) {
        is CookStartupException -> startupIssueMessage(throwable.issue)
        else -> throwable.message ?: strings.agentRequestFailed
    }

    private fun startupIssueMessage(issue: CookStartupIssue): String = when (issue) {
        is CookStartupIssue.MissingApiKey -> strings.missingApiKey(issue.environmentVariable)
        CookStartupIssue.UnsupportedPlatform -> strings.unsupportedPlatform
    }
}

/** Removes blank draft lines before a message is sent while retaining non-empty line breaks. */
internal fun removeEmptyLines(draft: String): String = draft.lineSequence()
    .filter(String::isNotBlank)
    .joinToString(separator = "\n")
    .trim()

private val AgentStatus?.isTerminal: Boolean
    get() = this == AgentStatus.Done || this == AgentStatus.Failed
