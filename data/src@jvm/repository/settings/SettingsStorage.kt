package repository.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import repository.agent.cookModelById

internal class SettingsStorage(
    private val dataStore: DataStore<Preferences>,
) : SettingsStore {
    override val userTextScale: Flow<Float?> = dataStore.data.map { preferences ->
        preferences[UserTextScaleKey]
            ?.takeIf(Float::isFinite)
            ?.let(::normalizeDisplayScale)
    }
    override val userUiScale: Flow<Float?> = dataStore.data.map { preferences ->
        preferences[UserUiScaleKey]
            ?.takeIf(Float::isFinite)
            ?.let(::normalizeDisplayScale)
    }
    override val selectedModelId: Flow<String> = dataStore.data.map { preferences ->
        cookModelById(preferences[SelectedModelIdKey]).id
    }
    override val isChatListVisible: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ChatListVisibleKey] ?: true
    }
    override val selectedConversationId: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[SelectedConversationIdKey]
    }

    /** Persists a normalized user-selected text scale. */
    override suspend fun setUserTextScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[UserTextScaleKey] = normalizeDisplayScale(scale)
        }
    }

    /** Removes the persisted text-scale override. */
    override suspend fun clearUserTextScale() {
        dataStore.edit { preferences -> preferences.remove(UserTextScaleKey) }
    }

    /** Persists a normalized user-selected UI scale. */
    override suspend fun setUserUiScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[UserUiScaleKey] = normalizeDisplayScale(scale)
        }
    }

    /** Removes the persisted UI-scale override. */
    override suspend fun clearUserUiScale() {
        dataStore.edit { preferences -> preferences.remove(UserUiScaleKey) }
    }

    /** Persists a supported chat-model identifier. */
    override suspend fun setSelectedModelId(modelId: String) {
        dataStore.edit { preferences ->
            preferences[SelectedModelIdKey] = cookModelById(modelId).id
        }
    }

    override suspend fun setChatListVisible(isVisible: Boolean) {
        dataStore.edit { preferences -> preferences[ChatListVisibleKey] = isVisible }
    }

    override suspend fun setSelectedConversationId(conversationId: Long?) {
        dataStore.edit { preferences ->
            if (conversationId == null) {
                preferences.remove(SelectedConversationIdKey)
            } else {
                preferences[SelectedConversationIdKey] = conversationId
            }
        }
    }

    private companion object {
        val UserTextScaleKey = floatPreferencesKey("user_text_scale")
        val UserUiScaleKey = floatPreferencesKey("user_ui_scale")
        val SelectedModelIdKey = stringPreferencesKey("selected_model_id")
        val ChatListVisibleKey = booleanPreferencesKey("chat_list_visible")
        val SelectedConversationIdKey = longPreferencesKey("selected_conversation_id")
    }
}
