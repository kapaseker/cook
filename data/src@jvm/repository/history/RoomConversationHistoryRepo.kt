package repository.history

import repository.history.ConversationHistory
import repository.history.ConversationHistoryRepo
import repository.history.ConversationHistoryTurn

internal class RoomConversationHistoryRepo(
    private val dao: ConversationHistoryDao,
) : ConversationHistoryRepo {
    override suspend fun listConversations(): List<ConversationSummary> =
        dao.conversationSummaries().map { it.toSummary() }

    override suspend fun createConversation(createdAtEpochMillis: Long): ConversationSummary {
        val id = dao.insertConversation(
            ConversationEntity(
                title = null,
                created_at = createdAtEpochMillis,
                updated_at = createdAtEpochMillis,
            ),
        )
        return requireNotNull(summary(id))
    }

    override suspend fun loadConversation(conversationId: Long): ConversationHistory? {
        val conversation = dao.conversation(conversationId) ?: return null
        return conversation.toHistory()
    }

    override suspend fun setInitialTitle(
        conversationId: Long,
        firstUserMessage: String,
        updatedAtEpochMillis: Long,
    ): ConversationSummary {
        dao.setInitialTitle(
            conversationId = conversationId,
            title = conversationTitle(firstUserMessage),
            updatedAt = updatedAtEpochMillis,
        )
        return requireNotNull(summary(conversationId))
    }

    override suspend fun loadLatestConversation(): ConversationHistory? {
        val conversation = dao.latestConversation() ?: return null
        return conversation.toHistory()
    }

    private suspend fun ConversationEntity.toHistory(): ConversationHistory {
        val messages = dao.messagesForConversation(id)
        val turns = messages.groupBy(ConversationMessageEntity::turn_sequence).mapNotNull { (_, turnMessages) ->
            val user = turnMessages.singleOrNull { it.role == UserRole } ?: return@mapNotNull null
            val assistant = turnMessages.singleOrNull { it.role == AssistantRole } ?: return@mapNotNull null
            ConversationHistoryTurn(
                sequence = user.turn_sequence,
                userContent = user.content,
                assistantContent = assistant.content,
                modelId = assistant.model_id.orEmpty(),
                completedAtEpochMillis = assistant.created_at,
            )
        }
        return ConversationHistory(id = id, turns = turns)
    }

    override suspend fun saveSuccessfulTurns(
        conversationId: Long?,
        turns: List<ConversationHistoryTurn>,
    ): Long {
        require(turns.all { it.userContent.isNotBlank() && it.assistantContent.isNotBlank() }) {
            "Only complete conversation turns can be saved."
        }
        return dao.saveTurns(
            conversationId = conversationId,
            title = conversationTitle(turns.first().userContent),
            createdAt = turns.first().completedAtEpochMillis,
            turns = turns,
        )
    }

    override suspend fun deleteConversation(conversationId: Long) {
        dao.deleteConversation(conversationId)
    }

    private suspend fun summary(conversationId: Long): ConversationSummary? =
        dao.conversationSummary(conversationId)?.toSummary()

    private fun ConversationSummaryRow.toSummary() = ConversationSummary(
        id = id,
        title = title,
        preview = preview,
        createdAtEpochMillis = createdAt,
        updatedAtEpochMillis = updatedAt,
    )

    private companion object {
        const val UserRole = "USER"
        const val AssistantRole = "ASSISTANT"
    }
}
