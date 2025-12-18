package com.android.sample.ui.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.android.sample.R
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette

/** Contains test tags for UI testing of the SignInScreen. */
object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_TITLE = "loginTitle"
  const val LOGIN_BUTTON = "loginButton"
}

/**
 * Displays the Demo Sign-In screen.
 *
 * Signs in anonymously as "Demo User" with full access.
 */
@Composable
fun SignInScreen(viewModel: SignInViewModel, onSignInSuccess: () -> Unit = {}) {
  // Observe loading and error states from the ViewModel
  val isLoading by viewModel.loading.collectAsState()
  val errorText by viewModel.errorText.collectAsState()

  // Main UI layout
  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(UiDimens.SpacingXl)
              .testTag(NavigationTestTags.LOGIN_SCREEN),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {

        // App logo icon
        Image(
            painter = painterResource(R.drawable.campusreach_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(UiDimens.IconLarge).testTag(SignInScreenTestTags.APP_LOGO))

        // Welcome title
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineMedium,
            modifier =
                Modifier.padding(bottom = UiDimens.SpacingLg)
                    .testTag(SignInScreenTestTags.LOGIN_TITLE))

        // Demo sign-in button
        Button(
            onClick = { viewModel.signInAnonymously(onSignInSuccess) },
            enabled = !isLoading,
            modifier =
                Modifier.fillMaxWidth()
                    .height(UiDimens.ButtonHeight)
                    .testTag(SignInScreenTestTags.LOGIN_BUTTON),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = appPalette().accent,
                    disabledContainerColor = appPalette().secondary)) {
              if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(UiDimens.ProgressSize), color = appPalette().surface)
              } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                          imageVector = Icons.Default.AccountCircle,
                          contentDescription = "Demo User",
                          modifier = Modifier.size(UiDimens.ProgressSize))
                      Spacer(modifier = Modifier.width(UiDimens.SpacingSm))
                      Text("Continue as Demo User")
                    }
              }
            }

        // Display error message if present
        if (errorText != null) {
          Spacer(modifier = Modifier.height(UiDimens.SpacingMd))
          Text(
              text = errorText!!,
              modifier = Modifier.padding(UiDimens.SpacingMd),
              color = appPalette().error)
        }
      }
}
