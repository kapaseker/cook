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
    val messageDateLabelHorizontalPadding = 13.dp
    val messageDateLabelVerticalPadding = 5.dp
    val messageLabelAvatarSize = 24.dp
    val messageLabelTextSpacing = 8.dp

    val composerPadding = 24.dp
    val composerSpacing = 8.dp
    val composerRowSpacing = 12.dp
    val composerContentPadding = 8.dp
    val composerTonalElevation = 2.dp
    val sendButtonHeight = 48.dp
    val buttonSpacing = 12.dp

    val chatListWidth = 320.dp
    val chatListHeaderHorizontalPadding = 24.dp
    val chatListHeaderVerticalPadding = 20.dp
    val chatListHorizontalPadding = 24.dp
    val chatListSearchCornerRadius = 8.dp
    val chatListSearchHorizontalPadding = 12.dp
    val chatListSearchVerticalPadding = 10.dp
    val chatListContentTopPadding = 16.dp
    val chatListContentBottomPadding = 8.dp
    val chatListDateLabelTopPadding = 16.dp
    val chatListDateLabelBottomPadding = 8.dp
    val chatListItemCornerRadius = 8.dp
    val chatListItemHorizontalMargin = 16.dp
    val chatListItemVerticalMargin = 4.dp
    val chatListItemHorizontalPadding = 16.dp
    val chatListItemVerticalPadding = 16.dp

    val markdownBlockSpacing = 8.dp
    val markdownListIndent = 12.dp
    val markdownListItemSpacing = 4.dp
    val markdownListMarkerSpacing = 8.dp
    val markdownLooseListBlockSpacing = 8.dp
    val markdownTightListBlockSpacing = 0.dp

    val sideNavigationWidth = 240.dp
    val sideNavigationHorizontalPadding = 16.dp
    val sideNavigationVerticalPadding = 24.dp
    val sideNavigationBrandHorizontalPadding = 8.dp
    val sideNavigationDestinationTopPadding = 48.dp
    val sideNavigationDestinationSpacing = 16.dp
    val sideNavigationItemCornerRadius = 8.dp
    val sideNavigationItemHorizontalPadding = 16.dp
    val sideNavigationItemVerticalPadding = 8.dp
    val sideNavigationItemContentSpacing = 20.dp
    val sideNavigationItemIconSize = 24.dp
    val sideNavigationDividerVerticalPadding = 24.dp
    val sideNavigationFooterSpacing = 4.dp

    val mediumIconButtonSize = 52.dp
    val mediumIconButtonContentPadding = 10.dp

    val pageGutter = 20.dp
    val contentMaxWidth = 896.dp
    val cardCornerRadius = 12.dp

    val conversationHeaderHorizontalPadding = 24.dp
    val conversationHeaderVerticalPadding = 12.dp
    val conversationHeaderAvatarSize = 40.dp
    val conversationHeaderTextSpacing = 16.dp
    val conversationHeaderOnlineIndicatorSize = 8.dp

    val settingsContentHorizontalPadding = 72.dp
    val settingsContentVerticalPadding = 64.dp
    val settingsSectionSpacing = 40.dp
    val settingsHeadingSpacing = 8.dp
    val settingsPreviewTopPadding = 16.dp
    val settingsPreviewCornerRadius = 8.dp
    val textAppearancePreviewHorizontalPadding = 24.dp
    val textAppearancePreviewVerticalPadding = 48.dp
    val aboutCardMinimumHeight = 408.dp
    val aboutCardActionSpacing = 16.dp
    val settingsCardTonalElevation = 1.dp
    val settingsCardShadowElevation = 2.dp
    val settingsCardContentPadding = 24.dp
    val settingsCardContentSpacing = 16.dp
    val settingsSliderTopPadding = 8.dp
    val settingsSliderContentSpacing = 8.dp
    val modelSelectorContentSpacing = 8.dp
    val scalePreviewContentPadding = 16.dp
    val scalePreviewControlSpacing = 12.dp

    val thinBorderWidth = 1.dp
}

object CookShapes {
    val messageBubble = RoundedCornerShape(CookDimensions.messageBubbleCornerRadius)
    val card = RoundedCornerShape(CookDimensions.cardCornerRadius)
}

object CookOpacity {
    const val pendingMessage = 0.70f
    const val agentStatus = 0.70f
}
