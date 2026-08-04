package page.settings.biz

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import repository.agent.GlmModelId
import repository.agent.OpenRouterCookModel
import repository.agent.OpenRouterModelId
import repository.settings.SettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest {
    /** Verifies UI dragging changes only its preview until the interaction finishes. */
    @Test
    fun `ui scale preview applies only after slider finishes`() = runBlocking {
        val store = FakeSettingsStore()
        val viewModel = SettingsViewModel(store)
        withTimeout(1_000) { viewModel.uiScaleState.first { state -> state.isLoaded } }

        viewModel.previewUiScale(2.04f)

        assertNull(viewModel.uiScaleState.value.userScale)
        assertEquals(2f, viewModel.uiScaleState.value.previewScale)

        viewModel.applyPreviewedUiScale()

        assertEquals(2f, viewModel.uiScaleState.value.userScale)
        assertEquals(2f, withTimeout(1_000) { store.uiScaleSaves.receive() })
    }

    /** Verifies resetting UI scale removes its override without changing text scale. */
    @Test
    fun `ui scale reset is independent from text scale`() = runBlocking {
        val store = FakeSettingsStore(userTextScale = 1.5f, userUiScale = 1.8f)
        val viewModel = SettingsViewModel(store)
        withTimeout(1_000) { viewModel.uiScaleState.first { state -> state.isLoaded } }

        assertEquals(1.8f, viewModel.uiScaleState.value.userScale ?: 0f, 0.001f)
        assertEquals(1.8f, viewModel.uiScaleState.value.previewScale, 0.001f)

        viewModel.resetUiScale()

        assertNull(viewModel.uiScaleState.value.userScale)
        assertEquals(1f, viewModel.uiScaleState.value.previewScale)
        assertEquals(1.5f, viewModel.textScaleState.value.userScale)
        assertNull(withTimeout(1_000) { store.uiScaleSaves.receive() })
    }

    /** Verifies text-scale changes do not replace model or UI-scale state. */
    @Test
    fun `text scale updates only text scale state`() = runBlocking {
        val viewModel = SettingsViewModel(FakeSettingsStore())
        withTimeout(1_000) { viewModel.textScaleState.first { state -> state.isLoaded } }
        val modelState = viewModel.modelState.value
        val uiScaleState = viewModel.uiScaleState.value

        viewModel.previewTextScale(1.7f)

        assertEquals(1.7f, viewModel.textScaleState.value.userScale)
        assertEquals(modelState, viewModel.modelState.value)
        assertEquals(uiScaleState, viewModel.uiScaleState.value)
    }

    /** Verifies one failed settings load does not mark unrelated states as failed. */
    @Test
    fun `text scale load failure is isolated`() = runBlocking {
        val viewModel = SettingsViewModel(FakeSettingsStore(failTextScaleLoad = true))
        withTimeout(1_000) { viewModel.modelState.first { state -> state.isLoaded } }
        withTimeout(1_000) { viewModel.textScaleState.first { state -> state.isLoaded } }
        withTimeout(1_000) { viewModel.uiScaleState.first { state -> state.isLoaded } }

        assertTrue(viewModel.textScaleState.value.loadFailed)
        assertEquals(false, viewModel.modelState.value.loadFailed)
        assertEquals(false, viewModel.uiScaleState.value.loadFailed)
    }

    /** Verifies model loading failure preserves independently loaded display settings. */
    @Test
    fun `model load failure is isolated`() = runBlocking {
        val viewModel = SettingsViewModel(FakeSettingsStore(failModelLoad = true))
        withTimeout(1_000) { viewModel.modelState.first { state -> state.isLoaded } }
        withTimeout(1_000) { viewModel.textScaleState.first { state -> state.isLoaded } }
        withTimeout(1_000) { viewModel.uiScaleState.first { state -> state.isLoaded } }

        assertTrue(viewModel.modelState.value.loadFailed)
        assertEquals(false, viewModel.textScaleState.value.loadFailed)
        assertEquals(false, viewModel.uiScaleState.value.loadFailed)
    }

    /** Verifies UI-scale loading failure preserves independently loaded model and text settings. */
    @Test
    fun `ui scale load failure is isolated`() = runBlocking {
        val viewModel = SettingsViewModel(FakeSettingsStore(failUiScaleLoad = true))
        withTimeout(1_000) { viewModel.modelState.first { state -> state.isLoaded } }
        withTimeout(1_000) { viewModel.textScaleState.first { state -> state.isLoaded } }
        withTimeout(1_000) { viewModel.uiScaleState.first { state -> state.isLoaded } }

        assertTrue(viewModel.uiScaleState.value.loadFailed)
        assertEquals(false, viewModel.modelState.value.loadFailed)
        assertEquals(false, viewModel.textScaleState.value.loadFailed)
    }

    /** Verifies failed UI-scale saves retain the applied session value and report the failure. */
    @Test
    fun `ui scale save failure retains session selection`() = runBlocking {
        val store = FakeSettingsStore(failUiScaleSave = true)
        val viewModel = SettingsViewModel(store)
        withTimeout(1_000) { viewModel.uiScaleState.first { state -> state.isLoaded } }

        viewModel.previewUiScale(1.7f)
        viewModel.applyPreviewedUiScale()
        withTimeout(1_000) { viewModel.uiScaleState.first { state -> state.saveFailed } }

        assertEquals(1.7f, viewModel.uiScaleState.value.userScale)
        assertTrue(viewModel.uiScaleState.value.saveFailed)
    }

    /** Verifies rapid UI-scale commits persist the latest requested value last. */
    @Test
    fun `rapid ui scale commits persist latest value last`() = runBlocking {
        val firstSaveStarted = CompletableDeferred<Unit>()
        val allowFirstSave = CompletableDeferred<Unit>()
        val store = FakeSettingsStore(
            beforeUiScaleSave = { scale ->
                if (scale == 1.5f) {
                    firstSaveStarted.complete(Unit)
                    allowFirstSave.await()
                }
            },
        )
        val viewModel = SettingsViewModel(store)
        withTimeout(1_000) { viewModel.uiScaleState.first { state -> state.isLoaded } }

        viewModel.previewUiScale(1.5f)
        viewModel.applyPreviewedUiScale()
        withTimeout(1_000) { firstSaveStarted.await() }
        viewModel.previewUiScale(2f)
        viewModel.applyPreviewedUiScale()
        allowFirstSave.complete(Unit)

        assertEquals(1.5f, withTimeout(1_000) { store.uiScaleSaves.receive() })
        assertEquals(2f, withTimeout(1_000) { store.uiScaleSaves.receive() })
        assertEquals(2f, store.savedUiScale)
    }

    @Test
    fun `loads persisted model and updates selection before persistence completes`() = runBlocking {
        val store = FakeSettingsStore(selectedModelId = OpenRouterModelId)
        val viewModel = SettingsViewModel(store)
        withTimeout(1_000) { viewModel.modelState.first { state -> state.isLoaded } }

        assertEquals(OpenRouterCookModel, viewModel.modelState.value.selectedModel)

        viewModel.selectModel(repository.agent.GlmCookModel)

        assertEquals(GlmModelId, viewModel.modelState.value.selectedModel.id)
        withTimeout(1_000) { store.modelSaved.await() }
        assertEquals(GlmModelId, store.savedModelId)
    }

    @Test
    fun `reports model persistence failure while retaining the session selection`() = runBlocking {
        val store = FakeSettingsStore(failModelSave = true)
        val viewModel = SettingsViewModel(store)
        withTimeout(1_000) { viewModel.modelState.first { state -> state.isLoaded } }

        viewModel.selectModel(OpenRouterCookModel)
        withTimeout(1_000) { viewModel.modelState.first { state -> state.saveFailed } }

        assertEquals(OpenRouterCookModel, viewModel.modelState.value.selectedModel)
        assertTrue(viewModel.modelState.value.saveFailed)
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
        withTimeout(1_000) { viewModel.modelState.first { state -> state.isLoaded } }

        viewModel.selectModel(OpenRouterCookModel)
        withTimeout(1_000) { firstSaveStarted.await() }
        viewModel.selectModel(repository.agent.GlmCookModel)
        allowFirstSave.complete(Unit)

        assertEquals(OpenRouterModelId, withTimeout(1_000) { store.modelSaves.receive() })
        assertEquals(GlmModelId, withTimeout(1_000) { store.modelSaves.receive() })
        assertEquals(GlmModelId, store.savedModelId)
        assertEquals(GlmModelId, viewModel.modelState.value.selectedModel.id)
    }
}

private class FakeSettingsStore(
    selectedModelId: String = GlmModelId,
    userTextScale: Float? = null,
    userUiScale: Float? = null,
    private val failModelSave: Boolean = false,
    private val failUiScaleSave: Boolean = false,
    private val failModelLoad: Boolean = false,
    private val failTextScaleLoad: Boolean = false,
    private val failUiScaleLoad: Boolean = false,
    private val beforeModelSave: suspend (String) -> Unit = {},
    private val beforeUiScaleSave: suspend (Float) -> Unit = {},
) : SettingsStore {
    override val isChatListVisible: Flow<Boolean> = MutableStateFlow(true)
    override val selectedConversationId: Flow<Long?> = MutableStateFlow(null)
    override val userTextScale: Flow<Float?> = if (failTextScaleLoad) {
        flow { error("Storage unavailable") }
    } else {
        MutableStateFlow(userTextScale)
    }
    override val userUiScale: Flow<Float?> = if (failUiScaleLoad) {
        flow { error("Storage unavailable") }
    } else {
        MutableStateFlow(userUiScale)
    }
    override val selectedModelId: Flow<String> = if (failModelLoad) {
        flow { error("Storage unavailable") }
    } else {
        MutableStateFlow(selectedModelId)
    }
    val modelSaved = CompletableDeferred<Unit>()
    val modelSaves = Channel<String>(Channel.UNLIMITED)
    val uiScaleSaves = Channel<Float?>(Channel.UNLIMITED)
    var savedModelId: String? = null
    var savedUiScale: Float? = null

    override suspend fun setUserTextScale(scale: Float) = Unit

    override suspend fun clearUserTextScale() = Unit

    override suspend fun setUserUiScale(scale: Float) {
        beforeUiScaleSave(scale)
        if (failUiScaleSave) error("Storage unavailable")
        savedUiScale = scale
        uiScaleSaves.send(scale)
    }

    override suspend fun clearUserUiScale() {
        uiScaleSaves.send(null)
    }

    override suspend fun setSelectedModelId(modelId: String) {
        beforeModelSave(modelId)
        if (failModelSave) error("Storage unavailable")
        savedModelId = modelId
        modelSaved.complete(Unit)
        modelSaves.send(modelId)
    }

    override suspend fun setChatListVisible(isVisible: Boolean) = Unit

    override suspend fun setSelectedConversationId(conversationId: Long?) = Unit
}
