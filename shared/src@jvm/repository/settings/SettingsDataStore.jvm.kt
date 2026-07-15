package repository.settings

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import java.io.File
import okio.FileSystem
import okio.Path.Companion.toPath

internal val settingsDataStore: DataStore<Preferences> by lazy {
    val settingsDirectory = File(System.getProperty("user.home"), ".cook")
    createSettingsDataStore(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = PreferencesSerializer,
            producePath = {
                File(settingsDirectory, SettingsDataStoreFileName).absolutePath.toPath()
            },
        ),
    )
}

@Composable
internal actual fun rememberSettingsDataStore(): DataStore<Preferences> = settingsDataStore
