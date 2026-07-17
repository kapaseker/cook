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

/** Persists completed conversations independently from the live chat UI. */
interface ConversationHistoryRepo {
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
