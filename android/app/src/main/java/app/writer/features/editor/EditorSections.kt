package app.writer.features.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.writer.common.FieldError
import app.writer.common.TemplateFiller
import app.writer.common.Validators
import app.writer.core.tr
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.components.GlassBottomSheet
import app.writer.design.components.GlassButton
import app.writer.design.components.GlassButtonStyle
import app.writer.design.components.GlassIconButton
import app.writer.design.components.GlassSegmentedControl
import app.writer.design.components.GlassText
import app.writer.design.components.GlassTextField
import app.writer.design.components.glassPanel
import app.writer.model.AppLanguage
import app.writer.model.Applicant
import app.writer.model.BodyAlign
import app.writer.model.FieldDef
import app.writer.model.FontChoice
import app.writer.model.Formatting
import app.writer.model.MarginPreset
import app.writer.model.PaperSize
import app.writer.model.Recipient
import app.writer.model.TextSpan

/** Remembers whether the user has already visited a field, so errors appear after they leave it, not while they type. */
internal class TouchState {
    var touched by mutableStateOf(false)
    private var hadFocus = false

    fun onFocus(focused: Boolean) {
        if (focused) hadFocus = true else if (hadFocus) touched = true
    }
}

@Composable
internal fun rememberTouch(): TouchState = remember { TouchState() }

/** A collapsible glass card that groups related fields. */
@Composable
internal fun EditorSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    hint: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Glass.colors
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Column(modifier = modifier.fillMaxWidth().glassPanel(shape = WriterShapes.card)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = Spacing.x2, vertical = Spacing.x1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            GlassText(text = title, style = WriterType.headline, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) tr("समेटें", "Collapse") else tr("खोलें", "Expand"),
                tint = c.textSecondary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = Spacing.x2, end = Spacing.x2, bottom = Spacing.x2),
                verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
            ) {
                if (hint != null) GlassText(text = hint, style = WriterType.caption, color = c.textSecondary)
                content()
            }
        }
    }
}

@Composable
internal fun TemplateQuestions(
    fields: List<FieldDef>,
    language: AppLanguage,
    values: Map<String, String>,
    onChange: (String, String) -> Unit,
) {
    fields.forEach { field ->
        val touch = rememberTouch()
        val value = values[field.key].orEmpty()
        GlassTextField(
            value = value,
            onValueChange = { onChange(field.key, it) },
            label = field.label[language],
            placeholder = field.hint[language],
            required = field.required,
            singleLine = !field.multiline,
            minLines = if (field.multiline) 3 else 1,
            keyboardOptions = if (field.multiline) {
                KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default)
            } else {
                KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
            },
            error = if (field.required && touch.touched && value.isBlank()) {
                tr("यह जानकारी ज़रूरी है", "This is needed")
            } else {
                null
            },
            onFocusChange = touch::onFocus,
        )
    }
}

@Composable
internal fun RecipientFields(value: Recipient, onChange: (Recipient) -> Unit) {
    val sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
    GlassTextField(
        value = value.designation,
        onValueChange = { onChange(value.copy(designation = it)) },
        label = tr("प्राप्तकर्ता का पदनाम", "Recipient's designation"),
        placeholder = tr("जैसे: जिला अधिकारी महोदय", "e.g. The District Magistrate"),
        keyboardOptions = sentences,
    )
    GlassTextField(
        value = value.office,
        onValueChange = { onChange(value.copy(office = it)) },
        label = tr("विभाग / कार्यालय का नाम", "Department or office"),
        keyboardOptions = sentences,
    )
    GlassTextField(
        value = value.location,
        onValueChange = { onChange(value.copy(location = it)) },
        label = tr("कार्यालय का स्थान", "Office location"),
        keyboardOptions = sentences,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x1)) {
        GlassTextField(
            value = value.district,
            onValueChange = { onChange(value.copy(district = it)) },
            label = tr("जनपद / जिला", "District"),
            keyboardOptions = sentences,
            modifier = Modifier.weight(1f),
        )
        GlassTextField(
            value = value.state,
            onValueChange = { onChange(value.copy(state = it)) },
            label = tr("राज्य", "State"),
            keyboardOptions = sentences,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun ApplicantFields(value: Applicant, onChange: (Applicant) -> Unit) {
    val words = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
    val sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
    val pinTouch = rememberTouch()
    val mobileTouch = rememberTouch()

    GlassTextField(
        value = value.name,
        onValueChange = { onChange(value.copy(name = it)) },
        label = tr("आवेदक का नाम", "Applicant's name"),
        keyboardOptions = words,
    )
    GlassTextField(
        value = value.parent,
        onValueChange = { onChange(value.copy(parent = it)) },
        label = tr("पिता / माता का नाम (जहाँ ज़रूरी हो)", "Father's or mother's name (if needed)"),
        keyboardOptions = words,
    )
    GlassTextField(
        value = value.address,
        onValueChange = { onChange(value.copy(address = it)) },
        label = tr("पूरा पता", "Full address"),
        placeholder = tr("मकान नंबर, गली, मोहल्ला", "House number, street, locality"),
        keyboardOptions = sentences,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x1)) {
        GlassTextField(
            value = value.village,
            onValueChange = { onChange(value.copy(village = it)) },
            label = tr("गाँव", "Village"),
            keyboardOptions = words,
            modifier = Modifier.weight(1f),
        )
        GlassTextField(
            value = value.postOffice,
            onValueChange = { onChange(value.copy(postOffice = it)) },
            label = tr("डाकघर", "Post office"),
            keyboardOptions = words,
            modifier = Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x1)) {
        GlassTextField(
            value = value.block,
            onValueChange = { onChange(value.copy(block = it)) },
            label = tr("ब्लॉक", "Block"),
            keyboardOptions = words,
            modifier = Modifier.weight(1f),
        )
        GlassTextField(
            value = value.district,
            onValueChange = { onChange(value.copy(district = it)) },
            label = tr("जनपद / जिला", "District"),
            keyboardOptions = words,
            modifier = Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x1)) {
        GlassTextField(
            value = value.state,
            onValueChange = { onChange(value.copy(state = it)) },
            label = tr("राज्य", "State"),
            keyboardOptions = words,
            modifier = Modifier.weight(1f),
        )
        GlassTextField(
            value = value.pin,
            onValueChange = { onChange(value.copy(pin = it)) },
            label = tr("पिन कोड", "PIN code"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            error = if (pinTouch.touched && Validators.pin(value.pin) == FieldError.PIN) {
                tr("6 अंकों का सही पिन कोड लिखें", "Enter a valid 6-digit PIN")
            } else {
                null
            },
            onFocusChange = pinTouch::onFocus,
            modifier = Modifier.weight(1f),
        )
    }
    GlassTextField(
        value = value.mobile,
        onValueChange = { onChange(value.copy(mobile = it)) },
        label = tr("मोबाइल नंबर (वैकल्पिक)", "Mobile number (optional)"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        error = if (mobileTouch.touched && Validators.mobile(value.mobile) == FieldError.MOBILE) {
            tr("10 अंकों का सही मोबाइल नंबर लिखें", "Enter a valid 10-digit mobile number")
        } else {
            null
        },
        onFocusChange = mobileTouch::onFocus,
    )
}

/** Shows bold and italic runs while typing, and marks `[blank]` placeholders so they are hard to miss. */
internal class BodyTransformation(
    private val spans: List<TextSpan>,
    private val blankColor: Color,
    private val blankBackground: Color,
) : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val length = text.text.length
        val styled = buildAnnotatedString {
            append(text.text)
            for (s in spans) {
                val a = s.start.coerceIn(0, length)
                val b = s.end.coerceIn(0, length)
                if (b > a) {
                    addStyle(
                        SpanStyle(
                            fontWeight = if (s.bold) FontWeight.Bold else null,
                            fontStyle = if (s.italic) FontStyle.Italic else null,
                        ),
                        a,
                        b,
                    )
                }
            }
            for (r in TemplateFiller.blankRanges(text.text)) {
                addStyle(SpanStyle(color = blankColor, background = blankBackground), r.first, r.last + 1)
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

/** The multi-line letter body. Unlike the small fields it needs the cursor and selection, so it takes a [TextFieldValue]. */
@Composable
internal fun BodyField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    spans: List<TextSpan>,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val c = Glass.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val transformation = remember(spans, c.warning) {
        BodyTransformation(spans, blankColor = c.warning, blankBackground = c.warning.copy(alpha = 0.16f))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .glassPanel(
                shape = WriterShapes.field,
                borderBrush = if (focused) SolidColor(c.electric.copy(alpha = 0.9f)) else null,
            )
            .padding(Spacing.x2),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            interactionSource = source,
            textStyle = WriterType.body.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.electric),
            visualTransformation = transformation,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default,
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.text.isEmpty()) {
                        GlassText(text = placeholder, color = c.textMuted)
                    }
                    inner()
                }
            },
        )
    }
}

// --- format sheet ---

@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        GlassText(text = label, style = WriterType.body, modifier = Modifier.weight(1f))
        GlassIconButton(
            icon = Icons.Rounded.Remove,
            contentDescription = tr("घटाएँ", "Decrease") + ": " + label,
            onClick = onMinus,
            enabled = minusEnabled,
        )
        GlassText(
            text = valueText,
            style = WriterType.bodyStrong,
            modifier = Modifier.padding(horizontal = Spacing.half),
        )
        GlassIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = tr("बढ़ाएँ", "Increase") + ": " + label,
            onClick = onPlus,
            enabled = plusEnabled,
        )
    }
}

@Composable
private fun FormatGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
        GlassText(text = title, style = WriterType.label, color = Glass.colors.textSecondary)
        content()
    }
}

internal val LINE_SPACINGS = listOf(1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.8f)
internal const val MIN_FONT_PT = 9f
internal const val MAX_FONT_PT = 18f

internal fun formatPt(value: Float): String =
    (if (value % 1f == 0f) value.toInt().toString() else value.toString()) + " pt"

internal fun nextAlign(current: BodyAlign): BodyAlign = when (current) {
    BodyAlign.LEFT -> BodyAlign.CENTER
    BodyAlign.CENTER -> BodyAlign.RIGHT
    BodyAlign.RIGHT -> BodyAlign.JUSTIFY
    BodyAlign.JUSTIFY -> BodyAlign.LEFT
}

internal fun nextLineSpacing(current: Float): Float =
    LINE_SPACINGS.firstOrNull { it > current + 0.001f } ?: LINE_SPACINGS.first()

/** Everything the document model can format: language, font, size, spacing, alignment, margins and paper. */
@Composable
internal fun FormatSheet(
    formatting: Formatting,
    language: AppLanguage,
    onFormatting: (Formatting) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onDone: () -> Unit,
) {
    GlassBottomSheet(onDismiss = onDone, title = tr("फ़ॉर्मेट", "Format")) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            FormatGroup(tr("पत्र की भाषा", "Letter language")) {
                GlassSegmentedControl(
                    options = listOf("हिंदी", "English"),
                    selectedIndex = if (language == AppLanguage.HI) 0 else 1,
                    onSelect = { onLanguage(if (it == 0) AppLanguage.HI else AppLanguage.EN) },
                )
            }
            FormatGroup(tr("फ़ॉन्ट", "Font")) {
                GlassSegmentedControl(
                    options = listOf(tr("सेरिफ़", "Serif"), tr("सैंस", "Sans")),
                    selectedIndex = if (formatting.font == FontChoice.SERIF) 0 else 1,
                    onSelect = { onFormatting(formatting.copy(font = if (it == 0) FontChoice.SERIF else FontChoice.SANS)) },
                )
            }
            StepperRow(
                label = tr("फ़ॉन्ट आकार", "Font size"),
                valueText = formatPt(formatting.fontSizePt),
                onMinus = { onFormatting(formatting.copy(fontSizePt = (formatting.fontSizePt - 1f).coerceAtLeast(MIN_FONT_PT))) },
                onPlus = { onFormatting(formatting.copy(fontSizePt = (formatting.fontSizePt + 1f).coerceAtMost(MAX_FONT_PT))) },
                minusEnabled = formatting.fontSizePt > MIN_FONT_PT,
                plusEnabled = formatting.fontSizePt < MAX_FONT_PT,
            )
            val spacingIndex = LINE_SPACINGS.indexOfFirst { kotlin.math.abs(it - formatting.lineSpacing) < 0.01f }
            StepperRow(
                label = tr("पंक्ति की दूरी", "Line spacing"),
                valueText = String.format(java.util.Locale.ENGLISH, "%.1f×", formatting.lineSpacing),
                onMinus = {
                    val i = if (spacingIndex < 0) 0 else (spacingIndex - 1).coerceAtLeast(0)
                    onFormatting(formatting.copy(lineSpacing = LINE_SPACINGS[i]))
                },
                onPlus = {
                    val i = if (spacingIndex < 0) LINE_SPACINGS.lastIndex else (spacingIndex + 1).coerceAtMost(LINE_SPACINGS.lastIndex)
                    onFormatting(formatting.copy(lineSpacing = LINE_SPACINGS[i]))
                },
                minusEnabled = spacingIndex != 0,
                plusEnabled = spacingIndex != LINE_SPACINGS.lastIndex,
            )
            FormatGroup(tr("मुख्य भाग की संरेखण", "Body alignment")) {
                val aligns = BodyAlign.entries
                GlassSegmentedControl(
                    options = listOf(tr("बाएँ", "Left"), tr("बीच में", "Center"), tr("दाएँ", "Right"), tr("बराबर", "Justify")),
                    selectedIndex = aligns.indexOf(formatting.bodyAlign),
                    onSelect = { onFormatting(formatting.copy(bodyAlign = aligns[it])) },
                )
            }
            FormatGroup(tr("हाशिए (मार्जिन)", "Margins")) {
                val presets = MarginPreset.entries
                val current = MarginPreset.matching(formatting.margins) ?: MarginPreset.NORMAL
                GlassSegmentedControl(
                    options = listOf(tr("सामान्य", "Normal"), tr("संकरे", "Narrow"), tr("चौड़े", "Wide")),
                    selectedIndex = presets.indexOf(current),
                    onSelect = { onFormatting(formatting.copy(margins = presets[it].toMargins())) },
                )
            }
            FormatGroup(tr("कागज़ का आकार", "Paper size")) {
                val papers = PaperSize.entries
                GlassSegmentedControl(
                    options = papers.map { it.label },
                    selectedIndex = papers.indexOf(formatting.paper),
                    onSelect = { onFormatting(formatting.copy(paper = papers[it])) },
                )
            }
            GlassButton(
                text = tr("हो गया", "Done"),
                onClick = onDone,
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
