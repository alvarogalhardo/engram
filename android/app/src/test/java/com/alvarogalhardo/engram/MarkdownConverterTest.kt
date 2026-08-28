package com.alvarogalhardo.engram

import com.alvarogalhardo.engram.domain.markdown.MarkdownConverter
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownConverterTest {
    @Test
    fun `bold becomes strong`() {
        assertTrue(MarkdownConverter.toHtml("**oi**").contains("<strong>oi</strong>"))
    }

    @Test
    fun `a fenced block becomes pre code`() {
        val html = MarkdownConverter.toHtml("```\nval x = 1\n```")
        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("val x = 1"))
    }

    @Test
    fun `inline code becomes code`() {
        assertTrue(MarkdownConverter.toHtml("use `map`").contains("<code>map</code>"))
    }

    @Test
    fun `a gfm table becomes table`() {
        val md = "| a | b |\n|---|---|\n| 1 | 2 |"
        assertTrue(MarkdownConverter.toHtml(md).contains("<table>"))
    }

    @Test
    fun `a relative image keeps its file name`() {
        val html = MarkdownConverter.toHtml("![](foto.png)")
        assertTrue(html.contains("src=\"foto.png\""))
    }
}
