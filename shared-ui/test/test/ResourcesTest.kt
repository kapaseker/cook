package test

import cook.generated.resources.Res
import cook.generated.resources.agent_request_failed
import cook.generated.resources.app_name
import cook.generated.resources.could_not_answer
import cook.generated.resources.create_chat
import cook.generated.resources.default_ui_scale
import cook.generated.resources.done
import cook.generated.resources.delete
import cook.generated.resources.delete_chat
import cook.generated.resources.delete_chat_message
import cook.generated.resources.delete_chat_title
import cook.generated.resources.empty_response
import cook.generated.resources.failed
import cook.generated.resources.history_clear_failed
import cook.generated.resources.history_load_failed
import cook.generated.resources.history_save_failed
import cook.generated.resources.hide_chat_list
import cook.generated.resources.missing_api_key
import cook.generated.resources.model
import cook.generated.resources.model_load_failed
import cook.generated.resources.model_save_failed
import cook.generated.resources.new_chat
import cook.generated.resources.no_messages_yet
import cook.generated.resources.powered_by_model
import cook.generated.resources.preparing
import cook.generated.resources.preview_button
import cook.generated.resources.responding
import cook.generated.resources.reset_ui_scale
import cook.generated.resources.send
import cook.generated.resources.sending
import cook.generated.resources.settings
import cook.generated.resources.show_chat_list
import cook.generated.resources.shortcut_hints
import cook.generated.resources.text_scale
import cook.generated.resources.thinking
import cook.generated.resources.ui_scale
import cook.generated.resources.ui_scale_load_failed
import cook.generated.resources.ui_scale_save_failed
import cook.generated.resources.using_tool
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
        assertEquals("Couldn't delete chat.", getString(Res.string.history_clear_failed))
        assertEquals("Sending", getString(Res.string.sending))
        assertEquals("Send", getString(Res.string.send))
        assertEquals("Settings", getString(Res.string.settings))
        assertEquals("New chat", getString(Res.string.new_chat))
        assertEquals("Create chat", getString(Res.string.create_chat))
        assertEquals("Show chat list", getString(Res.string.show_chat_list))
        assertEquals("Hide chat list", getString(Res.string.hide_chat_list))
        assertEquals("No messages yet", getString(Res.string.no_messages_yet))
        assertEquals("Delete chat", getString(Res.string.delete_chat))
        assertEquals("Delete \"Example chat\"?", getString(Res.string.delete_chat_title, "Example chat"))
        assertEquals(
            "This permanently deletes this chat and all its messages.",
            getString(Res.string.delete_chat_message),
        )
        assertEquals("Delete", getString(Res.string.delete))
        assertEquals("Model", getString(Res.string.model))
        assertEquals(
            "Couldn't load model selection. Using GLM.",
            getString(Res.string.model_load_failed),
        )
        assertEquals("Couldn't save model selection.", getString(Res.string.model_save_failed))
        assertEquals("Text scale", getString(Res.string.text_scale))
        assertEquals("UI scale", getString(Res.string.ui_scale))
        assertEquals("Default (1.0x)", getString(Res.string.default_ui_scale, "1.0x"))
        assertEquals("Reset UI scale", getString(Res.string.reset_ui_scale))
        assertEquals("Button preview", getString(Res.string.preview_button))
        assertEquals(
            "Couldn't load UI scale. Using 1.0x.",
            getString(Res.string.ui_scale_load_failed),
        )
        assertEquals("Couldn't save UI scale.", getString(Res.string.ui_scale_save_failed))
        assertEquals("Thinking...", getString(Res.string.thinking))
        assertEquals("Preparing...", getString(Res.string.preparing))
        assertEquals("Using lookup_english_word...", getString(Res.string.using_tool, "lookup_english_word"))
        assertEquals("Responding...", getString(Res.string.responding))
        assertEquals("Done", getString(Res.string.done))
        assertEquals("Failed", getString(Res.string.failed))
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
