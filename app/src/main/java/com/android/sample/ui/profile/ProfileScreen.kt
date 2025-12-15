package com.android.sample.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.profile.composables.EditProfileDialog
import com.android.sample.ui.profile.composables.LogoutDialog
import com.android.sample.ui.profile.composables.ProfileContent
import com.android.sample.ui.profile.composables.ProfileLoadingBuffer
import com.android.sample.ui.profile.composables.ProfileTopBar
import com.android.sample.ui.profile.follow.FollowListType
import com.android.sample.ui.theme.appPalette
import com.google.firebase.auth.FirebaseAuth

/**
 * Screen entry point for the Profile feature.
 * - Sub-components are small, focused composables for readability and easier testing.
 * - Keeps behavior identical to previous implementation while improving maintainability.
 */
const val PROFILE_OFFLINE_TEXT = "You are currently offline"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    navigationActions: NavigationActions? = null
) {
  val state by viewModel.state.collectAsState()

  // Debug logging
  LaunchedEffect(state.isLoading) {
    println("ProfileScreen - isLoading: ${state.isLoading}")
  }

  LaunchedEffect(Unit) { viewModel.loadUserProfile(FirebaseAuth.getInstance().currentUser) }

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.PROFILE_SCREEN),
      containerColor = appPalette().primary,
      topBar = { ProfileTopBar(onBackClick) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          when {
            state.isLoading -> ProfileLoadingBuffer(Modifier.fillMaxSize())
            else -> {
              Column {
                if (state.offlineMode) {
                  Text(
                      PROFILE_OFFLINE_TEXT,
                      color = appPalette().error,
                      textAlign = TextAlign.Center,
                      modifier = Modifier.fillMaxWidth())
                }
                ProfileContent(
                    state = state,
                    onLogoutRequested = { viewModel.showLogoutDialog() },
                    onMyRequestAction = { viewModel.onMyRequestsClick(navigationActions) },
                    onAcceptedRequestsAction = {
                      viewModel.onAcceptedRequestsClick(navigationActions)
                    },
                    onEditRequested = { viewModel.setEditMode(true) },
                    onFollowersClick = {
                      navigationActions?.navigateTo(
                          Screen.FollowList(state.profileId, FollowListType.FOLLOWERS))
                    },
                    onFollowingClick = {
                      navigationActions?.navigateTo(
                          Screen.FollowList(state.profileId, FollowListType.FOLLOWING))
                    },
                    modifier = Modifier.fillMaxSize())
              }
            }
          }

          // Logout confirmation dialog
          LogoutDialog(
              visible = state.isLoggingOut,
              onConfirm = { viewModel.logout() },
              onDismiss = { viewModel.hideLogoutDialog() })

          // Edit flow dialogs
          EditProfileDialog(
              visible = state.isEditMode,
              initialName = state.userName,
              initialSection = state.userSection,
              onSave = { newName, newSection ->
                viewModel.saveProfileChanges(newName, newSection)
                viewModel.setEditMode(false)
              },
              onCancel = { viewModel.setEditMode(false) })
        }
      }
}
