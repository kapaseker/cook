package page.chat.screen

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cook.generated.resources.Res
import cook.generated.resources.new_chat
import cook.generated.resources.no_messages_yet
import org.jetbrains.compose.resources.stringResource
import page.chat.biz.ChatListItemUiState
import page.chat.biz.ChatListUiState
import theme.CookDimensions

/** Renders persisted chats with their title, latest preview, and selected state. */
@Composable
internal fun ChatListScreen(
    state: ChatListUiState,
    enabled: Boolean,
    onSelectChat: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Box(
        modifier = modifier.width(CookDimensions.chatListWidth).fillMaxHeight(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(vertical = 8.dp),
        ) {
            items(state.items, key = ChatListItemUiState::id) { item ->
                ChatListItem(
                    item = item,
                    selected = item.id == state.selectedConversationId,
                    enabled = enabled,
                    onClick = { onSelectChat(item.id) },
                )
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
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .selectable(
                selected = selected,
                enabled = enabled && !selected,
                role = Role.Tab,
                onClick = onClick,
            ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = item.title ?: stringResource(Res.string.new_chat),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.preview ?: stringResource(Res.string.no_messages_yet),
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
