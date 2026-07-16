package navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CookRouteTest {

    @Test
    fun `chat route survives serialization round trip`() {
        val encoded = Json.encodeToString<CookRoute>(ChatRoute)

        assertEquals(ChatRoute, Json.decodeFromString<CookRoute>(encoded))
    }

    @Test
    fun `settings route survives serialization round trip`() {
        val encoded = Json.encodeToString<CookRoute>(SettingsRoute)

        assertEquals(SettingsRoute, Json.decodeFromString<CookRoute>(encoded))
    }
}
