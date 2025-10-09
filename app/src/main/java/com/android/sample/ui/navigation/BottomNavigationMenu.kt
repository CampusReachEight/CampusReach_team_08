package com.android.sample.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen

sealed class Tab(val name: String, val icon: ImageVector, val destination: Screen) {
    object Requests : Tab("Reach", Icons.Outlined.SyncAlt, Screen.Requests)

    object Events : Tab("Events", Icons.Outlined.Alarm, Screen.Events)

    object Map : Tab("Map", Icons.Outlined.Place, Screen.Map)

}

private val tabs =
    listOf(
        Tab.Requests,
        Tab.Events,
        Tab.Map
    )

@Composable
fun BottomNavigationMenu(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar (
        modifier =
            modifier.fillMaxWidth().height(60.dp).testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
        containerColor = MaterialTheme.colorScheme.surface,
        content = {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                NavigationBarItem(
                    selected = isSelected,
                    icon = { Icon( imageVector = tab.icon, contentDescription = null, tint =
                        if (isSelected) androidx.compose.ui.graphics.Color.White
                        else MaterialTheme.colorScheme.onSurface ) },
                    label = { Text(tab.name) },
                    onClick = { onTabSelected(tab) },
                    modifier =
                        Modifier.clip(RoundedCornerShape(50.dp))
                            .testTag(NavigationTestTags.getTabTestTag(tab)))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationMenuPreview() {
    MaterialTheme {
        BottomNavigationMenu(
            selectedTab = Tab.Requests, onTabSelected = {},
            )
    }
}
