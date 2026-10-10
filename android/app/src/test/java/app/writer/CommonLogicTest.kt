package app.writer

import app.writer.common.AddressFormatter
import app.writer.common.DocumentChecks
import app.writer.common.DocumentFactory
import app.writer.common.ExportIssue
import app.writer.common.FieldError
import app.writer.common.FileNameError
import app.writer.common.FileNames
import app.writer.common.TemplateFiller
import app.writer.common.Validators
import app.writer.model.AppLanguage
import app.writer.model.Applicant
import app.writer.model.ExportNameFormat
import app.writer.model.TemplateDef
import app.writer.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CommonLogicTest {

    // --- TemplateFiller ---

    @Test
    fun fillReplacesValuesAndMarksMissingOnes() {
        val out = TemplateFiller.fill("Hello {{name}}, from {{village}}.", mapOf("name" to "  Asha ")) { "label:$it" }
        assertEquals("Hello Asha, from [label:village].", out)
    }

    @Test
    fun countBlanksCountsBracketedMarkers() {
        assertEquals(2, TemplateFiller.countBlanks("a [b] c [d e]"))
        assertEquals(0, TemplateFiller.countBlanks("no markers here"))
    }

    @Test
    fun addressIsCombinedInOrder() {
        val a = Applicant(
            address = "12 Main Road", village = "Rampur", postOffice = "Sadar", block = "Haveli",
            district = "Lucknow", state = "Uttar Pradesh", pin = "226001",
        )
        assertEquals(
            "12 Main Road, Village Rampur, Post Office Sadar, Block Haveli, District Lucknow, Uttar Pradesh - 226001",
            AddressFormatter.full(a, AppLanguage.EN),
        )
        assertEquals(
            "12 Main Road, ग्राम Rampur, डाकघर Sadar, ब्लॉक Haveli, जनपद Lucknow, Uttar Pradesh - 226001",
            AddressFormatter.full(a, AppLanguage.HI),
        )
        assertEquals("", AddressFormatter.full(Applicant(), AppLanguage.EN))
    }

    // --- Validators ---

    @Test
    fun pinCodes() {
        assertNull(Validators.pin(""))
        assertNull(Validators.pin("226001"))
        assertNull(Validators.pin("२२६००१"))
        assertEquals(FieldError.PIN, Validators.pin("026001"))
        assertEquals(FieldError.PIN, Validators.pin("22600"))
        assertEquals(FieldError.PIN, Validators.pin("22600a"))
    }

    @Test
    fun mobileNumbers() {
        assertNull(Validators.mobile(""))
        assertNull(Validators.mobile("9876543210"))
        assertNull(Validators.mobile("+91 98765 43210"))
        assertNull(Validators.mobile("098765 43210"))
        assertEquals(FieldError.MOBILE, Validators.mobile("5876543210"))
        assertEquals(FieldError.MOBILE, Validators.mobile("98765"))
        assertEquals(FieldError.MOBILE, Validators.mobile("abc"))
    }

    @Test
    fun requiredRejectsBlank() {
        assertEquals(FieldError.REQUIRED, Validators.required("   "))
        assertNull(Validators.required("x"))
    }

    // --- FileNames ---

    @Test
    fun fileNameValidation() {
        assertEquals(FileNameError.EMPTY, FileNames.validate(""))
        assertEquals(FileNameError.EMPTY, FileNames.validate(".pdf"))
        assertEquals(FileNameError.INVALID_CHARS, FileNames.validate("a/b"))
        assertEquals(FileNameError.INVALID_CHARS, FileNames.validate("what?.pdf"))
        assertEquals(FileNameError.TOO_LONG, FileNames.validate("x".repeat(81)))
        assertNull(FileNames.validate("Application_2026-10-09.pdf"))
        assertNull(FileNames.validate("जिला अधिकारी आवेदन"))
    }

    @Test
    fun fileNameExtensionIsAddedOnce() {
        assertEquals("abc.pdf", FileNames.withPdfExtension("abc"))
        assertEquals("abc.pdf", FileNames.withPdfExtension("abc.PDF"))
        assertEquals("abc.pdf", FileNames.withPdfExtension("  abc.pdf "))
    }

    @Test
    fun sanitizeRemovesForbiddenCharacters() {
        assertEquals("a_b_c", FileNames.sanitize("a/b:c"))
        assertEquals("hello_world", FileNames.sanitize("  hello world "))
    }

    @Test
    fun suggestedNames() {
        val day = LocalDate.of(2026, 10, 9)
        val complaint = TestData.doc(templateId = "cmp_electricity", title = "बिजली शिकायत पत्र")
        assertEquals("Complaint_2026-10-09.pdf", FileNames.suggest(complaint, ExportNameFormat.TYPE_DATE, day))
        assertEquals("बिजली_शिकायत_पत्र_2026-10-09.pdf", FileNames.suggest(complaint, ExportNameFormat.TITLE_DATE, day))
        assertEquals("बिजली_शिकायत_पत्र.pdf", FileNames.suggest(complaint, ExportNameFormat.TITLE_ONLY, day))
        assertEquals("Document_2026-10-09.pdf", FileNames.suggest(TestData.doc(), ExportNameFormat.TYPE_DATE, day))
        assertEquals("Application_2026-10-09.pdf", FileNames.suggest(TestData.doc(templateId = "gov_dm"), ExportNameFormat.TYPE_DATE, day))
    }

    // --- DocumentFactory ---

    @Test
    fun createFromTemplateFillsRecipientTitleAndBlanks() {
        val t = TestData.template("gov_dm")
        val doc = DocumentFactory.create(
            template = t, language = AppLanguage.HI, prefs = UserPreferences(),
            applicant = Applicant(name = "Harsh"), now = 5L, dateText = "10/10/2026", id = "d1",
        )
        assertEquals("gov_dm", doc.templateId)
        assertEquals("जिला अधिकारी को आवेदन", doc.title)
        assertEquals("जिला अधिकारी महोदय", doc.recipient.designation)
        assertTrue("Harsh" in doc.body)
        assertTrue("[समस्या / विषय का विवरण]" in doc.body)
        assertEquals("10/10/2026", doc.dateText)
        assertEquals(5L, doc.createdAt)
    }

    @Test
    fun answeringTemplateQuestionsRewritesTheBody() {
        val t = TestData.template("gov_dm")
        val doc = DocumentFactory.create(t, AppLanguage.EN, UserPreferences(), Applicant(), 1L, "d", id = "d1")
        val answered = DocumentFactory.regenerate(doc.copy(fieldValues = mapOf("matter" to "The road is broken.")), t)
        assertTrue("The road is broken." in answered.body)
    }

    @Test
    fun editedBodyIsNeverOverwrittenUntilReset() {
        val t = TestData.template("gov_dm")
        val doc = DocumentFactory.create(t, AppLanguage.EN, UserPreferences(), Applicant(), 1L, "d", id = "d1")
        val edited = doc.copy(body = "My own words.", bodyEdited = true, fieldValues = mapOf("matter" to "X"))
        assertEquals("My own words.", DocumentFactory.regenerate(edited, t).body)
        assertTrue("X" in DocumentFactory.resetBody(edited, t).body)
    }

    @Test
    fun blankDocumentStartsEmpty() {
        val doc = DocumentFactory.create(TemplateDef.Blank, AppLanguage.HI, UserPreferences(), Applicant(), 1L, "d", id = "d1")
        assertNull(doc.templateId)
        assertEquals("", doc.body)
        assertTrue(doc.recipient.isEmpty)
    }

    // --- DocumentChecks ---

    @Test
    fun exportChecks() {
        assertEquals(listOf(ExportIssue.EmptyDocument), DocumentChecks.issues(TestData.doc()))
        val withBlank = DocumentChecks.issues(TestData.doc(body = "Dear [name], hello"))
        assertEquals(listOf<ExportIssue>(ExportIssue.Blanks(1)), withBlank)
        assertTrue(DocumentChecks.issues(TestData.doc(body = "All done.")).isEmpty())
        assertEquals(
            listOf<ExportIssue>(ExportIssue.MissingSubject),
            DocumentChecks.issues(TestData.doc(templateId = "gov_dm", body = "Body text.").copy(applicant = Applicant(name = "A"))),
        )
    }
}
