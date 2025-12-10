package com.android.sample.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette

sealed class NavigationTab(val name: String, val icon: ImageVector, val destination: Screen) {
  object Requests : NavigationTab("Reach", Icons.Outlined.SyncAlt, Screen.Requests)

  object Events : NavigationTab("Events", Icons.Outlined.Alarm, Screen.Events)

  object Map : NavigationTab("Map", Icons.Outlined.Place, Screen.Map())
}

private val navigationTabs = listOf(NavigationTab.Requests, NavigationTab.Events, NavigationTab.Map)

@Composable
fun BottomNavigationMenu(
    selectedNavigationTab: NavigationTab,
    navigationActions: NavigationActions? = null,
    modifier: Modifier = Modifier,
) {
  NavigationBar(
      modifier =
          modifier
              .fillMaxWidth()
              .windowInsetsPadding(WindowInsets.navigationBars)
              .height(UiDimens.ButtonHeight)
              .testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
      containerColor = appPalette().surface,
      content = {
        navigationTabs.forEach { tab ->
          val isSelected = tab == selectedNavigationTab
          NavigationBarItem(
              selected = isSelected,
              icon = { Icon(imageVector = tab.icon, contentDescription = null) },
              label = { Text(tab.name) },
              onClick = {
                when (tab) {
                  NavigationTab.Requests -> {
                    navigationActions?.navigateTo(Screen.Requests)
                  }
                  NavigationTab.Events -> {
                    navigationActions?.navigateTo(Screen.Events)
                  }
                  NavigationTab.Map -> {
                    navigationActions?.navigateTo(Screen.Map())
                  }
                }
              },
              modifier =
                  Modifier.clip(RoundedCornerShape(50.dp))
                      .testTag(NavigationTestTags.getTabTestTag(tab)),
              colors =
                  NavigationBarItemDefaults.colors(
                      selectedIconColor = appPalette().surface,
                      unselectedIconColor = appPalette().onSurface,
                      indicatorColor = appPalette().accent))
        }
      },
  )
}
