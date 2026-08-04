import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import navigation.ChatRoute
import navigation.SettingsRoute
import page.chat.ChatPage
import page.settings.SettingsPage
import page.settings.biz.SettingsViewModel
import page.settings.biz.selectedTextScale
import page.settings.biz.selectedUiScale
import theme.CookTheme
import org.koin.compose.viewmodel.koinViewModel

private val navigationStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(ChatRoute::class, ChatRoute.serializer())
            subclass(SettingsRoute::class, SettingsRoute.serializer())
        }
    }
}

/** Renders the root application navigation and theme. */
@Composable
@Preview
fun CookApp() {
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val modelState by settingsViewModel.modelState.collectAsState()
    val textScaleState by settingsViewModel.textScaleState.collectAsState()
    val uiScaleState by settingsViewModel.uiScaleState.collectAsState()
    val systemDensity = LocalDensity.current
    val backStack = rememberNavBackStack(navigationStateConfiguration, ChatRoute)

    if (!modelState.isLoaded || !textScaleState.isLoaded || !uiScaleState.isLoaded) {
        MaterialTheme {
            LoadingScreen()
        }
        return
    }

    val selectedScale = selectedTextScale(
        density = systemDensity.density,
        userScale = textScaleState.userScale,
    )
    val uiScale = selectedUiScale(uiScaleState.userScale)

    CookTheme(textScale = selectedScale, uiScale = uiScale) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<ChatRoute> {
                        ChatPage(
                            selectedModel = modelState.selectedModel,
                            onOpenSettings = {
                                if (backStack.lastOrNull() != SettingsRoute) {
                                    backStack.add(SettingsRoute)
                                }
                            },
                        )
                    }
                    entry<SettingsRoute> {
                        SettingsPage(
                            modelState = modelState,
                            textScaleState = textScaleState,
                            uiScaleState = uiScaleState,
                            selectedScale = selectedScale,
                            systemDensity = systemDensity,
                            onTextScaleChanged = settingsViewModel::previewTextScale,
                            onTextScaleChangeFinished = settingsViewModel::savePreviewedTextScale,
                            onResetTextScale = settingsViewModel::resetTextScale,
                            onUiScaleChanged = settingsViewModel::previewUiScale,
                            onUiScaleChangeFinished = settingsViewModel::applyPreviewedUiScale,
                            onResetUiScale = settingsViewModel::resetUiScale,
                            onModelSelected = settingsViewModel::selectModel,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                },
            )
        }
    }
}

/** Renders the initial loading state. */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
