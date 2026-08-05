package com.corverxis.nexgendriver.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NexgenColorScheme = darkColorScheme(
    background = NexgenBackground,
    surface = NexgenSurface,
    primary = NexgenAccent,
    onPrimary = NexgenAccentText,
    onBackground = NexgenText,
    onSurface = NexgenText
)

@Composable
fun NexgenDriverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NexgenColorScheme,
        content = content
    )
}
