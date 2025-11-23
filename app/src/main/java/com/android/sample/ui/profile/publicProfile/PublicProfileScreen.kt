package com.android.sample.ui.profile.publicProfile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.profile.UserProfile
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfileState
import com.android.sample.ui.profile.composables.ErrorBanner
import com.android.sample.ui.profile.composables.ProfileInformation
import com.android.sample.ui.profile.composables.ProfileLoadingBuffer
import com.android.sample.ui.profile.composables.ProfileStats
import com.android.sample.ui.profile.composables.ProfileTopBar
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

                        // ProfileHeader(state = state, onEditRequested = onEditRequested) TODO: create new public profile header
                        Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                        ProfileStats(state = shownState)
                        Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                        ProfileInformation(state = shownState)
                        Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                    }
            }
            // Follow / Unfollow button at the bottom
            FollowButton(
                isFollowing = isFollowing.value,
                onToggle = { isFollowing.value = !isFollowing.value },
                modifier = Modifier
                    .padding(
                        start = ProfileDimens.Horizontal,
                        end = ProfileDimens.Horizontal,
                        bottom = ProfileDimens.Vertical
                    )
            )
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
    ) {
        if (isFollowing) {
            androidx.compose.material3.Text(text = "Unfollow")
        } else {
            androidx.compose.material3.Text(text = "Follow")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PublicProfileScreenPreview() {
  PublicProfileScreen()
}
