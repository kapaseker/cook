package page.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cook.generated.resources.Res
import cook.generated.resources.app_name
import cook.generated.resources.create_chat
import cook.generated.resources.delete_chat
import cook.generated.resources.hide_chat_list
import cook.generated.resources.ic_clear
import cook.generated.resources.ic_left_bar
import cook.generated.resources.ic_plus
import cook.generated.resources.ic_settings
import cook.generated.resources.new_chat
import cook.generated.resources.powered_by_model
import cook.generated.resources.settings
import cook.generated.resources.show_chat_list
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import page.chat.biz.ChatViewModel
import page.chat.biz.ChatPersistenceFlushRegistry
import page.chat.screen.ChatConversationScreen
import page.chat.screen.ChatListScreen
import repository.agent.CookModel
import theme.CookDimensions
import widget.MediumIconButton

/** Navigation 3 destination that coordinates the chat header, list, and conversation. */
@Composable
internal fun ChatPage(
    selectedModel: CookModel,
    onOpenSettings: () -> Unit,
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

    Column(modifier = Modifier.fillMaxSize()) {
        ChatHeader(
            modelName = selectedModel.displayName,
            isChatListVisible = chatListState.isVisible,
            canCreateChat = canChangeChat && chatListState.selectedItem?.title != null,
            canDeleteChat = canChangeChat && chatListState.selectedConversationId != null,
            onCreateChat = viewModel::createNewChat,
            onToggleChatList = viewModel::toggleChatList,
            onDeleteChat = viewModel::requestClearHistory,
            onOpenSettings = onOpenSettings,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(modifier = Modifier.fillMaxSize()) {
            if (chatListState.isVisible) {
                ChatListScreen(
                    state = chatListState,
                    enabled = canChangeChat,
                    onSelectChat = viewModel::switchConversation,
                )
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            ChatConversationScreen(
                conversationState = conversationState,
                draftState = draftState,
                requestState = requestState,
                historyState = historyState,
                deleteChatTitle = selectedTitle,
                onDraftChanged = viewModel::onDraftChanged,
                onNavigateDraftHistory = viewModel::navigateDraftHistory,
                onSend = { viewModel.sendMessage(selectedModel) },
                onDismissDeleteChatConfirmation = viewModel::dismissClearHistoryConfirmation,
                onConfirmDeleteChat = viewModel::confirmClearHistory,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Renders chat identity and actions shared by the list and conversation panes. */
@Composable
private fun ChatHeader(
    modelName: String,
    isChatListVisible: Boolean,
    canCreateChat: Boolean,
    canDeleteChat: Boolean,
    onCreateChat: () -> Unit,
    onToggleChatList: () -> Unit,
    onDeleteChat: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = CookDimensions.contentHorizontalPadding,
            vertical = CookDimensions.contentVerticalPadding,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CookDimensions.headerTextSpacing)) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.powered_by_model, modelName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(CookDimensions.buttonSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediumIconButton(
                onClick = onCreateChat,
                enabled = canCreateChat,
                painter = painterResource(Res.drawable.ic_plus),
                contentDescription = stringResource(Res.string.create_chat),
            )
            IconToggleButton(
                checked = isChatListVisible,
                onCheckedChange = { onToggleChatList() },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isChatListVisible) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_left_bar),
                    contentDescription = stringResource(
                        if (isChatListVisible) Res.string.hide_chat_list else Res.string.show_chat_list,
                    ),
                    tint = if (isChatListVisible) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(10.dp).fillMaxSize(),
                )
            }
            MediumIconButton(
                onClick = onDeleteChat,
                enabled = canDeleteChat,
                painter = painterResource(Res.drawable.ic_clear),
                contentDescription = stringResource(Res.string.delete_chat),
            )
            MediumIconButton(
                onClick = onOpenSettings,
                painter = painterResource(Res.drawable.ic_settings),
                contentDescription = stringResource(Res.string.settings),
            )
        }
    }
}
