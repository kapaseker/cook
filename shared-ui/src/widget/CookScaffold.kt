package widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Keeps Cook's persistent app navigation beside content controlled by Navigation 3. */
@Composable
internal fun CookScaffold(
    selectedDestination: AppNavigationDestination,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        AppSideNavigation(
            selectedDestination = selectedDestination,
            onOpenChat = onOpenChat,
            onOpenSettings = onOpenSettings,
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        )
        Box(
            modifier = Modifier.fillMaxHeight().weight(1f),
        ) {
            content()
        }
    }
}
