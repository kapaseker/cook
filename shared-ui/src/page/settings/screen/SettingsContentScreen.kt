package page.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cook.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import widget.MediumIconButton
import page.settings.biz.textScaleLabel
import repository.settings.MaximumTextScale
import repository.settings.MinimumTextScale
import repository.settings.TextScaleSliderSteps
import repository.agent.CookModel

/** Renders the text-scale controls, status messages, and preview. */
@Composable
internal fun SettingsContentScreen(
    availableModels: List<CookModel>,
    selectedModel: CookModel,
    selectedScale: Float,
    isDeviceDefault: Boolean,
    loadFailed: Boolean,
    saveFailed: Boolean,
    modelSaveFailed: Boolean,
    onScaleChanged: (Float) -> Unit,
    onScaleChangeFinished: () -> Unit,
    onResetToDeviceDefault: () -> Unit,
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
                availableModels = availableModels,
                selectedModel = selectedModel,
                onModelSelected = onModelSelected,
            )
            if (modelSaveFailed) {
                ErrorText(stringResource(Res.string.model_save_failed))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(Res.string.text_scale),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (isDeviceDefault) {
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
                onValueChange = onScaleChanged,
                modifier = Modifier.fillMaxWidth(),
                valueRange = MinimumTextScale..MaximumTextScale,
                steps = TextScaleSliderSteps,
                onValueChangeFinished = onScaleChangeFinished,
            )
            Button(
                onClick = onResetToDeviceDefault,
                enabled = !isDeviceDefault,
            ) {
                Text(stringResource(Res.string.reset_to_device_default))
            }
            if (loadFailed) {
                ErrorText(stringResource(Res.string.text_scale_load_failed))
            }
            if (saveFailed) {
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
