package app.writer.navigation

/** Every destination in the app. Tab routes show the floating bottom bar. */
object Routes {
    const val HOME = "home"
    const val TEMPLATES = "templates"
    const val FILES = "files"
    const val PROFILE = "profile"

    val tabs = setOf(HOME, TEMPLATES, FILES, PROFILE)
}
