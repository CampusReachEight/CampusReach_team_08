package com.android.sample.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.android.sample.ui.navigation.NavigationTestTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavigationBar(
    selectedTab: Tab,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (selectedTab) {
        is Tab.Requests -> "Reach Out"
        is Tab.Events -> "Campus Events"
        is Tab.Map -> "Map"
    }

    CenterAlignedTopAppBar(
        modifier = modifier.testTag(NavigationTestTags.TOP_NAVIGATION_BAR),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag(NavigationTestTags.SETTINGS_BUTTON)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun TopNavigationBarPreview() {
    MaterialTheme {
        TopNavigationBar(
            selectedTab = Tab.Events,
            onSettingsClick = {}
        )
    }
}
