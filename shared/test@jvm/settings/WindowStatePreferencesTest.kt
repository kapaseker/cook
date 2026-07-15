package settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WindowStatePreferencesTest {
    @Test
    fun `window state can be saved and loaded`() = withPreferences { dataStore, preferences ->
        val expected = SavedWindowState(
            x = -1280,
            y = 40,
            width = 1200,
            height = 800,
            isMaximized = true,
        )

        preferences.save(expected)

        assertEquals(expected, preferences.load())
        dataStore.edit { stored -> stored.remove(intPreferencesKey("window_height")) }
        assertNull(preferences.load())
        dataStore.edit { stored ->
            stored[intPreferencesKey("window_width")] = 0
            stored[intPreferencesKey("window_height")] = 720
        }
        assertNull(preferences.load())
    }

    @Test
    fun `invalid window dimensions are rejected`() = withPreferences { _, preferences ->
        assertFailsWith<IllegalArgumentException> {
            preferences.save(
                SavedWindowState(
                    x = 0,
                    y = 0,
                    width = 0,
                    height = 720,
                    isMaximized = false,
                ),
            )
        }
    }

    private fun withPreferences(
        test: suspend (DataStore<Preferences>, WindowStatePreferences) -> Unit,
    ) = runBlocking {
        val directory = Files.createTempDirectory("cook-window-settings-test").toFile()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = createDataStore(directory, scope)

        try {
            test(dataStore, WindowStatePreferences(dataStore))
        } finally {
            scope.cancel()
            directory.deleteRecursively()
        }
    }

    private fun createDataStore(
        directory: File,
        scope: CoroutineScope,
    ): DataStore<Preferences> = DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = PreferencesSerializer,
            producePath = {
                directory.resolve("test.preferences_pb").absolutePath.toPath()
            },
        ),
        scope = scope,
    )
}
