package widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import theme.CookDimensions

/** Identifies the active destination in the shared desktop navigation rail. */
internal enum class AppNavigationDestination {
    Chat,
    Settings,
}

/** Renders the shared primary navigation shown beside chat and settings. */
@Composable
internal fun AppSideNavigation(
    selectedDestination: AppNavigationDestination,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(CookDimensions.sideNavigationWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = "Cook",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Your thoughtful AI kitchen",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.padding(top = 48.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NavigationItem(label = "Home", symbol = "⌂", selected = false)
            NavigationItem(
                label = "Chat",
                symbol = "▣",
                selected = selectedDestination == AppNavigationDestination.Chat,
                onClick = onOpenChat,
            )
            NavigationItem(
                label = "Settings",
                symbol = "⚙",
                selected = selectedDestination == AppNavigationDestination.Settings,
                onClick = onOpenSettings,
            )
        }
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text("Upgrade to Pro", style = MaterialTheme.typography.labelLarge)
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        )
        NavigationItem(label = "Help", symbol = "?", selected = false)
        Spacer(Modifier.padding(vertical = 4.dp))
        NavigationItem(label = "Log out", symbol = "↪", selected = false)
    }
}

@Composable
private fun NavigationItem(
    label: String,
    symbol: String,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val itemModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(background)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 16.dp, vertical = 8.dp)

    androidx.compose.foundation.layout.Row(
        modifier = itemModifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symbol, color = contentColor)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
        )
    }
}
