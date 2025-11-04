package com.android.sample.ui.profile.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.appPalette

/**
 * A composable function that displays a list of profile-related actions in the form of buttons.
 *
 * @param onLogoutClick A lambda function to handle the logout action. Defaults to an empty lambda.
 * @param palette An instance of [AppPalette] to define the color palette for the UI. Defaults to
 *   the global `appPalette()`.
 */
@Composable
fun ProfileActions(onLogoutClick: () -> Unit = {}, palette: AppPalette = appPalette()) {
  Column(
      modifier =
          Modifier.padding(horizontal = ProfileDimens.Horizontal)
              .testTag(ProfileTestTags.PROFILE_ACTIONS)) {
        // Header text for the actions section
        Text(
            text = "Actions",
            style = MaterialTheme.typography.titleMedium,
            color = palette.text,
            modifier = Modifier.padding(bottom = ProfileDimens.Vertical))
        // Logout action item
        ActionItem(
            icon = Icons.Default.Logout,
            title = "Log out",
            subtitle = "Further secure your account for safety",
            tag = ProfileTestTags.PROFILE_ACTION_LOG_OUT,
            onClick = onLogoutClick)
        // About app button
        ActionItem(
            icon = Icons.Default.Info,
            title = "About App",
            subtitle = "Find out more about CampusReach",
            tag = ProfileTestTags.PROFILE_ACTION_ABOUT_APP)
      }
}

/**
 * A composable function that represents a single action item in the profile actions list.
 *
 * @param icon The icon to display for the action.
 * @param title The title of the action.
 * @param subtitle A brief description of the action.
 * @param tag A unique test tag for UI testing purposes.
 * @param onClick A lambda function to handle the click event for the action. Defaults to an empty
 *   lambda.
 */
@Composable
fun ActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit = {},
    palette: AppPalette = appPalette()
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = ProfileDimens.ActionVerticalPadding)
              .testTag(tag)
              .clickable { onClick() },
      colors = CardDefaults.cardColors(containerColor = palette.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(ProfileDimens.ActionInternalPadding),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = icon,
                  contentDescription = null,
                  modifier = Modifier.padding(end = ProfileDimens.Horizontal),
                  tint = palette.accent)
              Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = palette.text)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.accent.copy(alpha = 0.6f))
              }
            }
      }
}
