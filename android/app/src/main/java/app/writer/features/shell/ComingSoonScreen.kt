package app.writer.features.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.writer.core.tr
import app.writer.design.components.GlassEmptyState
import app.writer.design.components.GlassTopBar

/** Stand-in for a screen that is built in a later step. The document it belongs to is already saved. */
@Composable
fun ComingSoonScreen(title: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(title = title, onBack = onBack, backLabel = tr("वापस", "Back"))
        GlassEmptyState(
            icon = Icons.Rounded.Construction,
            title = tr("यह स्क्रीन अगले चरण में बनेगी", "This screen is built in the next step"),
            message = tr("आपका दस्तावेज़ सेव हो चुका है और “मेरी फाइलें” में मिलेगा।", "Your document is already saved and is in My Files."),
            actionText = tr("वापस जाएं", "Go back"),
            onAction = onBack,
        )
    }
}
