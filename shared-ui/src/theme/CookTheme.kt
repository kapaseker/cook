package theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import page.settings.biz.scaledDensity

/** Applies the Material theme with independent text and UI scales. */
@Composable
fun CookTheme(
    textScale: Float,
    uiScale: Float,
    content: @Composable () -> Unit,
) {
    val systemDensity = LocalDensity.current

    CompositionLocalProvider(
        LocalDensity provides scaledDensity(systemDensity, textScale, uiScale),
    ) {
        MaterialTheme(content = content)
    }
}
