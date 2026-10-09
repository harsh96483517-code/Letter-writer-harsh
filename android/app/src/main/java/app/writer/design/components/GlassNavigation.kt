package app.writer.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.motionMs
import app.writer.design.rememberHaptic
import app.writer.design.rememberReducedMotion

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String,
)

/** Height of the floating bar including the raised Create button, before system insets. */
val BottomBarHeight: Dp = 86.dp

/**
 * 6. GlassBottomNavigation — floating pill with four destinations and a raised, emphasized
 * Create button in the middle. Respects gesture-navigation insets via [navigationBarsPadding].
 */
@Composable
fun GlassBottomNavigation(
    items: List<BottomNavItem>,
    selectedRoute: String?,
    createLabel: String,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    require(items.size == 4) { "Bottom navigation expects exactly four destinations" }
    val c = Glass.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.x2)
            .padding(bottom = Spacing.x1)
            .height(BottomBarHeight),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(68.dp)
                .glassPanel(
                    shape = WriterShapes.navBar,
                    elevated = true,
                    fillBrush = SolidColor(c.sheet.copy(alpha = 0.92f)),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavCell(items[0], items[0].route == selectedRoute, onSelect)
            NavCell(items[1], items[1].route == selectedRoute, onSelect)
            Spacer(Modifier.weight(1f))
            NavCell(items[2], items[2].route == selectedRoute, onSelect)
            NavCell(items[3], items[3].route == selectedRoute, onSelect)
        }
        GlassFloatingActionButton(
            icon = Icons.Rounded.Add,
            contentDescription = createLabel,
            onClick = onCreate,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun RowScope.NavCell(
    item: BottomNavItem,
    selected: Boolean,
    onSelect: (String) -> Unit,
) {
    val c = Glass.colors
    val reduced = rememberReducedMotion()
    val tick = rememberHaptic()
    val indicator by animateColorAsState(
        targetValue = if (selected) c.primary.copy(alpha = 0.24f) else Color.Transparent,
        animationSpec = tween(motionMs(reduced, 220)),
        label = "navIndicator",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) c.electric else c.textSecondary,
        animationSpec = tween(motionMs(reduced, 220)),
        label = "navTint",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = tween(motionMs(reduced, 220)),
        label = "navScale",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
            ) {
                if (!selected) tick()
                onSelect(item.route)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 30.dp)
                .clip(WriterShapes.pill)
                .background(indicator),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }
        GlassText(
            text = item.label,
            style = WriterType.caption,
            color = tint,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** 9. GlassTabBar — scrollable filter pills (All / Drafts / PDFs …). */
@Composable
fun GlassTabBar(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Glass.colors
    val reduced = rememberReducedMotion()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val textColor by animateColorAsState(
                targetValue = if (selected) c.onAccent else c.textSecondary,
                animationSpec = tween(motionMs(reduced, 200)),
                label = "tabText",
            )
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .glassPanel(
                        shape = WriterShapes.chip,
                        fillBrush = if (selected) c.accentBrush else null,
                        onClick = { onSelect(index) },
                        role = Role.Tab,
                    )
                    .padding(horizontal = Spacing.x2, vertical = Spacing.x1),
                contentAlignment = Alignment.Center,
            ) {
                GlassText(text = label, style = WriterType.label, color = textColor, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** 15. GlassSegmentedControl — fixed, equal-width segments with a sliding indicator. */
@Composable
fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Glass.colors
    val reduced = rememberReducedMotion()
    val tick = rememberHaptic()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .glassPanel(shape = WriterShapes.chip)
            .padding(4.dp),
    ) {
        val segmentWidth = maxWidth / options.size
        val offset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(motionMs(reduced, 250)),
            label = "segmentOffset",
        )
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(segmentWidth)
                .height(40.dp)
                .clip(WriterShapes.chip)
                .background(c.accentBrush),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.RadioButton,
                        ) {
                            if (!selected) tick()
                            onSelect(index)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    GlassText(
                        text = label,
                        style = WriterType.label,
                        color = if (selected) c.onAccent else c.textSecondary,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/** 10. GlassToolbar — floating, horizontally scrollable pill for editor controls. */
@Composable
fun GlassToolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val c = Glass.colors
    Row(
        modifier = modifier
            .glassPanel(
                shape = WriterShapes.pill,
                elevated = true,
                fillBrush = SolidColor(c.sheet.copy(alpha = 0.94f)),
            )
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.x1, vertical = Spacing.half),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** One button inside a [GlassToolbar]. 48 dp square touch target. */
@Composable
fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Glass.colors
    val tick = rememberHaptic()
    Box(
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(WriterShapes.circle)
            .background(if (selected) c.primary.copy(alpha = 0.28f) else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button) {
                tick()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) c.electric else c.textPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** A thin divider between groups of toolbar buttons. */
@Composable
fun ToolbarDivider() {
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(24.dp)
            .background(Glass.colors.glassBorder),
    )
}
