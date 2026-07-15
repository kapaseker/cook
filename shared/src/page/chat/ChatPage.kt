package page.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import page.chat.biz.ChatViewModel
import page.chat.screen.ChatConversationScreen
import repository.agent.createCookRepository

/** Navigation 3 destination for the chat feature. */
@Composable
internal fun ChatPage(onOpenSettings: () -> Unit) {
    val strings = chatStrings()
    val viewModel = viewModel {
        ChatViewModel(
            cookRepository = createCookRepository(),
            strings = strings,
        )
    }
    val state by viewModel.uiState.collectAsState()

    ChatConversationScreen(
        state = state,
        onDraftChanged = viewModel::onDraftChanged,
        onSend = viewModel::sendMessage,
        onOpenSettings = onOpenSettings,
    )
}
