package app.writer

import app.writer.common.SpanMath
import app.writer.common.TemplateFilter
import app.writer.common.TemplateSearch
import app.writer.database.Converters
import app.writer.model.TemplateItem
import app.writer.model.TemplateState
import app.writer.model.TextSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpansSearchConvertersTest {

    private val items get() = TestData.templates.map { TemplateItem(it, null) }

    // --- SpanMath ---

    @Test
    fun typingInsideABoldRunExtendsIt() {
        val out = SpanMath.applyEdit(listOf(TextSpan(1, 4, bold = true)), "abcdef", "abXcdef")
        assertEquals(listOf(TextSpan(1, 5, bold = true)), out)
    }

    @Test
    fun typingJustAfterABoldRunStaysPlain() {
        val out = SpanMath.applyEdit(listOf(TextSpan(0, 3, bold = true)), "abc", "abcd")
        assertEquals(listOf(TextSpan(0, 3, bold = true)), out)
    }

    @Test
    fun deletingAWholeRunRemovesIt() {
        val out = SpanMath.applyEdit(listOf(TextSpan(2, 4, bold = true)), "abcdef", "abef")
        assertTrue(out.isEmpty())
    }

    @Test
    fun runsAfterAnEditShift() {
        val out = SpanMath.applyEdit(listOf(TextSpan(4, 6, italic = true)), "abcdef", "XXabcdef")
        assertEquals(listOf(TextSpan(6, 8, italic = true)), out)
    }

    @Test
    fun toggleAddsAndRemovesBold() {
        val on = SpanMath.toggle(emptyList(), 1, 3, bold = true, italic = null, textLength = 6)
        assertEquals(listOf(TextSpan(1, 3, bold = true)), on)
        assertTrue(SpanMath.isAll(on, 1, 3, italic = false))
        val off = SpanMath.toggle(on, 1, 3, bold = false, italic = null, textLength = 6)
        assertTrue(off.isEmpty())
    }

    @Test
    fun overlappingStylesAreSplit() {
        val bold = SpanMath.toggle(emptyList(), 1, 3, bold = true, italic = null, textLength = 6)
        val both = SpanMath.toggle(bold, 2, 4, bold = null, italic = true, textLength = 6)
        assertEquals(
            listOf(TextSpan(1, 2, bold = true), TextSpan(2, 3, bold = true, italic = true), TextSpan(3, 4, italic = true)),
            both,
        )
    }

    @Test
    fun normalizeMergesNeighboursWithTheSameStyle() {
        val out = SpanMath.normalize(listOf(TextSpan(0, 2, bold = true), TextSpan(2, 5, bold = true)), 10)
        assertEquals(listOf(TextSpan(0, 5, bold = true)), out)
    }

    // --- TemplateSearch ---

    @Test
    fun emptyQueryReturnsEverything() {
        assertEquals(items.size, TemplateSearch.search(items, "  ").size)
    }

    @Test
    fun romanisedHindiFindsElectricityComplaint() {
        val ids = TemplateSearch.search(items, "bijli").map { it.def.id }
        assertTrue("cmp_electricity" in ids)
    }

    @Test
    fun hindiTitleSearchRanksTitleMatchFirst() {
        val ids = TemplateSearch.search(items, "बिजली").map { it.def.id }
        assertTrue("cmp_electricity" in ids.take(3))
    }

    @Test
    fun everyWordMustMatch() {
        val ids = TemplateSearch.search(items, "ration card").map { it.def.id }
        assertTrue("sch_ration_correction" in ids)
        assertTrue(TemplateSearch.search(items, "zzzzqq").isEmpty())
        assertTrue(TemplateSearch.search(items, "ration zzzzqq").isEmpty())
    }

    @Test
    fun englishTitleSearch() {
        val ids = TemplateSearch.search(items, "scholarship").map { it.def.id }
        assertTrue("edu_scholarship" in ids)
    }

    // --- TemplateFilter ---

    @Test
    fun categoryFilterKeepsOnlyThatCategory() {
        val gov = TemplateFilter.apply("gov", items)
        assertEquals(10, gov.size)
        assertTrue(gov.all { it.def.category == "gov" })
    }

    @Test
    fun favoritesAndRecentUseState() {
        val withState = items.map {
            if (it.def.id == "gov_dm") TemplateItem(it.def, TemplateState("gov_dm", isFavorite = true, lastUsedAt = 5L, useCount = 2)) else it
        }
        assertEquals(listOf("gov_dm"), TemplateFilter.apply(TemplateFilter.FAVORITES, withState).map { it.def.id })
        assertEquals(listOf("gov_dm"), TemplateFilter.apply(TemplateFilter.RECENT, withState).map { it.def.id })
        assertEquals("gov_dm", TemplateFilter.apply(TemplateFilter.POPULAR, withState).first().def.id)
    }

    @Test
    fun curatedCollectionsPointAtRealTemplates() {
        val ids = TestData.templates.map { it.id }.toSet()
        assertTrue(TemplateFilter.popularOrder.all { it in ids })
        assertTrue(TemplateFilter.newest.all { it in ids })
        assertEquals(2, TemplateFilter.apply(TemplateFilter.UTILITY, items).size)
        assertTrue(TemplateFilter.apply(TemplateFilter.RATION, items).size >= 3)
        assertFalse(TemplateFilter.apply(TemplateFilter.CERTIFICATES, items).isEmpty())
    }

    // --- Converters ---

    @Test
    fun spansRoundTripThroughJson() {
        val c = Converters()
        val spans = listOf(TextSpan(0, 3, bold = true), TextSpan(5, 9, italic = true))
        assertEquals(spans, c.stringToSpans(c.spansToString(spans)))
        assertTrue(c.stringToSpans("not json").isEmpty())
    }

    @Test
    fun fieldValuesRoundTripThroughJson() {
        val c = Converters()
        val map = mapOf("matter" to "सड़क खराब है", "request" to "a \"quoted\" line\nwith newline")
        assertEquals(map, c.stringToMap(c.mapToString(map)))
        assertTrue(c.stringToMap("[]").isEmpty())
    }
}
