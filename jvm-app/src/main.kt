import androidx.compose.ui.window.application
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import settings.SavedWindowState
import settings.WindowStatePreferences
import settings.windowStatePreferences

fun main() {
    configureUtf8ConsoleOutput()
    val preferences = windowStatePreferences()
    val initialWindowState = loadInitialWindowState(preferences)

    application {
        CookWindow(initialWindowState, preferences)
    }
}

private fun loadInitialWindowState(preferences: WindowStatePreferences): SavedWindowState? {
    val savedState = runBlocking(Dispatchers.IO) {
        try {
            preferences.load()
        } catch (error: Exception) {
            System.err.println("Unable to load window state: ${error.message}")
            null
        }
    } ?: return null
    val (workAreas, defaultWorkArea) = currentScreenWorkAreas()

    return savedState.clampToVisibleScreen(workAreas, defaultWorkArea)
}

private fun configureUtf8ConsoleOutput() {
    System.setOut(PrintStream(FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8))
    System.setErr(PrintStream(FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8))
}
