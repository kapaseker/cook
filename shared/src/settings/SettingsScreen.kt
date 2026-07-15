package settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cook.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import widget.MediumIconButton

@Composable
internal fun SettingsScreen(
    selectedScale: Float,
    isDeviceDefault: Boolean,
    loadFailed: Boolean,
    saveFailed: Boolean,
    onScaleChanged: (Float) -> Unit,
    onScaleChangeFinished: () -> Unit,
    onResetToDeviceDefault: () -> Unit,
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

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}
