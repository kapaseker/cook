package page.chat.biz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import page.chat.ChatStrings
import repository.agent.CookConversationMessage
import repository.agent.CookMessageRole
import repository.agent.CookRepo
import repository.agent.CookStartupException
import repository.agent.CookStartupIssue
import repository.history.ConversationHistoryRepo
import repository.history.ConversationHistoryTurn

class ChatViewModel(
    private val cookRepository: CookRepo,
    private val historyRepository: ConversationHistoryRepo,
    private val strings: ChatStrings,
) : ViewModel() {

    private var nextMessageId = 0L
    private var currentConversationId: Long? = null
    private val successfulTurns = mutableListOf<ConversationHistoryTurn>()
    private val sentUserMessages = mutableListOf<String>()
    private val pendingPersistenceTurns = mutableListOf<ConversationHistoryTurn>()
    private var draftHistoryIndex: Int? = null
    private var draftBeforeHistoryNavigation: String? = null

    private val initialMessage = when (cookRepository.startupIssue) {
        CookStartupIssue.MissingApiKey -> strings.missingApiKey
        CookStartupIssue.UnsupportedPlatform -> strings.unsupportedPlatform
        null -> strings.welcomeMessage
    }

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
    fun sendMessage() {
        val question = removeEmptyLines(_draftUiState.value.draft)
        if (
            question.isEmpty() ||
            _requestUiState.value.isSending ||
            !_historyUiState.value.isLoaded ||
            _historyUiState.value.isClearing
        ) {
            return
        }

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
                    text = strings.thinking,
                    isPending = true,
                ),
            )
        }
        _draftUiState.update { state -> state.copy(draft = "") }
        sentUserMessages += question
        resetDraftHistoryNavigation()
        _requestUiState.update { ChatRequestUiState(isSending = true) }

        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching {
                val collected = StringBuilder()
                cookRepository.sendMessage(modelConversation(question)).collect { chunk ->
                    collected.append(chunk)
                    updatePendingMessage(pendingMessageId, collected.toString())
                }
                collected.toString()
            }
            val answer = result.getOrNull()?.takeIf(String::isNotBlank)

            if (answer != null) {
                updatePendingMessage(pendingMessageId, answer, isPending = false)
                val turn = ConversationHistoryTurn(
                    sequence = successfulTurns.size.toLong(),
                    userContent = question,
                    assistantContent = answer,
                    modelId = cookRepository.model.id,
                    completedAtEpochMillis = System.currentTimeMillis(),
                )
                successfulTurns += turn
                pendingPersistenceTurns += turn
                persistPendingTurns()
                _requestUiState.update { ChatRequestUiState(isSending = false) }
            } else {
                val requestError = if (result.isSuccess) {
                    strings.emptyResponse
                } else {
                    result.exceptionOrNull()?.let(::errorMessage) ?: strings.agentRequestFailed
                }
                removeMessage(pendingMessageId)
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
                currentConversationId?.let { conversationId ->
                    historyRepository.deleteConversation(conversationId)
                }
            }
            if (result.isSuccess) {
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
        val turns = pendingPersistenceTurns.toList()
        if (turns.isEmpty()) return

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

    private fun modelConversation(question: String): List<CookConversationMessage> = successfulTurns.flatMap { turn ->
        listOf(
            CookConversationMessage(CookMessageRole.User, turn.userContent),
            CookConversationMessage(CookMessageRole.Assistant, turn.assistantContent),
        )
    } + CookConversationMessage(CookMessageRole.User, question)

    private fun updatePendingMessage(id: Long, text: String, isPending: Boolean = true) {
        _conversationUiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == id) message.copy(text = text, isPending = isPending) else message
                },
            )
        }
    }

    private fun removeMessage(id: Long) {
        _conversationUiState.update { state ->
            state.copy(messages = state.messages.filterNot { message -> message.id == id })
        }
    }

    private fun updateDraftFromHistory(value: String) {
        _draftUiState.update { state -> state.copy(draft = value) }
    }

    private fun resetDraftHistoryNavigation() {
        draftHistoryIndex = null
        draftBeforeHistoryNavigation = null
    }

    private fun initialAgentMessage() = ChatMessage(
        id = nextId(),
        author = MessageAuthor.Agent,
        text = initialMessage,
    )

    private fun nextId(): Long {
        nextMessageId += 1
        return nextMessageId
    }

    private fun errorMessage(throwable: Throwable): String = when (throwable) {
        is CookStartupException -> when (throwable.issue) {
            CookStartupIssue.MissingApiKey -> strings.missingApiKey
            CookStartupIssue.UnsupportedPlatform -> strings.unsupportedPlatform
        }
        else -> throwable.message ?: strings.agentRequestFailed
    }
}

/** Removes blank draft lines before a message is sent while retaining non-empty line breaks. */
internal fun removeEmptyLines(draft: String): String = draft.lineSequence()
    .filter(String::isNotBlank)
    .joinToString(separator = "\n")
    .trim()
