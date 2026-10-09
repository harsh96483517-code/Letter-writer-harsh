package app.writer.features.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.writer.core.tr
import app.writer.design.Accents
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.components.AccentIconTile
import app.writer.design.components.GlassBottomSheet
import app.writer.design.components.GlassText
import app.writer.design.components.glassPanel

/** The five ways to start a document from the center Create button. */
enum class CreateOption { Blank, Application, Complaint, Personal, ContinueDraft }

private data class CreateRow(
    val option: CreateOption,
    val icon: ImageVector,
    val accent: Color,
    val titleHi: String,
    val titleEn: String,
    val descHi: String,
    val descEn: String,
)

private val rows = listOf(
    CreateRow(CreateOption.Blank, Icons.Rounded.EditNote, Accents.Blue, "खाली दस्तावेज़", "Blank Document", "शुरू से अपना पत्र लिखें", "Write your own letter from scratch"),
    CreateRow(CreateOption.Application, Icons.Rounded.Description, Accents.Purple, "आवेदन टेम्पलेट", "Application Template", "तैयार आवेदन चुनें और बदलें", "Pick a ready application and edit it"),
    CreateRow(CreateOption.Complaint, Icons.Rounded.ReportProblem, Accents.Orange, "शिकायत पत्र", "Complaint Letter", "बिजली, पानी, सड़क आदि की शिकायत", "Electricity, water, road and more"),
    CreateRow(CreateOption.Personal, Icons.Rounded.Mail, Accents.Pink, "व्यक्तिगत पत्र", "Personal Letter", "धन्यवाद, निमंत्रण, अनुरोध आदि", "Thank-you, invitation, request and more"),
    CreateRow(CreateOption.ContinueDraft, Icons.Rounded.History, Accents.Teal, "अधूरा ड्राफ्ट जारी रखें", "Continue Recent Draft", "पिछला ड्राफ्ट खोलें", "Open the draft you left unfinished"),
)

/** Create flow bottom sheet (spec §14). */
@Composable
fun CreateSheet(
    onDismiss: () -> Unit,
    onPick: (CreateOption) -> Unit,
) {
    val c = Glass.colors
    GlassBottomSheet(
        onDismiss = onDismiss,
        title = tr("नया दस्तावेज़ बनाएं", "Create a document"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassPanel(shape = WriterShapes.cardSmall, onClick = { onPick(row.option) })
                        .padding(Spacing.x1_5),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x1_5),
                ) {
                    AccentIconTile(icon = row.icon, accent = row.accent, tileSize = 44.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        GlassText(text = tr(row.titleHi, row.titleEn), style = WriterType.bodyStrong)
                        GlassText(text = tr(row.descHi, row.descEn), style = WriterType.bodySmall, color = c.textSecondary)
                    }
                }
            }
        }
    }
}
