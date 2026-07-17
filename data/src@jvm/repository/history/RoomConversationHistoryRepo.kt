package repository.history

import repository.history.ConversationHistory
import repository.history.ConversationHistoryRepo
import repository.history.ConversationHistoryTurn

internal class RoomConversationHistoryRepo(
    private val dao: ConversationHistoryDao,
) : ConversationHistoryRepo {
    override suspend fun loadLatestConversation(): ConversationHistory? {
        val conversation = dao.latestConversation() ?: return null
        val messages = dao.messagesForConversation(conversation.id)
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
        return ConversationHistory(id = conversation.id, turns = turns)
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
            title = historyTitle(turns.first().userContent),
            createdAt = turns.first().completedAtEpochMillis,
            turns = turns,
        )
    }

    override suspend fun deleteConversation(conversationId: Long) {
        dao.deleteConversation(conversationId)
    }

    private fun historyTitle(question: String): String = question
        .lineSequence()
        .joinToString(separator = " ") { it.trim() }
        .trim()
        .take(80)

    private companion object {
        const val UserRole = "USER"
        const val AssistantRole = "ASSISTANT"
    }
}
