package app.writer.features.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.writer.common.DateText
import app.writer.common.FileEntry
import app.writer.common.FileList
import app.writer.common.FileSort
import app.writer.common.FileTab
import app.writer.common.SizeText
import app.writer.core.LocalAppLanguage
import app.writer.core.tr
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.bottomBarContentPadding
import app.writer.design.components.AccentIconTile
import app.writer.design.components.GlassBadge
import app.writer.design.components.GlassDocumentCard
import app.writer.design.components.GlassEmptyState
import app.writer.design.components.GlassIconButton
import app.writer.design.components.GlassLoadingIndicator
import app.writer.design.components.GlassMenuAction
import app.writer.design.components.GlassSearchBar
import app.writer.design.components.GlassTabBar
import app.writer.design.components.GlassText
import app.writer.design.components.GlassToastState
import app.writer.design.components.glassPanel
import app.writer.design.topContentPadding
import app.writer.features.common.DocDialogs
import app.writer.features.common.LibraryViewModel
import app.writer.features.common.docStatusLabel
import app.writer.features.common.documentMenu
import app.writer.features.export.DocActions
import app.writer.model.AppLanguage
import app.writer.model.DocStatus
import app.writer.model.ExportRecord

@Composable
fun FilesScreen(
    library: LibraryViewModel,
    toast: GlassToastState,
    dialogs: DocDialogs,
    docActions: DocActions,
    onOpenDocument: (String) -> Unit,
    onPreviewDocument: (String) -> Unit,
    onNewBlank: () -> Unit,
) {
    val language = LocalAppLanguage.current
    val documents by library.documents.collectAsStateWithLifecycle()
    val exports by library.exports.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(FileTab.ALL) }
    var sort by rememberSaveable { mutableStateOf(FileSort.DATE) }
    var grid by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val docs = documents
    val entries = if (docs == null) emptyList() else FileList.build(docs, exports, tab, query, sort, language)
    val nothingAtAll = docs != null && docs.isEmpty() && exports.isEmpty()

    val tabLabels = listOf(
        tr("सभी", "All"), tr("ड्राफ्ट", "Drafts"), tr("पूर्ण", "Completed"), "PDF", tr("पसंदीदा", "Favorites"),
    )
    val moreLabel = tr("और विकल्प", "More options")

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(top = topContentPadding())
                .padding(horizontal = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                GlassText(text = tr("मेरी फाइलें", "My Files"), style = WriterType.title, modifier = Modifier.weight(1f))
                SortMenu(sort = sort, onSort = { sort = it })
                GlassIconButton(
                    icon = if (grid) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                    contentDescription = if (grid) tr("सूची दृश्य", "List view") else tr("ग्रिड दृश्य", "Grid view"),
                    onClick = { grid = !grid },
                    size = 44.dp,
                )
            }
            GlassSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = tr("फ़ाइल खोजें", "Search files"),
                clearLabel = tr("खोज साफ़ करें", "Clear search"),
            )
        }
        GlassTabBar(
            tabs = tabLabels,
            selectedIndex = tab.ordinal,
            onSelect = { tab = FileTab.entries[it] },
            modifier = Modifier.padding(vertical = Spacing.x1_5),
        )

        val contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            bottom = bottomBarContentPadding(),
        )

        when {
            docs == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { GlassLoadingIndicator() }

            entries.isEmpty() -> Column(Modifier.weight(1f).fillMaxWidth()) {
                if (nothingAtAll) {
                    GlassEmptyState(
                        icon = Icons.Rounded.FolderOpen,
                        title = tr("अभी कोई दस्तावेज़ नहीं है", "No documents yet"),
                        message = tr("आपके बनाए दस्तावेज़ और PDF यहाँ मिलेंगे।", "Your documents and PDFs will be here."),
                        actionText = tr("पहला दस्तावेज़ बनाएं", "Create your first document"),
                        onAction = onNewBlank,
                    )
                } else {
                    GlassEmptyState(
                        icon = if (query.isNotBlank()) Icons.Rounded.Search else Icons.Rounded.FolderOpen,
                        title = if (query.isNotBlank()) tr("कोई फ़ाइल नहीं मिली", "No files found") else emptyTabTitle(tab),
                        message = null,
                    )
                }
            }

            grid -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5),
                verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
            ) {
                items(entries, key = { it.key }) { entry ->
                    val actions = entryActions(entry, library, toast, dialogs, docActions, onOpenDocument, onPreviewDocument)
                    FileGridCard(
                        entry = entry,
                        menuLabel = moreLabel,
                        actions = actions,
                        onClick = { entryOpen(entry, docActions, onOpenDocument) },
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
            ) {
                items(entries, key = { it.key }) { entry ->
                    val actions = entryActions(entry, library, toast, dialogs, docActions, onOpenDocument, onPreviewDocument)
                    val doc = (entry as? FileEntry.Doc)?.doc
                    GlassDocumentCard(
                        title = entry.name,
                        subtitle = entrySubtitle(entry),
                        statusLabel = doc?.let { docStatusLabel(it.status) },
                        isDraft = doc?.status == DocStatus.DRAFT,
                        isPdf = entry is FileEntry.Pdf,
                        isFavorite = doc?.isFavorite == true,
                        menuLabel = moreLabel,
                        menuActions = actions,
                        onClick = { entryOpen(entry, docActions, onOpenDocument) },
                    )
                }
            }
        }
    }
}

@Composable
private fun emptyTabTitle(tab: FileTab): String = when (tab) {
    FileTab.DRAFTS -> tr("कोई ड्राफ्ट नहीं है", "No drafts")
    FileTab.COMPLETED -> tr("कोई पूर्ण दस्तावेज़ नहीं है", "No completed documents")
    FileTab.PDFS -> tr("अभी कोई PDF नहीं बनी", "No PDFs yet")
    FileTab.FAVORITES -> tr("कोई पसंदीदा दस्तावेज़ नहीं है", "No favorite documents")
    FileTab.ALL -> tr("कोई फ़ाइल नहीं है", "No files")
}

private fun entryOpen(entry: FileEntry, docActions: DocActions, onOpenDocument: (String) -> Unit) {
    when (entry) {
        is FileEntry.Doc -> onOpenDocument(entry.doc.id)
        is FileEntry.Pdf -> docActions.openPdf(entry.record)
    }
}

@Composable
private fun entrySubtitle(entry: FileEntry): String {
    val hindi = LocalAppLanguage.current == AppLanguage.HI
    val date = DateText.listDate(entry.timestamp, hindi)
    return when (entry) {
        is FileEntry.Doc -> entry.typeLabel + " • " + date
        is FileEntry.Pdf -> {
            val pages = tr("${entry.record.pageCount} पेज", "${entry.record.pageCount} page(s)")
            "PDF • $date • $pages • ${SizeText.format(entry.record.sizeBytes)}"
        }
    }
}

@Composable
private fun entryActions(
    entry: FileEntry,
    library: LibraryViewModel,
    toast: GlassToastState,
    dialogs: DocDialogs,
    docActions: DocActions,
    onOpenDocument: (String) -> Unit,
    onPreviewDocument: (String) -> Unit,
): List<GlassMenuAction> = when (entry) {
    is FileEntry.Doc -> documentMenu(
        doc = entry.doc,
        library = library,
        dialogs = dialogs,
        toast = toast,
        onOpen = onOpenDocument,
        onExport = docActions::exportPdf,
        onPreview = onPreviewDocument,
        onShare = docActions::share,
    )
    is FileEntry.Pdf -> pdfMenu(entry.record, dialogs, docActions)
}

@Composable
private fun pdfMenu(record: ExportRecord, dialogs: DocDialogs, docActions: DocActions): List<GlassMenuAction> = listOf(
    GlassMenuAction(tr("PDF खोलें", "Open PDF"), Icons.Rounded.OpenInNew, { docActions.openPdf(record) }),
    GlassMenuAction(tr("शेयर करें", "Share"), Icons.Rounded.Share, { docActions.sharePdf(record) }),
    GlassMenuAction(tr("सूची से हटाएं", "Remove from list"), Icons.Rounded.Delete, { dialogs.deletePdfTarget = record }, destructive = true),
)

@Composable
private fun SortMenu(sort: FileSort, onSort: (FileSort) -> Unit) {
    val c = Glass.colors
    var open by remember { mutableStateOf(false) }
    val options = listOf(
        FileSort.DATE to tr("तारीख के अनुसार", "By date"),
        FileSort.NAME to tr("नाम के अनुसार", "By name"),
        FileSort.TYPE to tr("प्रकार के अनुसार", "By type"),
    )
    Box {
        GlassIconButton(
            icon = Icons.Rounded.SwapVert,
            contentDescription = tr("क्रमबद्ध करें", "Sort"),
            onClick = { open = true },
            size = 44.dp,
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(c.sheet)) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { GlassText(text = label) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (value == sort) c.electric else androidx.compose.ui.graphics.Color.Transparent,
                        )
                    },
                    onClick = {
                        open = false
                        onSort(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun FileGridCard(
    entry: FileEntry,
    menuLabel: String,
    actions: List<GlassMenuAction>,
    onClick: () -> Unit,
) {
    val c = Glass.colors
    var menuOpen by remember { mutableStateOf(false) }
    val doc = (entry as? FileEntry.Doc)?.doc
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(shape = WriterShapes.card, onClick = onClick)
            .padding(start = Spacing.x1_5, top = Spacing.x1_5, bottom = Spacing.x1_5, end = Spacing.half),
        verticalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccentIconTile(
                icon = if (entry is FileEntry.Pdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.Description,
                accent = if (entry is FileEntry.Pdf) c.error else c.electric,
                tileSize = 44.dp,
                modifier = Modifier.weight(1f, fill = false),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(WriterShapes.circle)
                        .clickable(role = Role.Button, onClickLabel = menuLabel) { menuOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = menuLabel, tint = c.textSecondary)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.background(c.sheet)) {
                    actions.forEach { action ->
                        DropdownMenuItem(
                            text = { GlassText(text = action.label, color = if (action.destructive) c.error else c.textPrimary) },
                            leadingIcon = {
                                Icon(action.icon, contentDescription = null, tint = if (action.destructive) c.error else c.textSecondary)
                            },
                            onClick = {
                                menuOpen = false
                                action.onClick()
                            },
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.half)) {
            GlassText(
                text = entry.name,
                style = WriterType.bodyStrong,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (doc?.isFavorite == true) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = c.warning, modifier = Modifier.size(16.dp))
            }
        }
        GlassText(text = entrySubtitle(entry), style = WriterType.caption, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (doc != null) {
            GlassBadge(text = docStatusLabel(doc.status), color = if (doc.status == DocStatus.DRAFT) c.warning else c.success)
        }
    }
}
