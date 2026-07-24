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
    val agentStatus: AgentStatus? = null,
    val isAgentStatusVisible: Boolean = false,
)

/** A transient, event-driven phase for the active agent message. */
sealed interface AgentStatus {
    data object Thinking : AgentStatus
    data object Preparing : AgentStatus
    data class UsingTool(val name: String) : AgentStatus
    data object Responding : AgentStatus
    data object Done : AgentStatus
    data object Failed : AgentStatus
}

internal const val AgentStatusFadeDurationMillis = 1_000

data class ChatTerminalStatusTiming(
    val holdMillis: Long = 2_000,
)

/** State rendered by the chat header and conversation list. */
data class ChatConversationUiState(
    val messages: List<ChatMessage> = emptyList(),
)

/** State rendered by the message composer input. */
data class ChatDraftUiState(
    val draft: String = "",
)

/** Direction for browsing user messages sent from the chat composer. */
enum class ChatDraftHistoryDirection {
    Previous,
    Next,
}

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
