package page.chat.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.ImeAction
import cook.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import theme.CookDimensions
import theme.CookOpacity
import theme.CookShapes
import widget.MediumIconButton
import page.chat.biz.ChatMessage
import page.chat.biz.ChatConversationUiState
import page.chat.biz.ChatDraftUiState
import page.chat.biz.ChatRequestUiState
import page.chat.biz.ChatHistoryUiState
import page.chat.biz.MessageAuthor
import page.chat.markdown.AgentMarkdownText

/** Renders the chat header, message list, and composer. */
@Composable
internal fun ChatConversationScreen(
    conversationState: ChatConversationUiState,
    draftState: ChatDraftUiState,
    requestState: ChatRequestUiState,
    historyState: ChatHistoryUiState,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRequestClearHistory: () -> Unit,
    onDismissClearHistoryConfirmation: () -> Unit,
    onConfirmClearHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
    ) {
        ChatHeader(
            canClearHistory = historyState.isLoaded &&
                !historyState.isClearing &&
                !requestState.isSending &&
                conversationState.messages.any { it.author == MessageAuthor.User },
            onClearHistory = onRequestClearHistory,
            onOpenSettings = onOpenSettings,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (historyState.isLoaded) {
            MessageList(
                messages = conversationState.messages,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MessageComposer(
            draft = draftState.draft,
            isSending = requestState.isSending || !historyState.isLoaded || historyState.isClearing,
            requestErrorMessage = requestState.errorMessage,
            historyErrorMessage = historyState.errorMessage,
            onDraftChanged = onDraftChanged,
            onSend = onSend,
        )
        if (historyState.isClearConfirmationVisible) {
            ClearHistoryConfirmation(
                onDismiss = onDismissClearHistoryConfirmation,
                onConfirm = onConfirmClearHistory,
            )
        }
    }
}

/** Renders the chat title and settings action. */
@Composable
private fun ChatHeader(
    canClearHistory: Boolean,
    onClearHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = CookDimensions.contentHorizontalPadding,
            vertical = CookDimensions.contentVerticalPadding,
        ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(CookDimensions.headerTextSpacing),
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.model_name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(CookDimensions.buttonSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediumIconButton(
                onClick = onClearHistory,
                enabled = canClearHistory,
                painter = painterResource(Res.drawable.ic_clear),
                contentDescription = stringResource(Res.string.clear_history),
            )
            MediumIconButton(
                onClick = onOpenSettings,
                painter = painterResource(Res.drawable.ic_settings),
                contentDescription = stringResource(Res.string.settings),
            )
        }
    }
}

/** Renders messages and scrolls when the latest message changes. */
@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scrollKey = latestMessageScrollKey(messages)

    LaunchedEffect(scrollKey) {
        if (scrollKey != null) {
            listState.scrollToItem(messages.size)
        }
    }

    LazyColumn(
        modifier = modifier.padding(
            horizontal = CookDimensions.contentHorizontalPadding,
            vertical = CookDimensions.contentVerticalPadding,
        ),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(CookDimensions.messageSpacing),
    ) {
        items(
            count = messages.size,
            key = { index -> messages[index].id },
        ) { index ->
            MessageBubble(message = messages[index])
        }
        item(key = "message-list-end") {
            Spacer(modifier = Modifier.height(CookDimensions.listEndAnchorHeight))
        }
    }
}

/** Returns a key that changes when the latest message changes. */
internal fun latestMessageScrollKey(messages: List<ChatMessage>): Pair<Long, String>? =
    messages.lastOrNull()?.let { message -> message.id to message.text }

/** Renders one chat message with author-specific styling. */
@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.author == MessageAuthor.User
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(CookDimensions.messageLabelSpacing),
        ) {
            Text(
                text = stringResource(
                    if (isUser) Res.string.user_label else Res.string.app_name,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier.clip(CookShapes.messageBubble).background(bubbleColor).padding(
                    horizontal = CookDimensions.messageBubbleHorizontalPadding,
                    vertical = CookDimensions.messageBubbleVerticalPadding,
                ),
            ) {
                val contentColor = if (message.isPending) {
                    textColor.copy(alpha = CookOpacity.pendingMessage)
                } else {
                    textColor
                }
                if (isUser) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor,
                    )
                } else {
                    AgentMarkdownText(
                        markdown = message.text,
                        color = contentColor,
                    )
                }
            }
        }
    }
}

/** Renders the input field and send action. */
@Composable
private fun MessageComposer(
    draft: String,
    isSending: Boolean,
    requestErrorMessage: String,
    historyErrorMessage: String,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    val canSend = draft.isNotBlank() && !isSending

    Column(
        modifier = Modifier.fillMaxWidth().padding(CookDimensions.composerPadding),
        verticalArrangement = Arrangement.spacedBy(CookDimensions.composerSpacing),
    ) {
        if (requestErrorMessage.isNotEmpty()) {
            Text(
                text = requestErrorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (historyErrorMessage.isNotEmpty()) {
            Text(
                text = historyErrorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CookDimensions.composerRowSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChanged,
                modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyUp && canSend) {
                        onSend()
                        true
                    } else {
                        false
                    }
                },
                enabled = !isSending,
                placeholder = { Text(stringResource(Res.string.message_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            )
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.height(CookDimensions.sendButtonHeight),
            ) {
                Text(
                    stringResource(if (isSending) Res.string.sending else Res.string.send),
                )
            }
        }
    }
}

/** Confirms permanent deletion of the persisted conversation. */
@Composable
private fun ClearHistoryConfirmation(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.clear_history_title)) },
        text = { Text(stringResource(Res.string.clear_history_message)) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.clear))
            }
        },
    )
}
