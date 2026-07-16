package resources

import cook.generated.resources.Res
import cook.generated.resources.agent_request_failed
import cook.generated.resources.app_name
import cook.generated.resources.could_not_answer
import cook.generated.resources.empty_response
import cook.generated.resources.message_placeholder
import cook.generated.resources.missing_api_key
import cook.generated.resources.send
import cook.generated.resources.sending
import cook.generated.resources.settings
import cook.generated.resources.text_scale
import cook.generated.resources.thinking
import cook.generated.resources.unsupported_platform
import cook.generated.resources.user_label
import cook.generated.resources.welcome_message
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourcesTest {

    /** Verifies that default english strings preserve the existing copy. */
    @Test
    fun `default English strings preserve the existing copy`() = runBlocking {
        assertEquals("Cook", getString(Res.string.app_name))
        assertEquals("You", getString(Res.string.user_label))
        assertEquals("Message Cook", getString(Res.string.message_placeholder))
        assertEquals("Sending", getString(Res.string.sending))
        assertEquals("Send", getString(Res.string.send))
        assertEquals("Settings", getString(Res.string.settings))
        assertEquals("Text scale", getString(Res.string.text_scale))
        assertEquals("Thinking...", getString(Res.string.thinking))
        assertEquals(
            "The agent returned an empty response.",
            getString(Res.string.empty_response),
        )
        assertEquals("The agent request failed.", getString(Res.string.agent_request_failed))
        assertEquals("I could not answer that request.", getString(Res.string.could_not_answer))
        assertEquals(
            "Set the GLM_API_KEY environment variable before starting Cook.",
            getString(Res.string.missing_api_key),
        )
        assertEquals(
            "Cook's AI agent is currently available on Desktop only.",
            getString(Res.string.unsupported_platform),
        )
        assertEquals(
            "\nHi, I'm Cook. 👋\n\n" +
                "Talk to me in English or Chinese, and I'll help you improve your English along the way.\n",
            getString(Res.string.welcome_message),
        )
    }
}
