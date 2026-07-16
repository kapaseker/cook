package settings

data class SavedWindowState(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val isMaximized: Boolean,
)

interface WindowStateStore {
    suspend fun load(): SavedWindowState?

    suspend fun save(state: SavedWindowState)
}

fun SavedWindowState.hasValidBounds(): Boolean = width > 0 && height > 0