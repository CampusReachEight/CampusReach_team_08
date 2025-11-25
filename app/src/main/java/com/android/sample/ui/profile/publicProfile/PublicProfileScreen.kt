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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.profile.UserProfile
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.profile.ProfileState
import com.android.sample.ui.profile.composables.ErrorBanner
import com.android.sample.ui.profile.composables.ProfileInformation
import com.android.sample.ui.profile.composables.ProfileLoadingBuffer
import com.android.sample.ui.profile.composables.ProfileStats
import com.android.sample.ui.profile.composables.ProfileTopBar
import com.android.sample.ui.theme.AppColors
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * One-off public profile screen: accepts a suspend loader that returns a `PublicProfile?`. Default
 * loader returns a deterministic fake for previews / quick wiring so callers don't need to provide
 * a repository when just rendering the UI.
 */
@Composable
fun PublicProfileScreen(
    viewModel: PublicProfileViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val isFollowing = remember { mutableStateOf(false) }
    val shownState = mapPublicToProfileState(
        publicProfile = state.profile,
        error = state.errorMessage,
        isLoading = state.isLoading
    )

    Scaffold(
        modifier = Modifier.testTag(NavigationTestTags.PUBLIC_PROFILE_SCREEN),
        containerColor = appPalette().primary,
        topBar = { ProfileTopBar(onBackClick) }) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when {
                state.isLoading -> ProfileLoadingBuffer(Modifier.fillMaxSize())
                else ->
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        state.errorMessage?.let {
                            ErrorBanner(it)
                            Spacer(modifier = Modifier.height(ProfileDimens.Vertical))
                        }

                        PublicProfileHeader(
                            state = state,
                            isFollowing = isFollowing.value,
                            onFollowToggle = { isFollowing.value = !isFollowing.value }
                        )
                        Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                        ProfileStats(state = shownState)
                        Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                        ProfileInformation(state = shownState)
                        Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                    }
            }
        }
    }
}

/**
 * Helper: map a `UserProfile` to `PublicProfile`. Use this in your caller when creating a loader
 * that reads from `UserProfileRepository`.
 */
fun userProfileToPublic(up: UserProfile): PublicProfile {
  val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
  val arrival =
      try {
        dateFormat.format(up.arrivalDate)
      } catch (_: Exception) {
        null
      }
  return PublicProfile(
      userId = up.id,
      name = listOf(up.name, up.lastName).joinToString(" ").trim().ifEmpty { "Unknown" },
      section = up.section.name,
      arrivalDate = arrival,
      pictureUriString = up.photo?.toString(),
      kudosReceived = up.kudos,
      helpReceived = 0,
      followers = 0,
      following = 0)
}

private fun mapPublicToProfileState(
    publicProfile: PublicProfile?,
    error: String?,
    isLoading: Boolean
): ProfileState {
    // Use ProfileState.default() when available to avoid missing fields; otherwise construct explicitly.
    return if (isLoading) {
        ProfileState.default()
    } else {
        if (publicProfile == null) {
            // minimal empty state with error shown
            ProfileState(
                userName = "Unknown",
                userEmail = "",
                profileId = "",
                kudosReceived = 0,
                helpReceived = 0,
                followers = 0,
                following = 0,
                arrivalDate = "",
                userSection = "",
                isLoading = false,
                errorMessage = error,
                isEditMode = false,
                profilePictureUrl = null,
                isLoggingOut = false
            )
        } else {
            ProfileState(
                userName = publicProfile.name,
                userEmail = "",
                profileId = publicProfile.userId,
                kudosReceived = publicProfile.kudosReceived,
                helpReceived = publicProfile.helpReceived,
                followers = publicProfile.followers,
                following = publicProfile.following,
                arrivalDate = publicProfile.arrivalDate ?: "",
                userSection = publicProfile.section,
                isLoading = false,
                errorMessage = error,
                isEditMode = false,
                profilePictureUrl = publicProfile.pictureUriString,
                isLoggingOut = false
            )
        }
    }
}


//Placeholder FollowButton composable
@Composable
fun FollowButton(
    isFollowing: Boolean,
    onToggle:() -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onToggle,
        modifier = modifier
            .size(width = UiDimens.IconMedium * 3, height = UiDimens.IconMedium)
    ) {
        if (isFollowing) {
            Text(text = "Unfollow")
        } else {
            Text(text = "Follow")
        }
    }
}

@Composable
fun PublicProfileHeader (
    state: PublicProfileUiState,
    modifier: Modifier = Modifier,
    isFollowing: Boolean = false,
    onFollowToggle: () -> Unit = { },
    palette: AppPalette = appPalette()
) {
    val accent = palette.accent
    val textColor = AppColors.WhiteColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(ProfileDimens.HeaderPadding)
            .testTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER),
        colors = CardDefaults.cardColors(containerColor = accent),
        elevation = CardDefaults.cardElevation(defaultElevation = ProfileDimens.CardElevation)
    ) {
        Box(modifier = Modifier.padding(ProfileDimens.HeaderPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePicture(
                    profileId = state.profile?.userId ?: "",
                    modifier =
                    Modifier.size(ProfileDimens.ProfilePicture)
                        .testTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_PROFILE_PICTURE)
                )

                Spacer(modifier = Modifier.width(ProfileDimens.HeaderSpacer))

                Column {
                    Text (
                        text = state.profile?.name ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        modifier = Modifier
                            .testTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_NAME)
                    )
                    Text (
                        text = state.profile?.section ?: "None",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier
                            .testTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_EMAIL)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                FollowButton(
                    isFollowing = isFollowing,
                    onToggle = onFollowToggle,
                    modifier = Modifier.testTag(
                        if (isFollowing) PublicProfileTestTags.UNFOLLOW_BUTTON
                        else PublicProfileTestTags.FOLLOW_BUTTON
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PublicProfileScreenPreview() {
  PublicProfileScreen()
}
