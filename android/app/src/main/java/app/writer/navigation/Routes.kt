package app.writer.navigation

/** Every destination in the app. Tab routes show the floating bottom bar. */
object Routes {
    const val HOME = "home"
    const val TEMPLATES = "templates"
    const val FILES = "files"
    const val PROFILE = "profile"

    const val ONBOARDING = "onboarding"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"

    /** Name of the single path argument used by the routes below (a template id or a document id). */
    const val ARG_ID = "id"
    const val TEMPLATE_DETAIL = "template/{$ARG_ID}"
    const val EDITOR = "editor/{$ARG_ID}"
    const val PREVIEW = "preview/{$ARG_ID}"

    fun templateDetail(id: String) = "template/$id"
    fun editor(id: String) = "editor/$id"
    fun preview(id: String) = "preview/$id"

    val tabs = setOf(HOME, TEMPLATES, FILES, PROFILE)
}
