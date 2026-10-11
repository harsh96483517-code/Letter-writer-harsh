package app.writer.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType

/** A quiet text-only action such as "सभी देखें". Keeps a 48 dp touch target. */
@Composable
fun GlassTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(WriterShapes.chip)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.x1_5),
        contentAlignment = Alignment.Center,
    ) {
        GlassText(text = text, style = WriterType.label, color = Glass.colors.electric, maxLines = 1)
    }
}
