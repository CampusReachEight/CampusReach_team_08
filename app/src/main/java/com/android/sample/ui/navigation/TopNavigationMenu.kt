package com.android.sample.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import com.android.sample.ui.map.ConstantMap
import com.android.sample.ui.navigation.NavigationTab
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.profile.ProfilePicture
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavigationBar(
    selectedTab: NavigationTab,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    onZoomSettingsClick: (() -> Unit)? = null
) {
  val title =
      when (selectedTab) {
        NavigationTab.Requests -> "Reach Out"
        NavigationTab.Events -> "Campus Events"
        NavigationTab.Map -> "Map"
      }

  CenterAlignedTopAppBar(
      modifier = modifier.testTag(NavigationTestTags.TOP_NAVIGATION_BAR).padding(),
      title = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = appPalette().onSurface)
      },
      navigationIcon = {
        if (onZoomSettingsClick != null) {
          IconButton(
              onClick = onZoomSettingsClick,
              modifier = Modifier.testTag(NavigationTestTags.SETTINGS_BUTTON)) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = ConstantMap.ZOOM_SETTING,
                    tint = appPalette().onSurface)
              }
        }
      },
      actions = {
        ProfilePicture(
            profileId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            onClick = onProfileClick,
            modifier =
                Modifier.size(UiDimens.IconMedium).testTag(NavigationTestTags.PROFILE_BUTTON))
      },
      colors =
          TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = appPalette().surface))
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun TopNavigationBarPreview() {
  MaterialTheme { TopNavigationBar(selectedTab = NavigationTab.Events, onProfileClick = {}) }
}
