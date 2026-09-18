package widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import cook.generated.resources.Res
import cook.generated.resources.ic_chat
import cook.generated.resources.ic_chat_fill
import cook.generated.resources.ic_settings
import cook.generated.resources.ic_settings_fill
import cook.generated.resources.ic_question
import cook.generated.resources.ic_question_fill
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import theme.CookDimensions

/** Identifies the active destination in the shared desktop navigation rail. */
internal enum class AppNavigationDestination {
    Chat,
    Settings,
    Help,
}

/** Renders the shared primary navigation shown beside chat and settings. */
@Composable
internal fun AppSideNavigation(
    selectedDestination: AppNavigationDestination,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(CookDimensions.sideNavigationWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = CookDimensions.sideNavigationHorizontalPadding,
                vertical = CookDimensions.sideNavigationVerticalPadding,
            ),
    ) {
        Column(modifier = Modifier.padding(horizontal = CookDimensions.sideNavigationBrandHorizontalPadding)) {
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
            modifier = Modifier.padding(top = CookDimensions.sideNavigationDestinationTopPadding).weight(1f),
            verticalArrangement = Arrangement.spacedBy(CookDimensions.sideNavigationDestinationSpacing),
        ) {
            NavigationItem(
                label = "Chat",
                leadingVisual = NavigationItemLeadingVisual.Icon(
                    deselected = Res.drawable.ic_chat,
                    selected = Res.drawable.ic_chat_fill,
                ),
                selected = selectedDestination == AppNavigationDestination.Chat,
                onClick = onOpenChat,
            )
            NavigationItem(
                label = "Settings",
                leadingVisual = NavigationItemLeadingVisual.Icon(
                    deselected = Res.drawable.ic_settings,
                    selected = Res.drawable.ic_settings_fill,
                ),
                selected = selectedDestination == AppNavigationDestination.Settings,
                onClick = onOpenSettings,
            )
        }
        NavigationItem(
            label = "Help",
            leadingVisual = NavigationItemLeadingVisual.Icon(
                deselected = Res.drawable.ic_question,
                selected = Res.drawable.ic_question_fill,
            ),
            selected = selectedDestination == AppNavigationDestination.Help,
            onClick = onOpenHelp,
        )
    }
}

private sealed interface NavigationItemLeadingVisual {
    data class Icon(
        val deselected: DrawableResource,
        val selected: DrawableResource,
    ) : NavigationItemLeadingVisual
}

@Composable
private fun NavigationItem(
    label: String,
    leadingVisual: NavigationItemLeadingVisual,
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
        .clip(RoundedCornerShape(CookDimensions.sideNavigationItemCornerRadius))
        .background(background)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(
            horizontal = CookDimensions.sideNavigationItemHorizontalPadding,
            vertical = CookDimensions.sideNavigationItemVerticalPadding,
        )

    Row(
        modifier = itemModifier,
        horizontalArrangement = Arrangement.spacedBy(CookDimensions.sideNavigationItemContentSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (leadingVisual) {
            is NavigationItemLeadingVisual.Icon -> Icon(
                painter = painterResource(
                    if (selected) leadingVisual.selected else leadingVisual.deselected,
                ),
                contentDescription = null,
                modifier = Modifier.size(CookDimensions.sideNavigationItemIconSize),
                tint = contentColor,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
        )
    }
}
