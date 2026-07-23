package page.settings.biz

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import repository.agent.GlmModelId
import repository.agent.OpenRouterCookModel
import repository.agent.OpenRouterModelId
import repository.settings.SettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsViewModelTest {
    @Test
    fun `loads persisted model and updates selection before persistence completes`() = runBlocking {
        val store = FakeSettingsStore(selectedModelId = OpenRouterModelId)
        val viewModel = SettingsViewModel(store)
        withTimeout(1_000) { viewModel.uiState.first { state -> state.isLoaded } }

        assertEquals(OpenRouterCookModel, viewModel.uiState.value.selectedModel)

        viewModel.selectModel(repository.agent.GlmCookModel)

        assertEquals(GlmModelId, viewModel.uiState.value.selectedModel.id)
        withTimeout(1_000) { store.modelSaved.await() }
        assertEquals(GlmModelId, store.savedModelId)
    }

    @Test
    fun `reports model persistence failure while retaining the session selection`() = runBlocking {
        val store = FakeSettingsStore(failModelSave = true)
        val viewModel = SettingsViewModel(store)
        withTimeout(1_000) { viewModel.uiState.first { state -> state.isLoaded } }

        viewModel.selectModel(OpenRouterCookModel)
        withTimeout(1_000) { viewModel.uiState.first { state -> state.modelSaveFailed } }

        assertEquals(OpenRouterCookModel, viewModel.uiState.value.selectedModel)
        assertTrue(viewModel.uiState.value.modelSaveFailed)
    }

    @Test
    fun `rapid model changes persist the latest selection last`() = runBlocking {
        val firstSaveStarted = CompletableDeferred<Unit>()
        val allowFirstSave = CompletableDeferred<Unit>()
        val store = FakeSettingsStore(
            beforeModelSave = { modelId ->
                if (modelId == OpenRouterModelId) {
                    firstSaveStarted.complete(Unit)
                    allowFirstSave.await()
                }
            },
        )
        val viewModel = SettingsViewModel(store)
        withTimeout(1_000) { viewModel.uiState.first { state -> state.isLoaded } }

        viewModel.selectModel(OpenRouterCookModel)
        withTimeout(1_000) { firstSaveStarted.await() }
        viewModel.selectModel(repository.agent.GlmCookModel)
        allowFirstSave.complete(Unit)

        assertEquals(OpenRouterModelId, withTimeout(1_000) { store.modelSaves.receive() })
        assertEquals(GlmModelId, withTimeout(1_000) { store.modelSaves.receive() })
        assertEquals(GlmModelId, store.savedModelId)
        assertEquals(GlmModelId, viewModel.uiState.value.selectedModel.id)
    }
}

private class FakeSettingsStore(
    selectedModelId: String = GlmModelId,
    private val failModelSave: Boolean = false,
    private val beforeModelSave: suspend (String) -> Unit = {},
) : SettingsStore {
    override val userScale: Flow<Float?> = MutableStateFlow(null)
    override val selectedModelId: Flow<String> = MutableStateFlow(selectedModelId)
    val modelSaved = CompletableDeferred<Unit>()
    val modelSaves = Channel<String>(Channel.UNLIMITED)
    var savedModelId: String? = null

    override suspend fun setUserScale(scale: Float) = Unit

    override suspend fun clearUserScale() = Unit

    override suspend fun setSelectedModelId(modelId: String) {
        beforeModelSave(modelId)
        if (failModelSave) error("Storage unavailable")
        savedModelId = modelId
        modelSaved.complete(Unit)
        modelSaves.send(modelId)
    }
}
