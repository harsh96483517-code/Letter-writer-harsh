package app.writer.features.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.writer.AppContainer
import app.writer.model.DocStatus
import app.writer.model.ExportRecord
import app.writer.model.TemplateItem
import app.writer.model.UserPreferences
import app.writer.model.WriterDocument
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide settings. `null` until the first read finishes, so the UI never flashes the wrong theme. */
class MainViewModel(private val container: AppContainer) : ViewModel() {
    val prefs: StateFlow<UserPreferences?> = container.settings.preferences
        .map<UserPreferences, UserPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Runs in the app scope so a setting is never lost when the screen goes away. */
    fun update(transform: (UserPreferences) -> UserPreferences) {
        container.appScope.launch { container.settings.update(transform) }
    }

    fun clearRecentSearches() {
        container.appScope.launch { container.settings.clearRecentSearches() }
    }

    fun clearSavedApplicant() {
        container.appScope.launch { container.settings.clearSavedApplicant() }
    }

    fun addRecentSearch(query: String) {
        container.appScope.launch { container.settings.addRecentSearch(query) }
    }
}

/** Templates, documents and exported PDFs, shared by Home, Templates, Files and Profile. */
class LibraryViewModel(private val container: AppContainer) : ViewModel() {
    val templates: StateFlow<List<TemplateItem>> = container.templates.items
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** `null` while the first load is running, so screens can show a spinner instead of "empty". */
    val documents: StateFlow<List<WriterDocument>?> = container.documents.all
        .map<List<WriterDocument>, List<WriterDocument>?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val exports: StateFlow<List<ExportRecord>> = container.documents.exports
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setTemplateFavorite(id: String, favorite: Boolean) {
        viewModelScope.launch { container.templates.setFavorite(id, favorite) }
    }

    fun setDocumentFavorite(id: String, favorite: Boolean) {
        viewModelScope.launch { container.documents.setFavorite(id, favorite) }
    }

    fun rename(id: String, title: String) {
        viewModelScope.launch { container.documents.rename(id, title, System.currentTimeMillis()) }
    }

    fun setStatus(id: String, status: DocStatus) {
        viewModelScope.launch { container.documents.setStatus(id, status, System.currentTimeMillis()) }
    }

    fun delete(id: String) {
        container.appScope.launch { container.documents.delete(id) }
    }

    fun deleteExport(id: String) {
        container.appScope.launch { container.documents.deleteExport(id) }
    }

    suspend fun duplicate(id: String, suffix: String): String? =
        container.documents.duplicate(id, suffix, System.currentTimeMillis())

    suspend fun start(templateId: String, language: app.writer.model.AppLanguage? = null): String? =
        container.startDocument(templateId, language)

    suspend fun latestDraftId(): String? = container.documents.latestDraft()?.id

    fun deleteAllDocuments() {
        container.appScope.launch { container.documents.deleteAll() }
    }

    fun clearTemplateUsage() {
        container.appScope.launch { container.templates.clearUsage() }
    }
}
