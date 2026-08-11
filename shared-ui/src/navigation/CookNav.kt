package navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CookNav : NavKey

@Serializable
internal data object ChatNav : CookNav

@Serializable
internal data object SettingsNav : CookNav
