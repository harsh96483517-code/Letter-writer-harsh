package app.writer.common

import app.writer.model.ExportNameFormat
import app.writer.model.WriterDocument
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class FileNameError { EMPTY, INVALID_CHARS, TOO_LONG }

object FileNames {
    private const val MAX_BASE_LENGTH = 80
    private val FORBIDDEN = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")

    /** Short English word for the kind of document, used in default file names. */
    fun typeWord(doc: WriterDocument): String {
        val id = doc.templateId ?: return "Document"
        return when {
            id.startsWith("cmp_") -> "Complaint"
            id.startsWith("per_") -> "Letter"
            id.startsWith("job_") -> "Job"
            id.startsWith("bank_") -> "Bank"
            else -> "Application"
        }
    }

    fun suggest(doc: WriterDocument, format: ExportNameFormat, date: LocalDate): String {
        val day = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val title = sanitize(doc.title).ifEmpty { typeWord(doc) }
        val base = when (format) {
            ExportNameFormat.TYPE_DATE -> "${typeWord(doc)}_$day"
            ExportNameFormat.TITLE_DATE -> "${title}_$day"
            ExportNameFormat.TITLE_ONLY -> title
        }
        return "$base.pdf"
    }

    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = LocalDate.now(zone)

    /** Replaces characters that are not allowed in file names and tidies whitespace. */
    fun sanitize(name: String): String =
        name.replace(FORBIDDEN, "_")
            .replace(Regex("\\s+"), "_")
            .trim('.', '_', ' ')
            .take(MAX_BASE_LENGTH)

    /** Checks a name the user typed. [input] may or may not already end in ".pdf". */
    fun validate(input: String): FileNameError? {
        val base = stripPdf(input.trim())
        if (base.isBlank() || base.trim('.', ' ').isEmpty()) return FileNameError.EMPTY
        if (FORBIDDEN.containsMatchIn(base)) return FileNameError.INVALID_CHARS
        if (base.length > MAX_BASE_LENGTH) return FileNameError.TOO_LONG
        return null
    }

    /** Returns the name with exactly one ".pdf" extension. */
    fun withPdfExtension(input: String): String = stripPdf(input.trim()).trim() + ".pdf"

    private fun stripPdf(value: String): String =
        if (value.endsWith(".pdf", ignoreCase = true)) value.dropLast(4) else value
}
