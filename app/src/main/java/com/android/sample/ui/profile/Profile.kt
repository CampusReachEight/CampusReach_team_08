package com.android.sample.ui.profile

import ProfileActions
import ProfileInformation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.profile.composables.ProfileHeader
import com.android.sample.ui.profile.composables.ProfileStats
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(), onBackClick: () -> Unit = {}) {
  val state by viewModel.state.collectAsState()

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.PROFILE_SCREEN),
      containerColor = appPalette().primary,
      topBar = {
        TopAppBar(
            title = { Text("Profile") },
            navigationIcon = {
              IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
              }
            })
      }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          when {
            state.isLoading -> {
              CircularProgressIndicator(
                  modifier = Modifier.align(Alignment.Center).testTag("profile_loading"))
            }
            else -> {
              Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                state.errorMessage?.let {
                  Text(
                      text = it,
                      color = MaterialTheme.colorScheme.error,
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(ProfileDimens.Horizontal)
                              .testTag("profile_error"),
                      textAlign = TextAlign.Center)
                  Spacer(modifier = Modifier.height(ProfileDimens.Vertical))
                }

                ProfileHeader(state = state)
                Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                ProfileStats(state = state)
                Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                ProfileInformation(state = state)
                Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
                ProfileActions(onLogoutClick = { viewModel.showLogoutDialog() })

                if (state.isLoggingOut) {
                  AlertDialog(
                      modifier = Modifier.testTag(ProfileTestTags.LOG_OUT_DIALOG),
                      onDismissRequest = { viewModel.hideLogoutDialog() },
                      title = { Text("Log out") },
                      text = { Text("Are you sure you want to log out?") },
                      confirmButton = {
                        TextButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM)) {
                              Text("Log out")
                            }
                      },
                      dismissButton = {
                        TextButton(
                            onClick = { viewModel.hideLogoutDialog() },
                            modifier = Modifier.testTag(ProfileTestTags.LOG_OUT_DIALOG_CANCEL)) {
                              Text("Cancel")
                            }
                      })
                }
              }
            }
          }
        }
      }
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
  ProfileScreen()
}
