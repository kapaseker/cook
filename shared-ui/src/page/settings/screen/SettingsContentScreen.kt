package page.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import cook.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import widget.MediumIconButton
import page.settings.biz.ModelSettingsUiState
import page.settings.biz.TextScaleUiState
import page.settings.biz.UiScaleUiState
import page.settings.biz.displayScaleLabel
import page.settings.biz.scaledDensity
import page.settings.biz.textScaleLabel
import repository.settings.DefaultUiScale
import repository.settings.DisplayScaleSliderSteps
import repository.settings.MaximumDisplayScale
import repository.settings.MinimumDisplayScale
import repository.agent.CookModel

/** Renders the model and independent text/UI scale settings. */
@Composable
internal fun SettingsContentScreen(
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SettingsHeader(onBack = onBack)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ModelSelector(
                availableModels = modelState.availableModels,
                selectedModel = modelState.selectedModel,
                onModelSelected = onModelSelected,
            )
            if (modelState.loadFailed) {
                ErrorText(stringResource(Res.string.model_load_failed))
            }
            if (modelState.saveFailed) {
                ErrorText(stringResource(Res.string.model_save_failed))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(Res.string.text_scale),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (textScaleState.userScale == null) {
                    stringResource(
                        Res.string.device_default_scale,
                        textScaleLabel(selectedScale),
                    )
                } else {
                    stringResource(Res.string.custom_scale, textScaleLabel(selectedScale))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = selectedScale,
                onValueChange = onTextScaleChanged,
                modifier = Modifier.fillMaxWidth(),
                valueRange = MinimumDisplayScale..MaximumDisplayScale,
                steps = DisplayScaleSliderSteps,
                onValueChangeFinished = onTextScaleChangeFinished,
            )
            Button(
                onClick = onResetTextScale,
                enabled = textScaleState.userScale != null,
            ) {
                Text(stringResource(Res.string.reset_to_device_default))
            }
            if (textScaleState.loadFailed) {
                ErrorText(stringResource(Res.string.text_scale_load_failed))
            }
            if (textScaleState.saveFailed) {
                ErrorText(stringResource(Res.string.text_scale_save_failed))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(Res.string.preview),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.preview_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(Res.string.preview_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(Res.string.preview_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            UiScaleSettings(
                state = uiScaleState,
                selectedTextScale = selectedScale,
                systemDensity = systemDensity,
                onScaleChanged = onUiScaleChanged,
                onScaleChangeFinished = onUiScaleChangeFinished,
                onReset = onResetUiScale,
            )
        }
    }
}

/** Renders the UI-scale slider, reset action, status, and focused preview. */
@Composable
private fun UiScaleSettings(
    state: UiScaleUiState,
    selectedTextScale: Float,
    systemDensity: Density,
    onScaleChanged: (Float) -> Unit,
    onScaleChangeFinished: () -> Unit,
    onReset: () -> Unit,
) {
    Text(
        text = stringResource(Res.string.ui_scale),
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        text = if (state.userScale == null && state.previewScale == DefaultUiScale) {
            stringResource(
                Res.string.default_ui_scale,
                displayScaleLabel(state.previewScale),
            )
        } else {
            stringResource(Res.string.custom_scale, displayScaleLabel(state.previewScale))
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Slider(
        value = state.previewScale,
        onValueChange = onScaleChanged,
        modifier = Modifier.fillMaxWidth(),
        valueRange = MinimumDisplayScale..MaximumDisplayScale,
        steps = DisplayScaleSliderSteps,
        onValueChangeFinished = onScaleChangeFinished,
    )
    Button(
        onClick = onReset,
        enabled = state.userScale != null,
    ) {
        Text(stringResource(Res.string.reset_ui_scale))
    }
    if (state.loadFailed) {
        ErrorText(stringResource(Res.string.ui_scale_load_failed))
    }
    if (state.saveFailed) {
        ErrorText(stringResource(Res.string.ui_scale_save_failed))
    }
    Text(
        text = stringResource(Res.string.preview),
        style = MaterialTheme.typography.titleMedium,
    )
    UiScaleButtonPreview(
        textScale = selectedTextScale,
        uiScale = state.previewScale,
        systemDensity = systemDensity,
    )
}

/** Shows representative controls at the pending scale without resizing the settings page. */
@Composable
private fun UiScaleButtonPreview(
    textScale: Float,
    uiScale: Float,
    systemDensity: Density,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        CompositionLocalProvider(
            LocalDensity provides scaledDensity(systemDensity, textScale, uiScale),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = {}) {
                    Text(stringResource(Res.string.preview_button))
                }
                MediumIconButton(
                    onClick = {},
                    painter = painterResource(Res.drawable.ic_settings),
                    contentDescription = stringResource(Res.string.settings),
                )
            }
        }
    }
}

/** Renders the global chat-model dropdown. */
@Composable
private fun ModelSelector(
    availableModels: List<CookModel>,
    selectedModel: CookModel,
    onModelSelected: (CookModel) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.model),
            style = MaterialTheme.typography.titleLarge,
        )
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedModel.displayName)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                availableModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.displayName) },
                        onClick = {
                            expanded = false
                            onModelSelected(model)
                        },
                        leadingIcon = {
                            RadioButton(
                                selected = model.id == selectedModel.id,
                                onClick = null,
                            )
                        },
                    )
                }
            }
        }
    }
}

/** Renders the settings title and back action. */
@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediumIconButton(
            onClick = onBack,
            painter = painterResource(Res.drawable.ic_left),
            contentDescription = stringResource(Res.string.back),
        )
        Text(
            text = stringResource(Res.string.settings),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

/** Renders an error message using the theme error color. */
@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}
