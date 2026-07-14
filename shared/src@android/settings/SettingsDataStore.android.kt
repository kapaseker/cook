package settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import java.io.File
import okio.FileSystem
import okio.Path.Companion.toPath

private val dataStoreLock = Any()

@Volatile
private var settingsDataStore: DataStore<Preferences>? = null

@Composable
internal actual fun rememberSettingsDataStore(): DataStore<Preferences> {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        settingsDataStore ?: synchronized(dataStoreLock) {
            settingsDataStore ?: createSettingsDataStore(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesSerializer,
                    producePath = {
                        File(context.filesDir, SettingsDataStoreFileName).absolutePath.toPath()
                    },
                ),
            ).also { created -> settingsDataStore = created }
        }
    }
}
