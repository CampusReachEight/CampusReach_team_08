package com.android.sample.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.profile.composables.LoadingIndicator
import com.android.sample.ui.profile.composables.LogoutDialog
import com.android.sample.ui.profile.composables.ProfileContent
import com.android.sample.ui.profile.composables.ProfileTopBar
import com.android.sample.ui.theme.appPalette

/**
 * Screen entry point for the Profile feature.
 * - Sub-components are small, focused composables for readability and easier testing.
 * - Keeps behavior identical to previous implementation while improving maintainability.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(), onBackClick: () -> Unit = {}) {
  val state by viewModel.state.collectAsState()

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.PROFILE_SCREEN),
      containerColor = appPalette().primary,
      topBar = { ProfileTopBar(onBackClick) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          when {
            state.isLoading ->
                LoadingIndicator(Modifier.align(Alignment.Center).testTag("profile_loading"))
            else ->
                ProfileContent(
                    state = state,
                    onLogoutRequested = { viewModel.showLogoutDialog() },
                    modifier = Modifier.fillMaxSize())
          }

          // Logout confirmation dialog
          LogoutDialog(
              visible = state.isLoggingOut,
              onConfirm = { viewModel.logout() },
              onDismiss = { viewModel.hideLogoutDialog() })
        }
      }
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
  ProfileScreen()
}
