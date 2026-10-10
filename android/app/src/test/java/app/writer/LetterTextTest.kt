package app.writer

import app.writer.common.DocumentChecks
import app.writer.common.ExportIssue
import app.writer.common.LetterText
import app.writer.model.AppLanguage
import app.writer.model.Applicant
import app.writer.model.LetterStyle
import app.writer.model.Recipient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterTextTest {

    @Test
    fun recipientWithoutDistrictShowsAVisibleBlank() {
        val doc = TestData.doc(templateId = "gov_dm")
            .copy(recipient = Recipient(designation = "जिला अधिकारी महोदय", office = "जिला अधिकारी कार्यालय"))
        assertEquals(
            listOf("जिला अधिकारी महोदय", "जिला अधिकारी कार्यालय", "जनपद — [जनपद का नाम]"),
            LetterText.recipientLines(doc),
        )
    }

    @Test
    fun recipientWithDistrictAndState() {
        val doc = TestData.doc().copy(
            recipient = Recipient(designation = "The District Magistrate", district = "Lucknow", state = "Uttar Pradesh"),
            language = AppLanguage.EN,
        )
        assertEquals(listOf("The District Magistrate", "Lucknow, Uttar Pradesh"), LetterText.recipientLines(doc))
        val hindi = doc.copy(language = AppLanguage.HI)
        assertEquals("जनपद — Lucknow, Uttar Pradesh", LetterText.recipientLines(hindi).last())
    }

    @Test
    fun emptyRecipientPrintsNothing() {
        assertTrue(LetterText.recipientLines(TestData.doc()).isEmpty())
    }

    @Test
    fun formalSignatureHasNameMobileAndDate() {
        val doc = TestData.doc(templateId = "gov_dm").copy(
            applicant = Applicant(name = "Harsh", mobile = "9876543210"),
            dateText = "10/10/2026",
        )
        assertEquals(listOf("Harsh", "मो.: 9876543210", "दिनांक: 10/10/2026"), LetterText.signatureLines(doc))
    }

    @Test
    fun templateLettersShowABlankNameButBlankDocumentsDoNot() {
        assertEquals(listOf("[आपका नाम]"), LetterText.signatureLines(TestData.doc(templateId = "gov_dm")))
        assertTrue(LetterText.signatureLines(TestData.doc()).isEmpty())
    }

    @Test
    fun personalLettersKeepOnlyTheNameAtTheBottom() {
        val doc = TestData.doc(templateId = "per_thank_you").copy(
            style = LetterStyle.PERSONAL,
            applicant = Applicant(name = "Asha", mobile = "9876543210"),
            dateText = "10/10/2026",
        )
        assertEquals(listOf("Asha"), LetterText.signatureLines(doc))
        assertEquals(listOf("दिनांक: 10/10/2026"), LetterText.personalTopLines(doc))
    }

    @Test
    fun subjectLineHasALabel() {
        assertEquals("विषय: राशन कार्ड सुधार", LetterText.subjectLine(TestData.doc(subject = " राशन कार्ड सुधार ")))
        assertEquals("Subject: Leave", LetterText.subjectLine(TestData.doc(subject = "Leave", language = AppLanguage.EN)))
        assertEquals("", LetterText.subjectLine(TestData.doc()))
    }

    @Test
    fun blanksInTheRecipientAreCountedBeforeExport() {
        val doc = TestData.doc(templateId = "gov_dm", body = "All filled in.", subject = "Leave").copy(
            recipient = Recipient(designation = "जिला अधिकारी महोदय"),
            applicant = Applicant(name = "Harsh"),
        )
        // The missing district is the only blank.
        assertEquals(listOf<ExportIssue>(ExportIssue.Blanks(1)), DocumentChecks.issues(doc))
    }
}
