package page.chat.biz

import page.chat.ChatStrings
import repository.agent.CookConversationMessage
import repository.agent.CookModel
import repository.agent.CookRepo
import repository.agent.CookStartupIssue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatViewModelTest {

    /** Verifies that shows the welcome message when cook starts normally. */
    @Test
    fun `shows the welcome message when Cook starts normally`() {
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(),
            strings = testChatStrings,
        )

        assertEquals(
            "\nHi, I'm Cook. 👋\n\n" +
                "Talk to me in English or Chinese, and I'll help you improve your English along the way.\n",
            viewModel.uiState.value.messages.single().text,
        )
    }

    /** Verifies that shows the startup error instead of the welcome message. */
    @Test
    fun `shows the startup error instead of the welcome message`() {
        val startupError = "Set the GLM_API_KEY environment variable before starting Cook."
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(startupIssue = CookStartupIssue.MissingApiKey),
            strings = testChatStrings,
        )

        assertEquals(startupError, viewModel.uiState.value.messages.single().text)
    }

    /** Verifies that shows the unsupported platform message for non desktop implementations. */
    @Test
    fun `shows the unsupported platform message for non desktop implementations`() {
        val viewModel = ChatViewModel(
            cookRepository = FakeCookRepo(
                startupIssue = CookStartupIssue.UnsupportedPlatform,
            ),
            strings = testChatStrings,
        )

        assertEquals(
            "Cook's AI agent is currently available on Desktop only.",
            viewModel.uiState.value.messages.single().text,
        )
    }
}

private class FakeCookRepo(
    override val startupIssue: CookStartupIssue? = null,
) : CookRepo {
    override val model = CookModel(id = "test", displayName = "Test model")

    /** Verifies that send message. */
    override fun sendMessage(conversation: List<CookConversationMessage>): Flow<String> = emptyFlow()
}

private val testChatStrings = ChatStrings(
    welcomeMessage = "\nHi, I'm Cook. 👋\n\n" +
        "Talk to me in English or Chinese, and I'll help you improve your English along the way.\n",
    thinking = "Thinking...",
    emptyResponse = "The agent returned an empty response.",
    agentRequestFailed = "The agent request failed.",
    couldNotAnswer = "I could not answer that request.",
    missingApiKey = "Set the GLM_API_KEY environment variable before starting Cook.",
    unsupportedPlatform = "Cook's AI agent is currently available on Desktop only.",
)
