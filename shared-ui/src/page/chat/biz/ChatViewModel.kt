package page.chat.biz

import repository.agent.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import page.chat.ChatStrings

class ChatViewModel(
    private val cookRepository: CookRepo,
    private val strings: ChatStrings,
) : ViewModel() {

    private var nextId = 0L

    private val initialMessage = when (cookRepository.startupIssue) {
        CookStartupIssue.MissingApiKey -> strings.missingApiKey
        CookStartupIssue.UnsupportedPlatform -> strings.unsupportedPlatform
        null -> strings.welcomeMessage
    }

    private val _conversationUiState = MutableStateFlow(
        ChatConversationUiState(
            messages = listOf(
                ChatMessage(
                    id = nextMessageId(),
                    author = MessageAuthor.Agent,
                    text = initialMessage,
                )
            ),
        )
    )

    val conversationUiState: StateFlow<ChatConversationUiState> =
        _conversationUiState.asStateFlow()

    private val _draftUiState = MutableStateFlow(ChatDraftUiState())

    val draftUiState: StateFlow<ChatDraftUiState> = _draftUiState.asStateFlow()

    private val _requestUiState = MutableStateFlow(ChatRequestUiState())

    val requestUiState: StateFlow<ChatRequestUiState> = _requestUiState.asStateFlow()

    /** Updates the draft and clears any displayed error. */
    fun onDraftChanged(value: String) {
        _draftUiState.update { state -> state.copy(draft = value) }
        _requestUiState.update { state -> state.copy(errorMessage = "") }
    }

    /** Streams the assistant response for the supplied conversation. */
    fun sendMessage() {
        val question = _draftUiState.value.draft.trim()
        if (question.isEmpty() || _requestUiState.value.isSending) {
            return
        }

        val conversation = _conversationUiState.value.messages.mapNotNull { message ->
            if (message.isPending || message.text.isBlank()) {
                null
            } else {
                CookConversationMessage(
                    role = when (message.author) {
                        MessageAuthor.User -> CookMessageRole.User
                        MessageAuthor.Agent -> CookMessageRole.Assistant
                    },
                    content = message.text,
                )
            }
        } + CookConversationMessage(
            role = CookMessageRole.User,
            content = question,
        )

        val pendingMessageId = nextMessageId()
        _conversationUiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    id = nextMessageId(),
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
        _requestUiState.update { state -> state.copy(isSending = true, errorMessage = "") }

        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching {
                // 流式 collect：每个 chunk 累加到 pending 消息的 text
                val collected = StringBuilder()
                cookRepository.sendMessage(conversation).collect { chunk ->
                    collected.append(chunk)
                    _conversationUiState.update { state ->
                        state.copy(
                            messages = state.messages.map { message ->
                                if (message.id == pendingMessageId) {
                                    message.copy(text = collected.toString())
                                } else {
                                    message
                                }
                            }
                        )
                    }
                }
                collected.toString()
            }

            val requestErrorMessage = result.exceptionOrNull()?.let(::errorMessage).orEmpty()
            _conversationUiState.update { state ->
                if (result.isSuccess) {
                    val answer = result.getOrThrow()
                    state.copy(
                        messages = state.messages.map { message ->
                            if (message.id == pendingMessageId) {
                                message.copy(
                                    text = answer.ifBlank { strings.emptyResponse },
                                    isPending = false,
                                )
                            } else {
                                message
                            }
                        },
                    )
                } else {
                    state.copy(
                        messages = state.messages.map { message ->
                            if (message.id == pendingMessageId) {
                                message.copy(
                                    text = "${strings.couldNotAnswer} $requestErrorMessage",
                                    isPending = false,
                                )
                            } else {
                                message
                            }
                        },
                    )
                }
            }
            _requestUiState.update {
                ChatRequestUiState(
                    isSending = false,
                    errorMessage = requestErrorMessage,
                )
            }
        }
    }

    /** Returns the next monotonically increasing message identifier. */
    private fun nextMessageId(): Long {
        nextId += 1
        return nextId
    }

    private fun errorMessage(throwable: Throwable): String = when (throwable) {
        is CookStartupException -> when (throwable.issue) {
            CookStartupIssue.MissingApiKey -> strings.missingApiKey
            CookStartupIssue.UnsupportedPlatform -> strings.unsupportedPlatform
        }
        else -> throwable.message ?: strings.agentRequestFailed
    }
}
