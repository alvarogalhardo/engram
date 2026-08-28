package com.alvarogalhardo.engram.importer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pure parser (no android.* dependencies) for the JSON blobs of a legacy Anki
 * collection: col.models, col.decks, and the `media` file at the zip root.
 */
object ApkgParser {
    data class Model(
        val id: Long,
        val name: String,
        val isCloze: Boolean,
        val fieldNames: List<String>,
    )

    fun parseModels(json: String): Map<Long, Model> {
        val root = Json.parseToJsonElement(json).jsonObject
        return root.entries.associate { (key, value) ->
            val obj = value.jsonObject
            val id = key.toLong()
            val isCloze = obj["type"]?.jsonPrimitive?.intOrNull == 1
            val fields = obj["flds"]?.jsonArray?.mapNotNull { field ->
                field.jsonObject["name"]?.jsonPrimitive?.content
            } ?: emptyList()
            val name = obj["name"]?.jsonPrimitive?.content ?: ""
            id to Model(id, name, isCloze, fields)
        }
    }

    /** Name of the first deck that is not Anki's "Default". */
    fun parseDeckName(json: String): String? {
        val root = Json.parseToJsonElement(json).jsonObject
        val names = root.values.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
        return names.firstOrNull { it != "Default" } ?: names.firstOrNull()
    }

    /** The `media` file: {"0": "image.png", ...} — zip entry number → original name. */
    fun parseMediaMap(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        val root = Json.parseToJsonElement(json).jsonObject
        return root.entries.associate { (k, v) -> k to v.jsonPrimitive.content }
    }
}
