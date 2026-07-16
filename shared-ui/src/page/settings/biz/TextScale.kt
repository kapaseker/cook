package page.settings.biz

import kotlin.math.roundToInt
import repository.settings.MaximumTextScale
import repository.settings.MinimumTextScale
import repository.settings.TextScaleSliderSteps
import repository.settings.normalizeTextScale

/** Normalizes the text scale derived from display density. */
internal fun deviceDefaultTextScale(density: Float): Float = normalizeTextScale(density)

/** Returns the valid user scale or the device-default scale. */
internal fun selectedTextScale(
    density: Float,
    userScale: Float?,
): Float = userScale
    ?.takeIf { scale -> scale.isFinite() }
    ?.let(::normalizeTextScale)
    ?: deviceDefaultTextScale(density)

/** Calculates the font scale required for the selected text scale. */
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

/** Formats a normalized text scale with one decimal place. */
internal fun textScaleLabel(scale: Float): String {
    val tenths = (normalizeTextScale(scale) * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}x"
}
