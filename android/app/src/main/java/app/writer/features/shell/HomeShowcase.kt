package app.writer.features.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import app.writer.core.tr
import app.writer.design.Accents
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.bottomBarContentPadding
import app.writer.design.components.GlassButton
import app.writer.design.components.GlassCategoryCard
import app.writer.design.components.GlassDocumentCard
import app.writer.design.components.GlassMenuAction
import app.writer.design.components.GlassSearchBar
import app.writer.design.components.GlassSegmentedControl
import app.writer.design.components.GlassTabBar
import app.writer.design.components.GlassTemplateCard
import app.writer.design.components.GlassText
import app.writer.design.components.WriterLogo
import app.writer.design.components.glassPanel
import app.writer.design.topContentPadding
import app.writer.model.AppLanguage
import app.writer.model.ThemeMode

/**
 * Step 1 home screen: it exercises the design system (theme + language switching, search bar,
 * hero, categories, cards) so the Liquid Glass look can be checked on a device. Step 2
 * replaces it with the real dashboard.
 */
@Composable
fun HomeShowcase(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onMessage: (String) -> Unit,
) {
    val c = Glass.colors
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(0) }
    var starred by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = topContentPadding(),
            bottom = bottomBarContentPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5)) {
                WriterLogo(size = 44.dp)
                Column {
                    GlassText("Writer", style = WriterType.headline)
                    GlassText("Write • Apply • Get Results", style = WriterType.caption, color = c.textSecondary)
                }
            }
        }
        item {
            Column {
                GlassText(tr("नमस्ते!", "Hello!"), style = WriterType.display)
                GlassText(
                    tr("आपका पत्र, आपका दस्तावेज़ — सब एक जगह।", "Your letters, your documents — all in one place."),
                    style = WriterType.body,
                    color = c.textSecondary,
                )
            }
        }
        item {
            GlassSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = tr("किस प्रकार का पत्र लिखना है?", "What kind of letter do you want to write?"),
                clearLabel = tr("साफ़ करें", "Clear"),
            )
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPanel(
                        shape = WriterShapes.cardLarge,
                        elevated = true,
                        fillBrush = Brush.linearGradient(
                            listOf(c.primary.copy(alpha = 0.55f), c.indigo.copy(alpha = 0.5f), c.purple.copy(alpha = 0.5f)),
                        ),
                    )
                    .padding(Spacing.x3),
                verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
            ) {
                GlassText(tr("आवेदन लिखना अब आसान", "Writing applications, made easy"), style = WriterType.title, color = androidx.compose.ui.graphics.Color.White)
                GlassText(
                    tr("सही फॉर्मेट में अपना आवेदन तैयार करें और PDF में सेव करें।", "Prepare your application in the right format and save it as a PDF."),
                    style = WriterType.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                )
                GlassButton(
                    text = tr("शुरू करें", "Get started"),
                    onClick = { onMessage(tr("टेम्पलेट अगले चरण में जुड़ेंगे", "Templates arrive in the next step")) },
                    trailingIcon = Icons.Rounded.ArrowForward,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                GlassText(tr("थीम", "Appearance"), style = WriterType.headline)
                GlassSegmentedControl(
                    options = listOf(tr("लाइट", "Light"), tr("डार्क", "Dark"), tr("सिस्टम", "System")),
                    selectedIndex = when (themeMode) {
                        ThemeMode.LIGHT -> 0
                        ThemeMode.DARK -> 1
                        ThemeMode.SYSTEM -> 2
                    },
                    onSelect = { onThemeChange(listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)[it]) },
                )
                GlassSegmentedControl(
                    options = listOf("हिंदी", "English"),
                    selectedIndex = if (language == AppLanguage.HI) 0 else 1,
                    onSelect = { onLanguageChange(if (it == 0) AppLanguage.HI else AppLanguage.EN) },
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                GlassText(tr("श्रेणियाँ", "Categories"), style = WriterType.headline)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5)) {
                    items(
                        listOf(
                            Triple(Icons.Rounded.AccountBalance, tr("सरकारी आवेदन", "Government"), Accents.Blue),
                            Triple(Icons.Rounded.School, tr("शिक्षा", "Education"), Accents.Green),
                            Triple(Icons.Rounded.Work, tr("नौकरी / करियर", "Jobs / Career"), Accents.Purple),
                            Triple(Icons.Rounded.Bolt, tr("बिजली / पानी", "Electricity / Water"), Accents.Orange),
                        ),
                    ) { (icon, label, accent) ->
                        GlassCategoryCard(icon = icon, label = label, accent = accent, onClick = { onMessage(label) })
                    }
                }
            }
        }
        item {
            GlassTabBar(
                tabs = listOf(tr("सभी", "All"), tr("ड्राफ्ट", "Drafts"), tr("पूर्ण", "Completed"), "PDFs"),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }
        item {
            GlassTemplateCard(
                title = tr("जिला अधिकारी को आवेदन", "Application to District Magistrate"),
                description = tr("जिला अधिकारी महोदय को आवेदन पत्र", "A formal application to the District Magistrate"),
                accent = Accents.Blue,
                badges = listOf("हिंदी", "English"),
                isFavorite = starred,
                favoriteLabel = tr("पसंदीदा", "Favorite"),
                onToggleFavorite = { starred = !starred },
                onClick = { onMessage(tr("टेम्पलेट विवरण अगले चरण में", "Template details arrive in the next step")) },
            )
        }
        item {
            GlassDocumentCard(
                title = tr("राशन कार्ड सुधार आवेदन", "Ration card correction"),
                subtitle = tr("09 अक्टू 2026 • आवेदन", "09 Oct 2026 • Application"),
                statusLabel = tr("ड्राफ्ट", "Draft"),
                isDraft = true,
                isPdf = false,
                isFavorite = false,
                menuLabel = tr("और विकल्प", "More options"),
                menuActions = listOf(
                    GlassMenuAction(tr("हटाएं", "Delete"), Icons.Rounded.Delete, onClick = { onMessage(tr("हटाने से पहले पुष्टि पूछी जाएगी", "Delete will ask for confirmation")) }, destructive = true),
                ),
                onClick = { onMessage(tr("संपादक अगले चरण में", "The editor arrives in the next step")) },
            )
        }
    }
}
