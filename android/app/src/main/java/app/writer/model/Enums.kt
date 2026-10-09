package app.writer.model

/** Appearance choice shown in Settings. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Interface language (and, separately, the default document language). */
enum class AppLanguage(val code: String) {
    HI("hi"),
    EN("en");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: HI
    }
}
