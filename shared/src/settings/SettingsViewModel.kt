package settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class SettingsUiState(
    val isLoaded: Boolean = false,
    val userScale: Float? = null,
    val loadFailed: Boolean = false,
    val saveFailed: Boolean = false,
)

internal class SettingsViewModel(
    private val preferences: TextScalePreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { preferences.userScale.first() }
                .onSuccess { userScale ->
                    _uiState.update { state ->
                        state.copy(
                            isLoaded = true,
                            userScale = userScale,
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

    fun previewScale(scale: Float) {
        _uiState.update { state ->
            state.copy(userScale = normalizeTextScale(scale), saveFailed = false)
        }
    }

    fun savePreviewedScale() {
        val scale = _uiState.value.userScale ?: return
        viewModelScope.launch {
            runCatching { preferences.setUserScale(scale) }
                .onFailure {
                    _uiState.update { state -> state.copy(saveFailed = true) }
                }
        }
    }

    fun resetToDeviceDefault() {
        _uiState.update { state ->
            state.copy(userScale = null, saveFailed = false)
        }
        viewModelScope.launch {
            runCatching { preferences.clearUserScale() }
                .onFailure {
                    _uiState.update { state -> state.copy(saveFailed = true) }
                }
        }
    }
}
