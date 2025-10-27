package com.android.sample.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
private fun ColorSwatch(name: String, color: Color, onColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .background(color)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = onColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ThemePreviewContent() {
    val cs = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorSwatch("Primary", cs.primary, cs.onPrimary)
            ColorSwatch("Secondary", cs.secondary, cs.onSecondary)
            ColorSwatch("Tertiary", cs.tertiary, cs.onTertiary)
            ColorSwatch("Background", cs.background, cs.onBackground)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorSwatch("Surface", cs.surface, cs.onSurface)
            ColorSwatch("Error", cs.error, cs.onError)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Typography sample", style = MaterialTheme.typography.titleMedium, color = cs.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Body / small / caption", style = MaterialTheme.typography.bodyMedium, color = cs.onBackground)
    }
}

@Preview(showBackground = true, name = "Light Theme", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun LightThemePreview() {
    SampleAppTheme(darkTheme = false) {
        Surface { ThemePreviewContent() }
    }
}

@Preview(showBackground = true, name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DarkThemePreview() {
    SampleAppTheme(darkTheme = true) {
        Surface { ThemePreviewContent() }
    }
}
