package app.writer.common

import app.writer.model.WriterDocument

enum class FieldError { REQUIRED, PIN, MOBILE }

object Validators {
    /** Converts Devanagari and other Unicode digits to ASCII and drops everything that is not a digit. */
    fun asciiDigits(value: String): String = buildString {
        value.forEach { c ->
            val d = Character.digit(c, 10)
            if (d >= 0) append(('0' + d))
        }
    }

    fun required(value: String): FieldError? = if (value.isBlank()) FieldError.REQUIRED else null

    /** Indian PIN codes are six digits and never start with 0. Empty is fine: the field is optional. */
    fun pin(value: String): FieldError? {
        if (value.isBlank()) return null
        val digits = asciiDigits(value)
        val onlyDigitsAndSpace = value.all { Character.digit(it, 10) >= 0 || it == ' ' }
        return if (onlyDigitsAndSpace && Regex("[1-9][0-9]{5}").matches(digits)) null else FieldError.PIN
    }

    /** Ten-digit Indian mobile numbers starting 6–9, optionally written with +91, 91 or a leading 0. */
    fun mobile(value: String): FieldError? {
        if (value.isBlank()) return null
        val allowed = value.all { Character.digit(it, 10) >= 0 || it in " -+()" }
        if (!allowed) return FieldError.MOBILE
        var digits = asciiDigits(value)
        if (digits.length == 12 && digits.startsWith("91")) digits = digits.drop(2)
        else if (digits.length == 11 && digits.startsWith("0")) digits = digits.drop(1)
        return if (Regex("[6-9][0-9]{9}").matches(digits)) null else FieldError.MOBILE
    }
}

/** Problems worth telling the user about before a PDF is made. */
sealed interface ExportIssue {
    /** Nothing meaningful to print. */
    data object EmptyDocument : ExportIssue

    /** The letter still contains [blank] markers. */
    data class Blanks(val count: Int) : ExportIssue
}

object DocumentChecks {
    fun issues(doc: WriterDocument): List<ExportIssue> {
        if (doc.body.isBlank() && doc.subject.isBlank()) return listOf(ExportIssue.EmptyDocument)
        val blanks = TemplateFiller.countBlanks(LetterText.printedText(doc))
        return if (blanks > 0) listOf(ExportIssue.Blanks(blanks)) else emptyList()
    }
}
