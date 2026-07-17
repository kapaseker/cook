package page.chat.biz

enum class MessageAuthor {
    User,
    Agent,
}

data class ChatMessage(
    val id: Long,
    val author: MessageAuthor,
    val text: String,
    val isPending: Boolean = false,
)

/** State rendered by the chat header and conversation list. */
data class ChatConversationUiState(
    val messages: List<ChatMessage> = emptyList(),
)

/** State rendered by the message composer input. */
data class ChatDraftUiState(
    val draft: String = "",
)

/** State for the lifecycle of an agent request. */
data class ChatRequestUiState(
    val isSending: Boolean = false,
    val errorMessage: String = "",
)

/** State for loading, saving, and clearing the persisted conversation. */
data class ChatHistoryUiState(
    val isLoaded: Boolean = false,
    val isClearing: Boolean = false,
    val errorMessage: String = "",
    val isClearConfirmationVisible: Boolean = false,
)
