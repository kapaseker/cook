package repository.settings

import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

const val MinimumDisplayScale = 0.5f
const val MaximumDisplayScale = 4.0f
const val DisplayScaleStep = 0.1f
const val DisplayScaleSliderSteps = 34
const val DefaultUiScale = 1.0f

interface SettingsStore {
    val userTextScale: Flow<Float?>
    val userUiScale: Flow<Float?>
    val selectedModelId: Flow<String>
    val isChatListVisible: Flow<Boolean>
    val selectedConversationId: Flow<Long?>

    /** Persists a normalized user-selected text scale. */
    suspend fun setUserTextScale(scale: Float)

    /** Removes the persisted text-scale override. */
    suspend fun clearUserTextScale()

    /** Persists a normalized user-selected UI scale. */
    suspend fun setUserUiScale(scale: Float)

    /** Removes the persisted UI-scale override. */
    suspend fun clearUserUiScale()

    /** Persists the selected chat model. */
    suspend fun setSelectedModelId(modelId: String)

    /** Persists whether the chat list is visible. */
    suspend fun setChatListVisible(isVisible: Boolean)

    /** Persists the selected conversation, or clears a stale selection. */
    suspend fun setSelectedConversationId(conversationId: Long?)
}

/** Rounds and bounds a display scale to the supported range. */
fun normalizeDisplayScale(value: Float): Float {
    if (!value.isFinite()) return MinimumDisplayScale

    return ((value / DisplayScaleStep).roundToInt() * DisplayScaleStep)
        .coerceIn(MinimumDisplayScale, MaximumDisplayScale)
}
