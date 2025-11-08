package com.android.sample.ui.profile.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfileState
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.theme.AppColors
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.appPalette

@Composable
fun ProfileHeader(
    state: ProfileState,
    modifier: Modifier = Modifier,
    onEditRequested: () -> Unit = {},
    palette: AppPalette = appPalette()
) {
  val accent = palette.accent
  val surface = palette.surface
  val textColor = AppColors.WhiteColor

  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(ProfileDimens.HeaderPadding)
              .testTag(ProfileTestTags.PROFILE_HEADER),
      colors = CardDefaults.cardColors(containerColor = accent),
      elevation = CardDefaults.cardElevation(defaultElevation = ProfileDimens.CardElevation)) {
        Box(modifier = Modifier.padding(ProfileDimens.HeaderPadding)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profile Picture",
                modifier =
                    Modifier.size(ProfileDimens.ProfilePicture)
                        .clip(CircleShape)
                        .background(surface),
                tint = accent)
            Spacer(modifier = Modifier.width(ProfileDimens.HeaderSpacer))
            Column {
              Text(
                  text = state.userName,
                  style = MaterialTheme.typography.titleMedium,
                  color = textColor,
                  modifier = Modifier.testTag(ProfileTestTags.PROFILE_HEADER_NAME))
              Text(
                  text = state.userEmail,
                  style = MaterialTheme.typography.bodyMedium,
                  color = textColor,
                  modifier = Modifier.testTag(ProfileTestTags.PROFILE_HEADER_EMAIL))
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onEditRequested,
                modifier = Modifier.testTag(ProfileTestTags.PROFILE_HEADER_EDIT_BUTTON)
                ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit", tint = surface)
            }
          }
        }
      }
}
