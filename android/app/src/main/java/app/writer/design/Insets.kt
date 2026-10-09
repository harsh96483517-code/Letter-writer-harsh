package app.writer.design

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import app.writer.design.components.BottomBarHeight

/** Bottom padding that keeps a scrolling list clear of the floating bar and gesture area. */
@Composable
fun bottomBarContentPadding(): Dp =
    BottomBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Spacing.x3

/** Top padding that clears the status bar / display cutout, plus one gutter. */
@Composable
fun topContentPadding(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Spacing.x2
