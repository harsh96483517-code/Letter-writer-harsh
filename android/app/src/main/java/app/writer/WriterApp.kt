package app.writer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.writer.common.TemplateFilter
import app.writer.core.LocalAppLanguage
import app.writer.core.tr
import app.writer.design.AppBackground
import app.writer.design.ProvideHaptics
import app.writer.design.Spacing
import app.writer.design.WriterTheme
import app.writer.design.components.BottomBarHeight
import app.writer.design.components.BottomNavItem
import app.writer.design.components.GlassBottomNavigation
import app.writer.design.components.GlassToastHost
import app.writer.design.components.ToastType
import app.writer.design.components.rememberGlassToastState
import app.writer.design.resolveDarkTheme
import app.writer.features.common.DocDialogHost
import app.writer.features.common.LibraryViewModel
import app.writer.features.common.LocalPrefs
import app.writer.features.common.MainViewModel
import app.writer.features.common.appViewModel
import app.writer.features.common.rememberDocDialogs
import app.writer.features.create.CreateOption
import app.writer.features.create.CreateSheet
import app.writer.features.export.DocActionsHost
import app.writer.features.export.rememberDocActions
import app.writer.features.files.FilesScreen
import app.writer.features.home.HomeActions
import app.writer.features.home.HomeScreen
import app.writer.features.launch.LaunchScreen
import app.writer.features.launch.OnboardingScreen
import app.writer.features.profile.AboutScreen
import app.writer.features.profile.HelpScreen
import app.writer.features.profile.PrivacyScreen
import app.writer.features.profile.ProfileScreen
import app.writer.features.profile.SettingsScreen
import app.writer.features.shell.ComingSoonScreen
import app.writer.features.templates.TemplateDetailScreen
import app.writer.features.templates.TemplatesScreen
import app.writer.model.AppLanguage
import app.writer.model.TemplateDef
import app.writer.model.ThemeMode
import app.writer.model.UserPreferences
import app.writer.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun WriterApp() {
    val mainVm = appViewModel { MainViewModel(it) }
    val library = appViewModel { LibraryViewModel(it) }
    val prefs by mainVm.prefs.collectAsStateWithLifecycle()
    var splashDone by rememberSaveable { mutableStateOf(false) }

    val loaded = prefs
    WriterTheme(darkTheme = resolveDarkTheme(loaded?.theme ?: ThemeMode.DARK)) {
        AppBackground {
            if (loaded == null || !splashDone) {
                CompositionLocalProvider(LocalAppLanguage provides (loaded?.language ?: AppLanguage.HI)) {
                    LaunchScreen(ready = loaded != null, onFinished = { splashDone = true })
                }
            } else {
                CompositionLocalProvider(
                    LocalAppLanguage provides loaded.language,
                    LocalPrefs provides loaded,
                ) {
                    ProvideHaptics(enabled = loaded.haptics) {
                        MainShell(prefs = loaded, mainVm = mainVm, library = library)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainShell(
    prefs: UserPreferences,
    mainVm: MainViewModel,
    library: LibraryViewModel,
) {
    val nav = rememberNavController()
    val toast = rememberGlassToastState()
    val scope = rememberCoroutineScope()
    val dialogs = rememberDocDialogs()
    val docActions = rememberDocActions(toast)
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var templatesFilter by rememberSaveable { mutableStateOf(TemplateFilter.ALL) }
    val startDestination = remember { if (prefs.onboardingDone) Routes.HOME else Routes.ONBOARDING }

    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showBottomBar = route in Routes.tabs

    val createFailed = tr("दस्तावेज़ नहीं बन सका। दोबारा कोशिश करें।", "Could not create the document. Please try again.")
    val noDraft = tr("कोई अधूरा ड्राफ्ट नहीं है।", "There is no unfinished draft.")

    val openEditor: (String) -> Unit = { id -> nav.navigate(Routes.editor(id)) }
    val openPreview: (String) -> Unit = { id -> nav.navigate(Routes.preview(id)) }
    val openTemplates: (String) -> Unit = { filter ->
        templatesFilter = filter
        nav.navigateTab(Routes.TEMPLATES)
    }
    val newBlank: () -> Unit = {
        scope.launch {
            val id = library.start(TemplateDef.BLANK_ID)
            if (id != null) openEditor(id) else toast.show(createFailed, ToastType.Error)
        }
    }

    val navItems = listOf(
        BottomNavItem(Routes.HOME, Icons.Outlined.Home, Icons.Rounded.Home, tr("होम", "Home")),
        BottomNavItem(Routes.TEMPLATES, Icons.Outlined.Description, Icons.Rounded.Description, tr("टेम्पलेट्स", "Templates")),
        BottomNavItem(Routes.FILES, Icons.Outlined.FolderOpen, Icons.Rounded.FolderOpen, tr("मेरी फाइलें", "My Files")),
        BottomNavItem(Routes.PROFILE, Icons.Outlined.Person, Icons.Rounded.Person, tr("प्रोफ़ाइल", "Profile")),
    )
    val back: () -> Unit = { nav.popBackStack() }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = startDestination,
            enterTransition = { fadeIn(tween(250)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = {
                        mainVm.update { it.copy(onboardingDone = true) }
                        nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    library = library,
                    mainVm = mainVm,
                    toast = toast,
                    dialogs = dialogs,
                    actions = HomeActions(
                        onOpenProfile = { nav.navigateTab(Routes.PROFILE) },
                        onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                        onOpenTemplates = openTemplates,
                        onOpenTemplate = { nav.navigate(Routes.templateDetail(it)) },
                        onNewBlank = newBlank,
                        onOpenDocument = openEditor,
                        onExportDocument = docActions::exportPdf,
                        onSeeAllDocuments = { nav.navigateTab(Routes.FILES) },
                    ),
                )
            }
            composable(Routes.TEMPLATES) {
                TemplatesScreen(
                    library = library,
                    mainVm = mainVm,
                    filter = templatesFilter,
                    onFilterChange = { templatesFilter = it },
                    onOpenTemplate = { nav.navigate(Routes.templateDetail(it)) },
                    onNewBlank = newBlank,
                )
            }
            composable(Routes.FILES) {
                FilesScreen(
                    library = library,
                    toast = toast,
                    dialogs = dialogs,
                    docActions = docActions,
                    onOpenDocument = openEditor,
                    onPreviewDocument = openPreview,
                    onNewBlank = newBlank,
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    library = library,
                    mainVm = mainVm,
                    toast = toast,
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenHelp = { nav.navigate(Routes.HELP) },
                    onOpenPrivacy = { nav.navigate(Routes.PRIVACY) },
                    onOpenAbout = { nav.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen(mainVm = mainVm, library = library, toast = toast, onBack = back) }
            composable(Routes.HELP) { HelpScreen(onBack = back) }
            composable(Routes.PRIVACY) { PrivacyScreen(onBack = back) }
            composable(Routes.ABOUT) { AboutScreen(onBack = back) }

            composable(
                route = Routes.TEMPLATE_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
            ) { entry ->
                TemplateDetailScreen(
                    templateId = entry.arguments?.getString(Routes.ARG_ID).orEmpty(),
                    library = library,
                    toast = toast,
                    onBack = back,
                    onStarted = { id ->
                        // Replace the details screen so Back from the editor returns to the template list.
                        nav.navigate(Routes.editor(id)) { popUpTo(Routes.TEMPLATE_DETAIL) { inclusive = true } }
                    },
                )
            }
            composable(
                route = Routes.EDITOR,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
            ) {
                ComingSoonScreen(title = tr("एडिटर", "Editor"), onBack = back)
            }
            composable(
                route = Routes.PREVIEW,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
            ) {
                ComingSoonScreen(title = tr("प्रीव्यू", "Preview"), onBack = back)
            }
        }

        AnimatedVisibility(
            visible = showBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 2 },
        ) {
            GlassBottomNavigation(
                items = navItems,
                selectedRoute = route,
                createLabel = tr("नया दस्तावेज़ बनाएं", "Create a document"),
                onSelect = { nav.navigateTab(it) },
                onCreate = { showCreate = true },
            )
        }

        GlassToastHost(
            state = toast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (showBottomBar) BottomBarHeight + Spacing.x2 else Spacing.x2),
        )
    }

    DocDialogHost(dialogs = dialogs, library = library, toast = toast)
    DocActionsHost(actions = docActions)

    if (showCreate) {
        CreateSheet(
            onDismiss = { showCreate = false },
            onPick = { option ->
                showCreate = false
                when (option) {
                    CreateOption.Blank -> newBlank()
                    CreateOption.Application -> openTemplates("gov")
                    CreateOption.Complaint -> openTemplates("complaints")
                    CreateOption.Personal -> openTemplates("personal")
                    CreateOption.ContinueDraft -> scope.launch {
                        val id = library.latestDraftId()
                        if (id != null) openEditor(id) else toast.show(noDraft, ToastType.Info)
                    }
                }
            },
        )
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
