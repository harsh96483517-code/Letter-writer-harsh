package app.writer.common

import app.writer.model.AppLanguage
import app.writer.model.DocStatus
import app.writer.model.ExportRecord
import app.writer.model.WriterDocument

/** Human-readable kind of a document, shown on cards and used for sorting. */
object DocTypes {
    fun label(templateId: String?, language: AppLanguage): String {
        val hi = language == AppLanguage.HI
        return when {
            templateId == null -> if (hi) "दस्तावेज़" else "Document"
            templateId.startsWith("cmp_") -> if (hi) "शिकायत पत्र" else "Complaint"
            templateId.startsWith("per_") -> if (hi) "व्यक्तिगत पत्र" else "Personal letter"
            templateId.startsWith("job_") -> if (hi) "नौकरी / करियर" else "Job / career"
            templateId.startsWith("bank_") -> if (hi) "बैंकिंग" else "Banking"
            else -> if (hi) "आवेदन" else "Application"
        }
    }

    fun pdfLabel(): String = "PDF"
}

/** One row of the My Files screen: either an editable document or an exported PDF. */
sealed interface FileEntry {
    val key: String
    val name: String
    val timestamp: Long
    val typeLabel: String

    data class Doc(val doc: WriterDocument, override val typeLabel: String) : FileEntry {
        override val key get() = "doc:${doc.id}"
        override val name get() = doc.title
        override val timestamp get() = doc.updatedAt
    }

    data class Pdf(val record: ExportRecord) : FileEntry {
        override val key get() = "pdf:${record.id}"
        override val name get() = record.fileName
        override val timestamp get() = record.createdAt
        override val typeLabel get() = DocTypes.pdfLabel()
    }
}

enum class FileTab { ALL, DRAFTS, COMPLETED, PDFS, FAVORITES }

enum class FileSort { DATE, NAME, TYPE }

object FileList {
    fun build(
        documents: List<WriterDocument>,
        exports: List<ExportRecord>,
        tab: FileTab,
        query: String,
        sort: FileSort,
        language: AppLanguage,
    ): List<FileEntry> {
        val docs = documents.map { FileEntry.Doc(it, DocTypes.label(it.templateId, language)) }
        val pdfs = exports.map { FileEntry.Pdf(it) }

        val pool: List<FileEntry> = when (tab) {
            FileTab.ALL -> docs + pdfs
            FileTab.DRAFTS -> docs.filter { it.doc.status == DocStatus.DRAFT }
            FileTab.COMPLETED -> docs.filter { it.doc.status == DocStatus.COMPLETED }
            FileTab.PDFS -> pdfs
            FileTab.FAVORITES -> docs.filter { it.doc.isFavorite }
        }

        val words = TemplateSearch.tokens(query)
        val filtered = if (words.isEmpty()) pool else pool.filter { entry ->
            val hay = TemplateSearch.normalize(entry.name + " " + entry.typeLabel)
            words.all { it in hay }
        }

        val newestFirst = compareByDescending<FileEntry> { it.timestamp }
        return when (sort) {
            FileSort.DATE -> filtered.sortedWith(newestFirst)
            FileSort.NAME -> filtered.sortedWith(compareBy<FileEntry, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.then(newestFirst))
            FileSort.TYPE -> filtered.sortedWith(compareBy<FileEntry, String>(String.CASE_INSENSITIVE_ORDER) { it.typeLabel }.then(newestFirst))
        }
    }
}

object SizeText {
    /** 1.5 KB, 2.3 MB … sizes are shown the way Android's own file apps show them. */
    fun format(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(java.util.Locale.ENGLISH, "%.1f KB", bytes / 1024.0)
        else -> String.format(java.util.Locale.ENGLISH, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
