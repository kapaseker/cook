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
 * Agent 内部使用的对话消息结构（与 UI 层 [ChatMessage] 解耦）。
 *
 * - role: "system" | "user" | "assistant" | "tool"
 * - toolCalls: assistant 消息携带的工具调用列表
 * - toolCallId: tool 消息关联的 tool call id
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
 * Cook Agent：负责在单次对话中运行「LLM 请求」流式循环。
 *
 * 当前注册一个词典工具，并在最多 [maxIterations] 轮内完成调用与结果回填。
 */
class CookAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val systemPrompt: String,
    private val dictionaryClient: DictionaryClient = DictionaryApiClient(),
    private val maxIterations: Int = 5,
) {

    /**
     * 接收完整对话历史，流式返回 LLM 的最终文本回复。
     *
     * 内部循环最多 [maxIterations] 轮；词典工具每次用户请求最多执行一次。
     */
    fun sendMessageStream(
        conversation: List<AgentMessage>,
    ): Flow<String> = flow {
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
                            emit(frame.text)
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
                messages += executeToolCall(calls.single())
            }
        }

        emit("I could not complete the dictionary lookup. Please try again.")
    }

    /**
     * 将 [AgentMessage] 列表构建为 Koog Prompt DSL。
     *
     * - system 消息映射为 system(...)
     * - user 消息映射为 user(...)
     * - assistant 消息还原为 assistant(...)
     * - tool 消息映射为 toolResult(...)
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

    private suspend fun executeToolCall(call: AgentToolCall): AgentMessage {
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

        return toolResultMessage(call, dictionaryClient.lookup(word))
    }

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

internal fun parseToolWord(arguments: String): String? = runCatching {
    Json.parseToJsonElement(arguments)
        .jsonObject["word"]
        ?.jsonPrimitive
        ?.contentOrNull
        ?.let(::normalizeWord)
}.getOrNull()
