package repository.settings

import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

const val MinimumTextScale = 0.5f
const val MaximumTextScale = 4.0f
const val TextScaleStep = 0.1f
const val TextScaleSliderSteps = 34

interface SettingsStore {
    val userScale: Flow<Float?>

    suspend fun setUserScale(scale: Float)

    suspend fun clearUserScale()
}

fun normalizeTextScale(value: Float): Float {
    if (!value.isFinite()) return MinimumTextScale

    return ((value / TextScaleStep).roundToInt() * TextScaleStep)
        .coerceIn(MinimumTextScale, MaximumTextScale)
}