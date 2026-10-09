package app.writer.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType

/** A rounded tile holding an icon in a soft accent color. */
@Composable
fun AccentIconTile(
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    tileSize: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(tileSize)
            .clip(WriterShapes.cardSmall)
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(tileSize * 0.5f))
    }
}

/** 12. GlassCategoryCard — compact card for the horizontally scrolling category row. */
@Composable
fun GlassCategoryCard(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(104.dp)
            .glassPanel(shape = WriterShapes.card, onClick = onClick)
            .padding(horizontal = Spacing.x1, vertical = Spacing.x1_5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        AccentIconTile(icon = icon, accent = accent)
        GlassText(
            text = label,
            style = WriterType.caption,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** A small rounded label, e.g. "हिंदी", "English", "Draft". */
@Composable
fun GlassBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(WriterShapes.pill)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = Spacing.x1, vertical = 2.dp),
    ) {
        GlassText(text = text, style = WriterType.caption, color = color, maxLines = 1, softWrap = false)
    }
}

/** 13. GlassTemplateCard — icon, title, description, language badges, favorite, arrow. */
@Composable
fun GlassTemplateCard(
    title: String,
    description: String,
    accent: Color,
    badges: List<String>,
    isFavorite: Boolean,
    favoriteLabel: String,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Glass.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(shape = WriterShapes.card, onClick = onClick)
            .padding(start = Spacing.x2, top = Spacing.x1_5, bottom = Spacing.x1_5, end = Spacing.x1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5),
    ) {
        AccentIconTile(icon = Icons.Rounded.Description, accent = accent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            GlassText(text = title, style = WriterType.bodyStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
            GlassText(
                text = description,
                style = WriterType.bodySmall,
                color = c.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = Spacing.half),
                horizontalArrangement = Arrangement.spacedBy(Spacing.half + 2.dp),
            ) {
                badges.forEach { GlassBadge(text = it, color = c.electric) }
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(WriterShapes.circle)
                .clickable(role = Role.Checkbox, onClickLabel = favoriteLabel, onClick = onToggleFavorite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = favoriteLabel,
                tint = if (isFavorite) c.warning else c.textMuted,
                modifier = Modifier.size(24.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = c.textMuted,
            modifier = Modifier
                .size(16.dp),
        )
    }
}

data class GlassMenuAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
)

/** 14. GlassDocumentCard — a saved document or exported PDF with a three-dot menu. */
@Composable
fun GlassDocumentCard(
    title: String,
    subtitle: String,
    statusLabel: String?,
    isDraft: Boolean,
    isPdf: Boolean,
    isFavorite: Boolean,
    menuLabel: String,
    menuActions: List<GlassMenuAction>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Glass.colors
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(shape = WriterShapes.card, onClick = onClick)
            .padding(start = Spacing.x2, top = Spacing.x1_5, bottom = Spacing.x1_5, end = Spacing.half),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5),
    ) {
        AccentIconTile(
            icon = if (isPdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.Description,
            accent = if (isPdf) c.error else c.electric,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.half)) {
                GlassText(
                    text = title,
                    style = WriterType.bodyStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isFavorite) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = c.warning, modifier = Modifier.size(16.dp))
                }
            }
            GlassText(text = subtitle, style = WriterType.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (statusLabel != null) {
                GlassBadge(
                    text = statusLabel,
                    color = if (isDraft) c.warning else c.success,
                    modifier = Modifier.padding(top = Spacing.half),
                )
            }
        }
        if (menuActions.isNotEmpty()) {
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(WriterShapes.circle)
                        .clickable(role = Role.Button, onClickLabel = menuLabel) { menuOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = menuLabel, tint = c.textSecondary)
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(c.sheet),
                ) {
                    menuActions.forEach { action ->
                        DropdownMenuItem(
                            text = {
                                GlassText(
                                    text = action.label,
                                    color = if (action.destructive) c.error else c.textPrimary,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    action.icon,
                                    contentDescription = null,
                                    tint = if (action.destructive) c.error else c.textSecondary,
                                )
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
    }
}

/** 18. GlassEmptyState — icon, message and one clear next action. */
@Composable
fun GlassEmptyState(
    icon: ImageVector,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = Glass.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.x3, vertical = Spacing.x4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .glassPanel(shape = WriterShapes.circle, elevated = true),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.electric, modifier = Modifier.size(40.dp))
        }
        GlassText(text = title, style = WriterType.headline, textAlign = TextAlign.Center)
        if (message != null) {
            GlassText(text = message, style = WriterType.bodySmall, color = c.textSecondary, textAlign = TextAlign.Center)
        }
        if (actionText != null && onAction != null) {
            GlassButton(text = actionText, onClick = onAction, modifier = Modifier.padding(top = Spacing.x1))
        }
    }
}
