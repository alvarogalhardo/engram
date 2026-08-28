package com.alvarogalhardo.engram.importer

data class ImportResult(
    val deckId: Long,
    val deckName: String,
    val cardCount: Int,
    val mediaCount: Int,
    val warnings: List<String>,
)

/** Import failure carrying a message meant for the user (pt-BR, like the rest of the UI). */
class ImportException(message: String) : Exception(message)
