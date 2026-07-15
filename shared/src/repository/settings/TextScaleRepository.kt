package repository.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal interface TextScaleRepository {
    val userScale: Flow<Float?>

    suspend fun setUserScale(scale: Float)

    suspend fun clearUserScale()
}

internal class DataStoreTextScaleRepository(
    private val dataStore: DataStore<Preferences>,
) : TextScaleRepository {
    override val userScale: Flow<Float?> = dataStore.data.map { preferences ->
        preferences[UserTextScaleKey]
            ?.takeIf { scale -> scale.isFinite() }
            ?.let(::normalizeTextScale)
    }

    override suspend fun setUserScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[UserTextScaleKey] = normalizeTextScale(scale)
        }
    }

    override suspend fun clearUserScale() {
        dataStore.edit { preferences ->
            preferences.remove(UserTextScaleKey)
        }
    }

    private companion object {
        val UserTextScaleKey = floatPreferencesKey("user_text_scale")
    }
}
