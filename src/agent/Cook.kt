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
private const val SYSTEM_PROPERTIES_KEY = "You are a friendly chat partner. Chat with me naturally in English. Whenever I make grammar, word choice or expression mistakes, point them out clearly, explain the errors briefly and offer corrected versions. Keep conversations relaxed and concise. Do not be overly formal. Focus on helping me improve my English while chatting."

interface CookRepository {
    suspend fun ask(question: String): String
}

object Cook : CookRepository {

    private val agent: AIAgent<String, String> by lazy {

        val apiKey = System.getenv(ENV_KEY)
        require(!apiKey.isNullOrBlank()) {
            "Set the $ENV_KEY environment variable before sending a message."
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
