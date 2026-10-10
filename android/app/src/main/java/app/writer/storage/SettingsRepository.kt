package app.writer.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.writer.model.AppLanguage
import app.writer.model.Applicant
import app.writer.model.ExportNameFormat
import app.writer.model.FontChoice
import app.writer.model.MarginPreset
import app.writer.model.PaperSize
import app.writer.model.ThemeMode
import app.writer.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.writerDataStore: DataStore<Preferences> by preferencesDataStore(name = "writer_prefs")

/**
 * Settings live in DataStore. Every change goes through [update], which reads the current values,
 * applies the change and writes them back in one atomic edit.
 */
class SettingsRepository(private val store: DataStore<Preferences>) {

    val preferences: Flow<UserPreferences> = store.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toUserPreferences() }
        .distinctUntilChanged()

    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        store.edit { prefs -> prefs.write(transform(prefs.toUserPreferences())) }
    }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        update { p ->
            val list = (listOf(clean) + p.recentSearches.filterNot { it.equals(clean, ignoreCase = true) }).take(MAX_RECENT_SEARCHES)
            p.copy(recentSearches = list)
        }
    }

    suspend fun clearRecentSearches() = update { it.copy(recentSearches = emptyList()) }

    suspend fun clearSavedApplicant() = update { it.copy(savedApplicant = Applicant()) }

    suspend fun incrementExportCount() = update { it.copy(exportCount = it.exportCount + 1) }

    private object K {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val theme = stringPreferencesKey("theme")
        val language = stringPreferencesKey("language")
        val userName = stringPreferencesKey("user_name")
        val profileStamp = longPreferencesKey("profile_image_stamp")
        val paper = stringPreferencesKey("paper")
        val font = stringPreferencesKey("font")
        val fontSize = floatPreferencesKey("font_size")
        val margins = stringPreferencesKey("margins")
        val lineSpacing = floatPreferencesKey("line_spacing")
        val docLanguage = stringPreferencesKey("doc_language")
        val autosave = booleanPreferencesKey("autosave")
        val charCount = booleanPreferencesKey("char_count")
        val haptics = booleanPreferencesKey("haptics")
        val exportName = stringPreferencesKey("export_name_format")
        val recentDocs = intPreferencesKey("recent_docs_count")
        val saveApplicant = booleanPreferencesKey("save_applicant")
        val recentSearches = stringPreferencesKey("recent_searches")
        val exportCount = intPreferencesKey("export_count")

        val apName = stringPreferencesKey("ap_name")
        val apParent = stringPreferencesKey("ap_parent")
        val apAddress = stringPreferencesKey("ap_address")
        val apVillage = stringPreferencesKey("ap_village")
        val apPost = stringPreferencesKey("ap_post")
        val apBlock = stringPreferencesKey("ap_block")
        val apDistrict = stringPreferencesKey("ap_district")
        val apState = stringPreferencesKey("ap_state")
        val apPin = stringPreferencesKey("ap_pin")
        val apMobile = stringPreferencesKey("ap_mobile")
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val d = UserPreferences()
        return UserPreferences(
            onboardingDone = this[K.onboardingDone] ?: d.onboardingDone,
            theme = enumOr(this[K.theme], d.theme),
            language = AppLanguage.fromCode(this[K.language] ?: d.language.code),
            userName = this[K.userName] ?: d.userName,
            profileImageStamp = this[K.profileStamp] ?: d.profileImageStamp,
            paper = enumOr(this[K.paper], d.paper),
            font = enumOr(this[K.font], d.font),
            fontSizePt = this[K.fontSize] ?: d.fontSizePt,
            margins = enumOr(this[K.margins], d.margins),
            lineSpacing = this[K.lineSpacing] ?: d.lineSpacing,
            docLanguage = AppLanguage.fromCode(this[K.docLanguage] ?: d.docLanguage.code),
            autosave = this[K.autosave] ?: d.autosave,
            charCount = this[K.charCount] ?: d.charCount,
            haptics = this[K.haptics] ?: d.haptics,
            exportNameFormat = enumOr(this[K.exportName], d.exportNameFormat),
            recentDocsCount = this[K.recentDocs] ?: d.recentDocsCount,
            saveApplicant = this[K.saveApplicant] ?: d.saveApplicant,
            savedApplicant = Applicant(
                name = this[K.apName].orEmpty(),
                parent = this[K.apParent].orEmpty(),
                address = this[K.apAddress].orEmpty(),
                village = this[K.apVillage].orEmpty(),
                postOffice = this[K.apPost].orEmpty(),
                block = this[K.apBlock].orEmpty(),
                district = this[K.apDistrict].orEmpty(),
                state = this[K.apState].orEmpty(),
                pin = this[K.apPin].orEmpty(),
                mobile = this[K.apMobile].orEmpty(),
            ),
            recentSearches = this[K.recentSearches].orEmpty().split(SEP).filter { it.isNotBlank() },
            exportCount = this[K.exportCount] ?: d.exportCount,
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(p: UserPreferences) {
        this[K.onboardingDone] = p.onboardingDone
        this[K.theme] = p.theme.name
        this[K.language] = p.language.code
        this[K.userName] = p.userName
        this[K.profileStamp] = p.profileImageStamp
        this[K.paper] = p.paper.name
        this[K.font] = p.font.name
        this[K.fontSize] = p.fontSizePt
        this[K.margins] = p.margins.name
        this[K.lineSpacing] = p.lineSpacing
        this[K.docLanguage] = p.docLanguage.code
        this[K.autosave] = p.autosave
        this[K.charCount] = p.charCount
        this[K.haptics] = p.haptics
        this[K.exportName] = p.exportNameFormat.name
        this[K.recentDocs] = p.recentDocsCount
        this[K.saveApplicant] = p.saveApplicant
        this[K.recentSearches] = p.recentSearches.joinToString(SEP)
        this[K.exportCount] = p.exportCount
        val a = p.savedApplicant
        this[K.apName] = a.name
        this[K.apParent] = a.parent
        this[K.apAddress] = a.address
        this[K.apVillage] = a.village
        this[K.apPost] = a.postOffice
        this[K.apBlock] = a.block
        this[K.apDistrict] = a.district
        this[K.apState] = a.state
        this[K.apPin] = a.pin
        this[K.apMobile] = a.mobile
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val SEP = "\u001F"
        const val MAX_RECENT_SEARCHES = 8
    }
}
