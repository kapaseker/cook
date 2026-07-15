package repository.settings

import kotlin.math.roundToInt

internal const val MinimumTextScale = 0.5f
internal const val MaximumTextScale = 4.0f
internal const val TextScaleStep = 0.1f
internal const val TextScaleSliderSteps = 34

internal fun normalizeTextScale(value: Float): Float {
    if (!value.isFinite()) {
        return MinimumTextScale
    }

    return ((value / TextScaleStep).roundToInt() * TextScaleStep)
        .coerceIn(MinimumTextScale, MaximumTextScale)
}
