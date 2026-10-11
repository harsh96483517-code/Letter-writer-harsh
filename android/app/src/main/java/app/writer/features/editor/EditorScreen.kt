package app.writer.features.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignJustify
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatLineSpacing
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.writer.common.AddressFormatter
import app.writer.common.SpanMath
import app.writer.common.TemplateFiller
import app.writer.core.LocalAppLanguage
import app.writer.core.tr
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterType
import app.writer.design.components.GlassBadge
import app.writer.design.components.GlassButton
import app.writer.design.components.GlassButtonStyle
import app.writer.design.components.GlassDialog
import app.writer.design.components.GlassEmptyState
import app.writer.design.components.GlassIconButton
import app.writer.design.components.GlassLoadingIndicator
import app.writer.design.components.GlassText
import app.writer.design.components.GlassTextButton
import app.writer.design.components.GlassTextField
import app.writer.design.components.GlassToastState
import app.writer.design.components.GlassToolbar
import app.writer.design.components.GlassTopBar
import app.writer.design.components.ToastType
import app.writer.design.components.ToolbarButton
import app.writer.design.components.ToolbarDivider
import app.writer.features.common.LocalPrefs
import app.writer.features.common.appViewModel
import app.writer.model.BodyAlign
import app.writer.model.LetterStyle
import app.writer.model.WriterDocument
import kotlinx.coroutines.launch

/**
 * The letter editor: a form on top of the letter model, a multi-line body with bold and italic,
 * and a floating toolbar. Everything typed is saved to the database after a short pause, so an
 * unfinished letter is still there after the app is closed and reopened.
 */
@Composable
fun EditorScreen(
    docId: String,
    toast: GlassToastState,
    onBack: () -> Unit,
    onPreview: (String) -> Unit,
) {
    val vm = appViewModel(key = "editor-$docId") { EditorViewModel(it, docId) }
    val prefs = LocalPrefs.current
    vm.autosave = prefs.autosave
    val scope = rememberCoroutineScope()

    var showFormat by rememberSaveable { mutableStateOf(false) }
    var showRebuild by rememberSaveable { mutableStateOf(false) }
    var showLeave by rememberSaveable { mutableStateOf(false) }

    val saveFailed = tr("सेव नहीं हो सका। दोबारा कोशिश करें।", "Could not save. Please try again.")
    val savedMessage = tr("सेव हो गया", "Saved")

    // Keep what was typed even if the system ends the app while it is in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.flushInBackground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val leave: () -> Unit = {
        scope.launch {
            if (!prefs.autosave && vm.hasUnsavedChanges) {
                showLeave = true
            } else {
                if (prefs.autosave) vm.saveNow()
                onBack()
            }
        }
    }
    BackHandler(onBack = leave)

    val doc = vm.doc
    when {
        vm.missing -> Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(title = tr("एडिटर", "Editor"), onBack = onBack, backLabel = tr("वापस", "Back"))
            GlassEmptyState(
                icon = Icons.Rounded.ErrorOutline,
                title = tr("दस्तावेज़ नहीं मिला", "Document not found"),
                message = tr("हो सकता है यह हटा दिया गया हो।", "It may have been deleted."),
                actionText = tr("वापस जाएं", "Go back"),
                onAction = onBack,
            )
        }

        doc == null -> Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(title = tr("एडिटर", "Editor"), onBack = onBack, backLabel = tr("वापस", "Back"))
            Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x4), contentAlignment = Alignment.Center) {
                GlassLoadingIndicator()
            }
        }

        else -> EditorContent(
            vm = vm,
            doc = doc,
            toast = toast,
            showFormat = showFormat,
            onShowFormat = { showFormat = it },
            onLeave = leave,
            onRebuild = { showRebuild = true },
            onSave = {
                scope.launch {
                    if (vm.saveNow()) toast.show(savedMessage, ToastType.Success) else toast.show(saveFailed, ToastType.Error)
                }
            },
            onPreview = {
                scope.launch {
                    if (vm.saveNow()) onPreview(docId) else toast.show(saveFailed, ToastType.Error)
                }
            },
        )
    }

    if (showRebuild) {
        GlassDialog(
            title = tr("टेम्पलेट से दोबारा बनाएं?", "Rebuild from the template?"),
            message = tr(
                "पत्र का विषय और मुख्य भाग टेम्पलेट और ऊपर के उत्तरों से दोबारा लिखा जाएगा। मुख्य भाग में आपके किए बदलाव हट जाएंगे। आप “पूर्ववत” से वापस ला सकते हैं।",
                "The subject and body are written again from the template and your answers. Changes you made in the body are replaced. You can bring them back with Undo.",
            ),
            confirmText = tr("दोबारा बनाएं", "Rebuild"),
            dismissText = tr("रद्द करें", "Cancel"),
            destructive = true,
            onConfirm = {
                showRebuild = false
                vm.rebuildFromTemplate()
            },
            onDismiss = { showRebuild = false },
        )
    }

    if (showLeave) {
        GlassDialog(
            title = tr("बदलाव सेव करें?", "Save your changes?"),
            message = tr(
                "ऑटो-सेव बंद है, इसलिए ये बदलाव अभी सेव नहीं हुए हैं।",
                "Autosave is off, so these changes are not saved yet.",
            ),
            confirmText = tr("सेव करें", "Save"),
            dismissText = tr("रुकें", "Keep editing"),
            onConfirm = {
                showLeave = false
                scope.launch {
                    if (vm.saveNow()) onBack() else toast.show(saveFailed, ToastType.Error)
                }
            },
            onDismiss = { showLeave = false },
            content = {
                GlassTextButton(
                    text = tr("बदलाव छोड़ें", "Discard changes"),
                    onClick = {
                        showLeave = false
                        onBack()
                    },
                )
            },
        )
    }
}

@Composable
private fun EditorContent(
    vm: EditorViewModel,
    doc: WriterDocument,
    toast: GlassToastState,
    showFormat: Boolean,
    onShowFormat: (Boolean) -> Unit,
    onLeave: () -> Unit,
    onRebuild: () -> Unit,
    onSave: () -> Unit,
    onPreview: () -> Unit,
) {
    val c = Glass.colors
    val prefs = LocalPrefs.current
    val template = vm.template
    val formatting = doc.formatting

    // The body field keeps its own cursor and selection. When the view-model replaces the text
    // (undo, rebuild, answering a template question) the field follows it.
    var bodyField by remember { mutableStateOf(TextFieldValue(doc.body, TextRange(doc.body.length))) }
    LaunchedEffect(vm.bodyRevision) {
        val current = vm.doc ?: return@LaunchedEffect
        if (bodyField.text != current.body) {
            bodyField = TextFieldValue(current.body, TextRange(current.body.length))
        }
    }
    val selection = bodyField.selection
    val hasSelection = !selection.collapsed
    val boldActive = hasSelection && SpanMath.isAll(doc.bodySpans, selection.min, selection.max, italic = false)
    val italicActive = hasSelection && SpanMath.isAll(doc.bodySpans, selection.min, selection.max, italic = true)

    val selectFirst = tr("पहले वह टेक्स्ट चुनें जिसे बदलना है।", "Select the text you want to change first.")
    val fillDetails = tr("पहले आवेदक की जानकारी भरें।", "Fill in the applicant details first.")
    val noDate = tr("तारीख खाली है।", "The date is empty.")
    val mobileLabel = tr("मो.: ", "Mob.: ")

    fun insertAtCursor(text: String) {
        val sel = bodyField.selection
        val start = sel.min.coerceIn(0, bodyField.text.length)
        val end = sel.max.coerceIn(start, bodyField.text.length)
        val newText = bodyField.text.replaceRange(start, end, text)
        bodyField = TextFieldValue(newText, TextRange(start + text.length))
        vm.onBodyChanged(newText)
    }

    val title = when {
        doc.templateId == null -> tr("खाली दस्तावेज़", "Blank document")
        doc.style == LetterStyle.PERSONAL -> tr("व्यक्तिगत पत्र", "Personal letter")
        doc.templateId.startsWith("cmp_") -> tr("शिकायत पत्र", "Complaint letter")
        else -> tr("नया आवेदन पत्र", "New application")
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        GlassTopBar(
            title = title,
            onBack = onLeave,
            backLabel = tr("वापस", "Back"),
            actions = {
                GlassIconButton(
                    icon = Icons.Rounded.Tune,
                    contentDescription = tr("फ़ॉर्मेट", "Format"),
                    onClick = { onShowFormat(true) },
                )
                GlassIconButton(
                    icon = Icons.Rounded.Visibility,
                    contentDescription = tr("पूर्वावलोकन", "Preview"),
                    onClick = onPreview,
                )
            },
        )

        // Save status and an explicit Save button.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (vm.saveState) {
                SaveState.SAVED -> GlassBadge(text = tr("सेव हो गया", "Saved"), color = c.success)
                SaveState.SAVING -> GlassBadge(text = tr("सेव हो रहा है…", "Saving…"), color = c.electric)
                SaveState.UNSAVED -> GlassBadge(
                    text = if (prefs.autosave) tr("सेव होने वाला है…", "Saving soon…") else tr("बिना सेव किए बदलाव", "Unsaved changes"),
                    color = c.warning,
                )
                SaveState.FAILED -> GlassBadge(text = tr("सेव नहीं हो सका", "Could not save"), color = c.error)
            }
            Box(modifier = Modifier.weight(1f))
            GlassTextButton(text = tr("सेव करें", "Save"), onClick = onSave)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.x1),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
        ) {
            GlassTextField(
                value = doc.title,
                onValueChange = vm::setTitle,
                label = tr("दस्तावेज़ का नाम", "Document name"),
            )

            if (template != null && template.fields.isNotEmpty()) {
                EditorSection(
                    title = tr("इस पत्र के बारे में", "About this letter"),
                    hint = tr(
                        "इन उत्तरों से पत्र का मुख्य भाग अपने आप बनता है। जवाब बदलें तो भाग बदल जाता है — जब तक आप खुद उसमें नहीं लिखते।",
                        "Your answers write the body for you. Change an answer and the body follows, until you start typing in it yourself.",
                    ),
                ) {
                    TemplateQuestions(
                        fields = template.fields,
                        language = doc.language,
                        values = doc.fieldValues,
                        onChange = vm::setField,
                    )
                }
            }

            EditorSection(
                title = if (doc.style == LetterStyle.PERSONAL) tr("प्राप्तकर्ता (वैकल्पिक)", "Recipient (optional)") else tr("प्राप्तकर्ता", "Recipient"),
                initiallyExpanded = doc.style == LetterStyle.APPLICATION && doc.templateId != null,
                hint = tr(
                    "सरकारी कार्यालय का पदनाम और पता आप बदल सकते हैं।",
                    "You can change the office designation and address.",
                ),
            ) {
                RecipientFields(value = doc.recipient, onChange = vm::setRecipient)
            }

            EditorSection(
                title = tr("आवेदक की जानकारी", "Applicant details"),
                initiallyExpanded = doc.applicant.isEmpty,
                hint = if (prefs.saveApplicant) {
                    tr("ये जानकारी इस फ़ोन पर अगले पत्रों के लिए सेव रहेगी (सेटिंग्स में बंद कर सकते हैं)।", "These details are kept on this phone for your next letters (switch it off in Settings).")
                } else {
                    tr("केवल वही जानकारी भरें जो पत्र में चाहिए।", "Only fill in what the letter needs.")
                },
            ) {
                ApplicantFields(value = doc.applicant, onChange = vm::setApplicant)
                GlassTextField(
                    value = doc.dateText,
                    onValueChange = vm::setDate,
                    label = tr("दिनांक", "Date"),
                )
            }

            EditorSection(title = tr("विषय", "Subject"), initiallyExpanded = doc.style == LetterStyle.APPLICATION) {
                GlassTextField(
                    value = doc.subject,
                    onValueChange = vm::setSubject,
                    label = tr("विषय", "Subject"),
                    placeholder = tr("संक्षिप्त विषय लिखें", "A short subject line"),
                )
                // The template's own wording is offered back once the user has changed it.
                val suggestion = remember(doc.applicant, doc.recipient, doc.dateText, doc.fieldValues, doc.language, template) {
                    if (template == null || template.isBlank) {
                        ""
                    } else {
                        TemplateFiller.fill(
                            template.subject[doc.language],
                            TemplateFiller.valuesFor(doc.applicant, doc.recipient, doc.dateText, doc.fieldValues, doc.language),
                            TemplateFiller.blankLabelFor(template, doc.language),
                        )
                    }
                }
                if (suggestion.isNotBlank() && suggestion != doc.subject) {
                    GlassText(text = tr("सुझाव", "Suggestion"), style = WriterType.caption, color = c.textSecondary)
                    GlassButton(
                        text = suggestion,
                        onClick = { vm.setSubject(suggestion) },
                        style = GlassButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            EditorSection(title = tr("पत्र का मुख्य भाग", "Letter body")) {
                GlassTextField(
                    value = doc.salutation,
                    onValueChange = vm::setSalutation,
                    label = tr("संबोधन", "Salutation"),
                    placeholder = tr("जैसे: महोदय,", "e.g. Dear Sir,"),
                )
                if (template != null && !template.isBlank && doc.bodyEdited) {
                    GlassText(
                        text = tr(
                            "आपने मुख्य भाग में खुद बदलाव किया है, इसलिए ऊपर के उत्तर अब उसे नहीं बदलेंगे।",
                            "You have edited the body yourself, so the answers above no longer change it.",
                        ),
                        style = WriterType.caption,
                        color = c.textSecondary,
                    )
                    GlassButton(
                        text = tr("टेम्पलेट से दोबारा बनाएं", "Rebuild from the template"),
                        onClick = onRebuild,
                        style = GlassButtonStyle.Secondary,
                        icon = Icons.Rounded.Refresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                BodyField(
                    value = bodyField,
                    onValueChange = { newValue ->
                        bodyField = newValue
                        if (newValue.text != doc.body) vm.onBodyChanged(newValue.text)
                    },
                    spans = doc.bodySpans,
                    placeholder = tr("यहाँ अपना पत्र लिखें…", "Write your letter here…"),
                )
                if (prefs.charCount) {
                    GlassText(
                        text = tr("${doc.body.length} अक्षर", "${doc.body.length} characters"),
                        style = WriterType.caption,
                        color = c.textMuted,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
                GlassTextField(
                    value = doc.closing,
                    onValueChange = vm::setClosing,
                    label = tr("समापन", "Closing"),
                    placeholder = tr("जैसे: भवदीय,", "e.g. Yours faithfully,"),
                )
            }

            GlassButton(
                text = tr("पूर्वावलोकन देखें", "See the preview"),
                onClick = onPreview,
                icon = Icons.Rounded.Visibility,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.x1),
            )
            Box(modifier = Modifier.padding(bottom = Spacing.x1))
        }

        // Floating toolbar. It sits above the keyboard and above the gesture bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.x1_5, vertical = Spacing.x1),
            contentAlignment = Alignment.Center,
        ) {
            GlassToolbar {
                ToolbarButton(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = tr("पूर्ववत करें", "Undo"),
                    onClick = vm::undo,
                    enabled = vm.canUndo,
                )
                ToolbarButton(
                    icon = Icons.AutoMirrored.Rounded.Redo,
                    contentDescription = tr("फिर से करें", "Redo"),
                    onClick = vm::redo,
                    enabled = vm.canRedo,
                )
                ToolbarDivider()
                ToolbarButton(
                    icon = Icons.Rounded.FormatBold,
                    contentDescription = tr("बोल्ड", "Bold"),
                    selected = boldActive,
                    onClick = {
                        if (hasSelection) vm.toggleStyle(selection.min, selection.max, italic = false) else toast.show(selectFirst)
                    },
                )
                ToolbarButton(
                    icon = Icons.Rounded.FormatItalic,
                    contentDescription = tr("इटैलिक", "Italic"),
                    selected = italicActive,
                    onClick = {
                        if (hasSelection) vm.toggleStyle(selection.min, selection.max, italic = true) else toast.show(selectFirst)
                    },
                )
                ToolbarButton(
                    icon = when (formatting.bodyAlign) {
                        BodyAlign.LEFT -> Icons.Rounded.FormatAlignLeft
                        BodyAlign.CENTER -> Icons.Rounded.FormatAlignCenter
                        BodyAlign.RIGHT -> Icons.Rounded.FormatAlignRight
                        BodyAlign.JUSTIFY -> Icons.Rounded.FormatAlignJustify
                    },
                    contentDescription = tr("संरेखण बदलें", "Change alignment"),
                    onClick = { vm.setFormatting(formatting.copy(bodyAlign = nextAlign(formatting.bodyAlign))) },
                )
                ToolbarDivider()
                ToolbarButton(
                    icon = Icons.Rounded.TextDecrease,
                    contentDescription = tr("फ़ॉन्ट छोटा करें", "Smaller text"),
                    enabled = formatting.fontSizePt > MIN_FONT_PT,
                    onClick = { vm.setFormatting(formatting.copy(fontSizePt = (formatting.fontSizePt - 1f).coerceAtLeast(MIN_FONT_PT))) },
                )
                ToolbarButton(
                    icon = Icons.Rounded.TextIncrease,
                    contentDescription = tr("फ़ॉन्ट बड़ा करें", "Larger text"),
                    enabled = formatting.fontSizePt < MAX_FONT_PT,
                    onClick = { vm.setFormatting(formatting.copy(fontSizePt = (formatting.fontSizePt + 1f).coerceAtMost(MAX_FONT_PT))) },
                )
                ToolbarButton(
                    icon = Icons.Rounded.FormatLineSpacing,
                    contentDescription = tr("पंक्ति की दूरी बदलें", "Change line spacing"),
                    onClick = { vm.setFormatting(formatting.copy(lineSpacing = nextLineSpacing(formatting.lineSpacing))) },
                )
                ToolbarDivider()
                ToolbarButton(
                    icon = Icons.Rounded.Today,
                    contentDescription = tr("तारीख डालें", "Insert date"),
                    onClick = {
                        if (doc.dateText.isBlank()) toast.show(noDate) else insertAtCursor(doc.dateText)
                    },
                )
                ToolbarButton(
                    icon = Icons.Rounded.Badge,
                    contentDescription = tr("आवेदक की जानकारी डालें", "Insert applicant details"),
                    onClick = {
                        val a = doc.applicant
                        val text = listOf(
                            a.name.trim(),
                            AddressFormatter.full(a, doc.language),
                            if (a.mobile.isBlank()) "" else (mobileLabel + a.mobile.trim()),
                        ).filter { it.isNotEmpty() }.joinToString("\n")
                        if (text.isEmpty()) toast.show(fillDetails) else insertAtCursor(text)
                    },
                )
                ToolbarDivider()
                ToolbarButton(
                    icon = Icons.Rounded.Visibility,
                    contentDescription = tr("पूर्वावलोकन", "Preview"),
                    onClick = onPreview,
                )
            }
        }
    }

    if (showFormat) {
        FormatSheet(
            formatting = formatting,
            language = doc.language,
            onFormatting = vm::setFormatting,
            onLanguage = vm::setLanguage,
            onDone = { onShowFormat(false) },
        )
    }
}
