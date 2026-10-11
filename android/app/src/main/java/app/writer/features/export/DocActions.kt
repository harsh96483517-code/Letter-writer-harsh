package app.writer.features.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import app.writer.AppContainer
import app.writer.common.DocumentChecks
import app.writer.common.ExportIssue
import app.writer.common.FileNames
import app.writer.container
import app.writer.core.LocalAppLanguage
import app.writer.core.pick
import app.writer.core.tr
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterType
import app.writer.design.components.GlassBottomSheet
import app.writer.design.components.GlassButton
import app.writer.design.components.GlassButtonStyle
import app.writer.design.components.GlassDialog
import app.writer.design.components.GlassProgressDialog
import app.writer.design.components.GlassText
import app.writer.design.components.GlassToastState
import app.writer.design.components.ToastType
import app.writer.model.AppLanguage
import app.writer.model.DocStatus
import app.writer.model.ExportRecord
import app.writer.model.PaperSize
import app.writer.model.WriterDocument
import app.writer.pdf.PdfPrintAdapter
import app.writer.pdf.PdfResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** What the user asked for once the document has passed the pre-export checks. */
internal enum class Task { SAVE, SHARE, PRINT }

internal class Confirm(val docId: String, val issues: List<ExportIssue>, val task: Task)

class ExportDone(val record: ExportRecord, val usedFallbackFonts: Boolean)

/**
 * Everything that gets a document out of the app: save as PDF through the system file picker,
 * share, print, and open or share an already exported PDF. Work runs in the app scope, so a
 * finished export is always recorded even if the screen it started on is gone.
 *
 * A message only says "saved", "shared" or "printed" when that really happened; failures say so.
 */
@Stable
class DocActions internal constructor(
    private val container: AppContainer,
    private val toast: GlassToastState,
) {
    internal var language: AppLanguage = AppLanguage.HI
    internal var activityContext: Context? = null
    internal var launchPicker: (String) -> Unit = {}

    internal var confirm by mutableStateOf<Confirm?>(null)
    internal var busy by mutableStateOf<String?>(null)
    internal var done by mutableStateOf<ExportDone?>(null)

    private var pendingSave: WriterDocument? = null

    private val appContext: Context get() = activityContext!!.applicationContext
    private fun t(hi: String, en: String) = pick(language, hi, en)

    // --- entry points ---

    fun exportPdf(docId: String) = begin(docId, Task.SAVE)

    fun share(docId: String) = begin(docId, Task.SHARE)

    fun print(docId: String) = begin(docId, Task.PRINT)

    private fun begin(docId: String, task: Task) {
        container.appScope.launch {
            val doc = container.documents.get(docId)
            if (doc == null) {
                toast.show(t("दस्तावेज़ नहीं मिला", "Document not found"), ToastType.Error)
                return@launch
            }
            val issues = DocumentChecks.issues(doc)
            when {
                ExportIssue.EmptyDocument in issues ->
                    toast.show(t("दस्तावेज़ खाली है। पहले कुछ लिखें।", "The document is empty. Write something first."), ToastType.Error)
                issues.isNotEmpty() -> confirm = Confirm(docId, issues, task)
                else -> proceed(doc, task)
            }
        }
    }

    internal fun confirmed() {
        val c = confirm ?: return
        confirm = null
        container.appScope.launch {
            val doc = container.documents.get(c.docId) ?: return@launch
            proceed(doc, c.task)
        }
    }

    internal fun dismissConfirm() {
        confirm = null
    }

    internal fun dismissDone() {
        done = null
    }

    private suspend fun proceed(doc: WriterDocument, task: Task) {
        when (task) {
            Task.SAVE -> {
                val prefs = container.settings.preferences.first()
                pendingSave = doc
                val name = FileNames.suggest(doc, prefs.exportNameFormat, FileNames.today())
                withContext(Dispatchers.Main) { launchPicker(name) }
            }
            Task.SHARE -> shareDocument(doc)
            Task.PRINT -> printDocument(doc)
        }
    }

    // --- save ---

    /** Called with the location the user picked in the system file picker, or null if they backed out. */
    internal fun onSaveTarget(uri: Uri?) {
        val doc = pendingSave
        pendingSave = null
        if (uri == null || doc == null) return
        container.appScope.launch { saveTo(uri, doc) }
    }

    private suspend fun saveTo(uri: Uri, doc: WriterDocument) {
        busy = t("आपकी PDF बन रही है…", "Creating your PDF…")
        try {
            val now = System.currentTimeMillis()
            val file = File(File(appContext.filesDir, EXPORT_DIR), "${doc.id}-$now.pdf")
            val result = withContext(Dispatchers.IO) { container.pdfExporter.writeToFile(doc, file) }
            val displayName = withContext(Dispatchers.IO) { displayNameOf(uri) }
                ?: FileNames.withPdfExtension(doc.title)
            try {
                withContext(Dispatchers.IO) {
                    val out = appContext.contentResolver.openOutputStream(uri, "wt")
                        ?: throw java.io.IOException("The chosen location is not writable")
                    out.use { sink -> file.inputStream().use { it.copyTo(sink) } }
                }
            } catch (e: Exception) {
                // Do not leave an empty or partial file at the place the user chose.
                runCatching { DocumentsContract.deleteDocument(appContext.contentResolver, uri) }
                runCatching { file.delete() }
                throw e
            }
            val record = ExportRecord(
                id = UUID.randomUUID().toString(),
                documentId = doc.id,
                documentTitle = doc.title,
                fileName = displayName,
                internalPath = file.absolutePath,
                sizeBytes = file.length(),
                pageCount = result.pageCount,
                createdAt = now,
            )
            container.documents.recordExport(record)
            container.settings.incrementExportCount()
            container.documents.setStatus(doc.id, DocStatus.COMPLETED, now)
            done = ExportDone(record, result.usedFallbackFonts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            toast.show(
                t("PDF सेव नहीं हो सकी। दोबारा कोशिश करें या कोई और जगह चुनें।", "The PDF could not be saved. Try again or choose another location."),
                ToastType.Error,
            )
        } finally {
            busy = null
        }
    }

    private fun displayNameOf(uri: Uri): String? =
        runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()

    // --- share and print (rendered into the cache, so nothing is added to My Files) ---

    private suspend fun renderToCache(doc: WriterDocument): Pair<File, PdfResult> = withContext(Dispatchers.IO) {
        val prefs = container.settings.preferences.first()
        val name = FileNames.suggest(doc, prefs.exportNameFormat, FileNames.today())
        val dir = File(appContext.cacheDir, CACHE_DIR).apply { mkdirs() }
        // Old shared copies are only needed for the moment of sharing.
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > STALE_MS }?.forEach { it.delete() }
        val file = File(dir, name)
        file to container.pdfExporter.writeToFile(doc, file)
    }

    private suspend fun shareDocument(doc: WriterDocument) {
        busy = t("शेयर के लिए PDF बन रही है…", "Preparing the PDF to share…")
        try {
            val (file, _) = renderToCache(doc)
            withContext(Dispatchers.Main) { startShare(file, doc.title) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            toast.show(t("PDF बनाई नहीं जा सकी।", "The PDF could not be created."), ToastType.Error)
        } finally {
            busy = null
        }
    }

    private suspend fun printDocument(doc: WriterDocument) {
        busy = t("प्रिंट के लिए PDF बन रही है…", "Preparing the PDF to print…")
        try {
            val (file, result) = renderToCache(doc)
            withContext(Dispatchers.Main) { startPrint(file, doc, result.pageCount) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            toast.show(t("प्रिंट के लिए PDF नहीं बन सकी।", "The PDF could not be prepared for printing."), ToastType.Error)
        } finally {
            busy = null
        }
    }

    private fun startPrint(file: File, doc: WriterDocument, pageCount: Int) {
        val ctx = activityContext ?: return
        val manager = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (manager == null) {
            toast.show(t("इस फ़ोन पर प्रिंट सुविधा उपलब्ध नहीं है।", "Printing is not available on this device."), ToastType.Error)
            return
        }
        val jobName = doc.title.ifBlank { "Writer" }
        val attributes = PrintAttributes.Builder()
            .setMediaSize(
                when (doc.formatting.paper) {
                    PaperSize.A4 -> PrintAttributes.MediaSize.ISO_A4
                    PaperSize.A5 -> PrintAttributes.MediaSize.ISO_A5
                    PaperSize.LETTER -> PrintAttributes.MediaSize.NA_LETTER
                },
            )
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        val job = try {
            manager.print(jobName, PdfPrintAdapter(file, jobName, pageCount), attributes)
        } catch (e: Exception) {
            toast.show(t("प्रिंट शुरू नहीं हो सका।", "Printing could not be started."), ToastType.Error)
            return
        }
        watchPrintJob(job)
    }

    /** Reports what the print system actually did instead of assuming it worked. */
    private fun watchPrintJob(job: PrintJob) {
        container.appScope.launch {
            var announcedSent = false
            repeat(PRINT_WATCH_STEPS) {
                delay(PRINT_WATCH_MS)
                when {
                    job.isFailed -> {
                        toast.show(t("प्रिंट विफल रहा।", "The print job failed."), ToastType.Error)
                        return@launch
                    }
                    job.isCancelled -> {
                        toast.show(t("प्रिंट रद्द किया गया।", "Printing was cancelled."), ToastType.Info)
                        return@launch
                    }
                    job.isCompleted -> {
                        toast.show(t("प्रिंट पूरा हुआ।", "Printing finished."), ToastType.Success)
                        return@launch
                    }
                    (job.isQueued || job.isStarted) && !announcedSent -> {
                        announcedSent = true
                        toast.show(t("प्रिंट जॉब प्रिंटर को भेजा गया।", "The print job was sent to the printer."), ToastType.Info)
                    }
                }
            }
        }
    }

    // --- exported PDFs ---

    fun openPdf(record: ExportRecord) {
        val file = File(record.internalPath)
        if (!file.exists()) {
            toast.show(t("यह PDF अब ऐप में नहीं है।", "This PDF is no longer stored in the app."), ToastType.Error)
            return
        }
        val uri = uriFor(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            (activityContext ?: appContext).startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast.show(t("PDF खोलने के लिए कोई ऐप नहीं मिला।", "No app was found to open PDFs."), ToastType.Error)
        }
    }

    fun sharePdf(record: ExportRecord) {
        val file = File(record.internalPath)
        if (!file.exists()) {
            toast.show(t("यह PDF अब ऐप में नहीं है।", "This PDF is no longer stored in the app."), ToastType.Error)
            return
        }
        startShare(file, record.documentTitle)
    }

    private fun startShare(file: File, title: String) {
        val uri = uriFor(file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            clipData = ClipData.newRawUri(title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            (activityContext ?: appContext).startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (e: ActivityNotFoundException) {
            toast.show(t("शेयर करने के लिए कोई ऐप नहीं मिला।", "No app was found to share with."), ToastType.Error)
        }
    }

    private fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.files", file)

    private companion object {
        const val EXPORT_DIR = "exports"
        const val CACHE_DIR = "shared"
        const val STALE_MS = 60 * 60 * 1000L
        const val PRINT_WATCH_MS = 700L
        const val PRINT_WATCH_STEPS = 600
    }
}

@Composable
fun rememberDocActions(toast: GlassToastState): DocActions {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val actions = remember { DocActions(context.container, toast) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        actions.onSaveTarget(uri)
    }
    SideEffect {
        actions.language = language
        actions.activityContext = context
        actions.launchPicker = { name -> launcher.launch(name) }
    }
    return actions
}

/** The dialogs that belong to [DocActions]. Place it once near the root of the screen that uses it. */
@Composable
fun DocActionsHost(actions: DocActions) {
    val c = Glass.colors
    actions.confirm?.let { confirm ->
        val lines = confirm.issues.map { issue ->
            when (issue) {
                ExportIssue.EmptyDocument -> ""
                ExportIssue.MissingSubject -> tr("विषय खाली है।", "The subject line is empty.")
                is ExportIssue.Blanks -> tr(
                    "${issue.count} जगह अभी भरनी बाकी है (चौकोर कोष्ठक [ ] में दिखती हैं)।",
                    "${issue.count} placeholder(s) are still unfilled (shown in square brackets [ ]).",
                )
            }
        }.filter { it.isNotEmpty() }
        GlassDialog(
            title = tr("पत्र अभी अधूरा लग रहा है", "This letter looks unfinished"),
            message = lines.joinToString("\n") + "\n\n" + tr("क्या फिर भी आगे बढ़ना है?", "Do you want to continue anyway?"),
            confirmText = tr("फिर भी आगे बढ़ें", "Continue anyway"),
            dismissText = tr("वापस जाएं", "Go back"),
            onConfirm = actions::confirmed,
            onDismiss = actions::dismissConfirm,
        )
    }

    actions.busy?.let { GlassProgressDialog(message = it) }

    actions.done?.let { done ->
        GlassBottomSheet(
            onDismiss = actions::dismissDone,
            title = tr("PDF सेव हो गई", "PDF saved"),
        ) {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.x1_5)) {
                GlassText(
                    text = tr(
                        "${done.record.fileName} — ${done.record.pageCount} पेज",
                        "${done.record.fileName} — ${done.record.pageCount} page(s)",
                    ),
                    style = WriterType.bodySmall,
                    color = c.textSecondary,
                )
                if (done.usedFallbackFonts) {
                    GlassText(
                        text = tr(
                            "कुछ अक्षरों के लिए सामान्य फ़ॉन्ट इस्तेमाल हुआ। कृपया PDF एक बार देख लें।",
                            "A fallback font was used for some characters. Please check the PDF once.",
                        ),
                        style = WriterType.caption,
                        color = c.warning,
                    )
                }
                GlassButton(
                    text = tr("PDF खोलें", "Open PDF"),
                    icon = Icons.Rounded.OpenInNew,
                    onClick = { actions.openPdf(done.record) },
                    modifier = Modifier.padding(top = Spacing.half),
                )
                GlassButton(
                    text = tr("शेयर करें", "Share"),
                    icon = Icons.Rounded.Share,
                    style = GlassButtonStyle.Secondary,
                    onClick = { actions.sharePdf(done.record) },
                )
                GlassButton(
                    text = tr("ठीक है", "Done"),
                    icon = Icons.Rounded.CheckCircle,
                    style = GlassButtonStyle.Secondary,
                    onClick = actions::dismissDone,
                )
            }
        }
    }
}
