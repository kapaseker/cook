package page.settings.biz

import kotlin.test.Test
import kotlin.test.assertEquals

class TextScaleTest {
    /** Verifies that device density becomes a bounded one decimal default. */
    @Test
    fun `device density becomes a bounded one decimal default`() {
        assertEquals(1.3f, deviceDefaultTextScale(1.25f), 0.001f)
        assertEquals(2.6f, deviceDefaultTextScale(2.625f), 0.001f)
        assertEquals(0.5f, deviceDefaultTextScale(0.2f), 0.001f)
        assertEquals(4.0f, deviceDefaultTextScale(4.5f), 0.001f)
    }

    /** Verifies that user scale replaces the density derived text scale. */
    @Test
    fun `user scale replaces the density derived text scale`() {
        assertEquals(2.6f, selectedTextScale(density = 2.625f, userScale = null), 0.001f)
        assertEquals(1.4f, selectedTextScale(density = 2.625f, userScale = 1.36f), 0.001f)
    }

    /** Verifies that injected font scale removes compose density from the text conversion. */
    @Test
    fun `injected font scale removes compose density from the text conversion`() {
        assertEquals(
            0.8f,
            injectedFontScale(systemFontScale = 1.2f, density = 3f, selectedScale = 2f),
            0.001f,
        )
    }

    /** Verifies that labels always show one decimal place. */
    @Test
    fun `labels always show one decimal place`() {
        assertEquals("1.0x", textScaleLabel(1f))
        assertEquals("2.6x", textScaleLabel(2.625f))
    }
}
