package widget

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import theme.CookDimensions

/** Renders a medium-sized icon button. */
@Composable
fun MediumIconButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(CookDimensions.mediumIconButtonSize)) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.padding(CookDimensions.mediumIconButtonContentPadding).fillMaxSize(),
        )
    }
}
