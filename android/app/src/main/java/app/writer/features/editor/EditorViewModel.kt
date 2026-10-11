package app.writer.features.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.writer.AppContainer
import app.writer.common.DocumentFactory
import app.writer.common.SpanMath
import app.writer.model.AppLanguage
import app.writer.model.Applicant
import app.writer.model.Formatting
import app.writer.model.Recipient
import app.writer.model.TemplateDef
import app.writer.model.TextSpan
import app.writer.model.WriterDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SaveState { SAVED, SAVING, UNSAVED, FAILED }

/** What the body looked like at one point, for undo and redo. */
private class BodySnapshot(val body: String, val spans: List<TextSpan>, val bodyEdited: Boolean)

/**
 * State of one open letter. Edits change the in-memory document at once; they reach the database
 * after a short pause (autosave), when the user presses Save, when the screen is left and when the
 * app goes to the background. The document id never changes, so reopening always finds the same letter.
 */
@Stable
class EditorViewModel(
    private val container: AppContainer,
    private val docId: String,
) : ViewModel() {

    var doc by mutableStateOf<WriterDocument?>(null)
        private set
    var template by mutableStateOf<TemplateDef?>(null)
        private set
    var missing by mutableStateOf(false)
        private set
    var saveState by mutableStateOf(SaveState.SAVED)
        private set

    /**
     * Goes up whenever the body text is replaced by something other than typing (undo, redo,
     * rebuilding from the template, answering a template question). The text field reloads on it.
     */
    var bodyRevision by mutableIntStateOf(0)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    /** Set by the screen from Settings. When off, only an explicit Save writes to the database. */
    var autosave: Boolean = true

    val hasUnsavedChanges: Boolean get() = saveState == SaveState.UNSAVED || saveState == SaveState.FAILED

    private var saveJob: Job? = null
    private var dirty = false
    private val undoStack = ArrayDeque<BodySnapshot>()
    private val redoStack = ArrayDeque<BodySnapshot>()
    private var lastBodyEditAt = 0L

    init {
        viewModelScope.launch {
            val loaded = container.documents.get(docId)
            if (loaded == null) {
                missing = true
            } else {
                template = loaded.templateId?.let { container.templates.find(it) }
                doc = loaded
            }
        }
    }

    // --- plain fields ---

    fun setTitle(value: String) = change { it.copy(title = value) }

    fun setRecipient(value: Recipient) = change { regenerate(it.copy(recipient = value)) }

    fun setApplicant(value: Applicant) = change { regenerate(it.copy(applicant = value)) }

    fun setField(key: String, value: String) =
        change { regenerate(it.copy(fieldValues = it.fieldValues + (key to value))) }

    fun setDate(value: String) = change { regenerate(it.copy(dateText = value)) }

    fun setSubject(value: String) = change { it.copy(subject = value, subjectEdited = true) }

    fun setSalutation(value: String) = change { it.copy(salutation = value) }

    fun setClosing(value: String) = change { it.copy(closing = value) }

    fun setFormatting(value: Formatting) = change { it.copy(formatting = value) }

    /** Changes the language of the letter. Text the user has not touched follows the template's wording. */
    fun setLanguage(language: AppLanguage) = change { current ->
        if (current.language == language) return@change current
        val t = template
        var next = current.copy(language = language)
        if (t != null && !t.isBlank) {
            val old = current.language
            if (current.title == t.title[old]) next = next.copy(title = t.title[language])
            if (current.salutation == t.salutation[old]) next = next.copy(salutation = t.salutation[language])
            if (current.closing == t.closing[old]) next = next.copy(closing = t.closing[language])
            t.recipient?.let { r ->
                var rec = next.recipient
                if (rec.designation == r.designation[old]) rec = rec.copy(designation = r.designation[language])
                if (rec.office == r.office[old]) rec = rec.copy(office = r.office[language])
                next = next.copy(recipient = rec)
            }
        }
        regenerate(next)
    }

    // --- body ---

    /** The user typed, deleted or pasted in the body. Bold and italic runs follow the text. */
    fun onBodyChanged(newText: String) {
        val current = doc ?: return
        if (newText == current.body) return
        val now = System.currentTimeMillis()
        // One undo step per burst of typing, not per letter.
        if (undoStack.isEmpty() || now - lastBodyEditAt > UNDO_BURST_MS) pushUndo(current)
        lastBodyEditAt = now
        redoStack.clear()
        val spans = SpanMath.applyEdit(current.bodySpans, current.body, newText)
        change { it.copy(body = newText, bodySpans = spans, bodyEdited = true) }
        refreshHistoryFlags()
    }

    /** Sets bold or italic on the selected text; if all of it already has the style, removes it. */
    fun toggleStyle(start: Int, end: Int, italic: Boolean) {
        val current = doc ?: return
        if (end <= start) return
        val all = SpanMath.isAll(current.bodySpans, start, end, italic)
        val spans = SpanMath.toggle(
            spans = current.bodySpans,
            start = start,
            end = end,
            bold = if (italic) null else !all,
            italic = if (italic) !all else null,
            textLength = current.body.length,
        )
        pushUndo(current)
        lastBodyEditAt = 0L
        redoStack.clear()
        change { it.copy(bodySpans = spans, bodyEdited = true) }
        refreshHistoryFlags()
    }

    fun undo() {
        val current = doc ?: return
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(snapshotOf(current))
        restore(current, previous)
    }

    fun redo() {
        val current = doc ?: return
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(snapshotOf(current))
        restore(current, next)
    }

    /** Writes the whole letter again from the template and the form answers. Typed text is replaced. */
    fun rebuildFromTemplate() {
        val t = template ?: return
        val current = doc ?: return
        pushUndo(current)
        redoStack.clear()
        lastBodyEditAt = 0L
        change { DocumentFactory.resetBody(it, t) }
        bodyRevision++
        refreshHistoryFlags()
    }

    private fun restore(current: WriterDocument, snapshot: BodySnapshot) {
        lastBodyEditAt = 0L
        change { it.copy(body = snapshot.body, bodySpans = snapshot.spans, bodyEdited = snapshot.bodyEdited) }
        if (doc?.body != current.body) bodyRevision++
        refreshHistoryFlags()
    }

    private fun pushUndo(current: WriterDocument) {
        undoStack.addLast(snapshotOf(current))
        if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
    }

    private fun snapshotOf(d: WriterDocument) = BodySnapshot(d.body, d.bodySpans, d.bodyEdited)

    private fun refreshHistoryFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    // --- template questions ---

    /** Re-fills subject and body from the template. Parts the user wrote themselves are kept. */
    private fun regenerate(d: WriterDocument): WriterDocument {
        val out = DocumentFactory.regenerate(d, template)
        if (out.body != d.body) {
            bodyRevision++
            // A regenerated body is a new starting point, so earlier typing history no longer applies.
            undoStack.clear()
            redoStack.clear()
            refreshHistoryFlags()
        }
        return out
    }

    // --- saving ---

    private inline fun change(transform: (WriterDocument) -> WriterDocument) {
        val current = doc ?: return
        val next = transform(current)
        if (next == current) return
        doc = next
        dirty = true
        saveState = SaveState.UNSAVED
        if (autosave) scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            saveNow()
        }
    }

    /** Saves right away (the Save button, opening the preview, leaving the screen). */
    suspend fun saveNow(): Boolean {
        saveJob?.cancel()
        val snapshot = doc ?: return false
        if (!dirty && saveState == SaveState.SAVED) return true
        saveState = SaveState.SAVING
        return try {
            persist(snapshot)
            if (doc == snapshot) {
                dirty = false
                saveState = SaveState.SAVED
            } else {
                // The user kept typing while the save ran; the next autosave picks that up.
                saveState = SaveState.UNSAVED
                if (autosave) scheduleSave()
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            saveState = SaveState.FAILED
            false
        }
    }

    /** The app is going to the background: keep what was typed even if the process is killed later. */
    fun flushInBackground() {
        val snapshot = doc ?: return
        if (!dirty || !autosave) return
        container.appScope.launch { runCatching { persist(snapshot) } }
    }

    private suspend fun persist(snapshot: WriterDocument) {
        withContext(NonCancellable) {
            container.documents.save(snapshot, System.currentTimeMillis())
            // Applicant details are only remembered when the user switched that on in Settings.
            val prefs = container.settings.preferences.first()
            if (prefs.saveApplicant && prefs.savedApplicant != snapshot.applicant) {
                container.settings.update { it.copy(savedApplicant = snapshot.applicant) }
            }
        }
    }

    override fun onCleared() {
        // Leaving the screen with edits still waiting for the pause: write them now.
        val snapshot = doc
        if (snapshot != null && dirty && autosave) {
            container.appScope.launch { runCatching { persist(snapshot) } }
        }
        super.onCleared()
    }

    private companion object {
        const val AUTOSAVE_DELAY_MS = 700L
        const val UNDO_BURST_MS = 1_000L
        const val MAX_UNDO = 100
    }
}
