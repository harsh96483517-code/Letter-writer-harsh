package app.writer.features.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.writer.AppContainer
import app.writer.model.DocStatus
import app.writer.model.WriterDocument
import app.writer.pdf.LaidOutDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PreviewState {
    data object Loading : PreviewState
    data object Missing : PreviewState
    data object Failed : PreviewState
    class Ready(val doc: WriterDocument, val laidOut: LaidOutDocument) : PreviewState
}

/**
 * Lays the saved document out with the same engine the PDF writer uses. The work runs off the
 * main thread and repeats whenever the saved document changes.
 */
class PreviewViewModel(
    private val container: AppContainer,
    private val docId: String,
) : ViewModel() {

    val state: StateFlow<PreviewState> = container.documents.observe(docId)
        .distinctUntilChanged()
        .map<WriterDocument?, PreviewState> { doc ->
            if (doc == null) {
                PreviewState.Missing
            } else {
                try {
                    PreviewState.Ready(doc, container.letterEngine.layout(doc))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    PreviewState.Failed
                }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreviewState.Loading)

    /** The letter is already stored; this files it under "Completed" in My Files. */
    fun markCompleted() {
        container.appScope.launch {
            container.documents.setStatus(docId, DocStatus.COMPLETED, System.currentTimeMillis())
        }
    }
}
