package repository.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import java.io.File
import okio.FileSystem
import okio.Path.Companion.toPath

internal val settingsDataStore: DataStore<Preferences> by lazy {
    val settingsDirectory = File(System.getProperty("user.home"), ".cook")
    DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = PreferencesSerializer,
            producePath = {
                File(settingsDirectory, "cook_settings.preferences_pb").absolutePath.toPath()
            },
        ),
    )
}