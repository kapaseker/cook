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

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val modelDisplayName: String,
)
