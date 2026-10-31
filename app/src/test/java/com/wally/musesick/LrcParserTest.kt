package com.wally.musesick

import com.wally.musesick.util.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun testParse_nullAndEmpty() {
        assertTrue(LrcParser.parse(null).isEmpty())
        assertTrue(LrcParser.parse("").isEmpty())
        assertTrue(LrcParser.parse("   \n\n  ").isEmpty())
    }

    @Test
    fun testParse_filtersMetadataTags() {
        val lrc = """
            [ti: Song Title]
            [ar: Artist Name]
            [al: Best Album]
            [by: Creator]
            [offset: 0]
            [length: 03:45]
        """.trimIndent()
        val result = LrcParser.parse(lrc)
        assertTrue("Metadata tags should be filtered out", result.isEmpty())
    }

    @Test
    fun testParse_standardLyrics() {
        val lrc = """
            [00:12.34] First line of the song
            [01:05.67] Second line in next minute
            [02:30.00] Third line
        """.trimIndent()

        val result = LrcParser.parse(lrc)
        assertEquals(3, result.size)

        assertEquals(12_340L, result[0].time)
        assertEquals("First line of the song", result[0].text)

        assertEquals(65_670L, result[1].time)
        assertEquals("Second line in next minute", result[1].text)

        assertEquals(150_000L, result[2].time)
        assertEquals("Third line", result[2].text)
    }

    @Test
    fun testParse_threeDigitMillisecondsAndNoFractions() {
        val lrc = """
            [00:05.123] High precision line
            [00:10] No fraction line
            [00:15.8] Single digit fraction line
        """.trimIndent()

        val result = LrcParser.parse(lrc)
        assertEquals(3, result.size)

        assertEquals(5_123L, result[0].time)
        assertEquals("High precision line", result[0].text)

        assertEquals(10_000L, result[1].time)
        assertEquals("No fraction line", result[1].text)

        assertEquals(15_800L, result[2].time)
        assertEquals("Single digit fraction line", result[2].text)
    }

    @Test
    fun testParse_multipleTimestampsOnSameLine() {
        val lrc = """
            [00:10.00][00:25.50] Repeated chorus line
        """.trimIndent()

        val result = LrcParser.parse(lrc)
        assertEquals(2, result.size)

        assertEquals(10_000L, result[0].time)
        assertEquals("Repeated chorus line", result[0].text)

        assertEquals(25_500L, result[1].time)
        assertEquals("Repeated chorus line", result[1].text)
    }

    @Test
    fun testParse_mixedMetadataAndSorting() {
        val lrc = """
            [ti: Test]
            [01:00.00] Later line
            [ar: Artist]
            [00:10.00] Earlier line
            [00:30.00]
            [al: Album]
        """.trimIndent()

        val result = LrcParser.parse(lrc)
        assertEquals(2, result.size)

        // Sorted ascending
        assertEquals(10_000L, result[0].time)
        assertEquals("Earlier line", result[0].text)

        assertEquals(60_000L, result[1].time)
        assertEquals("Later line", result[1].text)
    }
}

