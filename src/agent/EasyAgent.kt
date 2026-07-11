package agent

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
)

data class AgentToolCall(
    val id: String,
    val name: String,
    val arguments: String,
)

/**
 * 简易 Agent：负责在单次对话中运行「LLM 请求」流式循环。
 *
 * 当前版本不注册任何工具（ToolRegistry 为空），[maxIterations] 仍保留以便未来扩展：
 * - 无工具时不会进入第二轮循环
 * - 加入工具后可用于多轮工具调用循环
 */
class EasyAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val systemPrompt: String,
    private val maxIterations: Int = 5,
) {

    /**
     * 接收 system prompt + 单次 user question，流式返回 LLM 的最终文本回复。
     *
     * 内部循环最多 [maxIterations] 轮（本版本无工具时仅一轮生效）。
     */
    fun sendMessageStream(
        systemPrompt: String,
        question: String,
    ): Flow<String> = flow {
        val messages = listOf(
            AgentMessage(role = "system", content = systemPrompt),
            AgentMessage(role = "user", content = question),
        )

        repeat(maxIterations) {
            val prompt = buildPrompt(messages)
            val toolCallsThisTurn = mutableListOf<StreamFrame.ToolCallComplete>()

            // 流式执行 Prompt，收集文本片段与工具调用帧
            promptExecutor.executeStreaming(prompt, model, emptyToolDescriptors())
                .collect { frame ->
                    when (frame) {
                        is StreamFrame.TextDelta -> emit(frame.text)
                        is StreamFrame.ToolCallComplete -> toolCallsThisTurn.add(frame)
                        else -> Unit
                    }
                }

            // 无工具：toolCalls 始终为空，本轮就是最终答案，结束循环
            if (toolCallsThisTurn.isEmpty()) return@flow

            // 未来扩展点：把工具调用与结果回填到 messages，再次进入循环
            return@flow
        }
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
                "assistant" -> assistant(msg.content ?: "")
                "tool" -> user {
                    toolResult(
                        id = msg.toolCallId ?: "",
                        tool = msg.toolCalls.firstOrNull()?.name ?: "",
                        output = msg.content ?: "",
                    )
                }
            }
        }
    }

    private fun emptyToolDescriptors(): List<ToolDescriptor> = emptyList()
}
