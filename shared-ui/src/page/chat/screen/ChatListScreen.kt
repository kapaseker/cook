package page.chat.screen

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import cook.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import page.chat.biz.ChatListItemUiState
import page.chat.biz.ChatListUiState
import theme.CookDimensions

/** Renders persisted chats with their title, latest preview, and selected state. */
@Composable
internal fun ChatListScreen(
    state: ChatListUiState,
    enabled: Boolean,
    onCreateChat: () -> Unit,
    onSelectChat: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Box(
        modifier = modifier.width(CookDimensions.chatListWidth).fillMaxHeight(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = CookDimensions.chatListHeaderHorizontalPadding,
                    vertical = CookDimensions.chatListHeaderVerticalPadding,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Conversations", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCreateChat, enabled = enabled) {
                    Text("+", style = MaterialTheme.typography.headlineSmall)
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = CookDimensions.chatListHorizontalPadding),
                shape = RoundedCornerShape(CookDimensions.chatListSearchCornerRadius),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    CookDimensions.thinBorderWidth,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                ),
            ) {
                Text(
                    text = "⌕  Search history…",
                    modifier = Modifier.padding(
                        horizontal = CookDimensions.chatListSearchHorizontalPadding,
                        vertical = CookDimensions.chatListSearchVerticalPadding,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(
                    top = CookDimensions.chatListContentTopPadding,
                    bottom = CookDimensions.chatListContentBottomPadding,
                ),
            ) {
                if (state.items.isNotEmpty()) {
                    item(key = "today-label") {
                        Text(
                            text = "TODAY",
                            modifier = Modifier.padding(
                                start = CookDimensions.chatListHorizontalPadding,
                                top = CookDimensions.chatListDateLabelTopPadding,
                                bottom = CookDimensions.chatListDateLabelBottomPadding,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            items(state.items, key = ChatListItemUiState::id) { item ->
                ChatListItem(
                    item = item,
                    selected = item.id == state.selectedConversationId,
                    enabled = enabled,
                    onClick = { onSelectChat(item.id) },
                )
            }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

@Composable
private fun ChatListItem(
    item: ChatListItemUiState,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = RoundedCornerShape(CookDimensions.chatListItemCornerRadius),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = CookDimensions.chatListItemHorizontalMargin,
                vertical = CookDimensions.chatListItemVerticalMargin,
            )
            .selectable(
                selected = selected,
                enabled = enabled && !selected,
                role = Role.Tab,
                onClick = onClick,
            ),
        border = if (selected) {
            BorderStroke(CookDimensions.thinBorderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
        } else {
            null
        },
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = CookDimensions.chatListItemHorizontalPadding,
                vertical = CookDimensions.chatListItemVerticalPadding,
            ),
        ) {
            Text(
                text = item.title ?: stringResource(Res.string.new_chat),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.preview ?: stringResource(Res.string.no_messages_yet),
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
