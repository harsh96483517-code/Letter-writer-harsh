package app.writer.features.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.writer.common.TemplateFilter
import app.writer.common.TemplateSearch
import app.writer.core.LocalAppLanguage
import app.writer.core.tr
import app.writer.design.Accents
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.bottomBarContentPadding
import app.writer.design.components.AccentIconTile
import app.writer.design.components.GlassButton
import app.writer.design.components.GlassCategoryCard
import app.writer.design.components.GlassDocumentCard
import app.writer.design.components.GlassEmptyState
import app.writer.design.components.GlassIconButton
import app.writer.design.components.GlassLoadingIndicator
import app.writer.design.components.GlassSearchBar
import app.writer.design.components.GlassTemplateCard
import app.writer.design.components.GlassText
import app.writer.design.components.GlassTextButton
import app.writer.design.components.GlassToastState
import app.writer.design.components.WriterLogo
import app.writer.design.components.glassPanel
import app.writer.design.rememberReducedMotion
import app.writer.design.topContentPadding
import app.writer.features.common.DocDialogs
import app.writer.features.common.LibraryViewModel
import app.writer.features.common.LocalPrefs
import app.writer.features.common.MainViewModel
import app.writer.features.common.categoryStyle
import app.writer.features.common.docStatusLabel
import app.writer.features.common.docSubtitle
import app.writer.features.common.documentMenu
import app.writer.model.DocStatus

/** Everything Home can do to the rest of the app. Kept as one object so the screen stays readable. */
class HomeActions(
    val onOpenProfile: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenTemplates: (filter: String) -> Unit,
    val onOpenTemplate: (id: String) -> Unit,
    val onNewBlank: () -> Unit,
    val onOpenDocument: (id: String) -> Unit,
    val onExportDocument: (id: String) -> Unit,
    val onSeeAllDocuments: () -> Unit,
)

private val homeCategories = listOf(
    "gov", "education", "employment", "banking",
    TemplateFilter.RATION, TemplateFilter.UTILITY, "personal", "complaints",
)

@Composable
fun HomeScreen(
    library: LibraryViewModel,
    mainVm: MainViewModel,
    toast: GlassToastState,
    dialogs: DocDialogs,
    actions: HomeActions,
) {
    val c = Glass.colors
    val language = LocalAppLanguage.current
    val prefs = LocalPrefs.current
    val templates by library.templates.collectAsStateWithLifecycle()
    val documents by library.documents.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val searching = query.isNotBlank()
    val results = if (searching) TemplateSearch.search(templates, query) else emptyList()

    val name = prefs.userName.trim()
    val greeting = if (name.isEmpty()) tr("नमस्ते!", "Hello!") else tr("नमस्ते, $name 👋", "Hello, $name 👋")
    val favoriteLabel = tr("पसंदीदा", "Favorite")
    val moreLabel = tr("और विकल्प", "More options")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = topContentPadding(),
            bottom = bottomBarContentPadding(),
            start = Spacing.screen,
            end = Spacing.screen,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5)) {
                WriterLogo(size = 48.dp)
                Column(modifier = Modifier.weight(1f)) {
                    GlassText(text = greeting, style = WriterType.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    GlassText(
                        text = tr("आपका पत्र, आपका दस्तावेज़ — सब एक जगह।", "Your letter, your document — all in one place."),
                        style = WriterType.caption,
                        color = c.textSecondary,
                    )
                }
                GlassIconButton(Icons.Rounded.Person, tr("प्रोफ़ाइल", "Profile"), actions.onOpenProfile, size = 44.dp)
                GlassIconButton(Icons.Rounded.Settings, tr("सेटिंग्स", "Settings"), actions.onOpenSettings, size = 44.dp)
            }
        }

        item(key = "search") {
            GlassSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = tr("किस प्रकार का पत्र लिखना है?", "What kind of letter do you want to write?"),
                clearLabel = tr("खोज साफ़ करें", "Clear search"),
                onSearch = { mainVm.addRecentSearch(query) },
            )
        }

        if (searching) {
            if (results.isEmpty()) {
                item(key = "no-results") {
                    GlassEmptyState(
                        icon = Icons.Rounded.Search,
                        title = tr("कोई टेम्पलेट नहीं मिला", "No template found"),
                        message = tr(
                            "दूसरे शब्द आज़माएं, या खाली दस्तावेज़ से शुरू करें।",
                            "Try different words, or start from a blank document.",
                        ),
                        actionText = tr("खाली दस्तावेज़ बनाएं", "Create a blank document"),
                        onAction = actions.onNewBlank,
                    )
                }
            } else {
                items(results, key = { "result-" + it.def.id }) { item ->
                    val def = item.def
                    GlassTemplateCard(
                        title = def.title[language],
                        description = def.description[language],
                        accent = categoryStyle(def.category).accent,
                        badges = listOf("हिंदी", "English"),
                        isFavorite = item.isFavorite,
                        favoriteLabel = favoriteLabel,
                        onToggleFavorite = { library.setTemplateFavorite(def.id, !item.isFavorite) },
                        onClick = {
                            mainVm.addRecentSearch(query)
                            actions.onOpenTemplate(def.id)
                        },
                    )
                }
            }
        } else {
            if (prefs.recentSearches.isNotEmpty()) {
                item(key = "recent-searches") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassText(text = tr("हाल की खोज", "Recent searches"), style = WriterType.caption, color = c.textSecondary)
                        prefs.recentSearches.forEach { term ->
                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .glassPanel(shape = WriterShapes.pill, onClick = { query = term })
                                    .padding(horizontal = Spacing.x1_5),
                                contentAlignment = Alignment.Center,
                            ) {
                                GlassText(text = term, style = WriterType.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            }

            item(key = "hero") { HeroCard(onStart = { actions.onOpenTemplates("gov") }) }

            item(key = "quick") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                    QuickAction(Icons.Rounded.Description, Accents.Blue, tr("नया आवेदन", "New application"), { actions.onOpenTemplates("gov") }, Modifier.weight(1f))
                    QuickAction(Icons.Rounded.ReportProblem, Accents.Orange, tr("शिकायत पत्र", "Complaint letter"), { actions.onOpenTemplates("complaints") }, Modifier.weight(1f))
                    QuickAction(Icons.Rounded.Verified, Accents.Green, tr("प्रमाण पत्र", "Certificate"), { actions.onOpenTemplates(TemplateFilter.CERTIFICATES) }, Modifier.weight(1f))
                    QuickAction(Icons.Rounded.EditNote, Accents.Purple, tr("खाली दस्तावेज़", "Blank document"), actions.onNewBlank, Modifier.weight(1f))
                }
            }

            item(key = "categories") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassText(text = tr("श्रेणियाँ", "Categories"), style = WriterType.headline, modifier = Modifier.weight(1f))
                        GlassTextButton(text = tr("सभी देखें", "See all"), onClick = { actions.onOpenTemplates(TemplateFilter.ALL) })
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5)) {
                        items(homeCategories, key = { it }) { id ->
                            val style = categoryStyle(id)
                            GlassCategoryCard(
                                icon = style.icon,
                                label = TemplateFilter.title(id, language),
                                accent = style.accent,
                                onClick = { actions.onOpenTemplates(id) },
                            )
                        }
                    }
                }
            }

            item(key = "recent-title") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassText(text = tr("हाल के दस्तावेज़", "Recent documents"), style = WriterType.headline, modifier = Modifier.weight(1f))
                    if (!documents.isNullOrEmpty()) {
                        GlassTextButton(text = tr("सभी देखें", "See all"), onClick = actions.onSeeAllDocuments)
                    }
                }
            }

            val docs = documents
            when {
                docs == null -> item(key = "recent-loading") {
                    Box(Modifier.fillMaxWidth().padding(Spacing.x3), contentAlignment = Alignment.Center) { GlassLoadingIndicator() }
                }
                docs.isEmpty() -> item(key = "recent-empty") {
                    GlassEmptyState(
                        icon = Icons.Rounded.Description,
                        title = tr("अभी कोई दस्तावेज़ नहीं है", "No documents yet"),
                        message = tr("आपके बनाए दस्तावेज़ यहाँ दिखेंगे।", "The documents you create will appear here."),
                        actionText = tr("पहला दस्तावेज़ बनाएं", "Create your first document"),
                        onAction = actions.onNewBlank,
                    )
                }
                else -> items(docs.take(prefs.recentDocsCount), key = { "doc-" + it.id }) { doc ->
                    GlassDocumentCard(
                        title = doc.title,
                        subtitle = docSubtitle(doc),
                        statusLabel = docStatusLabel(doc.status),
                        isDraft = doc.status == DocStatus.DRAFT,
                        isPdf = false,
                        isFavorite = doc.isFavorite,
                        menuLabel = moreLabel,
                        menuActions = documentMenu(doc, library, dialogs, toast, actions.onOpenDocument, actions.onExportDocument),
                        onClick = { actions.onOpenDocument(doc.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    accent: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .glassPanel(shape = WriterShapes.cardSmall, onClick = onClick)
            .padding(horizontal = Spacing.half, vertical = Spacing.x1_5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        AccentIconTile(icon = icon, accent = accent, tileSize = 44.dp)
        GlassText(
            text = label,
            style = WriterType.caption,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(36.dp),
        )
    }
}

@Composable
private fun HeroCard(onStart: () -> Unit) {
    val c = Glass.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(
                shape = WriterShapes.cardLarge,
                elevated = true,
                fillBrush = Brush.linearGradient(
                    listOf(c.primary.copy(alpha = if (c.isDark) 0.42f else 0.30f), c.purple.copy(alpha = if (c.isDark) 0.36f else 0.24f)),
                ),
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            GlassText(text = tr("आवेदन लिखना अब आसान", "Writing applications is now easy"), style = WriterType.headline)
            GlassText(
                text = tr(
                    "सही फॉर्मेट में अपना आवेदन तैयार करें और PDF में सेव करें।",
                    "Prepare your application in the right format and save it as a PDF.",
                ),
                style = WriterType.bodySmall,
                color = c.textSecondary,
            )
            GlassButton(
                text = tr("शुरू करें", "Get started"),
                onClick = onStart,
                modifier = Modifier
                    .padding(top = Spacing.half)
                    .width(150.dp),
            )
        }
        FloatingPaper()
    }
}

/** A small tilted sheet that drifts up and down very slowly. Static when reduced motion is on. */
@Composable
private fun FloatingPaper() {
    val c = Glass.colors
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "paperFloat")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 6f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "paperDrift",
    )
    Box(
        modifier = Modifier
            .offset(y = drift.dp)
            .rotate(7f)
            .size(width = 76.dp, height = 96.dp)
            .clip(WriterShapes.chip)
            .background(Color.White.copy(alpha = if (c.isDark) 0.16f else 0.55f))
            .padding(Spacing.x1_5),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.width(34.dp).height(6.dp).clip(WriterShapes.pill).background(c.electric.copy(alpha = 0.9f)))
            listOf(1f, 0.9f, 1f, 0.7f, 0.85f).forEach { fraction ->
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(WriterShapes.pill)
                        .background(c.textMuted.copy(alpha = 0.55f)),
                )
            }
        }
    }
}
