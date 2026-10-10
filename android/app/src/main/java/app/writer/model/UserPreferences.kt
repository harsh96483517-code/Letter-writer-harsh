package app.writer.model

/** How exported PDFs are named by default. */
enum class ExportNameFormat {
    /** Application_2026-10-09.pdf */
    TYPE_DATE,

    /** <document title>_2026-10-09.pdf */
    TITLE_DATE,

    /** <document title>.pdf */
    TITLE_ONLY,
}

data class UserPreferences(
    val onboardingDone: Boolean = false,
    val theme: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.HI,
    /** Optional greeting name. Empty means the user chose not to give one. */
    val userName: String = "",
    /** Changes when the profile photo is replaced so the UI reloads it. 0 means no photo. */
    val profileImageStamp: Long = 0L,

    // Document defaults
    val paper: PaperSize = PaperSize.A4,
    val font: FontChoice = FontChoice.SERIF,
    val fontSizePt: Float = 12f,
    val margins: MarginPreset = MarginPreset.NORMAL,
    val lineSpacing: Float = 1.4f,
    val docLanguage: AppLanguage = AppLanguage.HI,

    // Editor
    val autosave: Boolean = true,
    val charCount: Boolean = true,
    val haptics: Boolean = true,

    // Files
    val exportNameFormat: ExportNameFormat = ExportNameFormat.TYPE_DATE,
    val recentDocsCount: Int = 5,

    // Privacy: applicant details are only kept when the user switches this on.
    val saveApplicant: Boolean = false,
    val savedApplicant: Applicant = Applicant(),
    val recentSearches: List<String> = emptyList(),

    val exportCount: Int = 0,
) {
    fun newDocumentFormatting() = Formatting(
        paper = paper,
        font = font,
        fontSizePt = fontSizePt,
        lineSpacing = lineSpacing,
        bodyAlign = BodyAlign.LEFT,
        margins = margins.toMargins(),
    )
}
