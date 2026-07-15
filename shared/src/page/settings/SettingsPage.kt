package page.settings

import androidx.compose.runtime.Composable
import page.settings.screen.SettingsContentScreen
import page.settings.biz.SettingsUiState

/** Navigation 3 destination for text-scale settings. */
@Composable
internal fun SettingsPage(
    state: SettingsUiState,
    selectedScale: Float,
    onScaleChanged: (Float) -> Unit,
    onScaleChangeFinished: () -> Unit,
    onResetToDeviceDefault: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsContentScreen(
        selectedScale = selectedScale,
        isDeviceDefault = state.userScale == null,
        loadFailed = state.loadFailed,
        saveFailed = state.saveFailed,
        onScaleChanged = onScaleChanged,
        onScaleChangeFinished = onScaleChangeFinished,
        onResetToDeviceDefault = onResetToDeviceDefault,
        onBack = onBack,
    )
}
