package app.writer.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.motionMs
import app.writer.design.rememberHaptic
import app.writer.design.rememberReducedMotion

/**
 * The one place glass is made. A translucent gradient fill, a thin specular border
 * (bright at the top-left edge, fading to the bottom-right), an optional soft shadow or
 * glow, and — when [onClick] is given — a press-scale and highlight with haptic feedback.
 *
 * No real-time backdrop blur is used: it is expensive on mid-range devices, and the
 * layered [app.writer.design.AppBackground] already gives the glass something to sit on.
 */
@Composable
fun Modifier.glassPanel(
    shape: Shape = WriterShapes.card,
    elevated: Boolean = false,
    fillBrush: Brush? = null,
    borderBrush: Brush? = null,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = if (onClick != null) Role.Button else null,
): Modifier {
    val c = Glass.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduced = rememberReducedMotion()
    val interactive = onClick != null && enabled
    val tick = rememberHaptic()

    val scale by animateFloatAsState(
        targetValue = if (pressed && interactive) 0.975f else 1f,
        animationSpec = tween(motionMs(reduced, 120)),
        label = "panelScale",
    )
    val highlight by animateFloatAsState(
        targetValue = if (pressed && interactive) 1f else 0f,
        animationSpec = tween(motionMs(reduced, 100)),
        label = "panelHighlight",
    )

    val base = if (elevated) c.glassElevated else c.glass
    val fill = fillBrush ?: Brush.linearGradient(
        listOf(base.copy(alpha = (base.alpha * 1.5f).coerceAtMost(1f)), base),
    )
    val border = borderBrush ?: if (c.isDark) {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.30f), c.glassBorder, Color.White.copy(alpha = 0.04f)),
        )
    } else {
        Brush.linearGradient(listOf(Color.White, c.glassBorder, c.glassBorder))
    }
    val highlightColor = if (c.isDark) Color.White else Color(0xFF1E2D50)

    var result: Modifier = this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    if (glowColor != null) {
        result = result.shadow(18.dp, shape, clip = false, ambientColor = glowColor, spotColor = glowColor)
    } else if (elevated) {
        val shadowColor = Color.Black.copy(alpha = if (c.isDark) 0.45f else 0.14f)
        result = result.shadow(12.dp, shape, clip = false, ambientColor = shadowColor, spotColor = shadowColor)
    }
    result = result
        .clip(shape)
        .background(fill)
        .border(BorderStroke(1.dp, border), shape)
        .drawWithContent {
            drawContent()
            if (highlight > 0f) {
                drawRect(highlightColor.copy(alpha = 0.10f * highlight))
            }
        }
    if (onClick != null) {
        result = result.clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
        ) {
            tick()
            onClick()
        }
    }
    return result
}

/** Text with design-system defaults. */
@Composable
fun GlassText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = WriterType.body,
    color: Color = Glass.colors.textPrimary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
    softWrap: Boolean = true,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
        softWrap = softWrap,
    )
}

/** 1. GlassCard — the basic translucent container. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = WriterShapes.card,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(Spacing.x2),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .glassPanel(shape = shape, elevated = elevated, onClick = onClick)
            .padding(contentPadding),
        content = content,
    )
}
