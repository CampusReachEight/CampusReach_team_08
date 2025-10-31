package com.android.sample.ui.authentication

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.android.sample.R
import com.android.sample.ui.navigation.NavigationTestTags
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.launch

/**
 * Contains test tags for UI testing of the SignInScreen.
 *
 * @author Thibaud Babin
 */
object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_TITLE = "loginTitle"
  const val LOGIN_BUTTON = "loginButton"
}

/**
 * Displays the Google Sign-In screen using Jetpack Compose.
 *
 * This composable is responsible for:
 * - Rendering the sign-in UI.
 * - Observing loading and error states from the ViewModel.
 * - Initiating the Google sign-in flow using CredentialManager.
 *
 * The ViewModel handles authentication and backend logic.
 *
 * @param viewModel The ViewModel managing sign-in logic.
 * @param onSignInSuccess Callback invoked when sign-in succeeds.
 * @param credentialManager Injected CredentialManager instance (for testability).
 * @author Thibaud Babin
 */
@Composable
fun SignInScreen(
    viewModel: SignInViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onSignInSuccess: () -> Unit = {},
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current)
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // Observe loading and error states from the ViewModel
  val isLoading by viewModel.loading.collectAsState()
  val errorText by viewModel.errorText.collectAsState()

  /**
   * Initiates the Google sign-in flow using CredentialManager. Handles exceptions and delegates
   * authentication to the ViewModel.
   *
   * @author Thibaud Babin
   */
  fun startGoogleLogin() {
    val clientId = context.getString(R.string.default_web_client_id)
    viewModel.setLoading(true)
    viewModel.clearError()

    scope.launch {
      try {
        val googleIdOption =
            GetGoogleIdOption.Builder()
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        val result = credentialManager.getCredential(context as Activity, request)
        viewModel.signInWithGoogle(result.credential, onSignInSuccess)
      } catch (e: NoCredentialException) {
        viewModel.setError("No Google account found")
      } catch (e: GetCredentialException) {
        when (e) {
          is GetCredentialCancellationException -> viewModel.setError("Connection cancelled")
          else -> viewModel.setError("Connection error: ${e.localizedMessage}")
        }
      }
    }
  }

  // Main UI layout
  Column(
      modifier = Modifier.fillMaxSize().padding(32.dp).testTag(NavigationTestTags.LOGIN_SCREEN),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        // App logo icon
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "App Logo",
            modifier = Modifier.size(80.dp).testTag(SignInScreenTestTags.APP_LOGO),
            tint = MaterialTheme.colorScheme.primary)

        // Welcome title
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp).testTag(SignInScreenTestTags.LOGIN_TITLE))

        // Google sign-in button
        Button(
            onClick = { startGoogleLogin() },
            enabled = !isLoading,
            modifier =
                Modifier.fillMaxWidth().height(56.dp).testTag(SignInScreenTestTags.LOGIN_BUTTON),
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
              if (isLoading) {
                // Show loading indicator while signing in
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
              } else {
                // Button content: Google icon and text
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                          imageVector = Icons.Default.AccountCircle,
                          contentDescription = "Google",
                          modifier = Modifier.size(20.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Sign in with Google")
                    }
              }
            }

        // Display error message if present
        if (errorText != null) {
          Spacer(modifier = Modifier.height(16.dp))
          Card(
              colors =
                  CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    text = errorText!!,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer)
              }
        }
      }
}
