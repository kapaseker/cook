package repository.settings

import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

const val MinimumTextScale = 0.5f
const val MaximumTextScale = 4.0f
const val TextScaleStep = 0.1f
const val TextScaleSliderSteps = 34

interface SettingsStore {
    val userScale: Flow<Float?>

    /** Persists a normalized user-selected text scale. */
    suspend fun setUserScale(scale: Float)

    /** Removes the persisted text-scale override. */
    suspend fun clearUserScale()
}

/** Rounds and bounds a text scale to the supported range. */
fun normalizeTextScale(value: Float): Float {
    if (!value.isFinite()) return MinimumTextScale

    return ((value / TextScaleStep).roundToInt() * TextScaleStep)
        .coerceIn(MinimumTextScale, MaximumTextScale)
}