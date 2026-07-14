package agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class CookModel(
    val id: String,
    val displayName: String,
)

interface CookRepository {
    val startupIssue: CookStartupIssue?
    val model: CookModel

    fun sendMessage(conversation: List<CookConversationMessage>): Flow<String>
}

enum class CookStartupIssue {
    MissingApiKey,
    UnsupportedPlatform,
}

internal class CookStartupException(
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

internal expect fun createCookRepository(): CookRepository

internal object UnsupportedCookRepository : CookRepository {
    override val startupIssue = CookStartupIssue.UnsupportedPlatform
    override val model = CookModel(
        id = "unsupported",
        displayName = "Desktop only",
    )

    override fun sendMessage(
        conversation: List<CookConversationMessage>,
    ): Flow<String> = flow {
        throw CookStartupException(CookStartupIssue.UnsupportedPlatform)
    }
}
