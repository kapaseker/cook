package page.settings.biz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import repository.settings.SettingsStore
import repository.settings.normalizeTextScale
import repository.agent.CookModel
import repository.agent.AvailableCookModels
import repository.agent.GlmCookModel
import repository.agent.cookModelById
import java.util.concurrent.atomic.AtomicLong

internal data class SettingsUiState(
    val isLoaded: Boolean = false,
    val userScale: Float? = null,
    val availableModels: List<CookModel> = AvailableCookModels,
    val selectedModel: CookModel = GlmCookModel,
    val loadFailed: Boolean = false,
    val saveFailed: Boolean = false,
    val modelSaveFailed: Boolean = false,
)

internal class SettingsViewModel(
    private val repository: SettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val modelSaveMutex = Mutex()
    private val modelSaveGeneration = AtomicLong()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                repository.userScale.combine(repository.selectedModelId) { userScale, modelId ->
                    userScale to cookModelById(modelId)
                }.first()
            }
                .onSuccess { (userScale, selectedModel) ->
                    _uiState.update { state ->
                        state.copy(
                            isLoaded = true,
                            userScale = userScale,
                            selectedModel = selectedModel,
                            loadFailed = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(isLoaded = true, userScale = null, loadFailed = true)
                    }
                }
        }
    }

    /** Updates the in-memory text-scale preview. */
    fun previewScale(scale: Float) {
        _uiState.update { state ->
            state.copy(userScale = normalizeTextScale(scale), saveFailed = false)
        }
    }

    /** Persists the currently previewed text scale. */
    fun savePreviewedScale() {
        val scale = _uiState.value.userScale ?: return
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { repository.setUserScale(scale) }
                .onFailure {
                    _uiState.update { state -> state.copy(saveFailed = true) }
                }
        }
    }

    /** Clears the override and restores the device text scale. */
    fun resetToDeviceDefault() {
        _uiState.update { state ->
            state.copy(userScale = null, saveFailed = false)
        }
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { repository.clearUserScale() }
                .onFailure {
                    _uiState.update { state -> state.copy(saveFailed = true) }
                }
        }
    }

    /** Selects the model for subsequent chat requests and persists the choice. */
    fun selectModel(model: CookModel) {
        val supportedModel = cookModelById(model.id)
        val generation = modelSaveGeneration.incrementAndGet()
        _uiState.update { state ->
            state.copy(selectedModel = supportedModel, modelSaveFailed = false)
        }
        viewModelScope.launch(Dispatchers.Default) {
            modelSaveMutex.withLock {
                if (generation != modelSaveGeneration.get()) return@withLock
                runCatching { repository.setSelectedModelId(supportedModel.id) }
                    .onFailure {
                        if (generation == modelSaveGeneration.get()) {
                            _uiState.update { state -> state.copy(modelSaveFailed = true) }
                        }
                    }
                }
            }
    }
}
