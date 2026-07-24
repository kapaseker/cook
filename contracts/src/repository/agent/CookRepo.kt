package repository.agent

import kotlinx.coroutines.flow.Flow

data class CookModel(
    val id: String,
    val displayName: String,
)

const val GlmModelId = "glm-4.7-flash"
const val OpenRouterModelId = "openrouter/free"

val GlmCookModel = CookModel(
    id = GlmModelId,
    displayName = "GLM-4.7 Flash",
)

val OpenRouterCookModel = CookModel(
    id = OpenRouterModelId,
    displayName = "OpenRouter Free",
)

val AvailableCookModels = listOf(GlmCookModel, OpenRouterCookModel)

fun findCookModelById(id: String?): CookModel? =
    AvailableCookModels.firstOrNull { model -> model.id == id }

fun cookModelById(id: String?): CookModel = findCookModelById(id) ?: GlmCookModel

interface CookRepo {
    fun startupIssue(model: CookModel): CookStartupIssue?

    /** Streams the assistant response for the supplied conversation. */
    fun sendMessage(
        model: CookModel,
        conversation: List<CookConversationMessage>,
    ): Flow<String>
}

sealed interface CookStartupIssue {
    data class MissingApiKey(val environmentVariable: String) : CookStartupIssue
    data object UnsupportedPlatform : CookStartupIssue
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
