package app.writer.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DocStatus { DRAFT, COMPLETED }

/** Formal letters have a recipient block and subject line; personal letters are friendlier. */
enum class LetterStyle { APPLICATION, PERSONAL }

enum class BodyAlign { LEFT, CENTER, RIGHT, JUSTIFY }

enum class FontChoice { SANS, SERIF }

enum class PaperSize(val widthMm: Float, val heightMm: Float, val label: String) {
    A4(210f, 297f, "A4"),
    A5(148f, 210f, "A5"),
    LETTER(215.9f, 279.4f, "Letter"),
}

/** Margin presets offered in Settings and the editor. Values are millimetres. */
enum class MarginPreset(val left: Float, val right: Float, val top: Float, val bottom: Float) {
    NORMAL(25f, 20f, 20f, 20f),
    NARROW(15f, 15f, 15f, 15f),
    WIDE(30f, 25f, 25f, 25f);

    fun toMargins() = Margins(left, right, top, bottom)

    companion object {
        fun matching(m: Margins): MarginPreset? = entries.firstOrNull { it.toMargins() == m }
    }
}

data class Recipient(
    val designation: String = "",
    val office: String = "",
    val location: String = "",
    val district: String = "",
    val state: String = "",
) {
    val isEmpty: Boolean
        get() = designation.isBlank() && office.isBlank() && location.isBlank() &&
            district.isBlank() && state.isBlank()
}

data class Applicant(
    val name: String = "",
    /** Father's or mother's name. */
    val parent: String = "",
    /** Free-form address line (house number, street, locality). */
    val address: String = "",
    val village: String = "",
    val postOffice: String = "",
    val block: String = "",
    val district: String = "",
    val state: String = "",
    val pin: String = "",
    val mobile: String = "",
) {
    val isEmpty: Boolean
        get() = listOf(name, parent, address, village, postOffice, block, district, state, pin, mobile)
            .all { it.isBlank() }
}

data class Margins(
    val leftMm: Float = 25f,
    val rightMm: Float = 20f,
    val topMm: Float = 20f,
    val bottomMm: Float = 20f,
)

data class Formatting(
    val paper: PaperSize = PaperSize.A4,
    val font: FontChoice = FontChoice.SERIF,
    val fontSizePt: Float = 12f,
    val lineSpacing: Float = 1.4f,
    val bodyAlign: BodyAlign = BodyAlign.LEFT,
    @Embedded(prefix = "m_") val margins: Margins = Margins(),
)

/** Bold/italic run inside the body text. Offsets are UTF-16 indexes into [WriterDocument.body]. */
data class TextSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
)

/**
 * One letter. This class is also the Room entity, so the whole document is one row with real
 * columns for the recipient, applicant and formatting (not one opaque text blob).
 */
@Entity(tableName = "documents")
data class WriterDocument(
    @PrimaryKey val id: String,
    val title: String,
    val templateId: String?,
    val language: AppLanguage,
    val style: LetterStyle,
    @Embedded(prefix = "rcp_") val recipient: Recipient = Recipient(),
    @Embedded(prefix = "app_") val applicant: Applicant = Applicant(),
    val subject: String = "",
    val salutation: String = "",
    val body: String = "",
    val bodySpans: List<TextSpan> = emptyList(),
    val closing: String = "",
    /** Display text of the letter date, e.g. 10/10/2026. Free text so users can write any format. */
    val dateText: String = "",
    /** Values of the template-specific questions, keyed by field key. */
    val fieldValues: Map<String, String> = emptyMap(),
    /** True once the user typed in the body; from then on template questions no longer rewrite it. */
    val bodyEdited: Boolean = false,
    val subjectEdited: Boolean = false,
    val status: DocStatus = DocStatus.DRAFT,
    val isFavorite: Boolean = false,
    @Embedded(prefix = "fmt_") val formatting: Formatting = Formatting(),
    val createdAt: Long,
    val updatedAt: Long,
)

/** Record of a PDF the user exported (the file itself also lives in app storage for re-sharing). */
@Entity(tableName = "exports")
data class ExportRecord(
    @PrimaryKey val id: String,
    val documentId: String?,
    val documentTitle: String,
    val fileName: String,
    /** Absolute path of the app-private copy. */
    val internalPath: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val createdAt: Long,
)

/** Per-template favorite flag and usage counters (templates themselves are read-only assets). */
@Entity(tableName = "template_state")
data class TemplateState(
    @PrimaryKey val templateId: String,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = 0L,
    val useCount: Int = 0,
)
