package repository.settings

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences

internal const val SettingsDataStoreFileName = "cook_settings.preferences_pb"

internal fun createSettingsDataStore(
    storage: Storage<Preferences>,
): DataStore<Preferences> = DataStoreFactory.create(storage = storage)

@Composable
internal expect fun rememberSettingsDataStore(): DataStore<Preferences>
