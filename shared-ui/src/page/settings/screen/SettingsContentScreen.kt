package page.settings.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import cook.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import page.settings.biz.ModelSettingsUiState
import page.settings.biz.TextScaleUiState
import page.settings.biz.UiScaleUiState
import page.settings.biz.displayScaleLabel
import page.settings.biz.scaledDensity
import page.settings.biz.textScaleLabel
import repository.agent.CookModel
import repository.settings.DisplayScaleSliderSteps
import repository.settings.MaximumDisplayScale
import repository.settings.MinimumDisplayScale
import theme.CookDimensions
import theme.CookShapes

/** Renders the model and independent text/UI scale settings in the settings destination. */
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
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = CookDimensions.settingsContentHorizontalPadding,
                vertical = CookDimensions.settingsContentVerticalPadding,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = CookDimensions.contentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(CookDimensions.settingsSectionSpacing),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(CookDimensions.settingsHeadingSpacing)) {
                Text("Settings", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Manage your preferences and account details.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextAppearanceCard(
                state = textScaleState,
                selectedScale = selectedScale,
                onScaleChanged = onTextScaleChanged,
                onScaleChangeFinished = onTextScaleChangeFinished,
                onReset = onResetTextScale,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CookDimensions.pageGutter),
                verticalAlignment = Alignment.Top,
            ) {
                ApplicationSettingsCard(
                    modelState = modelState,
                    uiScaleState = uiScaleState,
                    selectedTextScale = selectedScale,
                    systemDensity = systemDensity,
                    onModelSelected = onModelSelected,
                    onUiScaleChanged = onUiScaleChanged,
                    onUiScaleChangeFinished = onUiScaleChangeFinished,
                    onResetUiScale = onResetUiScale,
                    modifier = Modifier.weight(1f),
                )
                AboutCard(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TextAppearanceCard(
    state: TextScaleUiState,
    selectedScale: Float,
    onScaleChanged: (Float) -> Unit,
    onScaleChangeFinished: () -> Unit,
    onReset: () -> Unit,
) {
    SettingsCard(modifier = Modifier.fillMaxWidth()) {
        Text("Tᴛ", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Text Appearance", style = MaterialTheme.typography.titleLarge)
        Text(
            "Adjust how text looks across the app for better readability.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = CookDimensions.settingsPreviewTopPadding),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(CookDimensions.settingsPreviewCornerRadius),
        ) {
            Text(
                text = "“The quick brown fox jumps over the lazy dog”",
                modifier = Modifier.padding(
                    horizontal = CookDimensions.textAppearancePreviewHorizontalPadding,
                    vertical = CookDimensions.textAppearancePreviewVerticalPadding,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        SettingSlider(
            label = stringResource(Res.string.text_scale),
            valueLabel = textScaleLabel(selectedScale),
            value = selectedScale,
            onValueChange = onScaleChanged,
            onValueChangeFinished = onScaleChangeFinished,
        )
        TextButton(onClick = onReset, enabled = state.userScale != null) {
            Text(stringResource(Res.string.reset_to_device_default))
        }
        if (state.loadFailed) ErrorText(stringResource(Res.string.text_scale_load_failed))
        if (state.saveFailed) ErrorText(stringResource(Res.string.text_scale_save_failed))
    }
}

@Composable
private fun ApplicationSettingsCard(
    modelState: ModelSettingsUiState,
    uiScaleState: UiScaleUiState,
    selectedTextScale: Float,
    systemDensity: Density,
    onModelSelected: (CookModel) -> Unit,
    onUiScaleChanged: (Float) -> Unit,
    onUiScaleChangeFinished: () -> Unit,
    onResetUiScale: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier) {
        Text("⚙", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Application Settings", style = MaterialTheme.typography.titleLarge)
        ModelSelector(
            availableModels = modelState.availableModels,
            selectedModel = modelState.selectedModel,
            onModelSelected = onModelSelected,
        )
        if (modelState.loadFailed) ErrorText(stringResource(Res.string.model_load_failed))
        if (modelState.saveFailed) ErrorText(stringResource(Res.string.model_save_failed))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        SettingSlider(
            label = stringResource(Res.string.ui_scale),
            valueLabel = displayScaleLabel(uiScaleState.previewScale),
            value = uiScaleState.previewScale,
            onValueChange = onUiScaleChanged,
            onValueChangeFinished = onUiScaleChangeFinished,
        )
        TextButton(onClick = onResetUiScale, enabled = uiScaleState.userScale != null) {
            Text(stringResource(Res.string.reset_ui_scale))
        }
        if (uiScaleState.loadFailed) ErrorText(stringResource(Res.string.ui_scale_load_failed))
        if (uiScaleState.saveFailed) ErrorText(stringResource(Res.string.ui_scale_save_failed))
        UiScaleButtonPreview(
            textScale = selectedTextScale,
            uiScale = uiScaleState.previewScale,
            systemDensity = systemDensity,
        )
    }
}

@Composable
private fun AboutCard(modifier: Modifier = Modifier) {
    SettingsCard(
        modifier = modifier.heightIn(min = CookDimensions.aboutCardMinimumHeight),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text("ⓘ", style = MaterialTheme.typography.titleLarge)
        Text("About Cook", style = MaterialTheme.typography.titleLarge)
        Text(
            "A calm place for thoughtful cooking conversations.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Version 1.0 (Desktop)",
            style = MaterialTheme.typography.labelLarge,
        )
        Box(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(CookDimensions.aboutCardActionSpacing)) {
            Button(onClick = {}, modifier = Modifier.weight(1f)) {
                Text("Release notes")
            }
            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                Text("Privacy")
            }
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = CookShapes.card,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = CookDimensions.settingsCardTonalElevation,
        shadowElevation = CookDimensions.settingsCardShadowElevation,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(CookDimensions.settingsCardContentPadding),
            verticalArrangement = Arrangement.spacedBy(CookDimensions.settingsCardContentSpacing),
            content = content,
        )
    }
}

@Composable
private fun SettingSlider(
    label: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = CookDimensions.settingsSliderTopPadding),
        verticalArrangement = Arrangement.spacedBy(CookDimensions.settingsSliderContentSpacing),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                valueLabel,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            valueRange = MinimumDisplayScale..MaximumDisplayScale,
            steps = DisplayScaleSliderSteps,
            onValueChangeFinished = onValueChangeFinished,
        )
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
    Column(verticalArrangement = Arrangement.spacedBy(CookDimensions.modelSelectorContentSpacing)) {
        Text(stringResource(Res.string.model), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedModel.displayName, modifier = Modifier.weight(1f))
                Text("⌄")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                availableModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.displayName) },
                        onClick = {
                            expanded = false
                            onModelSelected(model)
                        },
                        leadingIcon = {
                            RadioButton(selected = model.id == selectedModel.id, onClick = null)
                        },
                    )
                }
            }
        }
    }
}

/** Shows representative controls at the pending scale without resizing the settings page. */
@Composable
private fun UiScaleButtonPreview(
    textScale: Float,
    uiScale: Float,
    systemDensity: Density,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(CookDimensions.settingsPreviewCornerRadius),
    ) {
        CompositionLocalProvider(
            LocalDensity provides scaledDensity(systemDensity, textScale, uiScale),
        ) {
            Row(
                modifier = Modifier.padding(CookDimensions.scalePreviewContentPadding),
                horizontalArrangement = Arrangement.spacedBy(CookDimensions.scalePreviewControlSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = {}) { Text(stringResource(Res.string.preview_button)) }
                OutlinedButton(onClick = {}) { Text("Preview") }
            }
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}
