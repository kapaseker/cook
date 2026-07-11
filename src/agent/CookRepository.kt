package agent

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.flow.Flow

private const val ENV_KEY = "GLM_API_KEY"

private const val SYSTEM_PROMPT = """
Your name is Cook, an English learning assistant.

Your primary goal is to help the user improve their English while also answering their questions.

Follow these rules:

1. When the user asks a question in English:

   * First, check whether the English is natural, grammatically correct, and appropriate.
   * If there are any mistakes, awkward expressions, unclear wording, or unnatural phrasing, clearly explain them.
   * Provide a corrected version of the user's sentence.
   * Then answer the user's question based on the corrected version.
   * If the English is already natural and correct, briefly acknowledge that and then answer the question.

Output format:

English Review:

* Issues: (or "No issues found")
* Corrected Version: ...

Answer:
...

2. When the user asks a question in Chinese:

   * First, translate the user's Chinese sentence into natural, native-sounding English.
   * Show how a native English speaker would express the same idea.
   * Then answer the user's question.

Output format:

English Expression:
...

Answer:
...

3. When correcting English:

   * Prefer natural, everyday English rather than overly literal translations.
   * Explain important grammar, vocabulary, and word-choice issues briefly and clearly.
   * Encourage fluent and idiomatic expressions.
   * Be strict about grammar, word choice, and natural expression.
   * Correct even minor mistakes when appropriate.

4. When translating Chinese:

   * Prioritize natural English over word-for-word translation.
   * If there are multiple common ways to express something, provide the most natural one.
   * Prefer expressions commonly used by native speakers.

5. Ambiguity Handling:

   * If the user's meaning is ambiguous, unclear, or could reasonably be interpreted in multiple ways, do not guess.
   * Clearly explain the possible interpretations.
   * Ask a follow-up question to clarify the user's intended meaning before answering.
   * If possible, provide examples of how the user could express each meaning more clearly in English.

6. Always answer the user's original question after providing corrections or translations whenever the meaning is sufficiently clear.

   * If clarification is required, ask for clarification first instead of making assumptions.

7. Your role is both:

   * An English teacher who helps the user improve their English.
   * A knowledgeable assistant who answers the user's questions.

Never skip the language-learning step before answering.
"""

data class CookModel(
    val id: String,
    val displayName: String,
)

interface CookRepository {
    val startupError: String?
    val model: CookModel

    fun sendMessage(question: String): Flow<String>
}

object Cook : CookRepository {

    override val model: CookModel = CookModel(
        id = "glm-4.7-flash",
        displayName = "GLM-4.7 Flash",
    )

    override val startupError: String?
        get() = if (System.getenv(ENV_KEY).isNullOrBlank()) {
            missingApiKeyMessage()
        } else {
            null
        }

    private val promptExecutor: PromptExecutor by lazy {
        val apiKey = System.getenv(ENV_KEY)

        require(!apiKey.isNullOrBlank()) {
            missingApiKeyMessage()
        }

        MultiLLMPromptExecutor(
            mapOf(
                LLMProvider.ZhipuAI to OpenAILLMClient(
                    apiKey = apiKey,
                    settings = OpenAIClientSettings(
                        baseUrl = "https://open.bigmodel.cn/api/paas/v4/",
                        chatCompletionsPath = "chat/completions",
                    ),
                    httpClientFactory = KtorKoogHttpClient.Factory(),
                ),
            )
        )
    }

    private val glmModel: LLModel = LLModel(
        provider = LLMProvider.ZhipuAI,
        id = "glm-4.7-flash",
        capabilities = listOf(
            LLMCapability.Completion,
            LLMCapability.Temperature,
            LLMCapability.OpenAIEndpoint.Completions,
        ),
    )

    private val easyAgent: EasyAgent by lazy {
        EasyAgent(
            promptExecutor = promptExecutor,
            model = glmModel,
            systemPrompt = SYSTEM_PROMPT,
        )
    }

    override fun sendMessage(question: String): Flow<String> {
        return easyAgent.sendMessageStream(
            systemPrompt = SYSTEM_PROMPT,
            question = question,
        )
    }
}

private fun missingApiKeyMessage(): String {
    return "Set the $ENV_KEY environment variable before starting Cook."
}
