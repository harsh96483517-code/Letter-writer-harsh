package app.writer

import app.writer.model.AppLanguage
import app.writer.model.LetterStyle
import app.writer.model.TemplateDef
import app.writer.model.WriterDocument
import app.writer.storage.TemplateParser
import java.io.File

object TestData {
    /** Unit tests run with the module directory (android/app) as the working directory. */
    private val dir = File("src/main/assets/templates")

    val templates: List<TemplateDef> by lazy {
        require(dir.isDirectory) { "Template folder not found: ${dir.absolutePath}" }
        dir.listFiles { f -> f.extension == "json" }!!
            .sortedBy { it.name }
            .flatMap { TemplateParser.parse(it.readText(Charsets.UTF_8)) }
    }

    fun template(id: String): TemplateDef = templates.first { it.id == id }

    fun doc(
        templateId: String? = null,
        title: String = "Test",
        body: String = "",
        subject: String = "",
        language: AppLanguage = AppLanguage.HI,
    ) = WriterDocument(
        id = "id-1",
        title = title,
        templateId = templateId,
        language = language,
        style = LetterStyle.APPLICATION,
        subject = subject,
        body = body,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
