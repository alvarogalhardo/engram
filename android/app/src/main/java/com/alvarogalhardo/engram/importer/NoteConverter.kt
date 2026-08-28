package com.alvarogalhardo.engram.importer

/**
 * Converts Anki notes into front/back pairs (pure, no android.* dependencies).
 *
 * Stated simplifications: one note = one card (templates/ords are ignored);
 * cloze is rendered naively ([…] on the front, the answer in bold on the back);
 * [sound:...] tags are stripped (no audio support).
 */
object NoteConverter {
    const val FIELD_SEPARATOR = '\u001f'

    private val clozeRegex = Regex("""\{\{c\d+::(.*?)(?:::(.*?))?\}\}""", RegexOption.DOT_MATCHES_ALL)
    private val soundRegex = Regex("""\[sound:[^\]]*\]""")

    data class CardContent(val front: String, val back: String)

    fun convert(flds: String, isCloze: Boolean): CardContent {
        val fields = flds.split(FIELD_SEPARATOR).map { soundRegex.replace(it, "").trim() }
        return if (isCloze) convertCloze(fields) else convertBasic(fields)
    }

    private fun convertBasic(fields: List<String>): CardContent {
        val front = fields.firstOrNull().orEmpty()
        val back = fields.drop(1).filter { it.isNotBlank() }.joinToString("<hr>")
        return CardContent(front, back)
    }

    private fun convertCloze(fields: List<String>): CardContent {
        val text = fields.firstOrNull().orEmpty()
        val extras = fields.drop(1).filter { it.isNotBlank() }.joinToString("<hr>")
        val front = clozeRegex.replace(text) { m ->
            val hint = m.groupValues[2]
            if (hint.isNotEmpty()) "<b>[$hint]</b>" else "<b>[…]</b>"
        }
        val revealed = clozeRegex.replace(text) { m -> "<b>${m.groupValues[1]}</b>" }
        val back = if (extras.isBlank()) revealed else "$revealed<hr>$extras"
        return CardContent(front, back)
    }

    fun containsCloze(flds: String): Boolean = clozeRegex.containsMatchIn(flds)

    /** Applies media renames (collision resolution) to the HTML src attributes. */
    fun rewriteMediaRefs(html: String, renames: Map<String, String>): String {
        var result = html
        for ((old, new) in renames) {
            result = result
                .replace("src=\"$old\"", "src=\"$new\"")
                .replace("src='$old'", "src='$new'")
        }
        return result
    }
}
