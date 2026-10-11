package app.writer.features.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.writer.common.DateText
import app.writer.common.DocTypes
import app.writer.core.LocalAppLanguage
import app.writer.core.tr
import app.writer.design.components.GlassDialog
import app.writer.design.components.GlassMenuAction
import app.writer.design.components.GlassTextField
import app.writer.design.components.GlassToastState
import app.writer.design.components.ToastType
import app.writer.model.AppLanguage
import app.writer.model.DocStatus
import app.writer.model.ExportRecord
import app.writer.model.WriterDocument
import kotlinx.coroutines.launch

/** Which rename / delete dialog is open. Hoisted so one host composable can draw all of them. */
@Stable
class DocDialogs {
    var renameTarget by mutableStateOf<WriterDocument?>(null)
    var deleteTarget by mutableStateOf<WriterDocument?>(null)
    var deletePdfTarget by mutableStateOf<ExportRecord?>(null)
}

@Composable
fun rememberDocDialogs(): DocDialogs = remember { DocDialogs() }

private const val MAX_TITLE = 100

@Composable
fun DocDialogHost(dialogs: DocDialogs, library: LibraryViewModel, toast: GlassToastState) {
    dialogs.renameTarget?.let { doc ->
        var text by remember(doc.id) { mutableStateOf(doc.title) }
        var error by remember(doc.id) { mutableStateOf(false) }
        val renamed = tr("नाम बदल दिया गया", "Renamed")
        val emptyError = tr("नाम खाली नहीं हो सकता", "The name cannot be empty")
        GlassDialog(
            title = tr("नाम बदलें", "Rename"),
            message = null,
            confirmText = tr("सेव करें", "Save"),
            dismissText = tr("रद्द करें", "Cancel"),
            onConfirm = {
                val clean = text.trim()
                if (clean.isEmpty()) {
                    error = true
                } else {
                    library.rename(doc.id, clean)
                    dialogs.renameTarget = null
                    toast.show(renamed, ToastType.Success)
                }
            },
            onDismiss = { dialogs.renameTarget = null },
        ) {
            GlassTextField(
                value = text,
                onValueChange = {
                    text = it.take(MAX_TITLE)
                    error = false
                },
                label = tr("दस्तावेज़ का नाम", "Document name"),
                error = if (error) emptyError else null,
            )
        }
    }

    dialogs.deleteTarget?.let { doc ->
        val deleted = tr("दस्तावेज़ हटा दिया गया", "Document deleted")
        GlassDialog(
            title = tr("यह दस्तावेज़ हटाएं?", "Delete this document?"),
            message = tr(
                "“${doc.title}” हमेशा के लिए हट जाएगा। इसे वापस नहीं लाया जा सकता।",
                "“${doc.title}” will be deleted permanently. This cannot be undone.",
            ),
            confirmText = tr("हटाएं", "Delete"),
            dismissText = tr("रद्द करें", "Cancel"),
            destructive = true,
            onConfirm = {
                library.delete(doc.id)
                dialogs.deleteTarget = null
                toast.show(deleted, ToastType.Success)
            },
            onDismiss = { dialogs.deleteTarget = null },
        )
    }

    dialogs.deletePdfTarget?.let { record ->
        val deleted = tr("PDF रिकॉर्ड हटा दिया गया", "PDF removed from the list")
        GlassDialog(
            title = tr("यह PDF सूची से हटाएं?", "Remove this PDF from the list?"),
            message = tr(
                "ऐप में रखी प्रति हट जाएगी। आपने जिस फ़ोल्डर में PDF सेव की थी, वहाँ की फ़ाइल नहीं हटेगी।",
                "The copy kept in the app will be removed. The file you saved in another folder is not deleted.",
            ),
            confirmText = tr("हटाएं", "Remove"),
            dismissText = tr("रद्द करें", "Cancel"),
            destructive = true,
            onConfirm = {
                library.deleteExport(record.id)
                dialogs.deletePdfTarget = null
                toast.show(deleted, ToastType.Success)
            },
            onDismiss = { dialogs.deletePdfTarget = null },
        )
    }
}

/** Display strings for a document card. */
@Composable
fun docSubtitle(doc: WriterDocument): String {
    val hindi = LocalAppLanguage.current == AppLanguage.HI
    return DocTypes.label(doc.templateId, LocalAppLanguage.current) + " • " + DateText.listDate(doc.updatedAt, hindi)
}

@Composable
fun docStatusLabel(status: DocStatus): String =
    if (status == DocStatus.DRAFT) tr("ड्राफ्ट", "Draft") else tr("पूर्ण", "Completed")

/** The three-dot menu of a saved document: Open, Rename, Duplicate, Export PDF, Favorite, Delete. */
@Composable
fun documentMenu(
    doc: WriterDocument,
    library: LibraryViewModel,
    dialogs: DocDialogs,
    toast: GlassToastState,
    onOpen: (String) -> Unit,
    onExport: (String) -> Unit,
    onPreview: ((String) -> Unit)? = null,
    onShare: ((String) -> Unit)? = null,
): List<GlassMenuAction> {
    val scope = rememberCoroutineScope()
    val copySuffix = tr(" (प्रति)", " (copy)")
    val copied = tr("प्रति बन गई", "Copy created")
    val failed = tr("प्रति नहीं बन सकी", "Could not create a copy")
    return listOfNotNull(
        GlassMenuAction(tr("खोलें", "Open"), Icons.Rounded.Edit, { onOpen(doc.id) }),
        onPreview?.let { GlassMenuAction(tr("प्रीव्यू देखें", "Preview"), Icons.Rounded.Visibility, { it(doc.id) }) },
        GlassMenuAction(tr("नाम बदलें", "Rename"), Icons.Rounded.DriveFileRenameOutline, { dialogs.renameTarget = doc }),
        GlassMenuAction(
            tr("प्रति बनाएं", "Duplicate"),
            Icons.Rounded.ContentCopy,
            {
                scope.launch {
                    val id = library.duplicate(doc.id, copySuffix)
                    if (id != null) toast.show(copied, ToastType.Success) else toast.show(failed, ToastType.Error)
                }
            },
        ),
        GlassMenuAction(tr("PDF बनाएं", "Export PDF"), Icons.Rounded.PictureAsPdf, { onExport(doc.id) }),
        onShare?.let { GlassMenuAction(tr("शेयर करें", "Share"), Icons.Rounded.Share, { it(doc.id) }) },
        GlassMenuAction(
            if (doc.isFavorite) tr("पसंदीदा से हटाएं", "Remove from favorites") else tr("पसंदीदा में जोड़ें", "Add to favorites"),
            if (doc.isFavorite) Icons.Rounded.StarBorder else Icons.Rounded.Star,
            { library.setDocumentFavorite(doc.id, !doc.isFavorite) },
        ),
        GlassMenuAction(tr("हटाएं", "Delete"), Icons.Rounded.Delete, { dialogs.deleteTarget = doc }, destructive = true),
    )
}
