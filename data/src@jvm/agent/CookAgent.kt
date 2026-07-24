package agent

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import repository.agent.CookResponseEvent

private const val DICTIONARY_TOOL_NAME = "lookup_english_word"

internal val dictionaryToolDescriptor = ToolDescriptor(
    name = DICTIONARY_TOOL_NAME,
    description = """
        Look up exactly one English word in a dictionary. Use this tool whenever the user asks
        for the meaning or explanation of a word, including indirect requests such as "this word"
        when the conversation identifies one unique word. Do not call it when the target word is
        ambiguous. The final answer must be based on the tool result rather than prior knowledge.
    """.trimIndent(),
    requiredParameters = listOf(
        ToolParameterDescriptor(
            name = "word",
            description = "Exactly one English word to look up.",
            type = ToolParameterType.String,
        )
    ),
)

/**
 * Conversation message used internally by the agent, independent of the UI message model.
 *
 * - `role`: `system`, `user`, `assistant`, or `tool`.
 * - `toolCalls`: Tool calls issued by an assistant message.
 * - `toolCallId`: The tool-call identifier associated with a tool message.
 */
data class AgentMessage(
    val role: String,
    val content: String? = null,
    val toolCalls: List<AgentToolCall> = emptyList(),
    val toolCallId: String? = null,
    val toolName: String? = null,
    val isError: Boolean = false,
)

data class AgentToolCall(
    val id: String?,
    val name: String,
    val arguments: String,
)

/**
 * Runs the streaming LLM loop for a single Cook conversation.
 *
 * The agent registers a dictionary tool and completes tool calls within at most [maxIterations] iterations.
 */
class CookAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val systemPrompt: String,
    private val dictionaryClient: DictionaryClient = DictionaryApiClient(),
    private val maxIterations: Int = 5,
) {

    /**
     * Streams the final LLM text response for the complete [conversation].
     *
     * The loop runs for at most [maxIterations] iterations and allows one dictionary lookup per request.
     */
    fun sendMessageStream(
        conversation: List<AgentMessage>,
    ): Flow<CookResponseEvent> = flow {
        val messages = mutableListOf(AgentMessage(role = "system", content = systemPrompt))
        messages += conversation.filter { it.role == "user" || it.role == "assistant" }
        var dictionaryToolAvailable = true

        repeat(maxIterations) {
            val prompt = buildPrompt(messages)
            val textChunks = mutableListOf<String>()
            val toolCallsThisTurn = mutableListOf<StreamFrame.ToolCallComplete>()

            // 流式执行 Prompt，收集文本片段与工具调用帧
            val tools = if (dictionaryToolAvailable) listOf(dictionaryToolDescriptor) else emptyList()
            promptExecutor.executeStreaming(prompt, model, tools)
                .collect { frame ->
                    when (frame) {
                        is StreamFrame.TextDelta -> {
                            textChunks += frame.text
                            if (textChunks.none(String::isNotBlank)) {
                                emit(CookResponseEvent.Preparing)
                            }
                            emit(CookResponseEvent.TextDelta(frame.text))
                        }
                        is StreamFrame.ToolCallComplete -> toolCallsThisTurn.add(frame)
                        else -> Unit
                    }
                }

            if (toolCallsThisTurn.isEmpty()) {
                return@flow
            }

            val calls = toolCallsThisTurn.map { frame ->
                AgentToolCall(
                    id = frame.id,
                    name = frame.name,
                    arguments = frame.content,
                )
            }
            messages += AgentMessage(
                role = "assistant",
                content = textChunks.joinToString("").ifBlank { null },
                toolCalls = calls,
            )

            dictionaryToolAvailable = false
            if (calls.size != 1) {
                calls.forEach { call ->
                    messages += toolResultMessage(
                        call = call,
                        output = """{"status":"invalid_request","message":"Call exactly one dictionary lookup tool."}""",
                        isError = true,
                    )
                }
            } else {
                val call = calls.single()
                messages += executeToolCall(
                    call = call,
                    onStarted = { emit(CookResponseEvent.ToolStarted(call.name)) },
                    onFinished = { emit(CookResponseEvent.ToolFinished(call.name)) },
                )
            }
        }

        emit(CookResponseEvent.TextDelta("I could not complete the dictionary lookup. Please try again."))
    }

    /**
     * Builds a Koog Prompt DSL from [messages].
     *
     * System, user, assistant, and tool messages retain their corresponding prompt roles and tool-call data.
     */
    private fun buildPrompt(messages: List<AgentMessage>) = prompt("chat") {
        messages.forEach { msg ->
            when (msg.role) {
                "system" -> system(msg.content ?: "")
                "user" -> user(msg.content ?: "")
                "assistant" -> if (msg.toolCalls.isEmpty()) {
                    assistant(msg.content ?: "")
                } else {
                    assistant {
                        msg.content?.let { text(it) }
                        msg.toolCalls.forEach { call ->
                            toolCall(
                                id = call.id,
                                tool = call.name,
                                args = call.arguments,
                            )
                        }
                    }
                }
                "tool" -> user {
                    toolResult(
                        id = msg.toolCallId,
                        tool = msg.toolName ?: "",
                        output = msg.content ?: "",
                        isError = msg.isError,
                    )
                }
            }
        }
    }

    /** Executes a dictionary tool call and returns its tool-result message. */
    private suspend fun executeToolCall(
        call: AgentToolCall,
        onStarted: suspend () -> Unit,
        onFinished: suspend () -> Unit,
    ): AgentMessage {
        if (call.name != DICTIONARY_TOOL_NAME) {
            return toolResultMessage(
                call = call,
                output = """{"status":"invalid_request","message":"Unknown tool."}""",
                isError = true,
            )
        }

        val word = parseToolWord(call.arguments)
        if (word == null) {
            return toolResultMessage(
                call = call,
                output = """{"status":"invalid_word","message":"Provide exactly one English word."}""",
                isError = true,
            )
        }

        onStarted()
        return try {
            toolResultMessage(call, dictionaryClient.lookup(word))
        } finally {
            onFinished()
        }
    }

    /** Creates a tool-role message from a tool call result. */
    private fun toolResultMessage(
        call: AgentToolCall,
        output: String,
        isError: Boolean = false,
    ) = AgentMessage(
        role = "tool",
        content = output,
        toolCallId = call.id,
        toolName = call.name,
        isError = isError,
    )
}

/** Extracts and validates one dictionary word from JSON tool arguments. */
internal fun parseToolWord(arguments: String): String? = runCatching {
    Json.parseToJsonElement(arguments)
        .jsonObject["word"]
        ?.jsonPrimitive
        ?.contentOrNull
        ?.let(::normalizeWord)
}.getOrNull()
