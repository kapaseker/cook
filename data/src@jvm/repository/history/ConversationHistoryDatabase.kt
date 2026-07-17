package repository.history

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["updated_at"])],
)
internal data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String?,
    val created_at: Long,
    val updated_at: Long,
)

@Entity(
    tableName = "conversation_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["conversation_id"]),
        Index(
            value = ["conversation_id", "turn_sequence", "message_sequence"],
            unique = true,
        ),
    ],
)
internal data class ConversationMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversation_id: Long,
    val turn_sequence: Long,
    val message_sequence: Int,
    val role: String,
    val content: String,
    val created_at: Long,
    val model_id: String?,
)

@Dao
internal interface ConversationHistoryDao {
    @Query("SELECT * FROM conversations ORDER BY updated_at DESC, id DESC LIMIT 1")
    suspend fun latestConversation(): ConversationEntity?

    @Query(
        "SELECT * FROM conversation_messages WHERE conversation_id = :conversationId " +
            "ORDER BY turn_sequence ASC, message_sequence ASC",
    )
    suspend fun messagesForConversation(conversationId: Long): List<ConversationMessageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query(
        "UPDATE conversations SET updated_at = :updatedAt " +
            "WHERE id = :conversationId",
    )
    suspend fun updateConversationTimestamp(conversationId: Long, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<ConversationMessageEntity>)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: Long)

    @Transaction
    suspend fun saveTurns(
        conversationId: Long?,
        title: String,
        createdAt: Long,
        turns: List<ConversationHistoryTurn>,
    ): Long {
        require(turns.isNotEmpty()) { "At least one successful turn is required." }

        val resolvedConversationId = conversationId ?: insertConversation(
            ConversationEntity(
                title = title,
                created_at = createdAt,
                updated_at = turns.last().completedAtEpochMillis,
            ),
        )
        if (conversationId != null) {
            updateConversationTimestamp(resolvedConversationId, turns.last().completedAtEpochMillis)
        }

        insertMessages(
            turns.flatMap { turn ->
                listOf(
                    ConversationMessageEntity(
                        conversation_id = resolvedConversationId,
                        turn_sequence = turn.sequence,
                        message_sequence = 0,
                        role = UserRole,
                        content = turn.userContent,
                        created_at = turn.completedAtEpochMillis,
                        model_id = null,
                    ),
                    ConversationMessageEntity(
                        conversation_id = resolvedConversationId,
                        turn_sequence = turn.sequence,
                        message_sequence = 1,
                        role = AssistantRole,
                        content = turn.assistantContent,
                        created_at = turn.completedAtEpochMillis,
                        model_id = turn.modelId,
                    ),
                )
            },
        )
        return resolvedConversationId
    }

    private companion object {
        const val UserRole = "USER"
        const val AssistantRole = "ASSISTANT"
    }
}

@Database(
    entities = [ConversationEntity::class, ConversationMessageEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class ConversationHistoryDatabase : RoomDatabase() {
    abstract fun conversationHistoryDao(): ConversationHistoryDao
}
