package page.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cook.generated.resources.Res
import cook.generated.resources.new_chat
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import page.chat.biz.ChatPersistenceFlushRegistry
import page.chat.biz.ChatViewModel
import page.chat.screen.ChatConversationScreen
import page.chat.screen.ChatListScreen
import repository.agent.CookModel

/** Navigation 3 entry content that coordinates the chat header, list, and conversation. */
@Composable
internal fun ChatPage(
    selectedModel: CookModel,
) {
    val strings = chatStrings()
    val viewModel = koinViewModel<ChatViewModel> { parametersOf(strings, selectedModel) }
    val flushRegistry = koinInject<ChatPersistenceFlushRegistry>()
    val conversationState by viewModel.conversationUiState.collectAsState()
    val draftState by viewModel.draftUiState.collectAsState()
    val requestState by viewModel.requestUiState.collectAsState()
    val historyState by viewModel.historyUiState.collectAsState()
    val chatListState by viewModel.chatListUiState.collectAsState()
    val selectedTitle = chatListState.selectedItem?.title ?: stringResource(Res.string.new_chat)
    val canChangeChat = historyState.isLoaded &&
        !historyState.isClearing &&
        !requestState.isSending &&
        !chatListState.isMutating

    LaunchedEffect(selectedModel.id) {
        viewModel.onModelChanged(selectedModel)
    }
    DisposableEffect(viewModel, flushRegistry) {
        flushRegistry.register(viewModel, viewModel::flushPersistence)
        onDispose { flushRegistry.unregister(viewModel) }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (chatListState.isVisible) {
            ChatListScreen(
                state = chatListState,
                enabled = canChangeChat,
                onCreateChat = viewModel::createNewChat,
                onSelectChat = viewModel::switchConversation,
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
            )
        }
        ChatConversationScreen(
            conversationState = conversationState,
            draftState = draftState,
            requestState = requestState,
            historyState = historyState,
            deleteChatTitle = selectedTitle,
            chatTitle = selectedTitle,
            modelName = selectedModel.displayName,
            canDeleteChat = canChangeChat && chatListState.selectedConversationId != null,
            isChatListVisible = chatListState.isVisible,
            onDraftChanged = viewModel::onDraftChanged,
            onNavigateDraftHistory = viewModel::navigateDraftHistory,
            onSend = { viewModel.sendMessage(selectedModel) },
            onToggleChatList = viewModel::toggleChatList,
            onDeleteChat = viewModel::requestClearHistory,
            onDismissDeleteChatConfirmation = viewModel::dismissClearHistoryConfirmation,
            onConfirmDeleteChat = viewModel::confirmClearHistory,
            modifier = Modifier.weight(1f),
        )
    }
}
