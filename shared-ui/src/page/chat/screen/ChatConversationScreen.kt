package page.chat.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import cook.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import theme.CookDimensions
import theme.CookOpacity
import theme.CookShapes
import page.chat.biz.ChatMessage
import page.chat.biz.ChatConversationUiState
import page.chat.biz.ChatDraftUiState
import page.chat.biz.ChatRequestUiState
import page.chat.biz.ChatHistoryUiState
import page.chat.biz.ChatDraftHistoryDirection
import page.chat.biz.AgentStatus
import page.chat.biz.AgentStatusFadeDurationMillis
import page.chat.biz.MessageAuthor
import page.chat.markdown.AgentMarkdownText
import kotlin.time.Duration.Companion.milliseconds

/** Renders the selected chat's message list, composer, and delete confirmation. */
@Composable
internal fun ChatConversationScreen(
    conversationState: ChatConversationUiState,
    draftState: ChatDraftUiState,
    requestState: ChatRequestUiState,
    historyState: ChatHistoryUiState,
    deleteChatTitle: String,
    chatTitle: String,
    modelName: String,
    canDeleteChat: Boolean,
    isChatListVisible: Boolean,
    onDraftChanged: (String) -> Unit,
    onNavigateDraftHistory: (ChatDraftHistoryDirection) -> Boolean,
    onSend: () -> Unit,
    onToggleChatList: () -> Unit,
    onDeleteChat: () -> Unit,
    onDismissDeleteChatConfirmation: () -> Unit,
    onConfirmDeleteChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
    ) {
        ConversationHeader(
            chatTitle = chatTitle,
            modelName = modelName,
            canDeleteChat = canDeleteChat,
            isChatListVisible = isChatListVisible,
            onToggleChatList = onToggleChatList,
            onDeleteChat = onDeleteChat,
        )
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
        MessageComposer(
            draft = draftState.draft,
            isSending = requestState.isSending || !historyState.isLoaded || historyState.isClearing,
            requestErrorMessage = requestState.errorMessage,
            historyErrorMessage = historyState.errorMessage,
            onDraftChanged = onDraftChanged,
            onNavigateDraftHistory = onNavigateDraftHistory,
            onSend = onSend,
        )
        if (historyState.isClearConfirmationVisible) {
            DeleteChatConfirmation(
                chatTitle = deleteChatTitle,
                onDismiss = onDismissDeleteChatConfirmation,
                onConfirm = onConfirmDeleteChat,
            )
        }
    }
}

/** Renders the conversation identity and its local actions. */
@Composable
private fun ConversationHeader(
    chatTitle: String,
    modelName: String,
    canDeleteChat: Boolean,
    isChatListVisible: Boolean,
    onToggleChatList: () -> Unit,
    onDeleteChat: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.88f))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("✦", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = chatTitle,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                )
                Text(
                    text = "  $modelName online",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleChatList) {
            Icon(
                painter = painterResource(Res.drawable.ic_left_bar),
                contentDescription = if (isChatListVisible) "Hide conversations" else "Show conversations",
            )
        }
        IconButton(onClick = onDeleteChat, enabled = canDeleteChat) {
            Icon(
                painter = painterResource(Res.drawable.ic_clear),
                contentDescription = stringResource(Res.string.delete_chat),
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
    val coroutineScope = rememberCoroutineScope()
    val scrollKey = latestMessageScrollKey(messages)
    val latestMessage = messages.lastOrNull()
    var previousLatestMessageId by remember { mutableStateOf<Long?>(null) }
    var isFollowingLatest by remember { mutableStateOf(true) }
    var isProgrammaticScrollInProgress by remember { mutableStateOf(false) }
    var isScrollbarVisible by remember { mutableStateOf(false) }
    val showScrollToBottomButton by remember(latestMessage?.id) {
        derivedStateOf {
            shouldShowScrollToBottomButton(
                latestMessageId = latestMessage?.id,
                visibleItemKeys = listState.layoutInfo.visibleItemsInfo.map { it.key },
            )
        }
    }

    LaunchedEffect(listState, latestMessage?.id) {
        snapshotFlow {
            listState.isScrollInProgress to shouldShowScrollToBottomButton(
                latestMessageId = latestMessage?.id,
                visibleItemKeys = listState.layoutInfo.visibleItemsInfo.map { it.key },
            )
        }.collect { (isScrollInProgress, isLatestMessageHidden) ->
            if (isScrollInProgress && !isProgrammaticScrollInProgress) {
                isFollowingLatest = !isLatestMessageHidden
            } else if (!isLatestMessageHidden) {
                isFollowingLatest = true
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.drop(1).collectLatest {
            if (!listState.canScrollBackward && !listState.canScrollForward) {
                isScrollbarVisible = false
                return@collectLatest
            }
            isScrollbarVisible = true
            delay(SCROLLBAR_HIDE_DELAY_MILLIS.milliseconds)
            isScrollbarVisible = false
        }
    }

    LaunchedEffect(scrollKey) {
        if (
            scrollKey != null && shouldAutoScrollToLatest(
                isFollowingLatest = isFollowingLatest,
                previousLatestMessageId = previousLatestMessageId,
                latestMessage = latestMessage,
            )
        ) {
            isFollowingLatest = true
            listState.scrollToItem(messages.size)
        }
        previousLatestMessageId = latestMessage?.id
    }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(
                horizontal = CookDimensions.contentHorizontalPadding,
                vertical = CookDimensions.contentVerticalPadding,
            ),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(CookDimensions.messageSpacing),
        ) {
            item(key = "today") {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = "Today",
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
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

        AnimatedVisibility(
            visible = isScrollbarVisible,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            enter = fadeIn(animationSpec = tween(SCROLLBAR_FADE_IN_DURATION_MILLIS)),
            exit = fadeOut(animationSpec = tween(SCROLLBAR_FADE_OUT_DURATION_MILLIS)),
        ) {
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.fillMaxHeight(),
            )
        }

        if (showScrollToBottomButton) {
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        isFollowingLatest = true
                        isProgrammaticScrollInProgress = true
                        try {
                            listState.animateScrollToItem(messages.size)
                        } finally {
                            isProgrammaticScrollInProgress = false
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(
                    horizontal = CookDimensions.contentHorizontalPadding,
                    vertical = CookDimensions.contentVerticalPadding,
                ),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_double_down),
                    contentDescription = stringResource(Res.string.scroll_to_bottom),
                )
            }
        }
    }
}

/** Returns whether the latest real message is completely outside the viewport. */
internal fun shouldShowScrollToBottomButton(
    latestMessageId: Long?,
    visibleItemKeys: List<Any>,
): Boolean = latestMessageId != null &&
    visibleItemKeys.isNotEmpty() &&
    latestMessageId !in visibleItemKeys

/** Returns whether a message update should move the viewport to the latest message. */
internal fun shouldAutoScrollToLatest(
    isFollowingLatest: Boolean,
    previousLatestMessageId: Long?,
    latestMessage: ChatMessage?,
): Boolean = isFollowingLatest || (
    latestMessage != null &&
        latestMessage.id != previousLatestMessageId &&
        latestMessage.author == MessageAuthor.User
    )

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
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.86f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(CookDimensions.messageLabelSpacing),
        ) {
            if (!isUser) {
                MessageLabel(message)
            }
            if (message.text.isNotBlank()) {
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
                    SelectionContainer {
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
    }
}

@Composable
private fun MessageLabel(message: ChatMessage) {
    val isUser = message.author == MessageAuthor.User
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("✦", style = MaterialTheme.typography.labelSmall)
        }
        Text(
            text = stringResource(if (isUser) Res.string.user_label else Res.string.app_name),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val status = message.agentStatus
        if (!isUser && status != null) {
            AnimatedVisibility(
                visible = message.isAgentStatusVisible,
                exit = fadeOut(animationSpec = tween(AgentStatusFadeDurationMillis)),
            ) {
                Text(
                    text = " · ${agentStatusText(status)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = CookOpacity.agentStatus,
                    ),
                )
            }
        }
    }
}

@Composable
private fun agentStatusText(status: AgentStatus): String = when (status) {
    AgentStatus.Thinking -> stringResource(Res.string.thinking)
    AgentStatus.Preparing -> stringResource(Res.string.preparing)
    is AgentStatus.UsingTool -> stringResource(Res.string.using_tool, status.name)
    AgentStatus.Responding -> stringResource(Res.string.responding)
    AgentStatus.Done -> stringResource(Res.string.done)
    AgentStatus.Failed -> stringResource(Res.string.failed)
}

/** Renders the input field and send action. */
@Composable
private fun MessageComposer(
    draft: String,
    isSending: Boolean,
    requestErrorMessage: String,
    historyErrorMessage: String,
    onDraftChanged: (String) -> Unit,
    onNavigateDraftHistory: (ChatDraftHistoryDirection) -> Boolean,
    onSend: () -> Unit,
) {
    val canSend = draft.isNotBlank() && !isSending
    var editorValue by remember { mutableStateOf(TextFieldValue(draft)) }
    val shortcutHints = stringArrayResource(Res.array.shortcut_hints)
    var shortcutHintIndex by remember { mutableStateOf(0) }

    LaunchedEffect(draft) {
        if (editorValue.text != draft) {
            editorValue = TextFieldValue(draft, selection = TextRange(draft.length))
        }
    }

    LaunchedEffect(shortcutHints.size) {
        while (true) {
            delay(SHORTCUT_HINT_DURATION_MILLIS.milliseconds)
            shortcutHintIndex = nextShortcutHintIndex(shortcutHintIndex, shortcutHints.size)
        }
    }

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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CookShapes.card,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(CookDimensions.composerRowSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            OutlinedTextField(
                value = editorValue,
                onValueChange = { value ->
                    editorValue = value
                    onDraftChanged(value.text)
                },
                modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                    when {
                        event.key == Key.DirectionUp &&
                            event.type == KeyEventType.KeyDown &&
                            isCursorOnFirstLine(editorValue) -> {
                            onNavigateDraftHistory(ChatDraftHistoryDirection.Previous)
                        }

                        event.key == Key.DirectionDown &&
                            event.type == KeyEventType.KeyDown &&
                            isCursorOnLastLine(editorValue) -> {
                            onNavigateDraftHistory(ChatDraftHistoryDirection.Next)
                        }

                        event.key == Key.Enter &&
                            event.type == KeyEventType.KeyDown &&
                            event.isShiftPressed -> {
                            editorValue = insertLineBreak(editorValue)
                            onDraftChanged(editorValue.text)
                            true
                        }

                        event.key == Key.Enter &&
                            event.type == KeyEventType.KeyDown &&
                            !event.isShiftPressed &&
                            canSend -> {
                            onSend()
                            true
                        }

                        else -> false
                    }
                },
                enabled = !isSending,
                placeholder = { Text(shortcutHints[shortcutHintIndex]) },
                singleLine = false,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            )
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.size(CookDimensions.sendButtonHeight),
                shape = CircleShape,
            ) {
                Text(if (isSending) "…" else "➤")
            }
            }
        }
        Text(
            text = "Cook can make mistakes. Verify important information.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outlineVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

private const val SHORTCUT_HINT_DURATION_MILLIS = 4_000L
private const val SCROLLBAR_HIDE_DELAY_MILLIS = 2_000L
private const val SCROLLBAR_FADE_IN_DURATION_MILLIS = 150
private const val SCROLLBAR_FADE_OUT_DURATION_MILLIS = 300

/** Returns the next shortcut hint position, wrapping to the first hint when needed. */
internal fun nextShortcutHintIndex(currentIndex: Int, hintCount: Int): Int =
    if (hintCount <= 0) 0 else (currentIndex + 1) % hintCount

/** Inserts a line break, replacing the current selection and placing the cursor after it. */
internal fun insertLineBreak(value: TextFieldValue): TextFieldValue = TextFieldValue(
    text = value.text.replaceRange(value.selection.start, value.selection.end, "\n"),
    selection = TextRange(value.selection.start + 1),
)

/** Returns whether the editor selection begins on its first line. */
internal fun isCursorOnFirstLine(value: TextFieldValue): Boolean =
    value.text.substring(0, value.selection.start).none { it == '\n' }

/** Returns whether the editor selection ends on its last line. */
internal fun isCursorOnLastLine(value: TextFieldValue): Boolean =
    value.text.substring(value.selection.end).none { it == '\n' }

/** Confirms permanent deletion of the selected chat. */
@Composable
private fun DeleteChatConfirmation(
    chatTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.delete_chat_title, chatTitle)) },
        text = { Text(stringResource(Res.string.delete_chat_message)) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.delete))
            }
        },
    )
}
