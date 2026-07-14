import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cook.generated.resources.Res
import cook.generated.resources.app_name
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import org.jetbrains.compose.resources.stringResource
import theme.CookDimensions

fun main() {
    configureUtf8ConsoleOutput()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(
                width = CookDimensions.windowWidth,
                height = CookDimensions.windowHeight,
            ),
            undecorated = false,
            title = stringResource(Res.string.app_name),
        ) {
            CookApp()
        }
    }
}

private fun configureUtf8ConsoleOutput() {
    System.setOut(PrintStream(FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8))
    System.setErr(PrintStream(FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8))
}
