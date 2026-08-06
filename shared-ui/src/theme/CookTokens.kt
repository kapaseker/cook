package theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object CookDimensions {
    val windowWidth = 1280.dp
    val windowHeight = 720.dp
    val contentHorizontalPadding = 24.dp
    val contentVerticalPadding = 18.dp
    val headerTextSpacing = 4.dp
    val messageSpacing = 12.dp
    val listEndAnchorHeight = 1.dp
    val messageLabelSpacing = 4.dp
    val messageBubbleCornerRadius = 12.dp
    val messageBubbleHorizontalPadding = 16.dp
    val messageBubbleVerticalPadding = 16.dp
    val composerPadding = 24.dp
    val composerSpacing = 8.dp
    val composerRowSpacing = 12.dp
    val sendButtonHeight = 48.dp
    val buttonSpacing = 12.dp
    val chatListWidth = 320.dp
    val sideNavigationWidth = 240.dp
    val pageGutter = 20.dp
    val contentMaxWidth = 896.dp
    val cardCornerRadius = 12.dp
}

object CookShapes {
    val messageBubble = RoundedCornerShape(CookDimensions.messageBubbleCornerRadius)
    val card = RoundedCornerShape(CookDimensions.cardCornerRadius)
}

object CookOpacity {
    const val pendingMessage = 0.70f
    const val agentStatus = 0.70f
}
