package agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

private const val ENV_KEY = "OPENROUTER_API_KEY"

private const val SYSTEM_PROPERTIES_KEY =
"""
Your name is Cook, your English learning assistant.

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


interface CookRepository {
    val startupError: String?

    suspend fun ask(question: String): String
}

object Cook : CookRepository {

    override val startupError: String?
        get() = if (System.getenv(ENV_KEY).isNullOrBlank()) {
            missingApiKeyMessage()
        } else {
            null
        }

    private val agent: AIAgent<String, String> by lazy {

        val apiKey = System.getenv(ENV_KEY)

        require(!apiKey.isNullOrBlank()) {
            missingApiKeyMessage()
        }

        // Describe the OpenRouter-hosted model and the Koog capabilities this agent expects to use.
        val glm4Flash = LLModel(
            provider = LLMProvider.OpenRouter,
            id = "deepseek/deepseek-v4-flash",   // free-tier, fast
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Tools,
                LLMCapability.Completion,
            )
        )

        // Build a Koog prompt executor around the OpenRouter client selected by the API key env var.
        val client = OpenRouterLLMClient(apiKey = apiKey)
        val executor = PromptExecutor.builder()
            .addClient(client)
            .build()

        // Keep one functional agent instance so calls to ask() reuse the same model and message history.
        AIAgent.builder()
            .promptExecutor(executor)
            .llmModel(glm4Flash)
            .systemPrompt(SYSTEM_PROPERTIES_KEY)
            .toolRegistry(ToolRegistry.EMPTY)
            .functionalStrategy(
                functionalStrategy<String, String>("direct_chat") { question ->
                    llm.writeSession {
                        appendPrompt {
                            user(question)
                        }
                        requestLLMWithoutTools().textContent()
                    }
                }
            )
            .build()
    }

    override suspend fun ask(question: String): String {
        // AIAgent.run is suspend; callers decide which coroutine scope owns the request.
        return agent.run(question)
    }
}

private fun missingApiKeyMessage(): String {
    return "Set the $ENV_KEY environment variable before starting Cook."
}
