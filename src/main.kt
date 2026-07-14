import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import chat.ChatScreen
import chat.ChatViewModel
import chat.chatStrings
import cook.generated.resources.Res
import cook.generated.resources.app_name
import theme.CookDimensions
import theme.CookTheme
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import org.jetbrains.compose.resources.stringResource

@Composable
@Preview
fun App() {
    val strings = chatStrings()
    val viewModel = viewModel { ChatViewModel(strings = strings) }
    val state by viewModel.uiState.collectAsState()

    CookTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            ChatScreen(
                state = state,
                onDraftChanged = viewModel::onDraftChanged,
                onSend = viewModel::sendMessage,
            )
        }
    }
}

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
            App()
        }
    }
}

private fun configureUtf8ConsoleOutput() {
    System.setOut(PrintStream(FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8))
    System.setErr(PrintStream(FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8))
}
