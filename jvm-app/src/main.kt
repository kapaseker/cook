import androidx.compose.ui.window.application
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import settings.SavedWindowState
import settings.WindowStateStore
import di.platformDataModule
import di.uiModule
import org.koin.core.context.startKoin
import org.koin.core.context.GlobalContext

fun main() {
    configureUtf8ConsoleOutput()
    startKoin { modules(uiModule, platformDataModule) }
    val windowStateStore = GlobalContext.get().get<WindowStateStore>()
    val initialWindowState = loadInitialWindowState(windowStateStore)

    application {
        CookWindow(initialWindowState, windowStateStore)
    }
}

private fun loadInitialWindowState(store: WindowStateStore): SavedWindowState? {
    val savedState = runBlocking(Dispatchers.IO) {
        try {
            store.load()
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
