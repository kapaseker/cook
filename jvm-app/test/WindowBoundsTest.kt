import java.awt.Rectangle
import settings.SavedWindowState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WindowBoundsTest {
    private val primary = Rectangle(0, 0, 1920, 1040)
    private val secondary = Rectangle(-1280, 0, 1280, 1024)

    @Test
    fun `valid bounds remain on their current display`() {
        val saved = savedWindow(x = -1200, y = 100, width = 900, height = 700)

        assertEquals(saved, saved.clampToVisibleScreen(listOf(primary, secondary), primary))
    }

    @Test
    fun `partially off-screen bounds are moved fully into the overlapping display`() {
        val saved = savedWindow(x = 1700, y = 900, width = 800, height = 600)

        assertEquals(
            saved.copy(x = 1120, y = 440),
            saved.clampToVisibleScreen(listOf(primary, secondary), primary),
        )
    }

    @Test
    fun `disconnected display falls back to the default display`() {
        val saved = savedWindow(x = 3000, y = 200, width = 1000, height = 700)

        assertEquals(
            saved.copy(x = 920, y = 200),
            saved.clampToVisibleScreen(listOf(primary, secondary), primary),
        )
    }

    @Test
    fun `oversized bounds shrink to the target work area`() {
        val saved = savedWindow(x = -200, y = -100, width = 2400, height = 1400)

        assertEquals(
            saved.copy(x = 0, y = 0, width = 1920, height = 1040),
            saved.clampToVisibleScreen(listOf(primary), primary),
        )
    }

    @Test
    fun `missing displays produce no restorable bounds`() {
        assertNull(savedWindow().clampToVisibleScreen(emptyList(), null))
    }

    private fun savedWindow(
        x: Int = 0,
        y: Int = 0,
        width: Int = 1280,
        height: Int = 720,
    ) = SavedWindowState(
        x = x,
        y = y,
        width = width,
        height = height,
        isMaximized = false,
    )
}
