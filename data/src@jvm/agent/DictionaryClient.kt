package agent

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val DICTIONARY_ENDPOINT =
    "https://api.dictionaryapi.dev/api/v2/entries/en"

private val ENGLISH_WORD = Regex("^[A-Za-z]+(?:['’-][A-Za-z]+)*$")

interface DictionaryClient {
    /** Looks up one normalized English word and returns a serialized result. */
    suspend fun lookup(word: String): String
}

class DictionaryApiClient(
    private val httpClient: HttpClient = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
    },
) : DictionaryClient {

    /** Looks up one normalized English word and returns a serialized result. */
    override suspend fun lookup(word: String): String {
        val normalizedWord = normalizeWord(word)
            ?: return toolError(
                status = "invalid_word",
                word = word,
                message = "The tool accepts exactly one English word.",
            )

        return runCatching {
            httpClient.get(DICTIONARY_ENDPOINT) {
                url { appendPathSegments(normalizedWord) }
                accept(ContentType.Application.Json)
            }
        }.fold(
            onSuccess = { response ->
                when (response.status) {
                    HttpStatusCode.OK -> parseDictionaryResponse(normalizedWord, response.bodyAsText())
                    HttpStatusCode.NotFound -> toolError(
                        status = "not_found",
                        word = normalizedWord,
                        message = "No dictionary entry was found. Ask the user to check the spelling.",
                    )

                    else -> toolError(
                        status = "unavailable",
                        word = normalizedWord,
                        message = "The dictionary service is temporarily unavailable.",
                    )
                }
            },
            onFailure = {
                toolError(
                    status = "unavailable",
                    word = normalizedWord,
                    message = "The dictionary service is temporarily unavailable.",
                )
            },
        )
    }
}

/** Normalizes and validates a single English dictionary word. */
internal fun normalizeWord(word: String): String? {
    val normalized = word.trim().replace('’', '\'').lowercase()
    return normalized.takeIf { it.matches(ENGLISH_WORD) }
}

/** Converts a dictionary API response into the tool result schema. */
internal fun parseDictionaryResponse(requestedWord: String, body: String): String {
    return runCatching {
        val entries = Json.parseToJsonElement(body).jsonArray
        require(entries.isNotEmpty())

        buildJsonObject {
            put("status", "success")
            put("requestedWord", requestedWord)
            put("entries", buildJsonArray {
                entries.forEach { entryElement ->
                    add(normalizeEntry(entryElement.jsonObject))
                }
            })
        }.toString()
    }.getOrElse {
        toolError(
            status = "unavailable",
            word = requestedWord,
            message = "The dictionary returned an invalid response.",
        )
    }
}

/** Converts one dictionary entry into the supported result schema. */
private fun normalizeEntry(entry: JsonObject): JsonObject = buildJsonObject {
    putIfPresent("word", entry.string("word"))
    putIfPresent("phonetic", entry.string("phonetic") ?: firstPhonetic(entry["phonetics"]))
    put("meanings", buildJsonArray {
        entry.array("meanings").forEach { meaningElement ->
            val meaning = meaningElement.jsonObject
            add(buildJsonObject {
                putIfPresent("partOfSpeech", meaning.string("partOfSpeech"))
                put("definitions", buildJsonArray {
                    meaning.array("definitions").forEach { definitionElement ->
                        val definition = definitionElement.jsonObject
                        add(buildJsonObject {
                            putIfPresent("definition", definition.string("definition"))
                            putIfPresent("example", definition.string("example"))
                            putStringArray("synonyms", definition.array("synonyms"))
                            putStringArray("antonyms", definition.array("antonyms"))
                        })
                    }
                })
                putStringArray("synonyms", meaning.array("synonyms"))
                putStringArray("antonyms", meaning.array("antonyms"))
            })
        }
    })
}

/** Returns the first available phonetic transcription. */
private fun firstPhonetic(element: JsonElement?): String? {
    return (element as? JsonArray)
        ?.firstNotNullOfOrNull { runCatching { it.jsonObject.string("text") }.getOrNull() }
}

/** Returns a nullable string property from this JSON object. */
private fun JsonObject.string(name: String): String? =
    this[name]?.jsonPrimitive?.contentOrNull

/** Returns an array property or an empty array when it is absent. */
private fun JsonObject.array(name: String): JsonArray =
    this[name] as? JsonArray ?: JsonArray(emptyList())

/** Adds a nonblank string property to this JSON object builder. */
private fun kotlinx.serialization.json.JsonObjectBuilder.putIfPresent(name: String, value: String?) {
    if (!value.isNullOrBlank()) put(name, value)
}

/** Adds a nonempty string array property to this JSON object builder. */
private fun kotlinx.serialization.json.JsonObjectBuilder.putStringArray(
    name: String,
    values: JsonArray,
) {
    val strings = values.mapNotNull { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
    if (strings.isNotEmpty()) {
        put(name, buildJsonArray { strings.forEach { add(JsonPrimitive(it)) } })
    }
}

/** Creates a serialized dictionary-tool error result. */
private fun toolError(status: String, word: String, message: String): String =
    buildJsonObject {
        put("status", status)
        put("word", word)
        put("message", message)
    }.toString()
