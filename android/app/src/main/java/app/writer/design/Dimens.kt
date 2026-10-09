package app.writer.design

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** The 8 dp spacing system. */
object Spacing {
    val half = 4.dp
    val x1 = 8.dp
    val x1_5 = 12.dp
    val x2 = 16.dp
    val x3 = 24.dp
    val x4 = 32.dp
    val x5 = 40.dp

    /** Horizontal gutter used by every screen. */
    val screen = 16.dp
}

/** Corner radii: cards 20–28 dp, buttons 16–22 dp. */
object WriterShapes {
    val card = RoundedCornerShape(24.dp)
    val cardLarge = RoundedCornerShape(28.dp)
    val cardSmall = RoundedCornerShape(20.dp)
    val button = RoundedCornerShape(18.dp)
    val field = RoundedCornerShape(18.dp)
    val chip = RoundedCornerShape(16.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
    val navBar = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(percent = 50)
    val circle = CircleShape
}
