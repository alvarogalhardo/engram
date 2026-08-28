package com.alvarogalhardo.engram

import com.alvarogalhardo.engram.importer.ApkgParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkgParserTest {
    private val modelsJson = """
        {
          "1111": {"name": "Basic", "type": 0, "flds": [{"name": "Front", "ord": 0}, {"name": "Back", "ord": 1}]},
          "2222": {"name": "Cloze", "type": 1, "flds": [{"name": "Text", "ord": 0}]}
        }
    """.trimIndent()

    @Test
    fun `parses models with type and fields`() {
        val models = ApkgParser.parseModels(modelsJson)
        assertEquals(2, models.size)
        assertFalse(models.getValue(1111L).isCloze)
        assertTrue(models.getValue(2222L).isCloze)
        assertEquals(listOf("Front", "Back"), models.getValue(1111L).fieldNames)
        assertEquals("Basic", models.getValue(1111L).name)
    }

    @Test
    fun `deck name skips Anki's Default`() {
        val json = """{"1": {"name": "Default"}, "1234": {"name": "Meu Deck"}}"""
        assertEquals("Meu Deck", ApkgParser.parseDeckName(json))
    }

    @Test
    fun `falls back to Default when it is the only deck`() {
        val json = """{"1": {"name": "Default"}}"""
        assertEquals("Default", ApkgParser.parseDeckName(json))
    }

    @Test
    fun `parses the media map`() {
        val map = ApkgParser.parseMediaMap("""{"0": "a.png", "1": "b.jpg"}""")
        assertEquals(mapOf("0" to "a.png", "1" to "b.jpg"), map)
    }

    @Test
    fun `empty or blank media map`() {
        assertEquals(emptyMap<String, String>(), ApkgParser.parseMediaMap("{}"))
        assertEquals(emptyMap<String, String>(), ApkgParser.parseMediaMap(""))
    }
}
