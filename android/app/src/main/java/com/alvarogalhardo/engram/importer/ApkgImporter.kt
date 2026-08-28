package com.alvarogalhardo.engram.importer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.alvarogalhardo.engram.data.db.AppDatabase
import com.alvarogalhardo.engram.data.db.entity.Card
import com.alvarogalhardo.engram.data.db.entity.Deck
import com.alvarogalhardo.engram.data.media.MediaFiles
import com.alvarogalhardo.engram.domain.scheduler.CardState
import com.alvarogalhardo.engram.util.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile

class ApkgImporter(
    private val context: Context,
    private val db: AppDatabase,
    private val mediaFiles: MediaFiles,
    private val clock: TimeProvider,
) {
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "import/${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val apkgFile = File(workDir, "in.apkg")
            (context.contentResolver.openInputStream(uri)
                ?: throw ImportException("Não foi possível ler o arquivo selecionado."))
                .use { input -> apkgFile.outputStream().use { input.copyTo(it) } }

            val extractDir = File(workDir, "x").apply { mkdirs() }
            extractZip(apkgFile, extractDir)

            val dbFile = listOf("collection.anki21", "collection.anki2")
                .map { File(extractDir, it) }
                .firstOrNull { it.exists() }
                ?: run {
                    if (File(extractDir, "collection.anki21b").exists()) {
                        throw ImportException(
                            "Este arquivo usa o formato novo do Anki (anki21b). " +
                                "Exporte o baralho marcando \"compatível com versões antigas\" e tente de novo."
                        )
                    }
                    throw ImportException("Arquivo .apkg inválido: banco de coleção não encontrado.")
                }

            val (modelsJson, decksJson, notes) = readCollection(dbFile)
            val models = ApkgParser.parseModels(modelsJson)

            val warnings = mutableListOf<String>()
            val renames = copyMedia(extractDir, warnings)

            val deckName = uniqueDeckName(
                ApkgParser.parseDeckName(decksJson) ?: displayName(uri) ?: "Importado"
            )

            val now = clock.nowMillis()
            var clozeCount = 0
            val contents = notes.mapIndexed { index, (mid, flds) ->
                val isCloze = models[mid]?.isCloze == true
                if (isCloze || NoteConverter.containsCloze(flds)) clozeCount++
                val c = NoteConverter.convert(flds, isCloze || NoteConverter.containsCloze(flds))
                CardContentWithOrder(
                    front = NoteConverter.rewriteMediaRefs(c.front, renames),
                    back = NoteConverter.rewriteMediaRefs(c.back, renames),
                    order = index,
                )
            }.filter { it.front.isNotBlank() }

            if (contents.isEmpty()) throw ImportException("Nenhuma carta encontrada no arquivo.")
            if (clozeCount > 0) warnings += "$clozeCount carta(s) cloze importada(s) de forma simplificada."

            val deckId = db.withTransaction {
                val id = db.deckDao().insert(Deck(name = deckName, createdAt = now))
                db.cardDao().insertAll(
                    contents.map {
                        Card(
                            deckId = id,
                            front = it.front,
                            back = it.back,
                            state = CardState.NEW,
                            dueAt = now + it.order, // preserve intake order
                            createdAt = now + it.order,
                        )
                    }
                )
                id
            }

            ImportResult(deckId, deckName, contents.size, renames.size, warnings)
        } finally {
            workDir.deleteRecursively()
        }
    }

    private data class CardContentWithOrder(val front: String, val back: String, val order: Int)

    private fun extractZip(zipFile: File, destDir: File) {
        try {
            ZipFile(zipFile).use { zip ->
                for (entry in zip.entries()) {
                    if (entry.isDirectory) continue
                    val out = File(destDir, entry.name)
                    // zip-slip guard
                    if (!out.canonicalPath.startsWith(destDir.canonicalPath + File.separator)) continue
                    out.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        out.outputStream().use { input.copyTo(it) }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is ImportException) throw e
            throw ImportException("O arquivo não é um .apkg válido (zip corrompido).")
        }
    }

    private data class CollectionData(
        val modelsJson: String,
        val decksJson: String,
        val notes: List<Pair<Long, String>>,
    )

    private fun readCollection(dbFile: File): CollectionData {
        val sql = try {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: SQLiteException) {
            throw ImportException("Não foi possível abrir o banco de coleção do Anki.")
        }
        sql.use { database ->
            try {
                val (modelsJson, decksJson) = database.rawQuery(
                    "SELECT models, decks FROM col LIMIT 1", null
                ).use { c ->
                    if (!c.moveToFirst()) throw ImportException("Coleção do Anki vazia.")
                    c.getString(0) to c.getString(1)
                }
                val notes = mutableListOf<Pair<Long, String>>()
                database.rawQuery("SELECT mid, flds FROM notes ORDER BY id", null).use { c ->
                    while (c.moveToNext()) notes += c.getLong(0) to c.getString(1)
                }
                return CollectionData(modelsJson, decksJson, notes)
            } catch (e: SQLiteException) {
                throw ImportException(
                    "Formato de coleção não suportado. Exporte o baralho no formato legado do Anki."
                )
            }
        }
    }

    /** Copies the numbered media from the zip into filesDir/media, resolving collisions by SHA-1. */
    private fun copyMedia(extractDir: File, warnings: MutableList<String>): Map<String, String> {
        val mediaJson = File(extractDir, "media")
        if (!mediaJson.exists()) return emptyMap()
        val map = try {
            ApkgParser.parseMediaMap(mediaJson.readText())
        } catch (e: Exception) {
            warnings += "Índice de mídia ilegível; imagens podem faltar."
            return emptyMap()
        }

        val renames = mutableMapOf<String, String>()
        var missing = 0
        for ((number, originalName) in map) {
            val source = File(extractDir, number)
            if (!source.exists()) {
                missing++
                continue
            }
            val safeName = File(originalName).name // drop any embedded path
            val target = File(mediaFiles.dir, safeName)
            val finalName = when {
                !target.exists() -> {
                    source.copyTo(target)
                    safeName
                }
                sha1(target) == sha1(source) -> safeName // same content, reuse it
                else -> {
                    val renamed = "${sha1(source).substring(0, 8)}-$safeName"
                    val renamedFile = File(mediaFiles.dir, renamed)
                    if (!renamedFile.exists()) source.copyTo(renamedFile)
                    renamed
                }
            }
            renames[safeName] = finalName
        }
        if (missing > 0) warnings += "$missing arquivo(s) de mídia ausente(s) no pacote."
        return renames.filter { (old, new) -> old != new }
    }

    private fun sha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun uniqueDeckName(base: String): String {
        val existing = db.deckDao().allNames().toSet()
        if (base !in existing) return base
        var i = 2
        while ("$base ($i)" in existing) i++
        return "$base ($i)"
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            ?.removeSuffix(".apkg")
}
