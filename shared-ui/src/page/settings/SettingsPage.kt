package page.settings

import androidx.compose.runtime.Composable
import page.settings.biz.SettingsUiState
import page.settings.screen.SettingsContentScreen
import repository.agent.CookModel

/** Renders the model and text-scale settings route. */
@Composable
internal fun SettingsPage(
    state: SettingsUiState,
    selectedScale: Float,
    onScaleChanged: (Float) -> Unit,
    onScaleChangeFinished: () -> Unit,
    onResetToDeviceDefault: () -> Unit,
    onModelSelected: (CookModel) -> Unit,
    onBack: () -> Unit,
) {
    SettingsContentScreen(
        availableModels = state.availableModels,
        selectedModel = state.selectedModel,
        selectedScale = selectedScale,
        isDeviceDefault = state.userScale == null,
        loadFailed = state.loadFailed,
        saveFailed = state.saveFailed,
        modelSaveFailed = state.modelSaveFailed,
        onScaleChanged = onScaleChanged,
        onScaleChangeFinished = onScaleChangeFinished,
        onResetToDeviceDefault = onResetToDeviceDefault,
        onModelSelected = onModelSelected,
        onBack = onBack,
    )
}
