package com.alvarogalhardo.engram.importer

data class ImportResult(
    val deckId: Long,
    val deckName: String,
    val cardCount: Int,
    val mediaCount: Int,
    val warnings: List<String>,
)

/** Erro de import com mensagem própria para o usuário (pt-BR). */
class ImportException(message: String) : Exception(message)
