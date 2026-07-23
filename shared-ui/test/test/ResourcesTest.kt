package resources

import cook.generated.resources.Res
import cook.generated.resources.agent_request_failed
import cook.generated.resources.app_name
import cook.generated.resources.could_not_answer
import cook.generated.resources.empty_response
import cook.generated.resources.history_clear_failed
import cook.generated.resources.history_load_failed
import cook.generated.resources.history_save_failed
import cook.generated.resources.missing_api_key
import cook.generated.resources.model
import cook.generated.resources.model_save_failed
import cook.generated.resources.powered_by_model
import cook.generated.resources.send
import cook.generated.resources.sending
import cook.generated.resources.settings
import cook.generated.resources.shortcut_hints
import cook.generated.resources.text_scale
import cook.generated.resources.thinking
import cook.generated.resources.unsupported_platform
import cook.generated.resources.user_label
import cook.generated.resources.welcome_message
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getStringArray
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourcesTest {

    /** Verifies that default english strings preserve the existing copy. */
    @Test
    fun `default English strings preserve the existing copy`() = runBlocking {
        assertEquals("Cook", getString(Res.string.app_name))
        assertEquals("Powered by GLM-4.7 Flash", getString(Res.string.powered_by_model, "GLM-4.7 Flash"))
        assertEquals("You", getString(Res.string.user_label))
        assertEquals(
            listOf(
                "Press Enter to send",
                "Press Shift+Enter for a new line",
                "Press ↑ at the first line or ↓ at the last line to browse sent messages",
            ),
            getStringArray(Res.array.shortcut_hints),
        )
        assertEquals("Couldn't load conversation history.", getString(Res.string.history_load_failed))
        assertEquals("Couldn't save conversation history.", getString(Res.string.history_save_failed))
        assertEquals("Couldn't clear conversation history.", getString(Res.string.history_clear_failed))
        assertEquals("Sending", getString(Res.string.sending))
        assertEquals("Send", getString(Res.string.send))
        assertEquals("Settings", getString(Res.string.settings))
        assertEquals("Model", getString(Res.string.model))
        assertEquals("Couldn't save model selection.", getString(Res.string.model_save_failed))
        assertEquals("Text scale", getString(Res.string.text_scale))
        assertEquals("Thinking...", getString(Res.string.thinking))
        assertEquals(
            "The agent returned an empty response.",
            getString(Res.string.empty_response),
        )
        assertEquals("The agent request failed.", getString(Res.string.agent_request_failed))
        assertEquals("I could not answer that request.", getString(Res.string.could_not_answer))
        assertEquals(
            "Set the {environment_variable} environment variable before starting Cook.",
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
