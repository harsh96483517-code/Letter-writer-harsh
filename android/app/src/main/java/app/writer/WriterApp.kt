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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.writer.core.LocalAppLanguage
import app.writer.core.tr
import app.writer.design.AppBackground
import app.writer.design.Spacing
import app.writer.design.WriterTheme
import app.writer.design.components.BottomBarHeight
import app.writer.design.components.BottomNavItem
import app.writer.design.components.GlassBottomNavigation
import app.writer.design.components.GlassToastHost
import app.writer.design.components.rememberGlassToastState
import app.writer.design.resolveDarkTheme
import app.writer.features.create.CreateSheet
import app.writer.features.shell.HomeShowcase
import app.writer.features.shell.TabPlaceholder
import app.writer.model.AppLanguage
import app.writer.model.ThemeMode
import app.writer.navigation.Routes

@Composable
fun WriterApp() {
    // Step 1 keeps these in saved instance state. Step 2 moves them to DataStore.
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
    var language by rememberSaveable { mutableStateOf(AppLanguage.HI) }

    WriterTheme(darkTheme = resolveDarkTheme(themeMode)) {
        CompositionLocalProvider(LocalAppLanguage provides language) {
            AppBackground {
                MainShell(
                    themeMode = themeMode,
                    onThemeChange = { themeMode = it },
                    language = language,
                    onLanguageChange = { language = it },
                )
            }
        }
    }
}

@Composable
private fun MainShell(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    val nav = rememberNavController()
    val toast = rememberGlassToastState()
    var showCreate by rememberSaveable { mutableStateOf(false) }

    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showBottomBar = route in Routes.tabs

    val navItems = listOf(
        BottomNavItem(Routes.HOME, Icons.Outlined.Home, Icons.Rounded.Home, tr("होम", "Home")),
        BottomNavItem(Routes.TEMPLATES, Icons.Outlined.Description, Icons.Rounded.Description, tr("टेम्पलेट्स", "Templates")),
        BottomNavItem(Routes.FILES, Icons.Outlined.FolderOpen, Icons.Rounded.FolderOpen, tr("मेरी फाइलें", "My Files")),
        BottomNavItem(Routes.PROFILE, Icons.Outlined.Person, Icons.Rounded.Person, tr("प्रोफ़ाइल", "Profile")),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            enterTransition = { fadeIn(tween(250)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            composable(Routes.HOME) {
                HomeShowcase(
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    language = language,
                    onLanguageChange = onLanguageChange,
                    onMessage = { toast.show(it) },
                )
            }
            composable(Routes.TEMPLATES) { TabPlaceholder(tr("टेम्पलेट्स", "Templates")) }
            composable(Routes.FILES) { TabPlaceholder(tr("मेरी फाइलें", "My Files")) }
            composable(Routes.PROFILE) { TabPlaceholder(tr("प्रोफ़ाइल", "Profile")) }
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
                .padding(bottom = BottomBarHeight + Spacing.x2),
        )
    }

    if (showCreate) {
        val comingSoon = tr("यह विकल्प अगले चरण में जुड़ेगा", "This option arrives in the next step")
        CreateSheet(
            onDismiss = { showCreate = false },
            onPick = {
                showCreate = false
                toast.show(comingSoon)
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
