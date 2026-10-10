package app.writer.common

import app.writer.model.AppLanguage
import app.writer.model.Applicant
import app.writer.model.Recipient
import app.writer.model.TemplateDef
import app.writer.model.UserPreferences
import app.writer.model.WriterDocument
import java.util.UUID

object DocumentFactory {

    fun create(
        template: TemplateDef,
        language: AppLanguage,
        prefs: UserPreferences,
        applicant: Applicant,
        now: Long,
        dateText: String,
        id: String = UUID.randomUUID().toString(),
    ): WriterDocument {
        val recipient = template.recipient?.let {
            Recipient(designation = it.designation[language], office = it.office[language])
        } ?: Recipient()
        val base = WriterDocument(
            id = id,
            title = template.title[language],
            templateId = if (template.isBlank) null else template.id,
            language = language,
            style = template.style,
            recipient = recipient,
            applicant = applicant,
            salutation = template.salutation[language],
            closing = template.closing[language],
            dateText = dateText,
            formatting = prefs.newDocumentFormatting(),
            createdAt = now,
            updatedAt = now,
        )
        return regenerate(base, template)
    }

    /**
     * Re-fills subject and body from the template using the document's current form values.
     * A part the user has typed into themselves is never overwritten.
     */
    fun regenerate(doc: WriterDocument, template: TemplateDef?): WriterDocument {
        if (template == null || template.isBlank) return doc
        val values = TemplateFiller.valuesFor(doc.applicant, doc.recipient, doc.dateText, doc.fieldValues, doc.language)
        val label = TemplateFiller.blankLabelFor(template, doc.language)
        var result = doc
        if (!doc.subjectEdited) {
            result = result.copy(subject = TemplateFiller.fill(template.subject[doc.language], values, label))
        }
        if (!doc.bodyEdited) {
            result = result.copy(
                body = TemplateFiller.fill(template.body[doc.language], values, label),
                bodySpans = emptyList(),
            )
        }
        return result
    }

    /** Regenerates the body even though the user edited it (after they confirm the reset). */
    fun resetBody(doc: WriterDocument, template: TemplateDef?): WriterDocument =
        regenerate(doc.copy(bodyEdited = false, subjectEdited = false), template)
}
