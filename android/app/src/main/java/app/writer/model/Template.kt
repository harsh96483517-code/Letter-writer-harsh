package app.writer.model

data class Bilingual(val hi: String, val en: String) {
    operator fun get(language: AppLanguage): String = if (language == AppLanguage.HI) hi else en

    companion object {
        val Empty = Bilingual("", "")
    }
}

data class RecipientDef(val designation: Bilingual, val office: Bilingual)

/** One template-specific question shown in the editor. */
data class FieldDef(
    val key: String,
    val label: Bilingual,
    val hint: Bilingual,
    val multiline: Boolean,
    val required: Boolean,
)

/** A built-in letter template loaded from `assets/templates/*.json`. */
data class TemplateDef(
    val id: String,
    val category: String,
    val style: LetterStyle,
    val title: Bilingual,
    val description: Bilingual,
    val keywords: List<String>,
    val recipient: RecipientDef?,
    val salutation: Bilingual,
    val subject: Bilingual,
    val body: Bilingual,
    val closing: Bilingual,
    val fields: List<FieldDef>,
) {
    val isBlank: Boolean get() = id == BLANK_ID

    companion object {
        const val BLANK_ID = "blank"

        /** The "write from scratch" template: nothing prefilled, every block optional. */
        val Blank = TemplateDef(
            id = BLANK_ID,
            category = "personal",
            style = LetterStyle.APPLICATION,
            title = Bilingual("खाली दस्तावेज़", "Blank document"),
            description = Bilingual("शुरू से अपना पत्र लिखें", "Write your own letter from scratch"),
            keywords = emptyList(),
            recipient = null,
            salutation = Bilingual.Empty,
            subject = Bilingual.Empty,
            body = Bilingual.Empty,
            closing = Bilingual.Empty,
            fields = emptyList(),
        )
    }
}

/** A template together with the user's favorite flag and usage counters. */
data class TemplateItem(val def: TemplateDef, val state: TemplateState?) {
    val isFavorite: Boolean get() = state?.isFavorite == true
    val lastUsedAt: Long get() = state?.lastUsedAt ?: 0L
    val useCount: Int get() = state?.useCount ?: 0
}

/** Template categories, as stored in the JSON `category` field. Icons and colors live in the UI layer. */
data class CategoryInfo(val id: String, val titleHi: String, val titleEn: String) {
    fun title(language: AppLanguage) = if (language == AppLanguage.HI) titleHi else titleEn
}

object Categories {
    val all = listOf(
        CategoryInfo("gov", "सरकारी आवेदन", "Government"),
        CategoryInfo("education", "शिक्षा", "Education"),
        CategoryInfo("schemes", "योजनाएँ व दस्तावेज़", "Schemes & documents"),
        CategoryInfo("complaints", "शिकायत पत्र", "Complaints"),
        CategoryInfo("banking", "बैंकिंग", "Banking"),
        CategoryInfo("employment", "नौकरी / करियर", "Jobs & career"),
        CategoryInfo("personal", "व्यक्तिगत पत्र", "Personal letters"),
    )

    fun byId(id: String): CategoryInfo? = all.firstOrNull { it.id == id }
}
