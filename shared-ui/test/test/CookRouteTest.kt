package navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CookRouteTest {

    /** Verifies that chat route survives serialization round trip. */
    @Test
    fun `chat route survives serialization round trip`() {
        val encoded = Json.encodeToString<CookRoute>(ChatRoute)

        assertEquals(ChatRoute, Json.decodeFromString<CookRoute>(encoded))
    }

    /** Verifies that settings route survives serialization round trip. */
    @Test
    fun `settings route survives serialization round trip`() {
        val encoded = Json.encodeToString<CookRoute>(SettingsRoute)

        assertEquals(SettingsRoute, Json.decodeFromString<CookRoute>(encoded))
    }
}
