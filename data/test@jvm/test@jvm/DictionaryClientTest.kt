package agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DictionaryClientTest {

    @Test
    fun `accepts a single English word and normalizes it`() {
        assertEquals("mother-in-law", normalizeWord(" Mother-in-law "))
        assertEquals("don't", normalizeWord("DON’T"))
        assertNull(normalizeWord("two words"))
        assertNull(normalizeWord("中文"))
    }

    @Test
    fun `extracts one validated word from tool arguments`() {
        assertEquals("ephemeral", parseToolWord("""{"word":"Ephemeral"}"""))
        assertNull(parseToolWord("""{"word":"two words"}"""))
        assertNull(parseToolWord("not-json"))
    }

    @Test
    fun `normalizes dictionary response for the model`() {
        val output = parseDictionaryResponse(
            requestedWord = "hello",
            body = """
                [{
                  "word": "hello",
                  "phonetics": [{"text": "həˈləʊ", "audio": "https://example.com/audio.mp3"}],
                  "origin": "ignored",
                  "meanings": [{
                    "partOfSpeech": "exclamation",
                    "definitions": [{
                      "definition": "Used as a greeting.",
                      "example": "Hello there!",
                      "synonyms": ["hi"],
                      "antonyms": []
                    }]
                  }]
                }]
            """.trimIndent(),
        )

        val result = Json.parseToJsonElement(output).jsonObject
        val entry = result.getValue("entries").jsonArray.single().jsonObject
        val meaning = entry.getValue("meanings").jsonArray.single().jsonObject
        val definition = meaning.getValue("definitions").jsonArray.single().jsonObject

        assertEquals("success", result.getValue("status").jsonPrimitive.content)
        assertEquals("həˈləʊ", entry.getValue("phonetic").jsonPrimitive.content)
        assertEquals("exclamation", meaning.getValue("partOfSpeech").jsonPrimitive.content)
        assertEquals("Used as a greeting.", definition.getValue("definition").jsonPrimitive.content)
        assertEquals("hi", definition.getValue("synonyms").jsonArray.single().jsonPrimitive.content)
        assertNull(entry["origin"])
    }
}
