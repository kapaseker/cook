package page.settings.biz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import repository.agent.CookModel
import repository.agent.AvailableCookModels
import repository.agent.GlmCookModel
import repository.agent.cookModelById
import repository.settings.DefaultUiScale
import repository.settings.SettingsStore
import repository.settings.normalizeDisplayScale
import java.util.concurrent.atomic.AtomicLong

internal data class ModelSettingsUiState(
    val isLoaded: Boolean = false,
    val availableModels: List<CookModel> = AvailableCookModels,
    val selectedModel: CookModel = GlmCookModel,
    val loadFailed: Boolean = false,
    val saveFailed: Boolean = false,
)

internal data class TextScaleUiState(
    val isLoaded: Boolean = false,
    val userScale: Float? = null,
    val loadFailed: Boolean = false,
    val saveFailed: Boolean = false,
)

internal data class UiScaleUiState(
    val isLoaded: Boolean = false,
    val userScale: Float? = null,
    val previewScale: Float = DefaultUiScale,
    val loadFailed: Boolean = false,
    val saveFailed: Boolean = false,
)

internal class SettingsViewModel(
    private val repository: SettingsStore,
) : ViewModel() {
    private val _modelState = MutableStateFlow(ModelSettingsUiState())
    val modelState: StateFlow<ModelSettingsUiState> = _modelState.asStateFlow()
    private val _textScaleState = MutableStateFlow(TextScaleUiState())
    val textScaleState: StateFlow<TextScaleUiState> = _textScaleState.asStateFlow()
    private val _uiScaleState = MutableStateFlow(UiScaleUiState())
    val uiScaleState: StateFlow<UiScaleUiState> = _uiScaleState.asStateFlow()
    private val modelSaveMutex = Mutex()
    private val uiScaleSaveMutex = Mutex()
    private val modelSaveGeneration = AtomicLong()
    private val uiScaleSaveGeneration = AtomicLong()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { cookModelById(repository.selectedModelId.first()) }
                .onSuccess { selectedModel ->
                    _modelState.value = ModelSettingsUiState(
                        isLoaded = true,
                        selectedModel = selectedModel,
                    )
                }
                .onFailure {
                    _modelState.value = ModelSettingsUiState(isLoaded = true, loadFailed = true)
                }
        }
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { repository.userTextScale.first() }
                .onSuccess { userTextScale ->
                    _textScaleState.value = TextScaleUiState(
                        isLoaded = true,
                        userScale = userTextScale,
                    )
                }
                .onFailure {
                    _textScaleState.value = TextScaleUiState(isLoaded = true, loadFailed = true)
                }
        }
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { repository.userUiScale.first() }
                .onSuccess { userUiScale ->
                    _uiScaleState.value = UiScaleUiState(
                        isLoaded = true,
                        userScale = userUiScale,
                        previewScale = selectedUiScale(userUiScale),
                    )
                }
                .onFailure {
                    _uiScaleState.value = UiScaleUiState(isLoaded = true, loadFailed = true)
                }
        }
    }

    /** Updates the in-memory text-scale preview. */
    fun previewTextScale(scale: Float) {
        _textScaleState.update { state ->
            state.copy(userScale = normalizeDisplayScale(scale), saveFailed = false)
        }
    }

    /** Persists the currently previewed text scale. */
    fun savePreviewedTextScale() {
        val scale = _textScaleState.value.userScale ?: return
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { repository.setUserTextScale(scale) }
                .onFailure {
                    _textScaleState.update { state -> state.copy(saveFailed = true) }
                }
        }
    }

    /** Clears the override and restores the device text scale. */
    fun resetTextScale() {
        _textScaleState.update { state ->
            state.copy(userScale = null, saveFailed = false)
        }
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { repository.clearUserTextScale() }
                .onFailure {
                    _textScaleState.update { state -> state.copy(saveFailed = true) }
                }
        }
    }

    /** Updates only the focused UI-scale preview until the slider interaction finishes. */
    fun previewUiScale(scale: Float) {
        _uiScaleState.update { state ->
            state.copy(previewScale = normalizeDisplayScale(scale), saveFailed = false)
        }
    }

    /** Applies and persists the currently previewed UI scale. */
    fun applyPreviewedUiScale() {
        val scale = _uiScaleState.value.previewScale
        _uiScaleState.update { state -> state.copy(userScale = scale, saveFailed = false) }
        persistUiScale(scale)
    }

    /** Restores the neutral UI scale and removes its persisted override. */
    fun resetUiScale() {
        _uiScaleState.update { state ->
            state.copy(userScale = null, previewScale = DefaultUiScale, saveFailed = false)
        }
        persistUiScale(null)
    }

    /** Serializes UI-scale writes and suppresses failures from superseded requests. */
    private fun persistUiScale(scale: Float?) {
        val generation = uiScaleSaveGeneration.incrementAndGet()
        viewModelScope.launch(Dispatchers.Default) {
            uiScaleSaveMutex.withLock {
                if (generation != uiScaleSaveGeneration.get()) return@withLock
                val result = runCatching {
                    if (scale == null) {
                        repository.clearUserUiScale()
                    } else {
                        repository.setUserUiScale(scale)
                    }
                }
                if (result.isFailure && generation == uiScaleSaveGeneration.get()) {
                    _uiScaleState.update { state -> state.copy(saveFailed = true) }
                }
            }
        }
    }

    /** Selects the model for subsequent chat requests and persists the choice. */
    fun selectModel(model: CookModel) {
        val supportedModel = cookModelById(model.id)
        val generation = modelSaveGeneration.incrementAndGet()
        _modelState.update { state ->
            state.copy(selectedModel = supportedModel, saveFailed = false)
        }
        viewModelScope.launch(Dispatchers.Default) {
            modelSaveMutex.withLock {
                if (generation != modelSaveGeneration.get()) return@withLock
                runCatching { repository.setSelectedModelId(supportedModel.id) }
                    .onFailure {
                        if (generation == modelSaveGeneration.get()) {
                            _modelState.update { state -> state.copy(saveFailed = true) }
                        }
                    }
                }
            }
    }
}
