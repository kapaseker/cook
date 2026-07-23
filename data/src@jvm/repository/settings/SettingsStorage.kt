package repository.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import repository.agent.cookModelById

internal class SettingsStorage(
    private val dataStore: DataStore<Preferences>,
) : SettingsStore {
    override val userScale: Flow<Float?> = dataStore.data.map { preferences ->
        preferences[UserTextScaleKey]
            ?.takeIf(Float::isFinite)
            ?.let(::normalizeTextScale)
    }
    override val selectedModelId: Flow<String> = dataStore.data.map { preferences ->
        cookModelById(preferences[SelectedModelIdKey]).id
    }

    /** Persists a normalized user-selected text scale. */
    override suspend fun setUserScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[UserTextScaleKey] = normalizeTextScale(scale)
        }
    }

    /** Removes the persisted text-scale override. */
    override suspend fun clearUserScale() {
        dataStore.edit { preferences -> preferences.remove(UserTextScaleKey) }
    }

    /** Persists a supported chat-model identifier. */
    override suspend fun setSelectedModelId(modelId: String) {
        dataStore.edit { preferences ->
            preferences[SelectedModelIdKey] = cookModelById(modelId).id
        }
    }

    private companion object {
        val UserTextScaleKey = floatPreferencesKey("user_text_scale")
        val SelectedModelIdKey = stringPreferencesKey("selected_model_id")
    }
}
