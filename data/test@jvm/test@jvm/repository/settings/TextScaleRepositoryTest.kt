package repository.settings

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
import repository.agent.GlmModelId
import repository.agent.OpenRouterModelId

class SettingsStorageTest {
    /** Verifies that text and UI overrides can be saved and removed independently. */
    @Test
    fun `display overrides can be saved and removed independently`() = runBlocking {
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
        val preferences = SettingsStorage(dataStore)

        try {
            assertNull(preferences.userTextScale.first())
            assertNull(preferences.userUiScale.first())

            preferences.setUserTextScale(1.36f)
            preferences.setUserUiScale(1.84f)
            val savedTextScale = assertNotNull(preferences.userTextScale.first())
            val savedUiScale = assertNotNull(preferences.userUiScale.first())
            assertEquals(1.4f, savedTextScale, 0.001f)
            assertEquals(1.8f, savedUiScale, 0.001f)

            preferences.clearUserTextScale()
            assertNull(preferences.userTextScale.first())
            assertEquals(1.8f, preferences.userUiScale.first() ?: 0f, 0.001f)

            preferences.clearUserUiScale()
            assertNull(preferences.userUiScale.first())
        } finally {
            scope.cancel()
            directory.deleteRecursively()
        }
    }

    /** Verifies that model selection defaults safely and survives persistence. */
    @Test
    fun `model selection defaults to GLM and persists supported values`() = runBlocking {
        val directory = Files.createTempDirectory("cook-model-settings-test").toFile()
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
        val preferences = SettingsStorage(dataStore)

        try {
            assertEquals(GlmModelId, preferences.selectedModelId.first())

            preferences.setSelectedModelId(OpenRouterModelId)
            assertEquals(OpenRouterModelId, preferences.selectedModelId.first())

            preferences.setSelectedModelId("unknown-model")
            assertEquals(GlmModelId, preferences.selectedModelId.first())
        } finally {
            scope.cancel()
            directory.deleteRecursively()
        }
    }

    @Test
    fun `chat list settings have safe defaults and persist`() = runBlocking {
        val directory = Files.createTempDirectory("cook-chat-settings-test").toFile()
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
        val preferences = SettingsStorage(dataStore)

        try {
            assertEquals(true, preferences.isChatListVisible.first())
            assertNull(preferences.selectedConversationId.first())

            preferences.setChatListVisible(false)
            preferences.setSelectedConversationId(42L)

            assertEquals(false, preferences.isChatListVisible.first())
            assertEquals(42L, preferences.selectedConversationId.first())

            preferences.setSelectedConversationId(null)
            assertNull(preferences.selectedConversationId.first())
        } finally {
            scope.cancel()
            directory.deleteRecursively()
        }
    }
}
