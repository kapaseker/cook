import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Toolkit
import settings.SavedWindowState

internal fun SavedWindowState.clampToVisibleScreen(
    workAreas: List<Rectangle>,
    defaultWorkArea: Rectangle?,
): SavedWindowState? {
    val availableAreas = workAreas.filter { area -> area.width > 0 && area.height > 0 }
    if (availableAreas.isEmpty()) return null

    val savedBounds = Rectangle(x, y, width, height)
    val targetArea = availableAreas
        .maxByOrNull { area -> intersectionArea(savedBounds, area) }
        ?.takeIf { area -> intersectionArea(savedBounds, area) > 0L }
        ?: defaultWorkArea?.takeIf { area -> area.width > 0 && area.height > 0 }
        ?: availableAreas.first()

    val clampedWidth = width.coerceAtMost(targetArea.width)
    val clampedHeight = height.coerceAtMost(targetArea.height)
    val minimumX = targetArea.x.toLong()
    val minimumY = targetArea.y.toLong()
    val maximumX = minimumX + targetArea.width - clampedWidth
    val maximumY = minimumY + targetArea.height - clampedHeight

    return copy(
        x = x.toLong().coerceIn(minimumX, maximumX).toInt(),
        y = y.toLong().coerceIn(minimumY, maximumY).toInt(),
        width = clampedWidth,
        height = clampedHeight,
    )
}

internal fun currentScreenWorkAreas(): Pair<List<Rectangle>, Rectangle?> = try {
    val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val toolkit = Toolkit.getDefaultToolkit()
    val devices = environment.screenDevices.toList()
    val workAreas = devices.mapNotNull { device ->
        val configuration = device.defaultConfiguration
        val bounds = configuration.bounds
        val insets = toolkit.getScreenInsets(configuration)
        Rectangle(
            bounds.x + insets.left,
            bounds.y + insets.top,
            bounds.width - insets.left - insets.right,
            bounds.height - insets.top - insets.bottom,
        ).takeIf { area -> area.width > 0 && area.height > 0 }
    }
    val defaultBounds = environment.defaultScreenDevice?.defaultConfiguration?.bounds
    val defaultWorkArea = workAreas.firstOrNull { area ->
        defaultBounds != null && area.intersects(defaultBounds)
    }

    workAreas to defaultWorkArea
} catch (_: Exception) {
    emptyList<Rectangle>() to null
}

private fun intersectionArea(first: Rectangle, second: Rectangle): Long {
    val left = maxOf(first.x.toLong(), second.x.toLong())
    val top = maxOf(first.y.toLong(), second.y.toLong())
    val right = minOf(
        first.x.toLong() + first.width,
        second.x.toLong() + second.width,
    )
    val bottom = minOf(
        first.y.toLong() + first.height,
        second.y.toLong() + second.height,
    )

    return (right - left).coerceAtLeast(0L) * (bottom - top).coerceAtLeast(0L)
}
