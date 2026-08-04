package page.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import page.settings.biz.ModelSettingsUiState
import page.settings.biz.TextScaleUiState
import page.settings.biz.UiScaleUiState
import page.settings.screen.SettingsContentScreen
import repository.agent.CookModel

/** Renders the model, text-scale, and UI-scale settings route. */
@Composable
internal fun SettingsPage(
    modelState: ModelSettingsUiState,
    textScaleState: TextScaleUiState,
    uiScaleState: UiScaleUiState,
    selectedScale: Float,
    systemDensity: Density,
    onTextScaleChanged: (Float) -> Unit,
    onTextScaleChangeFinished: () -> Unit,
    onResetTextScale: () -> Unit,
    onUiScaleChanged: (Float) -> Unit,
    onUiScaleChangeFinished: () -> Unit,
    onResetUiScale: () -> Unit,
    onModelSelected: (CookModel) -> Unit,
    onBack: () -> Unit,
) {
    SettingsContentScreen(
        modelState = modelState,
        textScaleState = textScaleState,
        uiScaleState = uiScaleState,
        selectedScale = selectedScale,
        systemDensity = systemDensity,
        onTextScaleChanged = onTextScaleChanged,
        onTextScaleChangeFinished = onTextScaleChangeFinished,
        onResetTextScale = onResetTextScale,
        onUiScaleChanged = onUiScaleChanged,
        onUiScaleChangeFinished = onUiScaleChangeFinished,
        onResetUiScale = onResetUiScale,
        onModelSelected = onModelSelected,
        onBack = onBack,
    )
}
