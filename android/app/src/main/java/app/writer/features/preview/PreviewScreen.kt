package app.writer.features.preview

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.WidthFull
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.writer.core.tr
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.components.GlassBadge
import app.writer.design.components.GlassEmptyState
import app.writer.design.components.GlassIconButton
import app.writer.design.components.GlassLoadingIndicator
import app.writer.design.components.GlassText
import app.writer.design.components.GlassToastState
import app.writer.design.components.GlassToolbar
import app.writer.design.components.GlassTopBar
import app.writer.design.components.ToastType
import app.writer.design.components.ToolbarButton
import app.writer.design.components.ToolbarDivider
import app.writer.design.components.glassPanel
import app.writer.design.motionMs
import app.writer.design.rememberReducedMotion
import app.writer.features.common.appViewModel
import app.writer.features.export.DocActions
import app.writer.pdf.LaidOutDocument
import app.writer.pdf.PageRenderer
import kotlinx.coroutines.launch

private val ZOOM_STEPS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f)

/**
 * A white sheet (or several) on the glass background. The pages are drawn by the same layout
 * engine and page renderer that write the PDF, so line breaks and page breaks match exactly.
 */
@Composable
fun PreviewScreen(
    docId: String,
    docActions: DocActions,
    toast: GlassToastState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val vm = appViewModel(key = "preview-$docId") { PreviewViewModel(it, docId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var focusMode by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = focusMode) { focusMode = false }

    val savedMessage = tr("सेव हो गया। यह “मेरी फाइलें” में पूर्ण दस्तावेज़ के रूप में दिखेगा।", "Saved. It now shows as completed in My Files.")
    val title = tr("पूर्वावलोकन", "Preview")
    val back = tr("वापस", "Back")

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = !focusMode) {
            GlassTopBar(
                title = title,
                onBack = onBack,
                backLabel = back,
                actions = {
                    if (state is PreviewState.Ready) {
                        GlassIconButton(
                            icon = Icons.Rounded.Fullscreen,
                            contentDescription = tr("सादा दृश्य", "Distraction-free view"),
                            onClick = { focusMode = true },
                        )
                    }
                },
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val s = state) {
                PreviewState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassLoadingIndicator()
                }

                PreviewState.Missing -> GlassEmptyState(
                    icon = Icons.Rounded.ErrorOutline,
                    title = tr("दस्तावेज़ नहीं मिला", "Document not found"),
                    message = tr("हो सकता है यह हटा दिया गया हो।", "It may have been deleted."),
                    actionText = tr("वापस जाएं", "Go back"),
                    onAction = onBack,
                )

                PreviewState.Failed -> GlassEmptyState(
                    icon = Icons.Rounded.ErrorOutline,
                    title = tr("पूर्वावलोकन नहीं बन सका", "The preview could not be drawn"),
                    message = tr("एडिटर में जाकर दोबारा कोशिश करें।", "Go back to the editor and try again."),
                    actionText = tr("एडिट करें", "Edit"),
                    onAction = { onEdit(docId) },
                )

                is PreviewState.Ready -> {
                    val reduced = rememberReducedMotion()
                    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                    AnimatedVisibility(
                        visibleState = visible,
                        enter = fadeIn(tween(motionMs(reduced, 300))) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(motionMs(reduced, 300))),
                        exit = fadeOut(tween(motionMs(reduced, 150))),
                    ) {
                        PagesView(laid = s.laidOut, showControls = true)
                    }
                    if (focusMode) {
                        GlassIconButton(
                            icon = Icons.Rounded.FullscreenExit,
                            contentDescription = tr("सादा दृश्य बंद करें", "Leave distraction-free view"),
                            onClick = { focusMode = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.x1),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = !focusMode && state is PreviewState.Ready) {
            ActionBar(
                onEdit = { onEdit(docId) },
                onSave = {
                    vm.markCompleted()
                    toast.show(savedMessage, ToastType.Success)
                },
                onExport = { docActions.exportPdf(docId) },
                onPrint = { docActions.print(docId) },
                onShare = { docActions.share(docId) },
            )
        }
    }
}

@Composable
private fun PagesView(laid: LaidOutDocument, showControls: Boolean) {
    val c = Glass.colors
    val scope = rememberCoroutineScope()
    val reduced = rememberReducedMotion()
    val listState = rememberLazyListState()
    val horizontal = rememberScrollState()
    var zoom by remember { mutableFloatStateOf(1f) }
    val gutter = 16.dp
    val pageCount = laid.pageCount
    val ratio = laid.heightPt / laid.widthPt

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fitWidth = max(maxWidth - gutter * 2, 120.dp)
        val pageWidth = fitWidth * zoom
        val pageHeight = pageWidth * ratio
        // Zoom at which one whole page is visible at once.
        val fitPageZoom = ((maxHeight - gutter * 2) / (fitWidth * ratio)).coerceIn(0.3f, 1f)

        Box(modifier = Modifier.fillMaxSize().horizontalScroll(horizontal)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(max(maxWidth, pageWidth + gutter * 2)).fillMaxHeight(),
                contentPadding = PaddingValues(top = gutter, bottom = if (showControls) 96.dp else gutter),
                verticalArrangement = Arrangement.spacedBy(gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (laid.usedFallbackFonts) {
                    item(key = "fallback-note") {
                        GlassBadge(
                            text = tr("कुछ अक्षर सिस्टम फ़ॉन्ट में दिख रहे हैं", "Some text is shown in the system font"),
                            color = c.warning,
                        )
                    }
                }
                itemsIndexed(laid.pages, key = { index, _ -> "page-$index" }) { index, _ ->
                    PageSheet(
                        laid = laid,
                        index = index,
                        width = pageWidth,
                        height = pageHeight,
                        label = tr("पृष्ठ ${index + 1} / $pageCount", "Page ${index + 1} of $pageCount"),
                    )
                }
            }
        }

        if (showControls) {
            val noteOffset = if (laid.usedFallbackFonts) 1 else 0
            val currentPage = (listState.firstVisibleItemIndex - noteOffset).coerceIn(0, pageCount - 1)
            GlassToolbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.x1_5),
            ) {
                ToolbarButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = tr("पिछला पृष्ठ", "Previous page"),
                    enabled = currentPage > 0,
                    onClick = {
                        scope.launch {
                            val target = currentPage - 1 + noteOffset
                            if (reduced) listState.scrollToItem(target) else listState.animateScrollToItem(target)
                        }
                    },
                )
                GlassText(
                    text = "${currentPage + 1} / $pageCount",
                    style = WriterType.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(64.dp),
                )
                ToolbarButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = tr("अगला पृष्ठ", "Next page"),
                    enabled = currentPage < pageCount - 1,
                    onClick = {
                        scope.launch {
                            val target = currentPage + 1 + noteOffset
                            if (reduced) listState.scrollToItem(target) else listState.animateScrollToItem(target)
                        }
                    },
                )
                ToolbarDivider()
                ToolbarButton(
                    icon = Icons.Rounded.ZoomOut,
                    contentDescription = tr("छोटा करें", "Zoom out"),
                    enabled = zoom > ZOOM_STEPS.first() + 0.01f,
                    onClick = { zoom = ZOOM_STEPS.lastOrNull { it < zoom - 0.01f } ?: ZOOM_STEPS.first() },
                )
                ToolbarButton(
                    icon = Icons.Rounded.ZoomIn,
                    contentDescription = tr("बड़ा करें", "Zoom in"),
                    enabled = zoom < ZOOM_STEPS.last() - 0.01f,
                    onClick = { zoom = ZOOM_STEPS.firstOrNull { it > zoom + 0.01f } ?: ZOOM_STEPS.last() },
                )
                ToolbarButton(
                    icon = Icons.Rounded.WidthFull,
                    contentDescription = tr("चौड़ाई के अनुसार", "Fit to width"),
                    selected = zoom == 1f,
                    onClick = { zoom = 1f },
                )
                ToolbarButton(
                    icon = Icons.Rounded.FitScreen,
                    contentDescription = tr("पूरा पृष्ठ दिखाएं", "Fit whole page"),
                    selected = kotlin.math.abs(zoom - fitPageZoom) < 0.01f,
                    onClick = { zoom = fitPageZoom },
                )
            }
        }
    }
}

/** One white sheet. The sheet is never transparent, whatever the theme. */
@Composable
private fun PageSheet(laid: LaidOutDocument, index: Int, width: Dp, height: Dp, label: String) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(width, height)
            .shadow(elevation = 10.dp, shape = shape, clip = false)
            .clip(shape)
            .background(Color.White)
            .semantics { contentDescription = label },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = size.width / laid.widthPt
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save()
                native.scale(scale, scale)
                PageRenderer.draw(native, laid, index)
                native.restore()
            }
        }
    }
}

@Composable
private fun ActionBar(
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.x1_5, vertical = Spacing.x1)
            .glassPanel(shape = WriterShapes.card, elevated = true)
            .padding(Spacing.half),
        horizontalArrangement = Arrangement.spacedBy(Spacing.half),
    ) {
        ActionTile(Icons.Rounded.Edit, tr("एडिट", "Edit"), onEdit, Modifier.weight(1f))
        ActionTile(Icons.Rounded.Save, tr("सेव", "Save"), onSave, Modifier.weight(1f))
        ActionTile(Icons.Rounded.PictureAsPdf, tr("PDF बनाएं", "Make PDF"), onExport, Modifier.weight(1.3f), primary = true)
        ActionTile(Icons.Rounded.Print, tr("प्रिंट", "Print"), onPrint, Modifier.weight(1f))
        ActionTile(Icons.Rounded.Share, tr("शेयर", "Share"), onShare, Modifier.weight(1f))
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val c = Glass.colors
    Column(
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(WriterShapes.chip)
            .background(if (primary) c.primary.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.x1, horizontal = Spacing.half),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (primary) c.electric else c.textPrimary, modifier = Modifier.size(24.dp))
        GlassText(
            text = label,
            style = WriterType.caption,
            color = if (primary) c.electric else c.textSecondary,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
