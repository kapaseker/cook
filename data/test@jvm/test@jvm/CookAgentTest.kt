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
import repository.agent.CookResponseEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CookAgentTest {

    @Test
    fun `blank text deltas report preparing before visible response text`() = runBlocking {
        val agent = CookAgent(
            promptExecutor = StaticPromptExecutor(
                flowOf(
                    StreamFrame.TextDelta("\n"),
                    StreamFrame.TextDelta("Visible answer"),
                ),
            ),
            model = testModel(),
            systemPrompt = "Always answer in English.",
        )

        assertEquals(
            listOf(
                CookResponseEvent.Preparing,
                CookResponseEvent.TextDelta("\n"),
                CookResponseEvent.TextDelta("Visible answer"),
            ),
            agent.sendMessageStream(listOf(AgentMessage(role = "user", content = "Answer"))).toList(),
        )
    }

    @Test
    fun `invalid tool arguments do not report tool execution`() = runBlocking {
        val executor = ToolCallingPromptExecutor(toolArguments = """{"word":"two words"}""")
        val dictionary = RecordingDictionaryClient()
        val agent = CookAgent(
            promptExecutor = executor,
            model = testModel(),
            systemPrompt = "Always answer in English.",
            dictionaryClient = dictionary,
        )

        val events = agent.sendMessageStream(
            listOf(AgentMessage(role = "user", content = "Define two words")),
        ).toList()

        assertEquals(
            listOf(CookResponseEvent.TextDelta("Ephemeral means lasting for a very short time.")),
            events,
        )
        assertTrue(dictionary.lookups.isEmpty())
    }

    /** Verifies that executes one dictionary tool call and feeds the result back to the model. */
    @Test
    fun `executes one dictionary tool call and feeds the result back to the model`() = runBlocking {
        val executor = ToolCallingPromptExecutor()
        val dictionary = RecordingDictionaryClient()
        val agent = CookAgent(
            promptExecutor = executor,
            model = testModel(),
            systemPrompt = "Always answer in English.",
            dictionaryClient = dictionary,
        )

        val events = agent.sendMessageStream(
            conversation = listOf(
                AgentMessage(role = "assistant", content = "The word is ephemeral."),
                AgentMessage(role = "user", content = "What does this word mean?"),
            ),
        ).toList()

        assertEquals(
            listOf(
                CookResponseEvent.ToolStarted("lookup_english_word"),
                CookResponseEvent.ToolFinished("lookup_english_word"),
                CookResponseEvent.TextDelta("Ephemeral means lasting for a very short time."),
            ),
            events,
        )
        assertEquals(listOf("ephemeral"), dictionary.lookups)
        assertEquals(listOf("lookup_english_word"), executor.toolsByRequest.first().map { it.name })
        assertTrue(executor.toolsByRequest.last().isEmpty())
        assertTrue(executor.prompts.first().messages.toString().contains("The word is ephemeral."))
        assertTrue(executor.prompts.last().messages.toString().contains("lasting for a very short time"))
    }
}

private fun testModel() = LLModel(
    provider = LLMProvider.ZhipuAI,
    id = "test-model",
    capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
)

private class RecordingDictionaryClient : DictionaryClient {
    val lookups = mutableListOf<String>()

    /** Verifies that lookup. */
    override suspend fun lookup(word: String): String {
        lookups += word
        return """{"status":"success","definition":"lasting for a very short time"}"""
    }
}

private class ToolCallingPromptExecutor(
    private val toolArguments: String = """{"word":"ephemeral"}""",
) : PromptExecutor() {
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
                    content = toolArguments,
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

private class StaticPromptExecutor(
    private val frames: Flow<StreamFrame>,
) : PromptExecutor() {
    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Flow<StreamFrame> = frames

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant = error("Not used by this test")

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        error("Not used by this test")

    override fun close() = Unit
}
