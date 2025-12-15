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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.profile.UserProfile
import com.android.sample.ui.profile.PROFILE_OFFLINE_TEXT
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.profile.ProfileState
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.profile.UserSections
import com.android.sample.ui.profile.composables.ErrorBanner
import com.android.sample.ui.profile.composables.ProfileInformation
import com.android.sample.ui.profile.composables.ProfileLoadingBuffer
import com.android.sample.ui.profile.composables.ProfileStats
import com.android.sample.ui.profile.composables.ProfileTopBar
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.appPalette

/**
 * PublicProfileScreen can be driven either by an external `profile` (static, UI-only) or by the
 * `PublicProfileViewModel` (loading / error / preview behavior). Passing a `profile` is useful for
 * tests and previews where no ViewModel logic is desired.
 */
@Composable
fun PublicProfileScreen(
    profile: UserProfile? = null,
    viewModel: PublicProfileViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    defaultProfileId: String = PublicProfileDefaults.DEFAULT_PUBLIC_PROFILE_ID
) {
  LaunchedEffect(defaultProfileId) {
    if (profile == null && defaultProfileId.isNotBlank()) {
      viewModel.loadPublicProfile(defaultProfileId)
      val currentUserId = viewModel.userProfileRepository.getCurrentUserId()
      if (currentUserId.isNotBlank()) {
        viewModel.checkFollowingStatus(currentUserId, defaultProfileId)
      }
    }
  }

  val vmState by viewModel.uiState.collectAsState()
  val shownState =
      if (profile != null) {
        PublicProfileUiState(isLoading = false, profile = profile, error = null)
      } else {
        vmState
      }
  val profileState =
      if (profile != null) {
        mapUserProfileToProfileState(profile)
      } else {
        mapUserProfileToProfileState(vmState.profile)
      }

  Scaffold(
      modifier = Modifier.testTag(PublicProfileTestTags.PUBLIC_PROFILE_SCREEN),
      containerColor = appPalette().primary,
      topBar = { ProfileTopBar(onBackClick) }) { padding ->
        PublicProfileContent(
            shownState = shownState,
            profileState = profileState,
            defaultProfileId = defaultProfileId,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize().padding(padding))
      }
}

@Composable
private fun PublicProfileContent(
    shownState: PublicProfileUiState,
    profileState: ProfileState,
    defaultProfileId: String,
    viewModel: PublicProfileViewModel,
    modifier: Modifier = Modifier
) {
  Box(modifier = modifier) {
    when {
      shownState.isLoading -> ProfileLoadingBuffer(Modifier.fillMaxSize())
      else ->
          PublicProfileScrollableContent(
              shownState = shownState,
              profileState = profileState,
              defaultProfileId = defaultProfileId,
              viewModel = viewModel)
    }
  }
}

@Composable
private fun PublicProfileScrollableContent(
    shownState: PublicProfileUiState,
    profileState: ProfileState,
    defaultProfileId: String,
    viewModel: PublicProfileViewModel
) {
  Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
    shownState.error?.let {
      ErrorBanner(it)
      Spacer(modifier = Modifier.height(ProfileDimens.Vertical))
    }

    if (shownState.offlineMode) {
      Text(
          PROFILE_OFFLINE_TEXT,
          color = appPalette().error,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth())
    }

    PublicProfileHeader(
        profile = shownState.profile,
        isFollowing = shownState.isFollowing,
        onFollowToggle = { viewModel.toggleFollow(defaultProfileId) },
        modifier = Modifier.testTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER),
        uiState = shownState)
    Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
    ProfileStats(state = profileState)
    Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
    ProfileInformation(state = profileState, showSensitiveInfo = false)
    Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
  }
}

private const val UNKNOWN = "Unknown"
private const val NONE = "None"
private const val MAX_LENGTH = 20
private const val ONE_LINE = 1
private const val BUTTON_WIDTH = 96
private const val WEIGHT = 1f

@Composable
fun PublicProfileHeader(
    profile: UserProfile?,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    modifier: Modifier = Modifier,
    palette: AppPalette = appPalette(),
    uiState: PublicProfileUiState
) {
  val accent = palette.accent
  val textColor = palette.onAccent
  val maxNameLength = MAX_LENGTH
  val uiUtils = com.android.sample.ui.UiUtils()

  val fullName =
      when {
        profile == null -> UNKNOWN
        profile.lastName.isBlank() -> profile.name
        else -> "${profile.name} ${profile.lastName}"
      }

  val sectionLabel =
      try {
        UserSections.entries
            .firstOrNull { it.name.equals(profile?.section.toString(), ignoreCase = true) }
            ?.label ?: profile?.section.toString()
      } catch (e: Exception) {
        NONE
      }

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
                profileId = profile?.id ?: "",
                onClick = {},
                modifier =
                    Modifier.size(ProfileDimens.ProfilePicture)
                        .testTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_PROFILE_PICTURE))
            Spacer(modifier = Modifier.width(ProfileDimens.HeaderSpacer))

            // Column takes remaining space so texts will ellipsize based on available width
            Column(modifier = Modifier.weight(WEIGHT)) {
              Text(
                  text = fullName,
                  style = MaterialTheme.typography.titleMedium,
                  color = textColor,
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_NAME),
                  maxLines = ONE_LINE,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis)
              Text(
                  text =
                      if (profile?.email != null) {
                        uiUtils.ellipsizeWithMiddle(
                            profile.email, maxLength = maxNameLength, keepSuffixLength = 10)
                      } else {
                        UNKNOWN
                      },
                  style = MaterialTheme.typography.bodyMedium,
                  color = textColor,
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_EMAIL),
                  maxLines = ONE_LINE,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(ProfileDimens.HeaderSpacer))

            if (!uiState.offlineMode) {
              FollowButton(
                  isFollowing = isFollowing,
                  onToggle = onFollowToggle,
                  isOperationInProgress = uiState.isFollowOperationInProgress)
            }
          }
        }
      }
}

private const val UNFOLLOW = "Unfollow"
private const val FOLLOW = "Follow"

@Composable
fun FollowButton(isFollowing: Boolean, onToggle: () -> Unit, isOperationInProgress: Boolean) {
  val tag =
      if (isFollowing) PublicProfileTestTags.UNFOLLOW_BUTTON
      else PublicProfileTestTags.FOLLOW_BUTTON
  ElevatedButton(
      onClick = onToggle,
      modifier = Modifier.testTag(tag),
      enabled = !isOperationInProgress,
      colors =
          ButtonDefaults.elevatedButtonColors(
              containerColor = appPalette().onAccent, contentColor = appPalette().accent)) {
        Text(
            text = if (isFollowing) UNFOLLOW else FOLLOW,
            maxLines = ONE_LINE,
            softWrap = false,
            overflow = TextOverflow.Ellipsis)
      }
}

private const val ZERO = 0
private const val FORMAT = "dd/MM/yyyy"

fun mapUserProfileToProfileState(userProfile: UserProfile?): ProfileState {
  if (userProfile == null) {
    return ProfileState(
        isLoading = false,
        userName = UNKNOWN,
        userSection = NONE,
        profilePictureUrl = null,
        kudosReceived = ZERO,
        helpReceived = ZERO,
        followers = ZERO,
        following = ZERO,
        isLoggingOut = false,
        isEditMode = false)
  }

  val fullName =
      if (userProfile.lastName.isBlank()) {
        userProfile.name
      } else {
        "${userProfile.name} ${userProfile.lastName}"
      }

  val sectionLabel =
      try {
        UserSections.entries
            .firstOrNull { it.name.equals(userProfile.section.toString(), ignoreCase = true) }
            ?.label ?: userProfile.section.toString()
      } catch (e: Exception) {
        NONE
      }

  return ProfileState(
      isLoading = false,
      userName = fullName,
      userSection = sectionLabel,
      profilePictureUrl = userProfile.photo?.toString(),
      kudosReceived = userProfile.kudos,
      helpReceived = userProfile.helpReceived,
      followers = userProfile.followerCount,
      following = userProfile.followingCount,
      arrivalDate =
          try {
            java.text
                .SimpleDateFormat(FORMAT, java.util.Locale.getDefault())
                .format(userProfile.arrivalDate)
          } catch (e: Exception) {
            ""
          },
      isLoggingOut = false,
      isEditMode = false)
}
