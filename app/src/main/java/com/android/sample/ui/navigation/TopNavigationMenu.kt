package com.android.sample.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
    selectedTab: NavigationTab,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val title =
      when (selectedTab) {
        NavigationTab.Requests -> "Reach Out"
        NavigationTab.Events -> "Campus Events"
        NavigationTab.Map -> "Map"
      }

  CenterAlignedTopAppBar(
      modifier = modifier.testTag(NavigationTestTags.TOP_NAVIGATION_BAR),
      title = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
      },
      actions = {
        IconButton(
            onClick = onProfileClick,
            modifier = Modifier.testTag(NavigationTestTags.PROFILE_BUTTON)) {
              Icon(
                  imageVector = Icons.Filled.AccountCircle,
                  contentDescription = "Profile",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
      },
      colors =
          TopAppBarDefaults.centerAlignedTopAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface))
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun TopNavigationBarPreview() {
  MaterialTheme { TopNavigationBar(selectedTab = NavigationTab.Events, onProfileClick = {}) }
}
