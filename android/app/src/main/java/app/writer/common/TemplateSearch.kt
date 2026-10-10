package app.writer.common

import app.writer.model.AppLanguage
import app.writer.model.Categories
import app.writer.model.TemplateDef
import app.writer.model.TemplateItem
import java.text.Normalizer
import java.util.Locale

/**
 * Template search across Hindi and English titles, descriptions, category names and keywords
 * (including romanised Hindi such as "bijli" or "praman patra"). Every word the user types has to
 * match somewhere, so "bijli shikayat" finds the electricity complaint.
 */
object TemplateSearch {

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFC)
            .replace("‍", "")
            .replace("‌", "")
            .lowercase(Locale.ROOT)
            .trim()

    fun tokens(query: String): List<String> =
        normalize(query).split(Regex("\\s+")).filter { it.isNotEmpty() }

    fun search(items: List<TemplateItem>, query: String): List<TemplateItem> {
        val words = tokens(query)
        if (words.isEmpty()) return items
        return items
            .mapNotNull { item -> score(item.def, words)?.let { item to it } }
            .sortedWith(compareByDescending<Pair<TemplateItem, Int>> { it.second }.thenBy { it.first.def.title.hi })
            .map { it.first }
    }

    /** Returns null when some word matches nothing at all. */
    internal fun score(def: TemplateDef, words: List<String>): Int? {
        val titles = listOf(normalize(def.title.hi), normalize(def.title.en))
        val descriptions = listOf(normalize(def.description.hi), normalize(def.description.en))
        val keywords = def.keywords.map(::normalize)
        val category = Categories.byId(def.category)
        val categoryNames = listOfNotNull(category?.titleHi, category?.titleEn).map(::normalize)

        var total = 0
        for (word in words) {
            var best = 0
            if (titles.any { it.startsWith(word) }) best = maxOf(best, 100)
            if (titles.any { word in it }) best = maxOf(best, 60)
            if (keywords.any { it == word }) best = maxOf(best, 50)
            if (keywords.any { it.startsWith(word) }) best = maxOf(best, 40)
            if (keywords.any { word in it }) best = maxOf(best, 30)
            if (categoryNames.any { word in it }) best = maxOf(best, 20)
            if (descriptions.any { word in it }) best = maxOf(best, 10)
            if (best == 0) return null
            total += best
        }
        return total
    }
}

/** The filter chips of the template library, plus the smart collections. */
object TemplateFilter {
    const val ALL = "all"
    const val FAVORITES = "favorites"
    const val RECENT = "recent"
    const val POPULAR = "popular"
    const val NEW = "new"
    const val CERTIFICATES = "certificates"
    const val RATION = "g_ration"
    const val UTILITY = "g_utility"

    /** Hand-picked starting order for "popular"; real usage counts take over as people use the app. */
    val popularOrder = listOf(
        "gov_dm", "sch_ration_correction", "cmp_electricity", "edu_leave", "job_application",
        "edu_scholarship", "sch_income_cert", "gov_tehsildar", "job_resignation", "bank_account_correction",
    )

    /** Templates added most recently. Updated by hand with each release. */
    val newest = listOf(
        "sch_pm_kisan", "sch_pension", "bank_loan", "job_cover_letter", "cmp_consumer", "edu_fee_concession",
    )

    fun apply(filter: String, items: List<TemplateItem>): List<TemplateItem> = when (filter) {
        ALL -> items
        FAVORITES -> items.filter { it.isFavorite }
        RECENT -> items.filter { it.lastUsedAt > 0 }.sortedByDescending { it.lastUsedAt }
        POPULAR -> items
            .filter { it.useCount > 0 || it.def.id in popularOrder }
            .sortedWith(
                compareByDescending<TemplateItem> { it.useCount }
                    .thenBy { popularOrder.indexOf(it.def.id).let { i -> if (i < 0) Int.MAX_VALUE else i } },
            )
        NEW -> newest.mapNotNull { id -> items.firstOrNull { it.def.id == id } }
        CERTIFICATES -> items.filter {
            val t = TemplateSearch.normalize(it.def.title.hi + " " + it.def.title.en)
            "प्रमाण" in t || "certificate" in t
        }
        RATION -> items.filter { it.def.id.startsWith("sch_ration") }
        UTILITY -> items.filter { it.def.id == "cmp_electricity" || it.def.id == "cmp_water" }
        else -> items.filter { it.def.category == filter }
    }

    fun title(filter: String, language: AppLanguage): String {
        val hi = language == AppLanguage.HI
        return when (filter) {
            ALL -> if (hi) "सभी" else "All"
            FAVORITES -> if (hi) "पसंदीदा" else "Favorites"
            RECENT -> if (hi) "हाल के" else "Recent"
            POPULAR -> if (hi) "लोकप्रिय" else "Popular"
            NEW -> if (hi) "नए" else "New"
            CERTIFICATES -> if (hi) "प्रमाण पत्र" else "Certificates"
            RATION -> if (hi) "राशन कार्ड" else "Ration card"
            UTILITY -> if (hi) "बिजली / पानी" else "Electricity / Water"
            else -> Categories.byId(filter)?.title(language) ?: filter
        }
    }
}
