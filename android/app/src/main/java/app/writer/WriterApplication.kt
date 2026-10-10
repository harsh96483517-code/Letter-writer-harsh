package app.writer

import android.app.Application
import android.content.Context
import app.writer.database.WriterDatabase
import app.writer.pdf.LetterEngine
import app.writer.pdf.PdfExporter
import app.writer.storage.DocumentRepository
import app.writer.storage.SettingsRepository
import app.writer.storage.TemplateRepository
import app.writer.storage.writerDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-wired dependencies: one database, one settings store, three repositories. */
class AppContainer(context: Context) {
    private val database = WriterDatabase.create(context)

    val settings = SettingsRepository(context.applicationContext.writerDataStore)
    val templates = TemplateRepository(context.applicationContext.assets, database.templateStates())
    val documents = DocumentRepository(database.documents(), database.exports())

    val letterEngine = LetterEngine(context)
    val pdfExporter = PdfExporter(letterEngine)

    /** For work that must finish even if the screen that started it goes away (final autosave, export record). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

class WriterApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer
    get() = (applicationContext as WriterApplication).container
