package settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class TextScalePreferences(
    private val dataStore: DataStore<Preferences>,
) {
    val userScale: Flow<Float?> = dataStore.data.map { preferences ->
        preferences[UserTextScaleKey]
            ?.takeIf { scale -> scale.isFinite() }
            ?.let(::normalizeTextScale)
    }

    suspend fun setUserScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[UserTextScaleKey] = normalizeTextScale(scale)
        }
    }

    suspend fun clearUserScale() {
        dataStore.edit { preferences ->
            preferences.remove(UserTextScaleKey)
        }
    }

    private companion object {
        val UserTextScaleKey = floatPreferencesKey("user_text_scale")
    }
}
