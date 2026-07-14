package settings

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

internal fun deviceDefaultTextScale(density: Float): Float = normalizeTextScale(density)

internal fun selectedTextScale(
    density: Float,
    userScale: Float?,
): Float = userScale
    ?.takeIf { scale -> scale.isFinite() }
    ?.let(::normalizeTextScale)
    ?: deviceDefaultTextScale(density)

internal fun injectedFontScale(
    systemFontScale: Float,
    density: Float,
    selectedScale: Float,
): Float {
    if (!density.isFinite() || density <= 0f) {
        return systemFontScale
    }

    return systemFontScale * normalizeTextScale(selectedScale) / density
}

internal fun textScaleLabel(scale: Float): String {
    val tenths = (normalizeTextScale(scale) * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}x"
}
