package com.alvarogalhardo.engram.data.media

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

class MediaFiles(private val context: Context) {
    val dir: File = File(context.filesDir, "media").apply { mkdirs() }

    /** Copies a user-picked image into the media folder and returns its file name. */
    fun saveFromUri(uri: Uri): String {
        val mime = context.contentResolver.getType(uri)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        val name = "img-" + UUID.randomUUID().toString().substring(0, 8) + "." + ext
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Não foi possível ler a imagem selecionada.")
        input.use { ins ->
            File(dir, name).outputStream().use { ins.copyTo(it) }
        }
        return name
    }
}
