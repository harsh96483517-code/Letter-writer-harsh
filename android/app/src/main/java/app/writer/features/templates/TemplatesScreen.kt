package app.writer.features.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.writer.common.TemplateFilter
import app.writer.common.TemplateSearch
import app.writer.core.LocalAppLanguage
import app.writer.core.tr
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterType
import app.writer.design.bottomBarContentPadding
import app.writer.design.components.AccentIconTile
import app.writer.design.components.GlassEmptyState
import app.writer.design.components.GlassSearchBar
import app.writer.design.components.GlassTabBar
import app.writer.design.components.GlassTemplateCard
import app.writer.design.components.GlassText
import app.writer.design.topContentPadding
import app.writer.features.common.LibraryViewModel
import app.writer.features.common.MainViewModel
import app.writer.features.common.categoryStyle
import app.writer.model.Categories
import app.writer.model.TemplateItem

private val baseFilters = listOf(
    TemplateFilter.ALL, TemplateFilter.RECENT, TemplateFilter.FAVORITES, TemplateFilter.POPULAR, TemplateFilter.NEW,
) + Categories.all.map { it.id }

@Composable
fun TemplatesScreen(
    library: LibraryViewModel,
    mainVm: MainViewModel,
    filter: String,
    onFilterChange: (String) -> Unit,
    onOpenTemplate: (String) -> Unit,
    onNewBlank: () -> Unit,
) {
    val language = LocalAppLanguage.current
    val templates by library.templates.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    // A shortcut filter from Home (ration card, certificates …) gets its own chip right after "All".
    val filters = if (filter in baseFilters) baseFilters else listOf(baseFilters.first(), filter) + baseFilters.drop(1)
    val labels = filters.map { TemplateFilter.title(it, language) }

    val shown = TemplateSearch.search(TemplateFilter.apply(filter, templates), query)
    val grouped = filter == TemplateFilter.ALL && query.isBlank()
    val favoriteLabel = tr("पसंदीदा", "Favorite")

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(top = topContentPadding())
                .padding(horizontal = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
        ) {
            GlassText(text = tr("टेम्पलेट्स", "Templates"), style = WriterType.title)
            GlassSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = tr("टेम्पलेट खोजें", "Search templates"),
                clearLabel = tr("खोज साफ़ करें", "Clear search"),
                onSearch = { mainVm.addRecentSearch(query) },
            )
        }
        GlassTabBar(
            tabs = labels,
            selectedIndex = filters.indexOf(filter),
            onSelect = { onFilterChange(filters[it]) },
            modifier = Modifier.padding(vertical = Spacing.x1_5),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                bottom = bottomBarContentPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
        ) {
            if (shown.isEmpty()) {
                item(key = "empty") {
                    EmptyForFilter(filter = filter, searching = query.isNotBlank(), loading = templates.isEmpty(), onNewBlank = onNewBlank)
                }
            } else if (grouped) {
                val groups = shown.groupBy { it.def.category }
                groups.forEach { (categoryId, group) ->
                    item(key = "header-$categoryId") {
                        CategoryHeader(categoryId = categoryId, count = group.size)
                    }
                    items(group, key = { it.def.id }) { item ->
                        TemplateRow(item, favoriteLabel, library, onOpenTemplate)
                    }
                }
            } else {
                item(key = "count") {
                    GlassText(
                        text = tr("${shown.size} टेम्पलेट", "${shown.size} templates"),
                        style = WriterType.caption,
                        color = Glass.colors.textSecondary,
                    )
                }
                items(shown, key = { it.def.id }) { item ->
                    TemplateRow(item, favoriteLabel, library, onOpenTemplate)
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(
    item: TemplateItem,
    favoriteLabel: String,
    library: LibraryViewModel,
    onOpen: (String) -> Unit,
) {
    val language = LocalAppLanguage.current
    val def = item.def
    GlassTemplateCard(
        title = def.title[language],
        description = def.description[language],
        accent = categoryStyle(def.category).accent,
        badges = listOf("हिंदी", "English"),
        isFavorite = item.isFavorite,
        favoriteLabel = favoriteLabel,
        onToggleFavorite = { library.setTemplateFavorite(def.id, !item.isFavorite) },
        onClick = { onOpen(def.id) },
    )
}

@Composable
private fun CategoryHeader(categoryId: String, count: Int) {
    val language = LocalAppLanguage.current
    val style = categoryStyle(categoryId)
    val title = Categories.byId(categoryId)?.title(language) ?: categoryId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.x1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5),
    ) {
        AccentIconTile(icon = style.icon, accent = style.accent, tileSize = 36.dp)
        GlassText(text = title, style = WriterType.headline, modifier = Modifier.weight(1f))
        GlassText(text = count.toString(), style = WriterType.label, color = Glass.colors.textSecondary)
    }
}

@Composable
private fun EmptyForFilter(filter: String, searching: Boolean, loading: Boolean, onNewBlank: () -> Unit) {
    when {
        loading -> Unit
        searching -> GlassEmptyState(
            icon = Icons.Rounded.Search,
            title = tr("कोई टेम्पलेट नहीं मिला", "No template found"),
            message = tr("दूसरे शब्द आज़माएं, या खाली दस्तावेज़ से शुरू करें।", "Try different words, or start from a blank document."),
            actionText = tr("खाली दस्तावेज़ बनाएं", "Create a blank document"),
            onAction = onNewBlank,
        )
        filter == TemplateFilter.FAVORITES -> GlassEmptyState(
            icon = Icons.Rounded.StarBorder,
            title = tr("अभी कोई पसंदीदा टेम्पलेट नहीं", "No favorite templates yet"),
            message = tr("किसी टेम्पलेट के तारे (★) को दबाकर उसे यहाँ जोड़ें।", "Tap the star (★) on a template to keep it here."),
        )
        filter == TemplateFilter.RECENT -> GlassEmptyState(
            icon = Icons.Rounded.History,
            title = tr("अभी कोई टेम्पलेट इस्तेमाल नहीं हुआ", "No templates used yet"),
            message = tr("जो टेम्पलेट आप इस्तेमाल करेंगे, वे यहाँ दिखेंगे।", "Templates you use will show up here."),
        )
        else -> GlassEmptyState(
            icon = Icons.Rounded.Description,
            title = tr("इस श्रेणी में कुछ नहीं मिला", "Nothing in this category"),
            message = null,
        )
    }
}
