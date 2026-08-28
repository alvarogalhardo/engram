package com.alvarogalhardo.engram

import com.alvarogalhardo.engram.importer.NoteConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteConverterTest {
    private val sep = '\u001f'

    @Test
    fun `nota basica separa frente e verso`() {
        val c = NoteConverter.convert("O que é TCP?${sep}Protocolo de transporte confiável", isCloze = false)
        assertEquals("O que é TCP?", c.front)
        assertEquals("Protocolo de transporte confiável", c.back)
    }

    @Test
    fun `campos extras vao para o verso com hr`() {
        val c = NoteConverter.convert("a${sep}b${sep}c", isCloze = false)
        assertEquals("a", c.front)
        assertEquals("b<hr>c", c.back)
    }

    @Test
    fun `campos vazios do verso sao descartados`() {
        val c = NoteConverter.convert("a${sep}b${sep}${sep}", isCloze = false)
        assertEquals("b", c.back)
    }

    @Test
    fun `tags de audio sao removidas`() {
        val c = NoteConverter.convert("palavra [sound:audio.mp3]${sep}significado", isCloze = false)
        assertEquals("palavra", c.front)
    }

    @Test
    fun `cloze sem dica vira lacuna generica`() {
        val c = NoteConverter.convert("O {{c1::céu}} é azul", isCloze = true)
        assertEquals("O <b>[…]</b> é azul", c.front)
        assertEquals("O <b>céu</b> é azul", c.back)
    }

    @Test
    fun `cloze com dica mostra a dica na frente`() {
        val c = NoteConverter.convert("O {{c1::céu::lugar}} é azul", isCloze = true)
        assertEquals("O <b>[lugar]</b> é azul", c.front)
        assertEquals("O <b>céu</b> é azul", c.back)
    }

    @Test
    fun `cloze com campo extra anexa no verso`() {
        val c = NoteConverter.convert("{{c1::resposta}}${sep}nota extra", isCloze = true)
        assertTrue(c.back.endsWith("<hr>nota extra"))
    }

    @Test
    fun `multiplos cloze na mesma nota`() {
        val c = NoteConverter.convert("{{c1::a}} e {{c2::b}}", isCloze = true)
        assertEquals("<b>[…]</b> e <b>[…]</b>", c.front)
        assertEquals("<b>a</b> e <b>b</b>", c.back)
    }

    @Test
    fun `detecta sintaxe cloze`() {
        assertTrue(NoteConverter.containsCloze("x {{c1::y}} z"))
        assertFalse(NoteConverter.containsCloze("sem cloze"))
    }

    @Test
    fun `renomeia referencias de midia nos dois estilos de aspas`() {
        val html = """<img src="a.png"> e <img src='a.png'>"""
        val out = NoteConverter.rewriteMediaRefs(html, mapOf("a.png" to "1234-a.png"))
        assertEquals("""<img src="1234-a.png"> e <img src='1234-a.png'>""", out)
    }
}
