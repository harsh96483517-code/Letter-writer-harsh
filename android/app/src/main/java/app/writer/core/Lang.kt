package app.writer.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import app.writer.model.AppLanguage

/** The interface language for the whole composition. Provided once at the app root. */
val LocalAppLanguage = compositionLocalOf { AppLanguage.HI }

/** Picks the Hindi or English string for the current interface language. */
@Composable
fun tr(hi: String, en: String): String =
    if (LocalAppLanguage.current == AppLanguage.HI) hi else en

/** Same as [tr] but for code that already knows the language. */
fun pick(language: AppLanguage, hi: String, en: String): String =
    if (language == AppLanguage.HI) hi else en
