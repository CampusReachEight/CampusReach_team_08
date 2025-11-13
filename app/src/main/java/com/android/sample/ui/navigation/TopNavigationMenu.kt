package com.android.sample.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.android.sample.ui.navigation.NavigationTab
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.profile.ProfilePicture
import com.google.firebase.auth.FirebaseAuth

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
      modifier = modifier.testTag(NavigationTestTags.TOP_NAVIGATION_BAR).padding(),
      title = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = appPalette().onSurface)
      },
      actions = {
        ProfilePicture(
            profileId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            onClick = onProfileClick,
            modifier = Modifier.size(UiDimens.IconMedium).testTag(NavigationTestTags.PROFILE_BUTTON))
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
