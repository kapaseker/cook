package agent

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CookAgentTest {

    /** Verifies that executes one dictionary tool call and feeds the result back to the model. */
    @Test
    fun `executes one dictionary tool call and feeds the result back to the model`() = runBlocking {
        val executor = ToolCallingPromptExecutor()
        val dictionary = RecordingDictionaryClient()
        val agent = CookAgent(
            promptExecutor = executor,
            model = LLModel(
                provider = LLMProvider.ZhipuAI,
                id = "test-model",
                capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
            ),
            systemPrompt = "Always answer in English.",
            dictionaryClient = dictionary,
        )

        val output = agent.sendMessageStream(
            conversation = listOf(
                AgentMessage(role = "assistant", content = "The word is ephemeral."),
                AgentMessage(role = "user", content = "What does this word mean?"),
            ),
        ).toList().joinToString("")

        assertEquals("Ephemeral means lasting for a very short time.", output)
        assertEquals(listOf("ephemeral"), dictionary.lookups)
        assertEquals(listOf("lookup_english_word"), executor.toolsByRequest.first().map { it.name })
        assertTrue(executor.toolsByRequest.last().isEmpty())
        assertTrue(executor.prompts.first().messages.toString().contains("The word is ephemeral."))
        assertTrue(executor.prompts.last().messages.toString().contains("lasting for a very short time"))
    }
}

private class RecordingDictionaryClient : DictionaryClient {
    val lookups = mutableListOf<String>()

    /** Verifies that lookup. */
    override suspend fun lookup(word: String): String {
        lookups += word
        return """{"status":"success","definition":"lasting for a very short time"}"""
    }
}

private class ToolCallingPromptExecutor : PromptExecutor() {
    val prompts = mutableListOf<Prompt>()
    val toolsByRequest = mutableListOf<List<ToolDescriptor>>()

    /** Verifies that execute streaming. */
    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Flow<StreamFrame> {
        prompts += prompt
        toolsByRequest += tools
        return if (prompts.size == 1) {
            flowOf(
                StreamFrame.ToolCallComplete(
                    id = "call-1",
                    name = "lookup_english_word",
                    content = """{"word":"ephemeral"}""",
                )
            )
        } else {
            flowOf(StreamFrame.TextDelta("Ephemeral means lasting for a very short time."))
        }
    }

    /** Verifies that execute. */
    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant = error("Not used by this test")

    /** Verifies that moderate. */
    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        error("Not used by this test")

    /** Verifies that close. */
    override fun close() = Unit
}
