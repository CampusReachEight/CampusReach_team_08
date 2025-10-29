package com.android.sample.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag

@Composable
fun PaletteDebugView() {
    val p = appPalette()

    Column {
        Text(
            text = String.format("#%08X", p.primary.toArgb()),
            modifier = Modifier.testTag(PaletteTestTags.PALETTE_PRIMARY)
        )
        Text(
            text = String.format("#%08X", p.secondary.toArgb()),
            modifier = Modifier.testTag(PaletteTestTags.PALETTE_SECONDARY)
        )
        Text(
            text = String.format("#%08X", p.accent.toArgb()),
            modifier = Modifier.testTag(PaletteTestTags.PALETTE_ACCENT)
        )
        Text(
            text = String.format("#%08X", p.text.toArgb()),
            modifier = Modifier.testTag(PaletteTestTags.PALETTE_TEXT)
        )
        Text(
            text = String.format("#%08X", p.background.toArgb()),
            modifier = Modifier.testTag(PaletteTestTags.PALETTE_BACKGROUND)
        )
        Text(
            text = String.format("#%08X", p.surface.toArgb()),
            modifier = Modifier.testTag(PaletteTestTags.PALETTE_SURFACE)
        )
        Text(
            text = String.format("#%08X", p.error.toArgb()),
            modifier = Modifier.testTag(PaletteTestTags.PALETTE_ERROR)
        )

        Text(
            text = String.format("#%08X", p.onPrimary.toArgb()),
            modifier = Modifier.testTag(PaletteTestTags.PALETTE_ON_PRIMARY)
        )
        Text(
            text = String.format("#%08X", p.onBackground.toArgb()),
            modifier = Modifier.testTag(PaletteTestTags.PALETTE_ON_BACKGROUND)
        )
        Text(
            text = String.format("#%08X", p.onSurface.toArgb()),
            modifier = Modifier.testTag(PaletteTestTags.PALETTE_ON_SURFACE)
        )
    }
}