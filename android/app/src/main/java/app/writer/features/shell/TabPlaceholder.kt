package app.writer.features.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.writer.core.tr
import app.writer.design.Spacing
import app.writer.design.WriterType
import app.writer.design.bottomBarContentPadding
import app.writer.design.components.GlassEmptyState
import app.writer.design.components.GlassText
import app.writer.design.topContentPadding

/** Stand-in for a tab whose real screen is built in a later step. */
@Composable
fun TabPlaceholder(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topContentPadding(), bottom = bottomBarContentPadding())
            .padding(horizontal = Spacing.screen),
    ) {
        GlassText(text = title, style = WriterType.title)
        Column(
            modifier = Modifier.weight(1f).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            GlassEmptyState(
                icon = Icons.Rounded.Construction,
                title = tr("यह स्क्रीन अगले चरण में बनेगी", "This screen is built in the next step"),
                message = null,
            )
        }
    }
}
