package com.android.sample.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.compareTo
import kotlin.comparisons.then
import kotlin.text.toDouble

@Composable
private fun ColorSwatch(name: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(110.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.BlackColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun ThemePreviewContent(textColor: Color = MaterialTheme.colorScheme.onBackground) {
    @Composable
    fun Swatch(name: String, color: Color) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(110.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(color)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            )
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Utility colors", style = MaterialTheme.typography.titleMedium, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Swatch("BlackColor", AppColors.BlackColor)
            Swatch("WhiteColor", AppColors.WhiteColor)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("App palette (light)", style = MaterialTheme.typography.titleMedium, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Swatch("PrimaryColor", AppColors.PrimaryColor)
            Swatch("SecondaryColor", AppColors.SecondaryColor)
            Swatch("AccentColor", AppColors.AccentColor)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Swatch("BackgroundColor", AppColors.BackgroundColor)
            Swatch("SurfaceColor", AppColors.SurfaceColor)
            Swatch("ErrorColor", AppColors.ErrorColor)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("App palette (dark)", style = MaterialTheme.typography.titleMedium, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Swatch("PrimaryDark", AppColors.PrimaryDark)
            Swatch("SecondaryDark", AppColors.SecondaryDark)
            Swatch("AccentDark", AppColors.AccentDark)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Swatch("BackgroundDark", AppColors.BackgroundDark)
            Swatch("SurfaceDark", AppColors.SurfaceDark)
            Swatch("ErrorDark", AppColors.ErrorDark)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Typography sample", style = MaterialTheme.typography.titleMedium, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Body / small / caption",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

@Preview(showBackground = true, name = "Light Theme", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun LightThemePreview() {
    SampleAppTheme(darkTheme = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ThemePreviewContent()
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DarkThemePreview() {
    SampleAppTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ThemePreviewContent()
        }
    }
}
