package com.alvarogalhardo.engram.importer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parser puro (sem android.*) dos JSONs do banco de coleção do Anki legado:
 * col.models, col.decks e o arquivo `media` na raiz do zip.
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

    /** Nome do primeiro baralho que não seja o "Default" do Anki. */
    fun parseDeckName(json: String): String? {
        val root = Json.parseToJsonElement(json).jsonObject
        val names = root.values.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
        return names.firstOrNull { it != "Default" } ?: names.firstOrNull()
    }

    /** Arquivo `media`: {"0": "imagem.png", ...} — número no zip → nome original. */
    fun parseMediaMap(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        val root = Json.parseToJsonElement(json).jsonObject
        return root.entries.associate { (k, v) -> k to v.jsonPrimitive.content }
    }
}
