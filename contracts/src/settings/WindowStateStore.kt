package settings

data class SavedWindowState(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val isMaximized: Boolean,
)

interface WindowStateStore {
    /** Loads the last persisted window state when one is available. */
    suspend fun load(): SavedWindowState?

    /** Persists a window state with valid bounds. */
    suspend fun save(state: SavedWindowState)
}

/** Returns whether this state has positive width and height. */
fun SavedWindowState.hasValidBounds(): Boolean = width > 0 && height > 0
