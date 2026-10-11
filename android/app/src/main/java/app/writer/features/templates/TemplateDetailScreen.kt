package app.writer.features.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.writer.common.TemplateFiller
import app.writer.core.LocalAppLanguage
import app.writer.core.pick
import app.writer.core.tr
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.components.AccentIconTile
import app.writer.design.components.GlassBadge
import app.writer.design.components.GlassButton
import app.writer.design.components.GlassButtonStyle
import app.writer.design.components.GlassEmptyState
import app.writer.design.components.GlassLoadingIndicator
import app.writer.design.components.GlassSegmentedControl
import app.writer.design.components.GlassText
import app.writer.design.components.GlassToastState
import app.writer.design.components.GlassTopBar
import app.writer.design.components.SectionTitle
import app.writer.design.components.ToastType
import app.writer.features.common.LibraryViewModel
import app.writer.features.common.LocalPrefs
import app.writer.features.common.categoryStyle
import app.writer.model.AppLanguage
import app.writer.model.Categories
import app.writer.model.TemplateDef
import kotlinx.coroutines.launch

private val PaperInk = Color(0xFF1C1F2A)

@Composable
fun TemplateDetailScreen(
    templateId: String,
    library: LibraryViewModel,
    toast: GlassToastState,
    onBack: () -> Unit,
    onStarted: (documentId: String) -> Unit,
) {
    val c = Glass.colors
    val uiLanguage = LocalAppLanguage.current
    val prefs = LocalPrefs.current
    val templates by library.templates.collectAsStateWithLifecycle()
    val item = templates.firstOrNull { it.def.id == templateId }
    var docLanguage by rememberSaveable { mutableStateOf(prefs.docLanguage) }
    var starting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val failedMessage = tr("दस्तावेज़ नहीं बन सका। दोबारा कोशिश करें।", "Could not create the document. Please try again.")
    val addedMessage = tr("पसंदीदा में जोड़ा गया", "Added to favorites")
    val removedMessage = tr("पसंदीदा से हटाया गया", "Removed from favorites")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(title = tr("टेम्पलेट", "Template"), onBack = onBack, backLabel = tr("वापस", "Back"))
            when {
                item == null && templates.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GlassLoadingIndicator() }
                item == null -> GlassEmptyState(
                    icon = Icons.Rounded.EditNote,
                    title = tr("यह टेम्पलेट नहीं मिला", "This template was not found"),
                    message = null,
                    actionText = tr("वापस जाएं", "Go back"),
                    onAction = onBack,
                )
                else -> {
                    val def = item.def
                    val style = categoryStyle(def.category)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.screen)
                            .padding(bottom = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5)) {
                            AccentIconTile(icon = style.icon, accent = style.accent, tileSize = 56.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                GlassText(text = def.title[uiLanguage], style = WriterType.title)
                                GlassText(
                                    text = Categories.byId(def.category)?.title(uiLanguage) ?: "",
                                    style = WriterType.caption,
                                    color = c.textSecondary,
                                )
                            }
                        }
                        GlassText(text = def.description[uiLanguage], style = WriterType.body, color = c.textSecondary)

                        SectionTitle(tr("उपलब्ध भाषाएँ", "Available languages"))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                            GlassBadge(text = "हिंदी", color = c.electric)
                            GlassBadge(text = "English", color = c.electric)
                        }

                        SectionTitle(tr("दस्तावेज़ की भाषा", "Document language"))
                        GlassSegmentedControl(
                            options = listOf("हिंदी", "English"),
                            selectedIndex = if (docLanguage == AppLanguage.HI) 0 else 1,
                            onSelect = { docLanguage = if (it == 0) AppLanguage.HI else AppLanguage.EN },
                        )

                        SectionTitle(tr("संक्षिप्त झलक", "Short preview"))
                        PaperPreview(def = def, language = docLanguage)

                        GlassText(
                            text = tr(
                                "यह एक संपादन योग्य उदाहरण है, कोई सरकारी फ़ॉर्म नहीं। भेजने से पहले जानकारी जाँच लें और ज़रूरत हो तो बदलें।",
                                "This is an editable example, not an official form. Check the details and change them before you send it.",
                            ),
                            style = WriterType.caption,
                            color = c.textMuted,
                        )
                    }
                }
            }
        }

        if (item != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, c.background.copy(alpha = 0.92f))))
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.screen)
                    .padding(top = Spacing.x3, bottom = Spacing.x1_5),
                verticalArrangement = Arrangement.spacedBy(Spacing.x1),
            ) {
                GlassButton(
                    text = tr("लिखना शुरू करें", "Start writing"),
                    icon = Icons.Rounded.EditNote,
                    loading = starting,
                    onClick = {
                        if (!starting) {
                            starting = true
                            scope.launch {
                                val id = library.start(item.def.id, docLanguage)
                                starting = false
                                if (id != null) onStarted(id) else toast.show(failedMessage, ToastType.Error)
                            }
                        }
                    },
                )
                GlassButton(
                    text = if (item.isFavorite) tr("पसंदीदा से हटाएं", "Remove from favorites") else tr("पसंदीदा में जोड़ें", "Add to favorites"),
                    icon = if (item.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    style = GlassButtonStyle.Secondary,
                    onClick = {
                        library.setTemplateFavorite(item.def.id, !item.isFavorite)
                        toast.show(if (item.isFavorite) removedMessage else addedMessage, ToastType.Success)
                    },
                )
            }
        }
    }
}

/** A paper-like thumbnail of the template with its blanks visible, so people see what they will fill in. */
@Composable
private fun PaperPreview(def: TemplateDef, language: AppLanguage) {
    val blank = TemplateFiller.blankLabelFor(def, language)
    val none = emptyMap<String, String>()
    fun fill(text: String) = TemplateFiller.fill(text, none, blank)

    val recipient = def.recipient
    val recipientText = if (recipient == null) "" else listOf(recipient.designation[language], recipient.office[language])
        .filter { it.isNotBlank() }.joinToString("\n")
    val subject = fill(def.subject[language])
    val salutation = fill(def.salutation[language])
    val body = fill(def.body[language])
    val closing = fill(def.closing[language])
    val subjectLabel = pick(language, "विषय: ", "Subject: ")

    val textStyle = WriterType.bodySmall.copy(color = PaperInk, fontFamily = FontFamily.Serif, fontSize = 13.sp, lineHeight = 20.sp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(WriterShapes.chip)
            .background(Color.White)
            .padding(Spacing.x2),
        verticalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        if (recipientText.isNotEmpty()) {
            androidx.compose.material3.Text(text = recipientText, style = textStyle)
        }
        if (subject.isNotBlank()) {
            androidx.compose.material3.Text(text = subjectLabel + subject, style = textStyle.copy(fontWeight = FontWeight.Bold))
        }
        if (salutation.isNotBlank()) {
            androidx.compose.material3.Text(text = salutation, style = textStyle)
        }
        androidx.compose.material3.Text(text = body, style = textStyle, maxLines = 9, overflow = TextOverflow.Ellipsis)
        if (closing.isNotBlank()) {
            androidx.compose.material3.Text(text = closing, style = textStyle, modifier = Modifier.fillMaxWidth())
        }
    }
}
