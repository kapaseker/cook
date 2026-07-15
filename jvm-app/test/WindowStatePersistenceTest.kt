import androidx.compose.ui.window.WindowPlacement
import settings.SavedWindowState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WindowStatePersistenceTest {
    @Test
    fun `initial floating state establishes a baseline without saving`() {
        val accumulator = WindowStateAccumulator(initialState = null)

        assertNull(accumulator.observe(observation(bounds = bounds())))
        assertNull(accumulator.observe(observation(bounds = bounds())))
    }

    @Test
    fun `changed floating bounds are emitted after the baseline`() {
        val accumulator = WindowStateAccumulator(initialState = null)
        accumulator.observe(observation(bounds = bounds()))
        val moved = bounds(x = 240, y = 160)

        assertEquals(moved, accumulator.observe(observation(bounds = moved)))
    }

    @Test
    fun `maximizing preserves the last floating bounds`() {
        val floating = bounds(x = 80, y = 60, width = 1200, height = 800)
        val accumulator = WindowStateAccumulator(initialState = floating)
        accumulator.observe(observation(bounds = floating))

        assertEquals(
            floating.copy(isMaximized = true),
            accumulator.observe(
                observation(
                    placement = WindowPlacement.Maximized,
                    bounds = bounds(x = 0, y = 0, width = 1920, height = 1040),
                ),
            ),
        )
    }

    @Test
    fun `minimized observations do not change persisted state`() {
        val floating = bounds()
        val accumulator = WindowStateAccumulator(initialState = floating)
        accumulator.observe(observation(bounds = floating))

        assertNull(
            accumulator.observe(
                observation(
                    placement = WindowPlacement.Maximized,
                    isMinimized = true,
                    bounds = bounds(width = 1920, height = 1040),
                ),
            ),
        )
        assertNull(accumulator.observe(observation(bounds = floating)))
    }

    @Test
    fun `fullscreen is reduced to the last floating state`() {
        val maximized = bounds(isMaximized = true)
        val accumulator = WindowStateAccumulator(initialState = maximized)
        accumulator.observe(
            observation(
                placement = WindowPlacement.Maximized,
                bounds = bounds(width = 1920, height = 1040),
            ),
        )

        assertEquals(
            maximized.copy(isMaximized = false),
            accumulator.observe(
                observation(
                    placement = WindowPlacement.Fullscreen,
                    bounds = bounds(width = 1920, height = 1080),
                ),
            ),
        )
    }

    private fun observation(
        placement: WindowPlacement = WindowPlacement.Floating,
        isMinimized: Boolean = false,
        bounds: SavedWindowState?,
    ) = WindowObservation(
        placement = placement,
        isMinimized = isMinimized,
        bounds = bounds,
    )

    private fun bounds(
        x: Int = 100,
        y: Int = 100,
        width: Int = 1280,
        height: Int = 720,
        isMaximized: Boolean = false,
    ) = SavedWindowState(
        x = x,
        y = y,
        width = width,
        height = height,
        isMaximized = isMaximized,
    )
}
