package theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import page.settings.biz.injectedFontScale

/** Applies the Material theme with the selected text-scale density. */
@Composable
fun CookTheme(
    textScale: Float,
    content: @Composable () -> Unit,
) {
    val systemDensity = LocalDensity.current
    val appDensity = Density(
        density = systemDensity.density,
        fontScale = injectedFontScale(
            systemFontScale = systemDensity.fontScale,
            density = systemDensity.density,
            selectedScale = textScale,
        ),
    )

    CompositionLocalProvider(LocalDensity provides appDensity) {
        MaterialTheme(content = content)
    }
}
