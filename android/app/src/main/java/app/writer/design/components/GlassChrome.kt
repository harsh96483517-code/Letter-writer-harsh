package app.writer.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType

/** Screen header: optional back button, title, and trailing actions. Clears the status bar and cutout. */
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backLabel: String = "",
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.x1, vertical = Spacing.half),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        if (onBack != null) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = backLabel,
                onClick = onBack,
            )
        } else {
            Spacer(Modifier.width(Spacing.x1))
        }
        GlassText(
            text = title,
            style = WriterType.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    GlassText(
        text = text,
        style = WriterType.headline,
        modifier = modifier.padding(top = Spacing.x1, bottom = Spacing.half),
    )
}

/** A card holding rows (settings, profile menu). Children manage their own padding. */
@Composable
fun GlassGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(shape = WriterShapes.card),
        content = content,
    )
}

@Composable
fun GroupDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 64.dp)
            .height(1.dp)
            .background(Glass.colors.glassBorder.copy(alpha = 0.6f)),
    )
}

/** One row of a [GlassGroup]: leading icon, title, optional subtitle, and a trailing control or arrow. */
@Composable
fun GlassRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    iconTint: Color = Glass.colors.electric,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = Glass.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(horizontal = Spacing.x2, vertical = Spacing.x1_5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            GlassText(text = title, style = WriterType.body)
            if (subtitle != null) {
                GlassText(text = subtitle, style = WriterType.caption, color = c.textSecondary)
            }
        }
        if (value != null) {
            GlassText(text = value, style = WriterType.bodySmall, color = c.textSecondary, maxLines = 1)
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = c.textMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** A row with a switch. The whole row toggles, so the touch target is large. */
@Composable
fun GlassSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    stateOn: String = "",
    stateOff: String = "",
) {
    val c = Glass.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics { stateDescription = if (checked) stateOn else stateOff }
            .padding(horizontal = Spacing.x2, vertical = Spacing.x1_5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5),
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) c.electric else c.textMuted, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            GlassText(text = title, style = WriterType.body, color = if (enabled) c.textPrimary else c.textMuted)
            if (subtitle != null) {
                GlassText(text = subtitle, style = WriterType.caption, color = c.textSecondary)
            }
        }
        // The row itself handles the click; the switch is display only.
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = c.primary,
                uncheckedThumbColor = c.textSecondary,
                uncheckedTrackColor = c.glassElevated,
                uncheckedBorderColor = c.glassBorder,
            ),
        )
    }
}

/** A labelled segmented control for settings. */
@Composable
fun GlassSettingSegment(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.x2, vertical = Spacing.x1_5),
        verticalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        GlassText(text = title, style = WriterType.body)
        if (subtitle != null) GlassText(text = subtitle, style = WriterType.caption, color = Glass.colors.textSecondary)
        GlassSegmentedControl(options = options, selectedIndex = selectedIndex, onSelect = onSelect)
    }
}
