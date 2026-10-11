package app.writer.features.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.writer.core.tr
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.bottomBarContentPadding
import app.writer.design.components.GlassButton
import app.writer.design.components.GlassButtonStyle
import app.writer.design.components.GlassCard
import app.writer.design.components.GlassDialog
import app.writer.design.components.GlassGroup
import app.writer.design.components.GlassRow
import app.writer.design.components.GlassSettingSegment
import app.writer.design.components.GlassSwitchRow
import app.writer.design.components.GlassText
import app.writer.design.components.GlassTextField
import app.writer.design.components.GlassToastState
import app.writer.design.components.GroupDivider
import app.writer.design.components.SectionTitle
import app.writer.design.components.ToastType
import app.writer.design.components.glassPanel
import app.writer.design.topContentPadding
import app.writer.features.common.LibraryViewModel
import app.writer.features.common.LocalPrefs
import app.writer.features.common.MainViewModel
import app.writer.model.AppLanguage
import app.writer.model.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    library: LibraryViewModel,
    mainVm: MainViewModel,
    toast: GlassToastState,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val c = Glass.colors
    val context = LocalContext.current
    val prefs = LocalPrefs.current
    val scope = rememberCoroutineScope()
    val documents by library.documents.collectAsStateWithLifecycle()
    val photo by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, prefs.profileImageStamp) {
        value = if (prefs.profileImageStamp == 0L) null else ProfileImage.load(context)?.asImageBitmap()
    }

    var name by rememberSaveable { mutableStateOf(prefs.userName) }
    val latestName by rememberUpdatedState(name)
    val savedName by rememberUpdatedState(prefs.userName)
    LaunchedEffect(name) {
        delay(600)
        if (name.trim() != savedName) mainVm.update { it.copy(userName = name.trim()) }
    }
    DisposableEffect(Unit) {
        onDispose { if (latestName.trim() != savedName) mainVm.update { it.copy(userName = latestName.trim()) } }
    }

    var confirmClearApplicant by rememberSaveable { mutableStateOf(false) }
    val photoFailed = tr("फ़ोटो नहीं जोड़ी जा सकी", "Could not use that photo")
    val applicantCleared = tr("सेव की गई जानकारी हटा दी गई", "Saved details removed")
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                if (ProfileImage.save(context, uri)) {
                    mainVm.update { it.copy(profileImageStamp = System.currentTimeMillis()) }
                } else {
                    toast.show(photoFailed, ToastType.Error)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = topContentPadding(),
            bottom = bottomBarContentPadding(),
            start = Spacing.screen,
            end = Spacing.screen,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
    ) {
        item(key = "title") { GlassText(text = tr("प्रोफ़ाइल", "Profile"), style = WriterType.title) }

        item(key = "identity") {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(WriterShapes.circle)
                            .background(c.glassElevated)
                            .clickable(role = Role.Button, onClickLabel = tr("फ़ोटो बदलें", "Change photo")) {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        val image = photo
                        if (image != null) {
                            Image(
                                bitmap = image,
                                contentDescription = tr("प्रोफ़ाइल फ़ोटो", "Profile photo"),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(92.dp),
                            )
                        } else {
                            androidx.compose.material3.Icon(
                                Icons.Rounded.Person,
                                contentDescription = tr("फ़ोटो जोड़ें", "Add a photo"),
                                tint = c.textSecondary,
                                modifier = Modifier.size(44.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                        GlassTextField(
                            value = name,
                            onValueChange = { name = it.take(40) },
                            label = tr("आपका नाम (वैकल्पिक)", "Your name (optional)"),
                            placeholder = tr("होम पर अभिवादन के लिए", "Used for the greeting on Home"),
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = Spacing.x1_5),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
                ) {
                    GlassButton(
                        text = tr("फ़ोटो चुनें", "Choose photo"),
                        icon = Icons.Rounded.CameraAlt,
                        style = GlassButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    )
                    if (photo != null) {
                        GlassButton(
                            text = tr("फ़ोटो हटाएं", "Remove photo"),
                            icon = Icons.Rounded.DeleteOutline,
                            style = GlassButtonStyle.Secondary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                ProfileImage.delete(context)
                                mainVm.update { it.copy(profileImageStamp = 0L) }
                            },
                        )
                    }
                }
                GlassText(
                    text = tr(
                        "नाम और फ़ोटो सिर्फ़ इस फ़ोन में रहते हैं। लॉगिन की ज़रूरत नहीं है।",
                        "Your name and photo stay on this phone. No sign-in is needed.",
                    ),
                    style = WriterType.caption,
                    color = c.textMuted,
                    modifier = Modifier.padding(top = Spacing.x1),
                )
            }
        }

        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5)) {
                StatCard(
                    icon = Icons.Rounded.Description,
                    value = (documents?.size ?: 0).toString(),
                    label = tr("सहेजे गए दस्तावेज़", "Saved documents"),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Rounded.PictureAsPdf,
                    value = prefs.exportCount.toString(),
                    label = tr("बनाई गई PDF", "PDFs exported"),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "prefs-title") { SectionTitle(tr("ऐप की पसंद", "App preferences")) }
        item(key = "prefs") {
            GlassGroup {
                GlassSettingSegment(
                    title = tr("रूप", "Appearance"),
                    options = listOf(tr("सिस्टम", "System"), tr("लाइट", "Light"), tr("डार्क", "Dark")),
                    selectedIndex = when (prefs.theme) {
                        ThemeMode.SYSTEM -> 0
                        ThemeMode.LIGHT -> 1
                        ThemeMode.DARK -> 2
                    },
                    onSelect = { index ->
                        mainVm.update { it.copy(theme = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)[index]) }
                    },
                )
                GroupDivider()
                GlassSettingSegment(
                    title = tr("ऐप की भाषा", "App language"),
                    options = listOf("हिंदी", "English"),
                    selectedIndex = if (prefs.language == AppLanguage.HI) 0 else 1,
                    onSelect = { index -> mainVm.update { it.copy(language = if (index == 0) AppLanguage.HI else AppLanguage.EN) } },
                )
            }
        }

        item(key = "applicant-title") { SectionTitle(tr("अक्सर इस्तेमाल होने वाली जानकारी", "Frequently used details")) }
        item(key = "applicant") {
            GlassGroup {
                GlassSwitchRow(
                    icon = Icons.Rounded.Badge,
                    title = tr("आवेदक की जानकारी सेव करें", "Save applicant details"),
                    subtitle = tr("नए दस्तावेज़ों में अपने-आप भरने के लिए", "To fill in new documents automatically"),
                    checked = prefs.saveApplicant,
                    onCheckedChange = { on -> mainVm.update { it.copy(saveApplicant = on) } },
                    stateOn = tr("चालू", "On"),
                    stateOff = tr("बंद", "Off"),
                )
                GlassText(
                    text = tr(
                        "क्या सेव होता है: आपका नाम, पिता/माता का नाम, पता, गाँव, डाकघर, ब्लॉक, जनपद, राज्य, पिन कोड और मोबाइल नंबर — सिर्फ़ तब, जब आप किसी दस्तावेज़ में ये भरते हैं और यह विकल्प चालू है। यह जानकारी केवल इस फ़ोन में रहती है, कहीं भेजी नहीं जाती। आप इसे नीचे के बटन से कभी भी हटा सकते हैं; विकल्प बंद करने पर भी पुरानी जानकारी तब तक रहती है जब तक आप उसे हटाते नहीं।",
                        "What is saved: your name, parent's name, address, village, post office, block, district, state, PIN code and mobile number — only when you fill them in a document while this option is on. It stays on this phone and is never sent anywhere. You can remove it any time with the button below; turning the option off keeps the old details until you remove them.",
                    ),
                    style = WriterType.caption,
                    color = c.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.x2).padding(bottom = Spacing.x1_5),
                )
                if (!prefs.savedApplicant.isEmpty) {
                    GroupDivider()
                    GlassRow(
                        icon = Icons.Rounded.DeleteOutline,
                        iconTint = c.error,
                        title = tr("सेव की गई जानकारी हटाएं", "Remove saved details"),
                        subtitle = prefs.savedApplicant.name.takeIf { it.isNotBlank() },
                        onClick = { confirmClearApplicant = true },
                    )
                }
            }
        }

        item(key = "more") {
            GlassGroup {
                GlassRow(Icons.Rounded.Settings, tr("सेटिंग्स", "Settings"), onClick = onOpenSettings)
                GroupDivider()
                GlassRow(Icons.Rounded.HelpOutline, tr("सहायता", "Help & support"), onClick = onOpenHelp)
                GroupDivider()
                GlassRow(Icons.Rounded.PrivacyTip, tr("गोपनीयता नीति", "Privacy policy"), onClick = onOpenPrivacy)
                GroupDivider()
                GlassRow(Icons.Rounded.Info, tr("Writer के बारे में", "About Writer"), onClick = onOpenAbout)
            }
        }
    }

    if (confirmClearApplicant) {
        GlassDialog(
            title = tr("सेव की गई जानकारी हटाएं?", "Remove the saved details?"),
            message = tr(
                "आपके पुराने दस्तावेज़ नहीं बदलेंगे। नए दस्तावेज़ों में यह जानकारी अपने-आप नहीं भरेगी।",
                "Your existing documents will not change. New documents will no longer be filled in automatically.",
            ),
            confirmText = tr("हटाएं", "Remove"),
            dismissText = tr("रद्द करें", "Cancel"),
            destructive = true,
            onConfirm = {
                mainVm.clearSavedApplicant()
                confirmClearApplicant = false
                toast.show(applicantCleared, ToastType.Success)
            },
            onDismiss = { confirmClearApplicant = false },
        )
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val c = Glass.colors
    Column(
        modifier = modifier
            .glassPanel(shape = WriterShapes.card)
            .padding(Spacing.x2),
        verticalArrangement = Arrangement.spacedBy(Spacing.half),
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = c.electric, modifier = Modifier.size(24.dp))
        GlassText(text = value, style = WriterType.display)
        GlassText(text = label, style = WriterType.caption, color = c.textSecondary)
    }
}
