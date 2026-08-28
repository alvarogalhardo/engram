package com.alvarogalhardo.engram

import com.alvarogalhardo.engram.importer.NoteConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteConverterTest {
    private val sep = '\u001f'

    @Test
    fun `a basic note splits into front and back`() {
        val c = NoteConverter.convert("O que é TCP?${sep}Protocolo de transporte confiável", isCloze = false)
        assertEquals("O que é TCP?", c.front)
        assertEquals("Protocolo de transporte confiável", c.back)
    }

    @Test
    fun `extra fields go to the back separated by hr`() {
        val c = NoteConverter.convert("a${sep}b${sep}c", isCloze = false)
        assertEquals("a", c.front)
        assertEquals("b<hr>c", c.back)
    }

    @Test
    fun `blank back fields are dropped`() {
        val c = NoteConverter.convert("a${sep}b${sep}${sep}", isCloze = false)
        assertEquals("b", c.back)
    }

    @Test
    fun `audio tags are stripped`() {
        val c = NoteConverter.convert("palavra [sound:audio.mp3]${sep}significado", isCloze = false)
        assertEquals("palavra", c.front)
    }

    @Test
    fun `a cloze without hint becomes a generic blank`() {
        val c = NoteConverter.convert("O {{c1::céu}} é azul", isCloze = true)
        assertEquals("O <b>[…]</b> é azul", c.front)
        assertEquals("O <b>céu</b> é azul", c.back)
    }

    @Test
    fun `a cloze with hint shows the hint on the front`() {
        val c = NoteConverter.convert("O {{c1::céu::lugar}} é azul", isCloze = true)
        assertEquals("O <b>[lugar]</b> é azul", c.front)
        assertEquals("O <b>céu</b> é azul", c.back)
    }

    @Test
    fun `a cloze with an extra field appends it to the back`() {
        val c = NoteConverter.convert("{{c1::resposta}}${sep}nota extra", isCloze = true)
        assertTrue(c.back.endsWith("<hr>nota extra"))
    }

    @Test
    fun `multiple clozes in the same note`() {
        val c = NoteConverter.convert("{{c1::a}} e {{c2::b}}", isCloze = true)
        assertEquals("<b>[…]</b> e <b>[…]</b>", c.front)
        assertEquals("<b>a</b> e <b>b</b>", c.back)
    }

    @Test
    fun `detects cloze syntax`() {
        assertTrue(NoteConverter.containsCloze("x {{c1::y}} z"))
        assertFalse(NoteConverter.containsCloze("sem cloze"))
    }

    @Test
    fun `rewrites media references in both quote styles`() {
        val html = """<img src="a.png"> e <img src='a.png'>"""
        val out = NoteConverter.rewriteMediaRefs(html, mapOf("a.png" to "1234-a.png"))
        assertEquals("""<img src="1234-a.png"> e <img src='1234-a.png'>""", out)
    }
}
