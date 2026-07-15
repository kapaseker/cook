package chat

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

@Composable
fun ChatScreen(
    state: ChatUiState,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
    ) {
        ChatHeader(
            modelDisplayName = state.modelDisplayName,
            onOpenSettings = onOpenSettings,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MessageList(
            messages = state.messages,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MessageComposer(
            draft = state.draft,
            isSending = state.isSending,
            error = state.error,
            onDraftChanged = onDraftChanged,
            onSend = onSend,
        )
    }
}

@Composable
private fun ChatHeader(
    modelDisplayName: String,
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
                text = modelDisplayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MediumIconButton(
            onClick = onOpenSettings,
            painter = painterResource(Res.drawable.ic_settings),
            contentDescription = stringResource(Res.string.settings),
        )
    }
}

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

internal fun latestMessageScrollKey(messages: List<ChatMessage>): Pair<Long, String>? =
    messages.lastOrNull()?.let { message -> message.id to message.text }

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
            modifier = Modifier.widthIn(max = CookDimensions.messageMaxWidth),
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
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.isPending) {
                        textColor.copy(alpha = CookOpacity.pendingMessage)
                    } else {
                        textColor
                    },
                )
            }
        }
    }
}

@Composable
private fun MessageComposer(
    draft: String,
    isSending: Boolean,
    error: String?,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    val canSend = draft.isNotBlank() && !isSending

    Column(
        modifier = Modifier.fillMaxWidth().padding(CookDimensions.composerPadding),
        verticalArrangement = Arrangement.spacedBy(CookDimensions.composerSpacing),
    ) {
        if (error != null) {
            Text(
                text = error,
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
