package navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CookNavTest {

    /** Verifies that chat navigation entry survives serialization round trip. */
    @Test
    fun `chat navigation entry survives serialization round trip`() {
        val encoded = Json.encodeToString<CookNav>(ChatNav)

        assertEquals(ChatNav, Json.decodeFromString<CookNav>(encoded))
    }

    /** Verifies that settings navigation entry survives serialization round trip. */
    @Test
    fun `settings navigation entry survives serialization round trip`() {
        val encoded = Json.encodeToString<CookNav>(SettingsNav)

        assertEquals(SettingsNav, Json.decodeFromString<CookNav>(encoded))
    }
}
