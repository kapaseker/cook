package page.settings.biz

import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayScaleTest {
    /** Verifies absent UI overrides use the neutral scale. */
    @Test
    fun `ui scale defaults to neutral`() {
        assertEquals(1f, selectedUiScale(null), 0.001f)
    }

    /** Verifies UI scaling changes DP density without changing the selected text pixel factor. */
    @Test
    fun `ui scale does not affect text size`() {
        val systemDensity = Density(density = 2f, fontScale = 1.25f)
        val compact = scaledDensity(systemDensity, textScale = 1.5f, uiScale = 0.5f)
        val expanded = scaledDensity(systemDensity, textScale = 1.5f, uiScale = 4f)

        assertEquals(1f, compact.density, 0.001f)
        assertEquals(8f, expanded.density, 0.001f)
        assertEquals(
            compact.density * compact.fontScale,
            expanded.density * expanded.fontScale,
            0.001f,
        )
    }

    /** Verifies display labels use one decimal place after normalization. */
    @Test
    fun `display scale labels use normalized tenths`() {
        assertEquals("1.4x", displayScaleLabel(1.36f))
        assertEquals("4.0x", displayScaleLabel(8f))
    }
}
