package app.writer.storage

import android.content.res.AssetManager
import app.writer.database.TemplateStateDao
import app.writer.model.Categories
import app.writer.model.TemplateDef
import app.writer.model.TemplateItem
import app.writer.model.TemplateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Built-in templates (offline, from assets) plus the user's favorites and usage counters. */
class TemplateRepository(
    private val assets: AssetManager,
    private val stateDao: TemplateStateDao,
) {
    private val mutex = Mutex()
    private var cache: List<TemplateDef>? = null

    suspend fun all(): List<TemplateDef> = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) { load() }.also { cache = it }
    }

    suspend fun find(id: String): TemplateDef? =
        if (id == TemplateDef.BLANK_ID) TemplateDef.Blank else all().firstOrNull { it.id == id }

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: Flow<List<TemplateItem>> = flow { emit(all()) }.flatMapLatest { defs ->
        stateDao.observeAll().map { states ->
            val byId = states.associateBy { it.templateId }
            defs.map { TemplateItem(it, byId[it.id]) }
        }
    }

    suspend fun setFavorite(id: String, favorite: Boolean) {
        val current = stateDao.get(id) ?: TemplateState(id)
        stateDao.upsert(current.copy(isFavorite = favorite))
    }

    suspend fun markUsed(id: String, now: Long) {
        val current = stateDao.get(id) ?: TemplateState(id)
        stateDao.upsert(current.copy(lastUsedAt = now, useCount = current.useCount + 1))
    }

    suspend fun clearUsage() = stateDao.deleteAll()

    private fun load(): List<TemplateDef> {
        val files = assets.list(DIR).orEmpty().filter { it.endsWith(".json") }.sorted()
        val parsed = files.flatMap { name ->
            val text = assets.open("$DIR/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
            TemplateParser.parse(text)
        }
        val order = Categories.all.map { it.id }
        // Category order first, then the order written in the files.
        return parsed.withIndex()
            .sortedWith(compareBy({ order.indexOf(it.value.category).let { i -> if (i < 0) Int.MAX_VALUE else i } }, { it.index }))
            .map { it.value }
    }

    private companion object {
        const val DIR = "templates"
    }
}
