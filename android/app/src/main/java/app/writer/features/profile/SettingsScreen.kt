package app.writer.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import app.writer.common.DateText
import app.writer.core.tr
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterType
import app.writer.design.components.GlassDialog
import app.writer.design.components.GlassGroup
import app.writer.design.components.GlassRow
import app.writer.design.components.GlassSettingSegment
import app.writer.design.components.GlassSwitchRow
import app.writer.design.components.GlassText
import app.writer.design.components.GlassToastState
import app.writer.design.components.GlassTopBar
import app.writer.design.components.GroupDivider
import app.writer.design.components.SectionTitle
import app.writer.design.components.ToastType
import app.writer.features.common.LibraryViewModel
import app.writer.features.common.LocalPrefs
import app.writer.features.common.MainViewModel
import app.writer.model.AppLanguage
import app.writer.model.ExportNameFormat
import app.writer.model.FontChoice
import app.writer.model.MarginPreset
import app.writer.model.PaperSize
import app.writer.model.ThemeMode

private enum class Confirm { SEARCHES, APPLICANT, DOCUMENTS }

private val fontSizes = listOf(11f, 12f, 13f, 14f)
private val lineSpacings = listOf(1.2f, 1.4f, 1.6f, 1.8f)
private val recentCounts = listOf(3, 5, 10)

@Composable
fun SettingsScreen(
    mainVm: MainViewModel,
    library: LibraryViewModel,
    toast: GlassToastState,
    onBack: () -> Unit,
) {
    val c = Glass.colors
    val prefs = LocalPrefs.current
    var confirm by rememberSaveable { mutableStateOf<Confirm?>(null) }

    val on = tr("चालू", "On")
    val off = tr("बंद", "Off")
    val searchesCleared = tr("हाल की खोज साफ़ हो गई", "Recent searches cleared")
    val applicantCleared = tr("सेव की गई जानकारी हटा दी गई", "Saved details removed")
    val documentsDeleted = tr("सभी दस्तावेज़ हटा दिए गए", "All documents deleted")

    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(title = tr("सेटिंग्स", "Settings"), onBack = onBack, backLabel = tr("वापस", "Back"))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                bottom = Spacing.x4,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
        ) {
            item(key = "appearance-title") { SectionTitle(tr("रूप और भाषा", "Appearance & language")) }
            item(key = "appearance") {
                GlassGroup {
                    GlassSettingSegment(
                        title = tr("रूप", "Appearance"),
                        options = listOf(tr("लाइट", "Light"), tr("डार्क", "Dark"), tr("सिस्टम डिफ़ॉल्ट", "System default")),
                        selectedIndex = when (prefs.theme) {
                            ThemeMode.LIGHT -> 0
                            ThemeMode.DARK -> 1
                            ThemeMode.SYSTEM -> 2
                        },
                        onSelect = { i -> mainVm.update { it.copy(theme = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)[i]) } },
                    )
                    GroupDivider()
                    GlassSettingSegment(
                        title = tr("ऐप की भाषा", "App language"),
                        options = listOf("हिंदी", "English"),
                        selectedIndex = if (prefs.language == AppLanguage.HI) 0 else 1,
                        onSelect = { i -> mainVm.update { it.copy(language = if (i == 0) AppLanguage.HI else AppLanguage.EN) } },
                    )
                }
            }

            item(key = "doc-title") { SectionTitle(tr("दस्तावेज़ की डिफ़ॉल्ट सेटिंग्स", "Document defaults")) }
            item(key = "doc") {
                GlassGroup {
                    GlassSettingSegment(
                        title = tr("पेपर साइज़", "Paper size"),
                        options = PaperSize.entries.map { it.label },
                        selectedIndex = PaperSize.entries.indexOf(prefs.paper),
                        onSelect = { i -> mainVm.update { it.copy(paper = PaperSize.entries[i]) } },
                    )
                    GroupDivider()
                    GlassSettingSegment(
                        title = tr("फ़ॉन्ट", "Font"),
                        options = listOf(tr("किताबी (Serif)", "Serif"), tr("सादा (Sans)", "Sans")),
                        selectedIndex = if (prefs.font == FontChoice.SERIF) 0 else 1,
                        onSelect = { i -> mainVm.update { it.copy(font = if (i == 0) FontChoice.SERIF else FontChoice.SANS) } },
                    )
                    GroupDivider()
                    GlassSettingSegment(
                        title = tr("फ़ॉन्ट साइज़", "Font size"),
                        options = fontSizes.map { "${it.toInt()} pt" },
                        selectedIndex = fontSizes.indexOf(prefs.fontSizePt).coerceAtLeast(0),
                        onSelect = { i -> mainVm.update { it.copy(fontSizePt = fontSizes[i]) } },
                    )
                    GroupDivider()
                    GlassSettingSegment(
                        title = tr("मार्जिन", "Margins"),
                        options = listOf(tr("सामान्य", "Normal"), tr("कम", "Narrow"), tr("ज़्यादा", "Wide")),
                        selectedIndex = MarginPreset.entries.indexOf(prefs.margins),
                        onSelect = { i -> mainVm.update { it.copy(margins = MarginPreset.entries[i]) } },
                    )
                    GroupDivider()
                    GlassSettingSegment(
                        title = tr("पंक्तियों के बीच दूरी", "Line spacing"),
                        options = lineSpacings.map { it.toString() },
                        selectedIndex = lineSpacings.indexOf(prefs.lineSpacing).coerceAtLeast(0),
                        onSelect = { i -> mainVm.update { it.copy(lineSpacing = lineSpacings[i]) } },
                    )
                    GroupDivider()
                    GlassSettingSegment(
                        title = tr("दस्तावेज़ की डिफ़ॉल्ट भाषा", "Default document language"),
                        subtitle = tr("नया दस्तावेज़ इसी भाषा में शुरू होता है", "New documents start in this language"),
                        options = listOf("हिंदी", "English"),
                        selectedIndex = if (prefs.docLanguage == AppLanguage.HI) 0 else 1,
                        onSelect = { i -> mainVm.update { it.copy(docLanguage = if (i == 0) AppLanguage.HI else AppLanguage.EN) } },
                    )
                }
            }
            item(key = "doc-note") {
                GlassText(
                    text = tr(
                        "ये सेटिंग्स सिर्फ़ नए दस्तावेज़ों पर लागू होती हैं। पुराने दस्तावेज़ नहीं बदलते।",
                        "These apply to new documents only. Existing documents do not change.",
                    ),
                    style = WriterType.caption,
                    color = c.textMuted,
                )
            }

            item(key = "editor-title") { SectionTitle(tr("एडिटर", "Editor")) }
            item(key = "editor") {
                GlassGroup {
                    GlassSwitchRow(
                        icon = Icons.Rounded.AutoMode,
                        title = tr("ऑटोसेव", "Autosave"),
                        subtitle = tr("लिखते समय ड्राफ्ट अपने-आप सेव होता है", "Saves your draft while you write"),
                        checked = prefs.autosave,
                        onCheckedChange = { v -> mainVm.update { it.copy(autosave = v) } },
                        stateOn = on,
                        stateOff = off,
                    )
                    GroupDivider()
                    GlassSwitchRow(
                        icon = Icons.Rounded.Numbers,
                        title = tr("अक्षर गिनती", "Character count"),
                        checked = prefs.charCount,
                        onCheckedChange = { v -> mainVm.update { it.copy(charCount = v) } },
                        stateOn = on,
                        stateOff = off,
                    )
                    GroupDivider()
                    GlassSwitchRow(
                        icon = Icons.Rounded.Vibration,
                        title = tr("हैप्टिक फ़ीडबैक", "Haptic feedback"),
                        checked = prefs.haptics,
                        onCheckedChange = { v -> mainVm.update { it.copy(haptics = v) } },
                        stateOn = on,
                        stateOff = off,
                    )
                }
            }

            item(key = "files-title") { SectionTitle(tr("फ़ाइलें", "Files")) }
            item(key = "files") {
                GlassGroup {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.x1),
                    ) {
                        GlassText(
                            text = tr("PDF का डिफ़ॉल्ट नाम", "Default PDF file name"),
                            style = WriterType.body,
                            modifier = Modifier.padding(horizontal = Spacing.x2, vertical = Spacing.x1),
                        )
                        val today = DateText.letterDate()
                        NameFormatOption(
                            selected = prefs.exportNameFormat == ExportNameFormat.TYPE_DATE,
                            title = tr("प्रकार + तारीख", "Type + date"),
                            example = "Application_$today.pdf".replace("/", "-"),
                            onClick = { mainVm.update { it.copy(exportNameFormat = ExportNameFormat.TYPE_DATE) } },
                        )
                        NameFormatOption(
                            selected = prefs.exportNameFormat == ExportNameFormat.TITLE_DATE,
                            title = tr("दस्तावेज़ का नाम + तारीख", "Document title + date"),
                            example = tr("मेरा आवेदन_", "My application_") + today.replace("/", "-") + ".pdf",
                            onClick = { mainVm.update { it.copy(exportNameFormat = ExportNameFormat.TITLE_DATE) } },
                        )
                        NameFormatOption(
                            selected = prefs.exportNameFormat == ExportNameFormat.TITLE_ONLY,
                            title = tr("सिर्फ़ दस्तावेज़ का नाम", "Document title only"),
                            example = tr("मेरा आवेदन.pdf", "My application.pdf"),
                            onClick = { mainVm.update { it.copy(exportNameFormat = ExportNameFormat.TITLE_ONLY) } },
                        )
                    }
                    GroupDivider()
                    GlassSettingSegment(
                        title = tr("होम पर हाल के दस्तावेज़", "Recent documents on Home"),
                        options = recentCounts.map { it.toString() },
                        selectedIndex = recentCounts.indexOf(prefs.recentDocsCount).coerceAtLeast(0),
                        onSelect = { i -> mainVm.update { it.copy(recentDocsCount = recentCounts[i]) } },
                    )
                }
            }

            item(key = "privacy-title") { SectionTitle(tr("गोपनीयता", "Privacy")) }
            item(key = "privacy") {
                GlassGroup {
                    GlassRow(
                        icon = Icons.Rounded.History,
                        title = tr("हाल की खोज साफ़ करें", "Clear recent searches"),
                        onClick = { confirm = Confirm.SEARCHES },
                    )
                    GroupDivider()
                    GlassRow(
                        icon = Icons.Rounded.PersonOff,
                        title = tr("सेव की गई आवेदक जानकारी हटाएं", "Clear saved applicant details"),
                        onClick = { confirm = Confirm.APPLICANT },
                    )
                    GroupDivider()
                    GlassRow(
                        icon = Icons.Rounded.DeleteForever,
                        iconTint = c.error,
                        title = tr("सभी स्थानीय दस्तावेज़ हटाएं", "Delete all local documents"),
                        onClick = { confirm = Confirm.DOCUMENTS },
                    )
                }
            }
            item(key = "privacy-note") {
                GlassText(
                    text = tr(
                        "दस्तावेज़ हटाने से सिर्फ़ ऐप के भीतर के दस्तावेज़ हटते हैं। जो PDF आपने अपने फ़ोन के किसी फ़ोल्डर में सेव की हैं, या किसी को भेजी हैं, वे नहीं हटेंगी।",
                        "Deleting documents removes only what is stored inside the app. PDFs you saved in a folder on your phone, or sent to someone, are not deleted.",
                    ),
                    style = WriterType.caption,
                    color = c.textMuted,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        }
    }

    when (confirm) {
        Confirm.SEARCHES -> GlassDialog(
            title = tr("हाल की खोज साफ़ करें?", "Clear recent searches?"),
            message = null,
            confirmText = tr("साफ़ करें", "Clear"),
            dismissText = tr("रद्द करें", "Cancel"),
            onConfirm = {
                mainVm.clearRecentSearches()
                confirm = null
                toast.show(searchesCleared, ToastType.Success)
            },
            onDismiss = { confirm = null },
        )
        Confirm.APPLICANT -> GlassDialog(
            title = tr("सेव की गई जानकारी हटाएं?", "Remove the saved details?"),
            message = tr(
                "आपके पुराने दस्तावेज़ नहीं बदलेंगे।",
                "Your existing documents will not change.",
            ),
            confirmText = tr("हटाएं", "Remove"),
            dismissText = tr("रद्द करें", "Cancel"),
            destructive = true,
            onConfirm = {
                mainVm.clearSavedApplicant()
                confirm = null
                toast.show(applicantCleared, ToastType.Success)
            },
            onDismiss = { confirm = null },
        )
        Confirm.DOCUMENTS -> GlassDialog(
            title = tr("सभी दस्तावेज़ हटाएं?", "Delete all documents?"),
            message = tr(
                "ऐप में रखे सभी दस्तावेज़ और ड्राफ्ट हमेशा के लिए हट जाएंगे। इसे वापस नहीं लाया जा सकता। आपके फ़ोन के फ़ोल्डरों में सेव की गई PDF फ़ाइलें नहीं हटेंगी।",
                "Every document and draft stored in the app will be deleted permanently. This cannot be undone. PDF files you saved in folders on your phone are not deleted.",
            ),
            confirmText = tr("सब हटाएं", "Delete all"),
            dismissText = tr("रद्द करें", "Cancel"),
            destructive = true,
            onConfirm = {
                library.deleteAllDocuments()
                confirm = null
                toast.show(documentsDeleted, ToastType.Success)
            },
            onDismiss = { confirm = null },
        )
        null -> Unit
    }
}

@Composable
private fun NameFormatOption(selected: Boolean, title: String, example: String, onClick: () -> Unit) {
    val c = Glass.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Spacing.x2, vertical = Spacing.half),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = c.electric, unselectedColor = c.textMuted),
        )
        Column(modifier = Modifier.weight(1f).padding(vertical = Spacing.x1)) {
            GlassText(text = title, style = WriterType.bodySmall)
            GlassText(text = example, style = WriterType.caption, color = c.textSecondary, maxLines = 1)
        }
    }
}
