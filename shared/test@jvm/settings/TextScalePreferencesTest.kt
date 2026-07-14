package settings

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferencesSerializer
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TextScalePreferencesTest {
    @Test
    fun `user override can be saved and removed`() = runBlocking {
        val directory = Files.createTempDirectory("cook-settings-test").toFile()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = DataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = {
                    directory.resolve("test.preferences_pb").absolutePath.toPath()
                },
            ),
            scope = scope,
        )
        val preferences = TextScalePreferences(dataStore)

        try {
            assertNull(preferences.userScale.first())

            preferences.setUserScale(1.36f)
            val savedScale = assertNotNull(preferences.userScale.first())
            assertEquals(1.4f, savedScale, 0.001f)

            preferences.clearUserScale()
            assertNull(preferences.userScale.first())
        } finally {
            scope.cancel()
            directory.deleteRecursively()
        }
    }
}
