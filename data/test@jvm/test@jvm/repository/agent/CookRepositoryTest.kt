package repository.agent

import agent.DictionaryClient
import ai.koog.prompt.llm.LLMProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CookRepositoryTest {
    @Test
    fun `OpenRouter model uses the native OpenRouter provider`() {
        val model = OpenRouterCookModel.toLLModel()

        assertEquals(LLMProvider.OpenRouter, model.provider)
        assertEquals("openrouter/free", model.id)
        assertEquals("OpenRouter Free", OpenRouterCookModel.displayName)
    }

    @Test
    fun `each model validates only its own API key`() {
        val repository = CookRepository(
            dictionaryClient = NoOpDictionaryClient,
            environment = { name -> if (name == "GLM_API_KEY") "glm-key" else null },
        )

        assertNull(repository.startupIssue(GlmCookModel))
        assertEquals(
            "OPENROUTER_API_KEY",
            assertIs<CookStartupIssue.MissingApiKey>(
                repository.startupIssue(OpenRouterCookModel),
            ).environmentVariable,
        )
    }
}

private data object NoOpDictionaryClient : DictionaryClient {
    override suspend fun lookup(word: String): String = error("Not used")
}
