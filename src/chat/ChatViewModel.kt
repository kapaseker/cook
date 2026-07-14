package chat

import agent.Cook
import agent.CookConversationMessage
import agent.CookMessageRole
import agent.CookRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val HELLO =
"""
Hi, I'm Cook. 👋

Talk to me in English or Chinese, and I'll help you improve your English along the way.
"""

class ChatViewModel(
    private val cookRepository: CookRepository = Cook,
) : ViewModel() {

    private var nextId = 0L

    private val initialMessage = cookRepository.startupError ?: HELLO

    private val _uiState = MutableStateFlow(
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    id = nextMessageId(),
                    author = MessageAuthor.Agent,
                    text = initialMessage,
                )
            ),
            modelDisplayName = cookRepository.model.displayName,
        )
    )

    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onDraftChanged(value: String) {
        _uiState.update { state ->
            state.copy(draft = value, error = null)
        }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        val question = currentState.draft.trim()
        if (question.isEmpty() || currentState.isSending) {
            return
        }

        val conversation = currentState.messages.mapNotNull { message ->
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
        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    id = nextMessageId(),
                    author = MessageAuthor.User,
                    text = question,
                ) + ChatMessage(
                    id = pendingMessageId,
                    author = MessageAuthor.Agent,
                    text = "Thinking...",
                    isPending = true,
                ),
                draft = "",
                isSending = true,
                error = null,
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching {
                // 流式 collect：每个 chunk 累加到 pending 消息的 text
                val collected = StringBuilder()
                cookRepository.sendMessage(conversation).collect { chunk ->
                    collected.append(chunk)
                    _uiState.update { state ->
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

            _uiState.update { state ->
                when {
                    result.isSuccess -> {
                        val answer = result.getOrThrow()
                        state.copy(
                            messages = state.messages.map { message ->
                                if (message.id == pendingMessageId) {
                                    message.copy(
                                        text = answer.ifBlank { "The agent returned an empty response." },
                                        isPending = false,
                                    )
                                } else {
                                    message
                                }
                            },
                            isSending = false,
                            error = null,
                        )
                    }

                    else -> {
                        val throwable = result.exceptionOrNull()
                        val errorText = throwable?.message ?: "The agent request failed."
                        state.copy(
                            messages = state.messages.map { message ->
                                if (message.id == pendingMessageId) {
                                    message.copy(
                                        text = "I could not answer that request. $errorText",
                                        isPending = false,
                                    )
                                } else {
                                    message
                                }
                            },
                            isSending = false,
                            error = errorText,
                        )
                    }
                }
            }
        }
    }

    private fun nextMessageId(): Long {
        nextId += 1
        return nextId
    }
}
