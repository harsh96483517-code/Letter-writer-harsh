package app.writer.common

import app.writer.model.AppLanguage
import app.writer.model.LetterStyle
import app.writer.model.WriterDocument

/**
 * The text of each printed part of a letter, independent of how it is drawn. The A4 preview and
 * the PDF both use these functions, and the "blanks left" check reads the same text, so what the
 * user is warned about is exactly what would be printed.
 */
object LetterText {

    fun recipientHeading(language: AppLanguage) = if (language == AppLanguage.HI) "सेवा में," else "To,"

    fun subjectLine(doc: WriterDocument): String {
        val subject = doc.subject.trim()
        if (subject.isEmpty()) return ""
        return (if (doc.language == AppLanguage.HI) "विषय: " else "Subject: ") + subject
    }

    /** Recipient lines. A recipient with no district shows a visible blank for it. */
    fun recipientLines(doc: WriterDocument): List<String> {
        val r = doc.recipient
        if (r.isEmpty) return emptyList()
        val hi = doc.language == AppLanguage.HI
        val lines = mutableListOf<String>()
        r.designation.trim().takeIf { it.isNotEmpty() }?.let(lines::add)
        r.office.trim().takeIf { it.isNotEmpty() }?.let(lines::add)
        r.location.trim().takeIf { it.isNotEmpty() }?.let(lines::add)
        val district = r.district.trim()
        val state = r.state.trim()
        val districtText = district.ifEmpty { if (hi) "[जनपद का नाम]" else "[district name]" }
        lines += if (hi) {
            "जनपद — $districtText" + if (state.isNotEmpty()) ", $state" else ""
        } else {
            districtText + if (state.isNotEmpty()) ", $state" else ""
        }
        return lines
    }

    /** Sender address and date shown at the top right of a personal letter. */
    fun personalTopLines(doc: WriterDocument): List<String> {
        val hi = doc.language == AppLanguage.HI
        val lines = mutableListOf<String>()
        AddressFormatter.full(doc.applicant, doc.language).takeIf { it.isNotEmpty() }?.let(lines::add)
        doc.dateText.trim().takeIf { it.isNotEmpty() }?.let { lines += (if (hi) "दिनांक: " else "Date: ") + it }
        return lines
    }

    /** Lines under the closing: name, then (formal letters) address, mobile number and date. */
    fun signatureLines(doc: WriterDocument): List<String> {
        val hi = doc.language == AppLanguage.HI
        val a = doc.applicant
        val lines = mutableListOf<String>()
        val name = a.name.trim()
        when {
            name.isNotEmpty() -> lines += name
            doc.templateId != null -> lines += if (hi) "[आपका नाम]" else "[your name]"
        }
        if (doc.style == LetterStyle.APPLICATION) {
            AddressFormatter.full(a, doc.language).takeIf { it.isNotEmpty() }?.let(lines::add)
            a.mobile.trim().takeIf { it.isNotEmpty() }?.let { lines += (if (hi) "मो.: " else "Mob.: ") + it }
            doc.dateText.trim().takeIf { it.isNotEmpty() }?.let { lines += (if (hi) "दिनांक: " else "Date: ") + it }
        }
        return lines
    }

    /** Everything that would be printed, as one string (used to count leftover blanks). */
    fun printedText(doc: WriterDocument): String {
        val parts = mutableListOf<String>()
        if (doc.style == LetterStyle.PERSONAL) parts += personalTopLines(doc)
        parts += recipientLines(doc)
        parts += subjectLine(doc)
        parts += doc.salutation
        parts += doc.body
        parts += doc.closing
        parts += signatureLines(doc)
        return parts.joinToString("\n")
    }
}
