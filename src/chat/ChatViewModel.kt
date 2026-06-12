package chat

import agent.Cook
import agent.CookRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _uiState = MutableStateFlow(
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    id = nextMessageId(),
                    author = MessageAuthor.Agent,
                    text = cookRepository.startupError ?: HELLO,
                )
            )
        )
    )

    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onDraftChanged(value: String) {
        _uiState.update { state ->
            state.copy(draft = value, error = null)
        }
    }

    fun sendMessage() {
        val question = _uiState.value.draft.trim()
        if (question.isEmpty() || _uiState.value.isSending) {
            return
        }

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
            val result = runCatching { cookRepository.ask(question) }
            _uiState.update { state ->
                val messagesWithoutPending = state.messages.filterNot { message ->
                    message.id == pendingMessageId
                }

                result.fold(
                    onSuccess = { answer ->
                        state.copy(
                            messages = messagesWithoutPending + ChatMessage(
                                id = nextMessageId(),
                                author = MessageAuthor.Agent,
                                text = answer.ifBlank { "The agent returned an empty response." },
                            ),
                            isSending = false,
                            error = null,
                        )
                    },
                    onFailure = { throwable ->
                        val errorText = throwable.message ?: "The agent request failed."
                        state.copy(
                            messages = messagesWithoutPending + ChatMessage(
                                id = nextMessageId(),
                                author = MessageAuthor.Agent,
                                text = "I could not answer that request. $errorText",
                            ),
                            isSending = false,
                            error = errorText,
                        )
                    },
                )
            }
        }
    }

    private fun nextMessageId(): Long {
        nextId += 1
        return nextId
    }
}
