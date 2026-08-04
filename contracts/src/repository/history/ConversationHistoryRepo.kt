package repository.history

/** A completed user-and-assistant exchange that is eligible for persistence. */
data class ConversationHistoryTurn(
    val sequence: Long,
    val userContent: String,
    val assistantContent: String,
    val modelId: String,
    val completedAtEpochMillis: Long,
)

/** A persisted conversation and its completed exchanges in chronological order. */
data class ConversationHistory(
    val id: Long,
    val turns: List<ConversationHistoryTurn>,
)

/** Lightweight metadata rendered in the chat list. */
data class ConversationSummary(
    val id: Long,
    val title: String?,
    val preview: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

/** Derives a stable title from the first three whitespace-separated words. */
fun conversationTitle(firstUserMessage: String): String = firstUserMessage
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(3)
    .joinToString(" ")

/** Persists completed conversations independently from the live chat UI. */
interface ConversationHistoryRepo {
    /** Lists conversations by most recent persisted activity. */
    suspend fun listConversations(): List<ConversationSummary>

    /** Creates and returns an empty persisted conversation. */
    suspend fun createConversation(createdAtEpochMillis: Long): ConversationSummary

    /** Loads one conversation and its completed exchanges. */
    suspend fun loadConversation(conversationId: Long): ConversationHistory?

    /** Sets the title from the first submitted user message without replacing an existing title. */
    suspend fun setInitialTitle(
        conversationId: Long,
        firstUserMessage: String,
        updatedAtEpochMillis: Long,
    ): ConversationSummary

    /** Loads the most recently updated conversation, when one exists. */
    suspend fun loadLatestConversation(): ConversationHistory?

    /** Atomically stores [turns] in one conversation and returns its database-generated id. */
    suspend fun saveSuccessfulTurns(
        conversationId: Long?,
        turns: List<ConversationHistoryTurn>,
    ): Long

    /** Deletes one conversation and all of its messages. */
    suspend fun deleteConversation(conversationId: Long)
}
