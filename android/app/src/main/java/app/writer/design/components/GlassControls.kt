package app.writer.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType

enum class GlassButtonStyle { Primary, Secondary, Destructive }

/** 2. GlassButton — Primary is the only gradient button; use it once per screen. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.Primary,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val c = Glass.colors
    val contentColor = when (style) {
        GlassButtonStyle.Primary -> c.onAccent
        GlassButtonStyle.Secondary -> c.textPrimary
        GlassButtonStyle.Destructive -> c.error
    }
    val fill: Brush? = when (style) {
        GlassButtonStyle.Primary -> c.accentBrush
        GlassButtonStyle.Secondary -> null
        GlassButtonStyle.Destructive -> SolidColor(c.error.copy(alpha = 0.14f))
    }
    val border: Brush? = when (style) {
        GlassButtonStyle.Primary -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f)),
        )
        GlassButtonStyle.Secondary -> null
        GlassButtonStyle.Destructive -> SolidColor(c.error.copy(alpha = 0.35f))
    }
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .heightIn(min = 52.dp)
            .glassPanel(
                shape = WriterShapes.button,
                fillBrush = fill,
                borderBrush = border,
                onClick = onClick,
                enabled = enabled && !loading,
            )
            .padding(horizontal = Spacing.x3, vertical = Spacing.x1_5),
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            GlassLoadingIndicator(size = 20.dp, color = contentColor)
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
        }
        GlassText(text, style = WriterType.button, color = contentColor, maxLines = 1)
        if (trailingIcon != null && !loading) {
            Icon(trailingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
        }
    }
}

/** 3. GlassIconButton — 48 dp touch target, always needs a content description. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = Glass.colors.textPrimary,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Glass.colors
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .glassPanel(
                shape = WriterShapes.circle,
                fillBrush = if (selected) SolidColor(c.primary.copy(alpha = 0.28f)) else null,
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) c.electric else tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** 11. GlassFloatingActionButton — the emphasized Create button. */
@Composable
fun GlassFloatingActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
) {
    val c = Glass.colors
    Box(
        modifier = modifier
            .size(size)
            .glassPanel(
                shape = WriterShapes.circle,
                fillBrush = c.accentBrush,
                borderBrush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.10f)),
                ),
                glowColor = c.primary.copy(alpha = 0.55f),
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(30.dp),
        )
    }
}

/** 17. GlassLoadingIndicator — a light spinner; never an infinite glow. */
@Composable
fun GlassLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = Glass.colors.electric,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        strokeWidth = 3.dp,
        trackColor = Color.Transparent,
    )
}
