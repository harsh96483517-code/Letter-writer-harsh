package app.writer.features.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.writer.AppContainer
import app.writer.common.DateText
import app.writer.common.DocumentFactory
import app.writer.common.TemplateFilter
import app.writer.container
import app.writer.design.Accents
import app.writer.model.AppLanguage
import app.writer.model.Applicant
import app.writer.model.UserPreferences
import kotlinx.coroutines.flow.first

/** Creates a ViewModel that needs the app's hand-wired dependencies. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline build: (AppContainer) -> VM,
): VM {
    val container = LocalContext.current.container
    return viewModel(
        key = key,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = build(container) as T
        },
    )
}

/** The current settings, provided once at the app root. */
val LocalPrefs = compositionLocalOf { UserPreferences() }

/** Icon and accent color of a template category or smart filter. */
data class CategoryStyle(val icon: ImageVector, val accent: Color)

fun categoryStyle(id: String): CategoryStyle = when (id) {
    "gov" -> CategoryStyle(Icons.Rounded.AccountBalance, Accents.Blue)
    "education" -> CategoryStyle(Icons.Rounded.School, Accents.Green)
    "schemes" -> CategoryStyle(Icons.Rounded.VerifiedUser, Accents.Teal)
    "complaints" -> CategoryStyle(Icons.Rounded.ReportProblem, Accents.Orange)
    "banking" -> CategoryStyle(Icons.Rounded.AccountBalanceWallet, Accents.Amber)
    "employment" -> CategoryStyle(Icons.Rounded.Work, Accents.Purple)
    "personal" -> CategoryStyle(Icons.Rounded.Mail, Accents.Pink)
    TemplateFilter.RATION -> CategoryStyle(Icons.Rounded.CreditCard, Accents.Green)
    TemplateFilter.UTILITY -> CategoryStyle(Icons.Rounded.Bolt, Accents.Orange)
    TemplateFilter.CERTIFICATES -> CategoryStyle(Icons.Rounded.Verified, Accents.Blue)
    else -> CategoryStyle(Icons.Rounded.Description, Accents.Blue)
}

/**
 * Creates a new, separate document from a template and saves it right away, so an interrupted
 * session never loses it. An existing document is never touched.
 */
suspend fun AppContainer.startDocument(
    templateId: String,
    language: AppLanguage? = null,
    now: Long = System.currentTimeMillis(),
): String? {
    val template = templates.find(templateId) ?: return null
    val prefs = settings.preferences.first()
    val applicant = if (prefs.saveApplicant) prefs.savedApplicant else Applicant()
    val doc = DocumentFactory.create(
        template = template,
        language = language ?: prefs.docLanguage,
        prefs = prefs,
        applicant = applicant,
        now = now,
        dateText = DateText.letterDate(),
    )
    documents.save(doc, now)
    if (!template.isBlank) templates.markUsed(templateId, now)
    return doc.id
}
