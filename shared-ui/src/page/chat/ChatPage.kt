package page.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import page.chat.biz.ChatViewModel
import page.chat.screen.ChatConversationScreen
import repository.agent.CookModel

/** Navigation 3 destination for the chat feature. */
/** Renders the chat route and connects it to its view model. */
@Composable
internal fun ChatPage(
    selectedModel: CookModel,
    onOpenSettings: () -> Unit,
) {
    val strings = chatStrings()
    val viewModel = koinViewModel<ChatViewModel> { parametersOf(strings, selectedModel) }
    val conversationState by viewModel.conversationUiState.collectAsState()
    val draftState by viewModel.draftUiState.collectAsState()
    val requestState by viewModel.requestUiState.collectAsState()
    val historyState by viewModel.historyUiState.collectAsState()

    LaunchedEffect(selectedModel.id) {
        viewModel.onModelChanged(selectedModel)
    }

    ChatConversationScreen(
        conversationState = conversationState,
        draftState = draftState,
        requestState = requestState,
        historyState = historyState,
        modelName = selectedModel.displayName,
        onDraftChanged = viewModel::onDraftChanged,
        onNavigateDraftHistory = viewModel::navigateDraftHistory,
        onSend = { viewModel.sendMessage(selectedModel) },
        onRequestClearHistory = viewModel::requestClearHistory,
        onDismissClearHistoryConfirmation = viewModel::dismissClearHistoryConfirmation,
        onConfirmClearHistory = viewModel::confirmClearHistory,
        onOpenSettings = onOpenSettings,
    )
}
