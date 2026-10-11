package app.writer

import app.writer.common.DocTypes
import app.writer.common.FileEntry
import app.writer.common.FileList
import app.writer.common.FileSort
import app.writer.common.FileTab
import app.writer.common.SizeText
import app.writer.model.AppLanguage
import app.writer.model.DocStatus
import app.writer.model.ExportRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class FileListTest {

    private val docs = listOf(
        TestData.doc(templateId = "gov_dm", title = "DM application").copy(id = "a", updatedAt = 100, status = DocStatus.DRAFT),
        TestData.doc(templateId = "cmp_electricity", title = "Bijli complaint").copy(id = "b", updatedAt = 300, status = DocStatus.COMPLETED, isFavorite = true),
        TestData.doc(title = "Zebra note").copy(id = "c", updatedAt = 200, status = DocStatus.DRAFT),
    )
    private val pdfs = listOf(
        ExportRecord("p1", "b", "Bijli complaint", "Complaint_2026-10-09.pdf", "/x/p1.pdf", 1000, 1, 250),
    )

    private fun names(tab: FileTab, query: String = "", sort: FileSort = FileSort.DATE) =
        FileList.build(docs, pdfs, tab, query, sort, AppLanguage.EN).map { it.name }

    @Test
    fun allMixesDocumentsAndPdfsNewestFirst() {
        assertEquals(
            listOf("Bijli complaint", "Complaint_2026-10-09.pdf", "Zebra note", "DM application"),
            names(FileTab.ALL),
        )
    }

    @Test
    fun tabsFilterByStatusPdfAndFavorite() {
        assertEquals(listOf("Zebra note", "DM application"), names(FileTab.DRAFTS))
        assertEquals(listOf("Bijli complaint"), names(FileTab.COMPLETED))
        assertEquals(listOf("Complaint_2026-10-09.pdf"), names(FileTab.PDFS))
        assertEquals(listOf("Bijli complaint"), names(FileTab.FAVORITES))
    }

    @Test
    fun searchMatchesNameAndTypeWords() {
        assertEquals(listOf("Zebra note"), names(FileTab.ALL, "zebra"))
        assertEquals(listOf("Bijli complaint", "Complaint_2026-10-09.pdf"), names(FileTab.ALL, "complaint"))
        assertEquals(listOf("DM application"), names(FileTab.ALL, "application dm"))
        assertEquals(emptyList<String>(), names(FileTab.ALL, "nothing here"))
    }

    @Test
    fun sortByNameAndType() {
        assertEquals(
            listOf("Bijli complaint", "Complaint_2026-10-09.pdf", "DM application", "Zebra note"),
            names(FileTab.ALL, sort = FileSort.NAME),
        )
        // Application, Complaint, Document, PDF
        assertEquals(
            listOf("DM application", "Bijli complaint", "Zebra note", "Complaint_2026-10-09.pdf"),
            names(FileTab.ALL, sort = FileSort.TYPE),
        )
    }

    @Test
    fun entriesCarryTheirKind() {
        val all = FileList.build(docs, pdfs, FileTab.ALL, "", FileSort.DATE, AppLanguage.HI)
        assertEquals(2, all.count { it is FileEntry.Doc && it.typeLabel in listOf("आवेदन", "शिकायत पत्र") })
        assertEquals("PDF", all.filterIsInstance<FileEntry.Pdf>().single().typeLabel)
    }

    @Test
    fun documentTypeLabels() {
        assertEquals("Document", DocTypes.label(null, AppLanguage.EN))
        assertEquals("शिकायत पत्र", DocTypes.label("cmp_water", AppLanguage.HI))
        assertEquals("Personal letter", DocTypes.label("per_apology", AppLanguage.EN))
        assertEquals("Application", DocTypes.label("sch_income_cert", AppLanguage.EN))
    }

    @Test
    fun fileSizes() {
        assertEquals("512 B", SizeText.format(512))
        assertEquals("1.5 KB", SizeText.format(1536))
        assertEquals("2.0 MB", SizeText.format(2L * 1024 * 1024))
    }
}
