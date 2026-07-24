package repository.agent

import agent.AgentMessage
import agent.CookAgent
import agent.DictionaryClient
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.flow.Flow

private const val SYSTEM_PROMPT = """
Your name is Cook, an English learning assistant.

Your primary goal is to help the user improve their English while also answering their questions.

Follow these rules:

1. When the user writes in English:

   * First, judge whether the English is natural, grammatically correct, and appropriate.
   * If there ARE any mistakes, awkward expressions, unclear wording, or unnatural phrasing:
     - Briefly explain the issues.
     - Provide a corrected, natural version of the user's sentence.
     - Then answer the user's question.
   * If the English is already natural and correct, DO NOT add any language review, correction, or note of any kind. Just answer the user's question directly and naturally, exactly like a normal conversation. Do not output "English Review", "Issues", "Corrected Version", "Looks good", "No issues found", or any similar section.

2. When the user writes in Chinese:

   * First, translate the user's Chinese sentence into natural, native-sounding English.
   * Show how a native English speaker would express the same idea.
   * Then answer the user's question.

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

   * Resolve references from the conversation history before deciding that a request is ambiguous.
   * If the user's meaning is ambiguous, unclear, or could reasonably be interpreted in multiple ways, do not guess.
   * Clearly explain the possible interpretations.
   * Ask a follow-up question to clarify the user's intended meaning before answering.
   * If possible, provide examples of how the user could express each meaning more clearly in English.

6. Your role is both:

   * An English teacher who helps the user improve their English.
   * A knowledgeable assistant who answers the user's questions.

7. Dictionary Tool:

   * When the user wants the meaning or explanation of one English word, call lookup_english_word.
   * Detect the intent from natural language rather than relying on an exact phrase.
   * Resolve references such as "this word" from the conversation when one unique word is identifiable.
   * Query exactly one word. If no unique word can be identified, ask a brief clarifying question instead.
   * Base the explanation on the tool result. Never replace a failed lookup with an invented definition.
   * Give a concise learning-focused response with the word, phonetic spelling, part of speech, a simplified accurate English definition, and one useful example.
   * Add synonyms or antonyms only when useful. Do not show raw JSON, audio links, etymology, or exhaustive rare meanings.

CRITICAL OUTPUT RULES (these override everything else):

* The language-learning step (review / translation / correction) is CONDITIONAL, not mandatory. It MUST be skipped entirely when the user's English is already natural and correct.
* When the language-learning step is skipped, output ONLY the answer to the user's question, in plain prose, with no headings, no labels, and no meta-commentary about the user's English.
* Never invent issues that are not actually present just to justify showing a review section.
* Never output filler like "No issues found" or "Your English is correct" when there is nothing to correct.
* Every response must be entirely in English, even when the user writes in Chinese. Never output Chinese text or a Chinese-language explanation.
"""

private const val GLM_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/"

internal fun apiKeyEnvironmentVariable(model: CookModel): String = when (model.id) {
    GlmCookModel.id -> "GLM_API_KEY"
    OpenRouterCookModel.id -> "OPENROUTER_API_KEY"
    else -> error("Unsupported Cook model: ${model.id}")
}

internal fun CookModel.toLLModel(): LLModel = LLModel(
    provider = when (id) {
        GlmCookModel.id -> LLMProvider.ZhipuAI
        OpenRouterCookModel.id -> LLMProvider.OpenRouter
        else -> error("Unsupported Cook model: $id")
    },
    id = id,
    capabilities = listOf(
        LLMCapability.Completion,
        LLMCapability.Temperature,
        LLMCapability.Tools,
        LLMCapability.OpenAIEndpoint.Completions,
    ),
)

internal class CookRepository(
    private val dictionaryClient: DictionaryClient,
    private val environment: (String) -> String? = System::getenv,
) : CookRepo {
    override fun startupIssue(model: CookModel): CookStartupIssue? {
        val canonicalModel = findCookModelById(model.id)
            ?: error("Unsupported Cook model: ${model.id}")
        val environmentVariable = apiKeyEnvironmentVariable(canonicalModel)
        return if (environment(environmentVariable).isNullOrBlank()) {
            CookStartupIssue.MissingApiKey(environmentVariable)
        } else {
            null
        }
    }

    private val glmAgent by lazy {
        createAgent(
            llmModel = GlmCookModel.toLLModel(),
            promptExecutor = createGlmPromptExecutor(apiKey(GlmCookModel)),
        )
    }

    private val openRouterAgent by lazy {
        createAgent(
            llmModel = OpenRouterCookModel.toLLModel(),
            promptExecutor = MultiLLMPromptExecutor(
                mapOf(
                    LLMProvider.OpenRouter to OpenRouterLLMClient(
                        apiKey = apiKey(OpenRouterCookModel),
                    ),
                ),
            ),
        )
    }

    /** Streams the assistant response for the supplied conversation. */
    override fun sendMessage(
        model: CookModel,
        conversation: List<CookConversationMessage>,
    ): Flow<CookResponseEvent> {
        val canonicalModel = findCookModelById(model.id)
            ?: error("Unsupported Cook model: ${model.id}")
        startupIssue(canonicalModel)?.let { issue -> throw CookStartupException(issue) }
        val agent = when (canonicalModel.id) {
            GlmCookModel.id -> glmAgent
            OpenRouterCookModel.id -> openRouterAgent
            else -> error("Unsupported Cook model: ${model.id}")
        }
        return agent.sendMessageStream(
            conversation = conversation.map { message ->
                AgentMessage(
                    role = when (message.role) {
                        CookMessageRole.User -> "user"
                        CookMessageRole.Assistant -> "assistant"
                    },
                    content = message.content,
                )
            },
        )
    }

    private fun createAgent(
        llmModel: LLModel,
        promptExecutor: PromptExecutor,
    ): CookAgent {
        return CookAgent(
            promptExecutor = promptExecutor,
            model = llmModel,
            systemPrompt = SYSTEM_PROMPT,
            dictionaryClient = dictionaryClient,
        )
    }

    private fun apiKey(model: CookModel): String {
        val environmentVariable = apiKeyEnvironmentVariable(model)
        val apiKey = environment(environmentVariable)
        if (apiKey.isNullOrBlank()) {
            throw CookStartupException(
                CookStartupIssue.MissingApiKey(environmentVariable),
            )
        }
        return apiKey
    }

    private fun createGlmPromptExecutor(apiKey: String): PromptExecutor {
        val glmClient = OpenAILLMClient(
            apiKey = apiKey,
            settings = OpenAIClientSettings(
                baseUrl = GLM_BASE_URL,
                chatCompletionsPath = "chat/completions",
            ),
            httpClientFactory = KtorKoogHttpClient.Factory(),
        )
        return MultiLLMPromptExecutor(
            mapOf(LLMProvider.ZhipuAI to glmClient),
        )
    }
}
