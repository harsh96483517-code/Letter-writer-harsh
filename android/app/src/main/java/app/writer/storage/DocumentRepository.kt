package app.writer.storage

import app.writer.database.DocumentDao
import app.writer.database.ExportDao
import app.writer.model.DocStatus
import app.writer.model.ExportRecord
import app.writer.model.WriterDocument
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

/** Documents and the records of exported PDFs. Everything stays on the device. */
class DocumentRepository(
    private val documents: DocumentDao,
    private val exportDao: ExportDao,
) {
    val all: Flow<List<WriterDocument>> = documents.observeAll()
    val exports: Flow<List<ExportRecord>> = exportDao.observeAll()

    fun observe(id: String): Flow<WriterDocument?> = documents.observe(id)

    suspend fun get(id: String): WriterDocument? = documents.get(id)

    suspend fun latestDraft(): WriterDocument? = documents.latestDraft()

    /** Saves the document and stamps it as modified now. Returns the saved copy. */
    suspend fun save(doc: WriterDocument, now: Long): WriterDocument {
        val saved = doc.copy(updatedAt = now)
        documents.upsert(saved)
        return saved
    }

    suspend fun rename(id: String, title: String, now: Long) = documents.rename(id, title.trim(), now)

    suspend fun setFavorite(id: String, favorite: Boolean) = documents.setFavorite(id, favorite)

    suspend fun setStatus(id: String, status: DocStatus, now: Long) = documents.setStatus(id, status.name, now)

    /** Makes an independent copy (new id, draft status) and returns its id. */
    suspend fun duplicate(id: String, titleSuffix: String, now: Long): String? {
        val original = documents.get(id) ?: return null
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            title = original.title + titleSuffix,
            status = DocStatus.DRAFT,
            isFavorite = false,
            createdAt = now,
            updatedAt = now,
        )
        documents.upsert(copy)
        return copy.id
    }

    suspend fun delete(id: String) = documents.delete(id)

    suspend fun deleteAll() = documents.deleteAll()

    // --- exported PDFs ---

    suspend fun recordExport(record: ExportRecord) = exportDao.upsert(record)

    suspend fun exportRecord(id: String): ExportRecord? = exportDao.get(id)

    /** Removes the record and the app-private copy. A copy the user saved elsewhere is never touched. */
    suspend fun deleteExport(id: String) {
        exportDao.get(id)?.let { runCatching { File(it.internalPath).delete() } }
        exportDao.delete(id)
    }

    suspend fun deleteAllExports() {
        exportDao.getAll().forEach { runCatching { File(it.internalPath).delete() } }
        exportDao.deleteAll()
    }
}
