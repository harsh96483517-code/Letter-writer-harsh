package app.writer.design.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType

/** 4. GlassSearchBar — pill search field with a clear button. */
@Composable
fun GlassSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    val c = Glass.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .glassPanel(
                shape = WriterShapes.pill,
                borderBrush = if (focused) SolidColor(c.electric.copy(alpha = 0.85f)) else null,
            )
            .padding(start = Spacing.x2, end = Spacing.half),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = c.textSecondary,
            modifier = Modifier.size(22.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = source,
                textStyle = WriterType.body.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.electric),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearch?.invoke()
                    focusManager.clearFocus()
                }),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            GlassText(
                                text = placeholder,
                                color = c.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
            )
        }
        if (value.isNotEmpty()) {
            GlassIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = clearLabel,
                onClick = { onValueChange("") },
                size = 44.dp,
                tint = c.textSecondary,
            )
        }
    }
}

/**
 * 5. GlassTextField — label above the field (friendlier than a floating label for Hindi),
 * a clear focus ring, an inline error line and an optional character counter.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    required: Boolean = false,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: ImageVector? = null,
    showCounter: Boolean = false,
    textStyle: TextStyle = WriterType.body,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    val c = Glass.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val borderColor = when {
        error != null -> c.error
        focused -> c.electric
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.half)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GlassText(
                text = if (required) "$label *" else label,
                style = WriterType.label,
                color = if (error != null) c.error else c.textSecondary,
            )
            if (showCounter) {
                GlassText(text = value.length.toString(), style = WriterType.caption, color = c.textMuted)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .glassPanel(
                    shape = WriterShapes.field,
                    borderBrush = borderColor?.let { SolidColor(it.copy(alpha = 0.9f)) },
                )
                .padding(horizontal = Spacing.x2, vertical = Spacing.x1_5),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = minLines,
                interactionSource = source,
                textStyle = textStyle.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.electric),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onFocusChange?.invoke(it.isFocused) },
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty() && placeholder != null) {
                            GlassText(text = placeholder, style = textStyle, color = c.textMuted)
                        }
                        inner()
                    }
                },
            )
        }
        if (error != null) {
            GlassText(
                text = error,
                style = WriterType.caption,
                color = c.error,
                modifier = Modifier.padding(horizontal = Spacing.half),
            )
        }
    }
}
