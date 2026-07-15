package settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import repository.settings.settingsDataStore

data class SavedWindowState(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val isMaximized: Boolean,
)

class WindowStatePreferences internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun load(): SavedWindowState? {
        val preferences = dataStore.data.first()
        val x = preferences[WindowXKey] ?: return null
        val y = preferences[WindowYKey] ?: return null
        val width = preferences[WindowWidthKey] ?: return null
        val height = preferences[WindowHeightKey] ?: return null
        val isMaximized = preferences[WindowMaximizedKey] ?: return null

        return SavedWindowState(
            x = x,
            y = y,
            width = width,
            height = height,
            isMaximized = isMaximized,
        ).takeIf { state -> state.hasValidBounds() }
    }

    suspend fun save(state: SavedWindowState) {
        require(state.hasValidBounds()) { "Window dimensions must be positive" }

        dataStore.edit { preferences ->
            preferences[WindowXKey] = state.x
            preferences[WindowYKey] = state.y
            preferences[WindowWidthKey] = state.width
            preferences[WindowHeightKey] = state.height
            preferences[WindowMaximizedKey] = state.isMaximized
        }
    }

    private fun SavedWindowState.hasValidBounds(): Boolean = width > 0 && height > 0

    private companion object {
        val WindowXKey = intPreferencesKey("window_x")
        val WindowYKey = intPreferencesKey("window_y")
        val WindowWidthKey = intPreferencesKey("window_width")
        val WindowHeightKey = intPreferencesKey("window_height")
        val WindowMaximizedKey = booleanPreferencesKey("window_maximized")
    }
}

fun windowStatePreferences(): WindowStatePreferences = WindowStatePreferences(settingsDataStore)
