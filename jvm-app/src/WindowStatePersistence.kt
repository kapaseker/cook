import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import cook.generated.resources.Res
import cook.generated.resources.app_name
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import settings.SavedWindowState
import settings.WindowStateStore
import theme.CookDimensions
import kotlin.math.roundToInt

private const val WindowSaveDebounceMillis = 500L

/** Renders the desktop window and coordinates persisted window state. */
@Composable
internal fun ApplicationScope.CookWindow(
    initialState: SavedWindowState?,
    store: WindowStateStore,
) {
    val windowState = rememberWindowState(
        placement = if (initialState?.isMaximized == true) {
            WindowPlacement.Maximized
        } else {
            WindowPlacement.Floating
        },
        isMinimized = false,
        position = initialState?.let { state -> WindowPosition(state.x.dp, state.y.dp) }
            ?: WindowPosition.PlatformDefault,
        width = initialState?.width?.dp ?: CookDimensions.windowWidth,
        height = initialState?.height?.dp ?: CookDimensions.windowHeight,
    )
    val coroutineScope = rememberCoroutineScope()
    val accumulator = remember(initialState) { WindowStateAccumulator(initialState) }
    val saveCoordinator = remember(store, coroutineScope) {
        WindowStateSaveCoordinator(store, coroutineScope)
    }
    val closeCoordinator = remember(saveCoordinator, coroutineScope) {
        WindowCloseCoordinator(coroutineScope, saveCoordinator, ::exitApplication)
    }

    LaunchedEffect(windowState, accumulator, saveCoordinator) {
        snapshotFlow { windowState.toObservation() }
            .collect { observation ->
                accumulator.observe(observation)?.let(saveCoordinator::submit)
            }
    }

    Window(
        onCloseRequest = closeCoordinator::close,
        state = windowState,
        undecorated = false,
        title = stringResource(Res.string.app_name),
    ) {
        CookApp()
    }
}

internal data class WindowObservation(
    val placement: WindowPlacement,
    val isMinimized: Boolean,
    val bounds: SavedWindowState?,
)

internal class WindowStateAccumulator(initialState: SavedWindowState?) {
    private var normalBounds = initialState?.copy(isMaximized = false)
    private var lastObservedState = initialState
    private var hasStableInitialState = false

    /** Records an observation and returns a state when it should be persisted. */
    fun observe(observation: WindowObservation): SavedWindowState? {
        if (observation.isMinimized) return null

        if (observation.placement == WindowPlacement.Floating) {
            normalBounds = observation.bounds?.copy(isMaximized = false) ?: normalBounds
        }

        val currentState = normalBounds?.copy(
            isMaximized = observation.placement == WindowPlacement.Maximized,
        ) ?: return null

        if (!hasStableInitialState) {
            hasStableInitialState = true
            lastObservedState = currentState
            return null
        }

        if (currentState == lastObservedState) return null
        lastObservedState = currentState
        return currentState
    }
}

private class WindowStateSaveCoordinator(
    private val store: WindowStateStore,
    private val coroutineScope: CoroutineScope,
) {
    private var pendingState: SavedWindowState? = null
    private var debounceJob: Job? = null

    /** Queues a window state for debounced persistence. */
    fun submit(state: SavedWindowState) {
        pendingState = state
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(WindowSaveDebounceMillis)
            persistPendingState()
        }
    }

    /** Persists any queued window state immediately. */
    suspend fun flush() {
        debounceJob?.cancelAndJoin()
        debounceJob = null
        persistPendingState()
    }

    /** Writes the queued state and clears it after a successful save. */
    private suspend fun persistPendingState() {
        val state = pendingState ?: return
        try {
            withContext(Dispatchers.IO) {
                store.save(state)
            }
            if (pendingState == state) {
                pendingState = null
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            System.err.println("Unable to save window state: ${error.message}")
        }
    }
}

private class WindowCloseCoordinator(
    private val coroutineScope: CoroutineScope,
    private val saveCoordinator: WindowStateSaveCoordinator,
    private val exitApplication: () -> Unit,
) {
    private var isClosing = false

    /** Flushes pending state once while the application window closes. */
    fun close() {
        if (isClosing) return
        isClosing = true
        coroutineScope.launch {
            saveCoordinator.flush()
            exitApplication()
        }
    }
}

/** Converts this Compose window state into a persistence observation. */
private fun WindowState.toObservation(): WindowObservation {
    val x = position.x.value
    val y = position.y.value
    val width = size.width.value
    val height = size.height.value
    val hasValidBounds = position.isSpecified &&
        size.width.isSpecified &&
        size.height.isSpecified &&
        x.isFinite() &&
        y.isFinite() &&
        width.isFinite() &&
        height.isFinite() &&
        width > 0f &&
        height > 0f

    return WindowObservation(
        placement = placement,
        isMinimized = isMinimized,
        bounds = if (hasValidBounds) {
            SavedWindowState(
                x = x.roundToInt(),
                y = y.roundToInt(),
                width = width.roundToInt(),
                height = height.roundToInt(),
                isMaximized = false,
            )
        } else {
            null
        },
    )
}
