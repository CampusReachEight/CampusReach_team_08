package com.android.sample.ui.profile.publicProfile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.profile.ProfileState
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.profile.composables.ErrorBanner
import com.android.sample.ui.profile.composables.ProfileInformation
import com.android.sample.ui.profile.composables.ProfileLoadingBuffer
import com.android.sample.ui.profile.composables.ProfileStats
import com.android.sample.ui.profile.composables.ProfileTopBar
import com.android.sample.ui.theme.AppColors
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.appPalette

/**
 * PublicProfileScreen can be driven either by an external `profile` (static, UI-only) or by the
 * `PublicProfileViewModel` (loading / error / preview behavior). Passing a `profile` is useful for
 * tests and previews where no ViewModel logic is desired.
 */
@Composable
fun PublicProfileScreen(
    profile: PublicProfile? = null,
    viewModel: PublicProfileViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    defaultProfileId: String = PublicProfileDefaults.DEFAULT_PUBLIC_PROFILE_ID
) {
  // If caller provided an explicit profile, render static UI only.
  // Otherwise use the ViewModel (and auto-load preview id).
  if (profile == null) {
    LaunchedEffect(defaultProfileId) { viewModel.loadPublicProfile(defaultProfileId) }
  }

  val vmState by viewModel.uiState.collectAsState()

  val shownState =
      if (profile != null) {
        PublicProfileUiState(isLoading = false, profile = profile, error = null)
      } else {
        PublicProfileUiState(
            isLoading = vmState.isLoading, profile = vmState.profile, error = vmState.error)
      }

  val hiddenState =
      if (profile != null) {
        mapPublicToProfile(profile)
      } else {
        ProfileState(
            isLoading = vmState.isLoading,
            userName = vmState.profile?.name ?: "",
            userSection = vmState.profile?.section ?: "",
            profilePictureUrl = vmState.profile?.pictureUriString,
            kudosReceived = vmState.profile?.kudosReceived ?: 0,
            helpReceived = vmState.profile?.helpReceived ?: 0,
            followers = vmState.profile?.followers ?: 0,
            following = vmState.profile?.following ?: 0,
            isLoggingOut = false,
            isEditMode = false)
      }

  var isFollowing by remember { mutableStateOf(false) }

  Scaffold(
      modifier = Modifier.testTag(PublicProfileTestTags.PUBLIC_PROFILE_SCREEN),
      containerColor = appPalette().primary,
      topBar = { ProfileTopBar(onBackClick) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          when {
            shownState.isLoading -> ProfileLoadingBuffer(Modifier.fillMaxSize())
            else ->
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                  shownState.error?.let {
                    ErrorBanner(it)
                    Spacer(modifier = Modifier.height(ProfileDimens.Vertical))
                  }

                  // Use the VM-backed state for header composition when available, otherwise derive
                  // from
                  // the provided profile.
                  PublicProfileHeader(
                      state = if (profile != null) shownState else vmState,
                      isFollowing = isFollowing,
                      onFollowToggle = { isFollowing = !isFollowing })
                  Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                  ProfileStats(state = hiddenState)
                  Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                  ProfileInformation(state = hiddenState)
                  Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                }
          }
        }
      }
}

@Composable
fun PublicProfileHeader(
    state: PublicProfileUiState,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    modifier: Modifier = Modifier,
    palette: AppPalette = appPalette()
) {
  val accent = palette.accent
  val textColor = AppColors.WhiteColor

  // Text limits to avoid overflow in smaller devices
  val maxNameLength = 25
  val maxEmailLength = 30

  val uiUtils = com.android.sample.ui.UiUtils()
  val displayName =
      uiUtils.ellipsizeWithMiddle(state.profile?.name ?: "Unknown", maxLength = maxNameLength)
  val displayEmail =
      uiUtils.ellipsizeWithMiddle(state.profile?.email ?: "unknown", maxLength = maxEmailLength)

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
            ProfilePicture(
                profileId = state.profile?.userId ?: "",
                modifier =
                    Modifier.size(ProfileDimens.ProfilePicture)
                        .testTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_PROFILE_PICTURE))
            Spacer(modifier = Modifier.width(ProfileDimens.HeaderSpacer))
            Column {
              Text(
                  text = displayName,
                  style = MaterialTheme.typography.titleMedium,
                  color = textColor,
                  modifier = Modifier.testTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_NAME))
              Text(
                  text = displayEmail,
                  style = MaterialTheme.typography.bodyMedium,
                  color = textColor,
                  modifier = Modifier.testTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_EMAIL))
            }
            Spacer(modifier = Modifier.weight(1f))
            FollowButton(isFollowing = isFollowing, onToggle = onFollowToggle)
          }
        }
      }
}

/**
 * Simple follow button used by the public profile UI and tests. Adds distinct test tags for follow
 * vs unfollow states.
 */
@Composable
fun FollowButton(isFollowing: Boolean, onToggle: () -> Unit) {
  val tag =
      if (isFollowing) PublicProfileTestTags.UNFOLLOW_BUTTON
      else PublicProfileTestTags.FOLLOW_BUTTON
  ElevatedButton(onClick = onToggle, modifier = Modifier.testTag(tag)) {
    Text(text = if (isFollowing) "Unfollow" else "Follow")
  }
}

fun mapPublicToProfile(publicProfile: PublicProfile): ProfileState {
  return ProfileState(
      isLoading = false,
      userName = publicProfile.name,
      userSection = publicProfile.section,
      profilePictureUrl = publicProfile.pictureUriString,
      kudosReceived = publicProfile.kudosReceived,
      helpReceived = publicProfile.helpReceived,
      followers = publicProfile.followers,
      following = publicProfile.following,
      isLoggingOut = false,
      userEmail = publicProfile.email,
      profileId = publicProfile.userId,
      isEditMode = false)
}

@Preview
@Composable
fun PublicProfileScreenPreview() {
  PublicProfileScreen()
}
