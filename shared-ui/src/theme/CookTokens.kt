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
    val messageBubbleCornerRadius = 8.dp
    val messageBubbleHorizontalPadding = 14.dp
    val messageBubbleVerticalPadding = 10.dp
    val composerPadding = 20.dp
    val composerSpacing = 8.dp
    val composerRowSpacing = 12.dp
    val sendButtonHeight = 56.dp
    val buttonSpacing = 12.dp
}

object CookShapes {
    val messageBubble = RoundedCornerShape(CookDimensions.messageBubbleCornerRadius)
}

object CookOpacity {
    const val pendingMessage = 0.70f
}
