package page.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import page.chat.biz.ChatViewModel
import page.chat.screen.ChatConversationScreen

/** Navigation 3 destination for the chat feature. */
/** Renders the chat route and connects it to its view model. */
@Composable
internal fun ChatPage(onOpenSettings: () -> Unit) {
    val strings = chatStrings()
    val viewModel = koinViewModel<ChatViewModel> { parametersOf(strings) }
    val conversationState by viewModel.conversationUiState.collectAsState()
    val draftState by viewModel.draftUiState.collectAsState()
    val requestState by viewModel.requestUiState.collectAsState()
    val historyState by viewModel.historyUiState.collectAsState()

    ChatConversationScreen(
        conversationState = conversationState,
        draftState = draftState,
        requestState = requestState,
        historyState = historyState,
        onDraftChanged = viewModel::onDraftChanged,
        onSend = viewModel::sendMessage,
        onRequestClearHistory = viewModel::requestClearHistory,
        onDismissClearHistoryConfirmation = viewModel::dismissClearHistoryConfirmation,
        onConfirmClearHistory = viewModel::confirmClearHistory,
        onOpenSettings = onOpenSettings,
    )
}
