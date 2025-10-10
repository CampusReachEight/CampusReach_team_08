package com.android.sample.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen

sealed class NavigationTab(val name: String, val icon: ImageVector, val destination: Screen) {
  object Requests : NavigationTab("Reach", Icons.Outlined.SyncAlt, Screen.Requests)

  object Events : NavigationTab("Events", Icons.Outlined.Alarm, Screen.Events)

  object Map : NavigationTab("Map", Icons.Outlined.Place, Screen.Map)

  object Profile : NavigationTab("Profile", Icons.Outlined.AccountCircle, Screen.Profile("TODO"))
}

private val navigationTabs =
    listOf(NavigationTab.Requests, NavigationTab.Events, NavigationTab.Map, NavigationTab.Profile)

@Composable
fun BottomNavigationMenu(
    selectedNavigationTab: NavigationTab,
    navigationActions: NavigationActions? = null,
    modifier: Modifier = Modifier,
) {
  NavigationBar(
      modifier =
          modifier.fillMaxWidth().height(60.dp).testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
      containerColor = MaterialTheme.colorScheme.surface,
      content = {
        navigationTabs.forEach { tab ->
          val isSelected = tab == selectedNavigationTab
          NavigationBarItem(
              selected = isSelected,
              icon = {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint =
                        if (isSelected) androidx.compose.ui.graphics.Color.White
                        else MaterialTheme.colorScheme.onSurface)
              },
              label = { Text(tab.name) },
              onClick = {
                  when (selectedNavigationTab) {
                      NavigationTab.Requests -> {
                          navigationActions?.navigateTo(Screen.Requests)
                      }
                      NavigationTab.Events -> {
                          navigationActions?.navigateTo(Screen.Events)
                      }
                      NavigationTab.Map -> {
                          navigationActions?.navigateTo(Screen.Map)
                      }
                      NavigationTab.Profile -> {
                          navigationActions?.navigateTo(Screen.Profile("TODO"))
                      }
                  }
              },
              modifier =
                  Modifier.clip(RoundedCornerShape(50.dp))
                      .testTag(NavigationTestTags.getTabTestTag(tab)))
        }
      },
  )
}

/* commented because marked as uncovered code by SonarQube
@Preview(showBackground = true)
@Composable
fun BottomNavigationMenuPreview() {
  MaterialTheme {
    BottomNavigationMenu(
        selectedNavigationTab = NavigationTab.Requests,
        onTabSelected = {},
    )
  }
}
*/