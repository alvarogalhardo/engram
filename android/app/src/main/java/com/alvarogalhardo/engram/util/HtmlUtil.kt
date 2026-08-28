package com.alvarogalhardo.engram.util

object HtmlUtil {
    private val tagRegex = Regex("<[^>]*>")
    private val spaceRegex = Regex("\\s+")

    /** Remove tags HTML para exibir um preview em texto puro nas listas. */
    fun stripHtml(html: String): String =
        tagRegex.replace(html, " ")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .let { spaceRegex.replace(it, " ") }
            .trim()
}
