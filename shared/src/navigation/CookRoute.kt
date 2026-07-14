package navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CookRoute : NavKey

@Serializable
internal data object ChatRoute : CookRoute

@Serializable
internal data object SettingsRoute : CookRoute
