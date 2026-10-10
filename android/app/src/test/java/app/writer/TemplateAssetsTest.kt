package app.writer

import app.writer.common.TemplateFiller
import app.writer.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateAssetsTest {

    @Test
    fun allSixtyTemplatesParse() {
        assertEquals(60, TestData.templates.size)
    }

    @Test
    fun idsAreUnique() {
        val ids = TestData.templates.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyDeclaredFieldIsUsedInBothLanguages() {
        for (t in TestData.templates) for (f in t.fields) {
            assertTrue("${t.id}: ${f.key} unused in hi body", "{{${f.key}}}" in t.body.hi)
            assertTrue("${t.id}: ${f.key} unused in en body", "{{${f.key}}}" in t.body.en)
        }
    }

    @Test
    fun fillingWithNoValuesLeavesNoRawPlaceholders() {
        for (t in TestData.templates) for (lang in AppLanguage.entries) {
            val label = TemplateFiller.blankLabelFor(t, lang)
            val body = TemplateFiller.fill(t.body[lang], emptyMap(), label)
            val subject = TemplateFiller.fill(t.subject[lang], emptyMap(), label)
            assertFalse("${t.id}/$lang body", "{{" in body)
            assertFalse("${t.id}/$lang subject", "{{" in subject)
            assertTrue("${t.id}/$lang should show blanks", TemplateFiller.countBlanks(body) > 0)
        }
    }

    @Test
    fun hindiBodiesAvoidGenderedVerbForms() {
        val gendered = listOf("ता हूँ", "ती हूँ", "ता हूं", "ती हूं", "चाहता", "चाहती")
        for (t in TestData.templates) for (g in gendered) {
            assertFalse("${t.id} contains $g", g in t.body.hi)
        }
    }

    @Test
    fun applicationTemplatesHaveARecipient() {
        for (t in TestData.templates.filter { it.style == app.writer.model.LetterStyle.APPLICATION }) {
            assertTrue("${t.id} needs a recipient", t.recipient != null)
        }
    }
}
