package app.writer.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.motionMs
import app.writer.design.rememberReducedMotion
import kotlinx.coroutines.delay

/**
 * 7. GlassBottomSheet — a floating glass panel in a modal sheet. The sheet slides in with the
 * platform's spring-style motion, dismisses by swipe, back gesture or tapping outside.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Glass.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.48f),
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.x1_5, vertical = Spacing.x1)
                .glassPanel(
                    shape = WriterShapes.cardLarge,
                    elevated = true,
                    fillBrush = SolidColor(c.sheet.copy(alpha = 0.97f)),
                )
                .padding(Spacing.x2),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(WriterShapes.pill)
                    .background(c.glassBorder),
            )
            if (title != null) {
                GlassText(
                    text = title,
                    style = WriterType.headline,
                    modifier = Modifier.padding(vertical = Spacing.x1),
                )
            }
            content()
        }
    }
}

/** 8. GlassDialog — confirmation and message dialog. Use [destructive] for deletes. */
@Composable
fun GlassDialog(
    title: String,
    message: String?,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String? = null,
    destructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = Glass.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.x3)
                .glassPanel(
                    shape = WriterShapes.cardLarge,
                    elevated = true,
                    fillBrush = SolidColor(c.sheet.copy(alpha = 0.98f)),
                )
                .padding(Spacing.x3),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
        ) {
            GlassText(text = title, style = WriterType.headline)
            if (message != null) {
                GlassText(text = message, style = WriterType.body, color = c.textSecondary)
            }
            if (content != null) content()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.x1),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5),
            ) {
                if (dismissText != null) {
                    GlassButton(
                        text = dismissText,
                        onClick = onDismiss,
                        style = GlassButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
                GlassButton(
                    text = confirmText,
                    onClick = onConfirm,
                    style = if (destructive) GlassButtonStyle.Destructive else GlassButtonStyle.Primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A dialog that blocks input while work runs (PDF export). Not dismissible. */
@Composable
fun GlassProgressDialog(message: String) {
    val c = Glass.colors
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.x4)
                .glassPanel(
                    shape = WriterShapes.card,
                    elevated = true,
                    fillBrush = SolidColor(c.sheet.copy(alpha = 0.98f)),
                )
                .padding(Spacing.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            GlassLoadingIndicator()
            GlassText(text = message, style = WriterType.body)
        }
    }
}

enum class ToastType { Info, Success, Error }

@Stable
class GlassToastState {
    private var counter = 0
    private var last: ToastMessage? = null
    var current by mutableStateOf<ToastMessage?>(null)
        private set

    /** The message to draw, including while the exit animation is still running. */
    val display: ToastMessage? get() = current ?: last

    fun show(message: String, type: ToastType = ToastType.Info) {
        counter += 1
        val next = ToastMessage(counter, message, type)
        last = next
        current = next
    }

    fun dismiss() {
        current = null
    }
}

class ToastMessage(val id: Int, val text: String, val type: ToastType)

@Composable
fun rememberGlassToastState(): GlassToastState = remember { GlassToastState() }

/** 16. GlassToast — a short, polite, auto-dismissing message. Place once near the root. */
@Composable
fun GlassToastHost(state: GlassToastState, modifier: Modifier = Modifier) {
    val c = Glass.colors
    val reduced = rememberReducedMotion()
    val message = state.current
    LaunchedEffect(message?.id) {
        if (message != null) {
            delay(3600)
            state.dismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(tween(motionMs(reduced, 200))) +
            slideInVertically(tween(motionMs(reduced, 250))) { it / 2 },
        exit = fadeOut(tween(motionMs(reduced, 160))) +
            slideOutVertically(tween(motionMs(reduced, 200))) { it / 2 },
    ) {
        val shown = state.display
        if (shown != null) {
            val (icon, tint) = when (shown.type) {
                ToastType.Success -> Icons.Rounded.CheckCircle to c.success
                ToastType.Error -> Icons.Rounded.ErrorOutline to c.error
                ToastType.Info -> Icons.Rounded.Info to c.electric
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = Spacing.x2)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .glassPanel(
                        shape = WriterShapes.cardSmall,
                        elevated = true,
                        fillBrush = SolidColor(c.sheet.copy(alpha = 0.97f)),
                    )
                    .padding(horizontal = Spacing.x2, vertical = Spacing.x1_5),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5),
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                GlassText(text = shown.text, style = WriterType.bodySmall, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}
