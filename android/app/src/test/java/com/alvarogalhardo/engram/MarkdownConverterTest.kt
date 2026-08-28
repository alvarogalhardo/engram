package com.alvarogalhardo.engram

import com.alvarogalhardo.engram.domain.markdown.MarkdownConverter
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownConverterTest {
    @Test
    fun `negrito vira strong`() {
        assertTrue(MarkdownConverter.toHtml("**oi**").contains("<strong>oi</strong>"))
    }

    @Test
    fun `bloco de codigo vira pre code`() {
        val html = MarkdownConverter.toHtml("```\nval x = 1\n```")
        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("val x = 1"))
    }

    @Test
    fun `codigo inline vira code`() {
        assertTrue(MarkdownConverter.toHtml("use `map`").contains("<code>map</code>"))
    }

    @Test
    fun `tabela gfm vira table`() {
        val md = "| a | b |\n|---|---|\n| 1 | 2 |"
        assertTrue(MarkdownConverter.toHtml(md).contains("<table>"))
    }

    @Test
    fun `imagem relativa preserva o nome do arquivo`() {
        val html = MarkdownConverter.toHtml("![](foto.png)")
        assertTrue(html.contains("src=\"foto.png\""))
    }
}
