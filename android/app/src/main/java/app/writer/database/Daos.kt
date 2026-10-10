package app.writer.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.writer.model.ExportRecord
import app.writer.model.TemplateState
import app.writer.model.WriterDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WriterDocument>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observe(id: String): Flow<WriterDocument?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun get(id: String): WriterDocument?

    @Query("SELECT * FROM documents WHERE status = 'DRAFT' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun latestDraft(): WriterDocument?

    @Upsert
    suspend fun upsert(document: WriterDocument)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM documents")
    suspend fun deleteAll()

    @Query("UPDATE documents SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: String, title: String, now: Long)

    @Query("UPDATE documents SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE documents SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun setStatus(id: String, status: String, now: Long)
}

@Dao
interface ExportDao {
    @Query("SELECT * FROM exports ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExportRecord>>

    @Query("SELECT * FROM exports WHERE id = :id")
    suspend fun get(id: String): ExportRecord?

    @Query("SELECT * FROM exports")
    suspend fun getAll(): List<ExportRecord>

    @Upsert
    suspend fun upsert(record: ExportRecord)

    @Query("DELETE FROM exports WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM exports")
    suspend fun deleteAll()
}

@Dao
interface TemplateStateDao {
    @Query("SELECT * FROM template_state")
    fun observeAll(): Flow<List<TemplateState>>

    @Query("SELECT * FROM template_state WHERE templateId = :id")
    suspend fun get(id: String): TemplateState?

    @Upsert
    suspend fun upsert(state: TemplateState)

    @Query("DELETE FROM template_state")
    suspend fun deleteAll()
}
