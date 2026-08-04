package page.chat.biz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
import repository.history.ConversationHistory
import repository.history.ConversationHistoryRepo
import repository.history.ConversationHistoryTurn
import repository.history.ConversationSummary
import repository.history.conversationTitle
import repository.settings.SettingsStore

class ChatViewModel(
    private val cookRepository: CookRepo,
    private val historyRepository: ConversationHistoryRepo,
    private val strings: ChatStrings,
    initialModel: CookModel = GlmCookModel,
    private val terminalStatusTiming: ChatTerminalStatusTiming = ChatTerminalStatusTiming(),
    private val settingsStore: SettingsStore? = null,
) : ViewModel() {

    private var nextMessageId = 0L
    private var selectedModel = initialModel
    private var currentConversationId: Long? = null
    private val successfulTurns = mutableListOf<ConversationHistoryTurn>()
    private val sentUserMessages = mutableListOf<String>()
    private val pendingTurns = ConcurrentHashMap<Long, ConcurrentLinkedQueue<ConversationHistoryTurn>>()
    private val pendingTitles = ConcurrentHashMap<Long, PendingTitle>()
    private val sessionCache = mutableMapOf<Long, ChatSessionSnapshot>()
    private var draftHistoryIndex: Int? = null
    private var draftBeforeHistoryNavigation: String? = null
    private var terminalStatusJob: Job? = null
    private val persistenceMutex = Mutex()
    private val settingsMutex = Mutex()
    private var desiredChatListVisibility = true
    private var desiredSelectedConversationId: Long? = null

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

    private val _chatListUiState = MutableStateFlow(ChatListUiState())
    val chatListUiState: StateFlow<ChatListUiState> = _chatListUiState.asStateFlow()

    init {
        loadChats()
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

    /** Shows or hides the persisted chat list. */
    fun toggleChatList() {
        desiredChatListVisibility = !_chatListUiState.value.isVisible
        _chatListUiState.update { state -> state.copy(isVisible = desiredChatListVisibility) }
        persistDesiredSettings()
    }

    /** Creates and selects a new empty chat after the current chat has been used. */
    fun createNewChat() {
        if (!canChangeConversation() || !hasSubmittedCurrentMessage()) return

        _chatListUiState.update { state -> state.copy(isMutating = true) }
        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching {
                cacheCurrentSession()
                historyRepository.createConversation(System.currentTimeMillis())
            }
            result.onSuccess { summary ->
                updateSummary(summary.toUiState())
                activateEmptyConversation(summary.id)
                selectConversation(summary.id)
                _historyUiState.update { state -> state.copy(errorMessage = "") }
            }.onFailure {
                _historyUiState.update { state -> state.copy(errorMessage = strings.historySaveFailed) }
            }
            _chatListUiState.update { state -> state.copy(isMutating = false) }
        }
    }

    /** Switches to a persisted chat while retaining this run's transient state. */
    fun switchConversation(conversationId: Long) {
        if (!canChangeConversation() || conversationId == currentConversationId) return
        if (_chatListUiState.value.items.none { it.id == conversationId }) return

        _chatListUiState.update { state -> state.copy(isMutating = true) }
        clearTerminalStatus()
        viewModelScope.launch(Dispatchers.Default) {
            cacheCurrentSession()
            val result = runCatching { activateConversation(conversationId) }
            result.onSuccess {
                selectConversation(conversationId)
                _historyUiState.update { state -> state.copy(errorMessage = "") }
            }.onFailure {
                currentConversationId?.let { previousId -> activateCachedConversation(previousId) }
                _historyUiState.update { state -> state.copy(errorMessage = strings.historyLoadFailed) }
            }
            _chatListUiState.update { state -> state.copy(isMutating = false) }
        }
    }

    /** Browses user messages sent in the selected chat without discarding its current draft. */
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
        val conversationId = currentConversationId
        if (
            question.isEmpty() ||
            conversationId == null ||
            _requestUiState.value.isSending ||
            !_historyUiState.value.isLoaded ||
            _historyUiState.value.isClearing ||
            _chatListUiState.value.isMutating
        ) {
            return
        }
        cookRepository.startupIssue(model)?.let { issue ->
            _requestUiState.update { state -> state.copy(errorMessage = startupIssueMessage(issue)) }
            return
        }

        val submittedAt = System.currentTimeMillis()
        val shouldPersistInitialTitle = ensureInitialTitle(conversationId, question, submittedAt)
        updateLiveSummary(conversationId, question, submittedAt)
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

        if (shouldPersistInitialTitle) {
            viewModelScope.launch(Dispatchers.Default) {
                persistInitialTitle(conversationId)
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            val collected = StringBuilder()
            val result = runCatching {
                cookRepository.sendMessage(model, modelConversation(question)).collect { event ->
                    when (event) {
                        CookResponseEvent.Preparing -> updateAgentStatus(pendingMessageId, AgentStatus.Preparing)
                        is CookResponseEvent.TextDelta -> {
                            collected.append(event.text)
                            if (collected.isNotBlank()) {
                                val answer = collected.toString()
                                updatePendingMessage(
                                    id = pendingMessageId,
                                    text = answer,
                                    status = AgentStatus.Responding,
                                )
                                updateLiveSummary(conversationId, answer, System.currentTimeMillis())
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
                pendingTurns.getOrPut(conversationId) { ConcurrentLinkedQueue() }.add(turn)
                updateLiveSummary(conversationId, answer, turn.completedAtEpochMillis)
                scheduleTerminalStatusCleanup(pendingMessageId)
                _requestUiState.update { ChatRequestUiState(isSending = false) }
                persistPendingTurns(conversationId)
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

    /** Opens the destructive delete confirmation for the selected chat. */
    fun requestClearHistory() {
        if (!canChangeConversation() || currentConversationId == null) return
        _historyUiState.update { state -> state.copy(isClearConfirmationVisible = true) }
    }

    /** Hides the delete confirmation without changing data. */
    fun dismissClearHistoryConfirmation() {
        _historyUiState.update { state -> state.copy(isClearConfirmationVisible = false) }
    }

    /** Deletes the selected chat and activates the most recent remaining chat. */
    fun confirmClearHistory() {
        val conversationId = currentConversationId
        if (!_historyUiState.value.isClearConfirmationVisible || conversationId == null) return

        _historyUiState.update {
            it.copy(isClearing = true, errorMessage = "", isClearConfirmationVisible = false)
        }
        _chatListUiState.update { state -> state.copy(isMutating = true) }
        viewModelScope.launch(Dispatchers.Default) {
            val deleteResult = runCatching {
                persistenceMutex.withLock {
                    historyRepository.deleteConversation(conversationId)
                    pendingTurns.remove(conversationId)
                    pendingTitles.remove(conversationId)
                }
            }
            if (deleteResult.isFailure) {
                _historyUiState.update {
                    it.copy(isClearing = false, errorMessage = strings.historyClearFailed)
                }
                _chatListUiState.update { state -> state.copy(isMutating = false) }
                return@launch
            }

            terminalStatusJob?.cancel()
            terminalStatusJob = null
            sessionCache.remove(conversationId)
            val remaining = _chatListUiState.value.items.filterNot { it.id == conversationId }
            _chatListUiState.update { state ->
                state.copy(items = remaining, selectedConversationId = null)
            }
            currentConversationId = null

            val activateResult = runCatching {
                val nextId = remaining.firstOrNull()?.id ?: run {
                    val created = historyRepository.createConversation(System.currentTimeMillis())
                    updateSummary(created.toUiState())
                    activateEmptyConversation(created.id)
                    created.id
                }
                if (remaining.isNotEmpty()) activateConversation(nextId)
                nextId
            }
            activateResult.onSuccess { nextId ->
                selectConversation(nextId)
                _requestUiState.update { ChatRequestUiState() }
                _historyUiState.update { ChatHistoryUiState(isLoaded = true) }
            }.onFailure {
                desiredSelectedConversationId = null
                persistDesiredSettings()
                successfulTurns.clear()
                sentUserMessages.clear()
                resetDraftHistoryNavigation()
                _conversationUiState.update {
                    ChatConversationUiState(listOf(initialAgentMessage()))
                }
                _draftUiState.update { ChatDraftUiState() }
                _requestUiState.update { ChatRequestUiState() }
                _historyUiState.update {
                    ChatHistoryUiState(isLoaded = true, errorMessage = strings.historyLoadFailed)
                }
            }
            _chatListUiState.update { state -> state.copy(isMutating = false) }
        }
    }

    /** Waits for queued chat history and preference writes before application exit. */
    suspend fun flushPersistence() {
        persistenceMutex.withLock {
            persistPendingTitlesLocked()
            for (conversationId in pendingTurns.keys.toList()) {
                persistPendingTurnsLocked(conversationId)
            }
        }
        settingsMutex.withLock { persistDesiredSettingsLocked() }
    }

    private fun loadChats() {
        viewModelScope.launch(Dispatchers.Default) {
            desiredChatListVisibility = runCatching {
                settingsStore?.isChatListVisible?.first() ?: true
            }.getOrDefault(true)
            val storedConversationId = runCatching {
                settingsStore?.selectedConversationId?.first()
            }.getOrNull()
            val result = runCatching {
                val summaries = historyRepository.listConversations().ifEmpty {
                    listOf(historyRepository.createConversation(System.currentTimeMillis()))
                }
                val selected = summaries.firstOrNull { it.id == storedConversationId } ?: summaries.first()
                val history = requireNotNull(historyRepository.loadConversation(selected.id)) {
                    "Conversation ${selected.id} no longer exists."
                }
                Triple(summaries, selected, history)
            }
            result.onSuccess { (summaries, selected, history) ->
                restoreHistory(selected.id, history)
                desiredSelectedConversationId = selected.id
                _historyUiState.update { ChatHistoryUiState(isLoaded = true) }
                _chatListUiState.update {
                    ChatListUiState(
                        isLoaded = true,
                        isVisible = desiredChatListVisibility,
                        items = summaries.map(ConversationSummary::toUiState).sortedByRecentActivity(),
                        selectedConversationId = selected.id,
                    )
                }
                persistDesiredSettings()
            }.onFailure {
                _chatListUiState.update {
                    ChatListUiState(isLoaded = true, isVisible = desiredChatListVisibility)
                }
                _historyUiState.update {
                    ChatHistoryUiState(isLoaded = true, errorMessage = strings.historyLoadFailed)
                }
            }
        }
    }

    private suspend fun activateConversation(conversationId: Long) {
        sessionCache[conversationId]?.let {
            restoreSession(conversationId, it)
            return
        }
        val history = requireNotNull(historyRepository.loadConversation(conversationId)) {
            "Conversation $conversationId no longer exists."
        }
        restoreHistory(conversationId, history)
    }

    private fun activateCachedConversation(conversationId: Long) {
        sessionCache[conversationId]?.let { restoreSession(conversationId, it) }
    }

    private fun activateEmptyConversation(conversationId: Long) {
        currentConversationId = conversationId
        successfulTurns.clear()
        sentUserMessages.clear()
        resetDraftHistoryNavigation()
        _conversationUiState.update { ChatConversationUiState(listOf(initialAgentMessage())) }
        _draftUiState.update { ChatDraftUiState() }
        _requestUiState.update { ChatRequestUiState() }
    }

    private fun restoreHistory(conversationId: Long, history: ConversationHistory) {
        currentConversationId = conversationId
        successfulTurns.clear()
        successfulTurns += history.turns
        sentUserMessages.clear()
        sentUserMessages += history.turns.map(ConversationHistoryTurn::userContent)
        resetDraftHistoryNavigation()
        val messages = history.turns.flatMap { turn ->
            listOf(
                ChatMessage(nextId(), MessageAuthor.User, turn.userContent),
                ChatMessage(nextId(), MessageAuthor.Agent, turn.assistantContent),
            )
        }
        _conversationUiState.update {
            ChatConversationUiState(messages.ifEmpty { listOf(initialAgentMessage()) })
        }
        _draftUiState.update { ChatDraftUiState() }
        _requestUiState.update { ChatRequestUiState() }
    }

    private fun restoreSession(conversationId: Long, snapshot: ChatSessionSnapshot) {
        currentConversationId = conversationId
        successfulTurns.clear()
        successfulTurns += snapshot.successfulTurns
        sentUserMessages.clear()
        sentUserMessages += snapshot.sentUserMessages
        draftHistoryIndex = snapshot.draftHistoryIndex
        draftBeforeHistoryNavigation = snapshot.draftBeforeHistoryNavigation
        _conversationUiState.update { snapshot.conversationState }
        _draftUiState.update { snapshot.draftState }
        _requestUiState.update { ChatRequestUiState() }
    }

    private fun cacheCurrentSession() {
        val conversationId = currentConversationId ?: return
        sessionCache[conversationId] = ChatSessionSnapshot(
            conversationState = _conversationUiState.value,
            draftState = _draftUiState.value,
            successfulTurns = successfulTurns.toList(),
            sentUserMessages = sentUserMessages.toList(),
            draftHistoryIndex = draftHistoryIndex,
            draftBeforeHistoryNavigation = draftBeforeHistoryNavigation,
        )
    }

    private fun selectConversation(conversationId: Long) {
        desiredSelectedConversationId = conversationId
        _chatListUiState.update { state -> state.copy(selectedConversationId = conversationId) }
        persistDesiredSettings()
    }

    private fun ensureInitialTitle(conversationId: Long, question: String, updatedAt: Long): Boolean {
        val current = _chatListUiState.value.items.firstOrNull { it.id == conversationId } ?: return false
        if (current.title != null) return false

        val title = conversationTitle(question)
        pendingTitles[conversationId] = PendingTitle(question, updatedAt)
        updateSummary(current.copy(title = title, updatedAtEpochMillis = updatedAt))
        return true
    }

    private fun updateLiveSummary(conversationId: Long, preview: String, updatedAt: Long) {
        val current = _chatListUiState.value.items.firstOrNull { it.id == conversationId } ?: return
        updateSummary(current.copy(preview = preview, updatedAtEpochMillis = updatedAt))
    }

    private fun updateSummary(summary: ChatListItemUiState) {
        _chatListUiState.update { state ->
            state.copy(
                items = (state.items.filterNot { it.id == summary.id } + summary)
                    .sortedByRecentActivity(),
            )
        }
    }

    private suspend fun persistInitialTitle(conversationId: Long) {
        persistenceMutex.withLock { persistInitialTitleLocked(conversationId) }
    }

    private suspend fun persistInitialTitleLocked(conversationId: Long) {
        val pending = pendingTitles[conversationId] ?: return
        runCatching {
            historyRepository.setInitialTitle(
                conversationId = conversationId,
                firstUserMessage = pending.firstUserMessage,
                updatedAtEpochMillis = pending.updatedAtEpochMillis,
            )
        }.onSuccess { summary ->
            pendingTitles.remove(conversationId, pending)
            val current = _chatListUiState.value.items.firstOrNull { it.id == conversationId }
            updateSummary(
                summary.toUiState().copy(
                    preview = summary.preview ?: current?.preview,
                    updatedAtEpochMillis = maxOf(
                        summary.updatedAtEpochMillis,
                        current?.updatedAtEpochMillis ?: Long.MIN_VALUE,
                    ),
                ),
            )
            _historyUiState.update { state -> state.copy(errorMessage = "") }
        }.onFailure {
            _historyUiState.update { state -> state.copy(errorMessage = strings.historySaveFailed) }
        }
    }

    private suspend fun persistPendingTitlesLocked() {
        for (conversationId in pendingTitles.keys.toList()) {
            persistInitialTitleLocked(conversationId)
        }
    }

    private suspend fun persistPendingTurns(conversationId: Long) {
        persistenceMutex.withLock { persistPendingTurnsLocked(conversationId) }
    }

    private suspend fun persistPendingTurnsLocked(conversationId: Long) {
        val queue = pendingTurns[conversationId] ?: return
        val turns = queue.toList()
        if (turns.isEmpty()) return

        runCatching {
            historyRepository.saveSuccessfulTurns(conversationId, turns)
        }.onSuccess {
            queue.removeAll(turns.toSet())
            if (queue.isEmpty()) pendingTurns.remove(conversationId, queue)
            _historyUiState.update { state -> state.copy(errorMessage = "") }
        }.onFailure {
            _historyUiState.update { state -> state.copy(errorMessage = strings.historySaveFailed) }
        }
    }

    private fun persistDesiredSettings() {
        if (settingsStore == null) return
        viewModelScope.launch(Dispatchers.Default) {
            settingsMutex.withLock { persistDesiredSettingsLocked() }
        }
    }

    private suspend fun persistDesiredSettingsLocked() {
        val store = settingsStore ?: return
        runCatching {
            store.setChatListVisible(desiredChatListVisibility)
            store.setSelectedConversationId(desiredSelectedConversationId)
        }.onFailure {
            _historyUiState.update { state -> state.copy(errorMessage = strings.historySaveFailed) }
        }
    }

    private fun hasSubmittedCurrentMessage(): Boolean =
        _chatListUiState.value.selectedItem?.title != null || sentUserMessages.isNotEmpty()

    private fun canChangeConversation(): Boolean =
        _historyUiState.value.isLoaded &&
            !_historyUiState.value.isClearing &&
            !_requestUiState.value.isSending &&
            !_chatListUiState.value.isMutating

    private fun modelConversation(question: String): List<CookConversationMessage> =
        successfulTurns.flatMap { turn ->
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

private fun ConversationSummary.toUiState() = ChatListItemUiState(
    id = id,
    title = title?.let(::conversationTitle),
    preview = preview,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun List<ChatListItemUiState>.sortedByRecentActivity(): List<ChatListItemUiState> =
    sortedWith(
        compareByDescending<ChatListItemUiState> { it.updatedAtEpochMillis }
            .thenByDescending(ChatListItemUiState::id),
    )

private data class PendingTitle(
    val firstUserMessage: String,
    val updatedAtEpochMillis: Long,
)

private data class ChatSessionSnapshot(
    val conversationState: ChatConversationUiState,
    val draftState: ChatDraftUiState,
    val successfulTurns: List<ConversationHistoryTurn>,
    val sentUserMessages: List<String>,
    val draftHistoryIndex: Int?,
    val draftBeforeHistoryNavigation: String?,
)

private val AgentStatus?.isTerminal: Boolean
    get() = this == AgentStatus.Done || this == AgentStatus.Failed
