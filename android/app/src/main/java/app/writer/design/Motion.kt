package app.writer.design

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/** Whether the user's Settings → "Remove animations" (animator scale 0) is on. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/** Duration helper: 0 ms when reduced motion is on. Keep durations in the 150–350 ms range. */
fun motionMs(reduced: Boolean, ms: Int): Int = if (reduced) 0 else ms

/** Set from the "Haptic feedback" setting. */
val LocalHapticsEnabled = compositionLocalOf { true }

/** Returns a function that plays a light tick, if the user allows haptics. */
@Composable
fun rememberHaptic(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(haptics, enabled) {
        {
            if (enabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

@Composable
fun ProvideHaptics(enabled: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHapticsEnabled provides enabled, content = content)
}
