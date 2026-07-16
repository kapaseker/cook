package repository.agent

import kotlinx.coroutines.flow.Flow

data class CookModel(
    val id: String,
    val displayName: String,
)

interface CookRepo {
    val startupIssue: CookStartupIssue?
    val model: CookModel

    /** Streams the assistant response for the supplied conversation. */
    fun sendMessage(conversation: List<CookConversationMessage>): Flow<String>
}

enum class CookStartupIssue {
    MissingApiKey,
    UnsupportedPlatform,
}

class CookStartupException(
    val issue: CookStartupIssue,
) : IllegalStateException()

enum class CookMessageRole {
    User,
    Assistant,
}

data class CookConversationMessage(
    val role: CookMessageRole,
    val content: String,
)