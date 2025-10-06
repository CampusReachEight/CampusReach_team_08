/**
 * This file contains the implementation of the Google Sign-In screen using Jetpack Compose. It
 * handles the UI for the sign-in screen and the logic for authenticating with Google and Firebase.
 */
package com.android.sample.ui.authentication

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.android.sample.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

/**
 * Test tags for UI testing purposes.
 *
 * @author Thibaud Babin
 */
object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_TITLE = "loginTitle"
  const val LOGIN_BUTTON = "loginButton"
}

/**
 * Composable function for the sign-in screen.
 *
 * This function sets up the UI for the sign-in screen and handles the Google sign-in process.
 *
 * @param credentialManager The credential manager used to handle Google sign-in. Defaults to a new
 *   instance.
 * @param onSignInSuccess Callback invoked when the sign-in is successful.
 * @author Thibaud Babin
 */
@Composable
fun SignInScreen(
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignInSuccess: () -> Unit = {}
) {
  // Get the current application context and coroutine scope
  val appContext = LocalContext.current
  val scope = rememberCoroutineScope()
  // Initialize Firebase Auth instance
  val firebaseAuth = Firebase.auth
  // State to manage loading and error messages
  val showLoading = remember { mutableStateOf(false) }
  val errorText = remember { mutableStateOf<String?>(null) }

  /**
   * Initiates the Google sign-in process.
   *
   * @author Thibaud Babin
   */
  fun startGoogleLogin() {
    // Get the client ID from resources
    val clientId = appContext.getString(R.string.default_web_client_id)
    showLoading.value = true
    errorText.value = null

    scope.launch {
      try {
        // Configure Google ID option for the credential request
        val googleIdOption = GetGoogleIdOption.Builder()
        googleIdOption.setServerClientId(clientId)
        googleIdOption.setFilterByAuthorizedAccounts(false)
        val builtOption = googleIdOption.build()

        // Build the credential request
        val requestBuilder = GetCredentialRequest.Builder()
        requestBuilder.addCredentialOption(builtOption)
        val builtRequest = requestBuilder.build()

        // Get the credential using the credential manager
        val activity = appContext as Activity
        val result = credentialManager.getCredential(activity, builtRequest)
        val userCredential = result.credential

        // Handle the credential received from Google sign-in
        handleCredential(userCredential, firebaseAuth, onSignInSuccess, errorText, showLoading)
      } catch (e: GetCredentialException) {
        // Handle different types of credential exceptions
        when (e) {
          is GetCredentialCancellationException -> errorText.value = "Connection cancelled"
          is NoCredentialException -> errorText.value = "No Google account found"
          else -> errorText.value = "Connection error: ${e.localizedMessage}"
        }
        showLoading.value = false
      }
    }
  }

  // UI layout for the sign-in screen
  Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        // App logo
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "App Logo",
            modifier = Modifier.size(80.dp).testTag(SignInScreenTestTags.APP_LOGO),
            tint = MaterialTheme.colorScheme.primary)

        // Welcome text
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp).testTag(SignInScreenTestTags.LOGIN_TITLE))

        Button(
            onClick = { startGoogleLogin() },
            enabled = !showLoading.value,
            modifier =
                Modifier.fillMaxWidth().height(56.dp).testTag(SignInScreenTestTags.LOGIN_BUTTON),
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
              if (showLoading.value) {
                // Show loading indicator while the sign-in process is ongoing
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
              } else {
                // Show Google sign-in button with icon and text
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                          imageVector = Icons.Default.AccountCircle,
                          contentDescription = "Google",
                          modifier = Modifier.size(20.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Se connecter avec Google")
                    }
              }
            }

        // Show error message if there is any
        if (errorText.value != null) {
          Spacer(modifier = Modifier.height(16.dp))
          Card(
              colors =
                  CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    text = errorText.value!!,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer)
              }
        }
      }
}

/**
 * Handles the credential received from Google sign-in.
 *
 * @param credential The credential received from the credential manager.
 * @param auth The FirebaseAuth instance used for authentication.
 * @param onSuccess Callback invoked when the sign-in is successful.
 * @param errorText State to update with error messages.
 * @param showLoading State to update the loading status.
 * @author Thibaud Babin
 */
fun handleCredential(
    credential: Credential,
    auth: FirebaseAuth,
    onSuccess: () -> Unit,
    errorText: androidx.compose.runtime.MutableState<String?>,
    showLoading: androidx.compose.runtime.MutableState<Boolean>
) {
  // Check if the credential is a custom credential
  val isCustom = credential is CustomCredential
  if (isCustom) {
    val customCred = credential as CustomCredential
    // Check if the credential is of type Google ID token
    val isGoogleType = customCred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    if (isGoogleType) {
      try {
        // Create Google ID token credential from the received data
        val googleCred = GoogleIdTokenCredential.createFrom(customCred.data)
        val token = googleCred.idToken
        // Authenticate with Firebase using the Google ID token
        loginWithFirebase(token, auth, onSuccess, errorText, showLoading)
      } catch (e: GoogleIdTokenParsingException) {
        errorText.value = "Connection error while loading in google: ${e.localizedMessage}"
        showLoading.value = false
      }
    } else {
      errorText.value = "Credential type is not Google ID!"
      showLoading.value = false
    }
  } else {
    errorText.value = "Credential is not CustomCredential!"
    showLoading.value = false
  }
}

/**
 * Authenticates with Firebase using the Google ID token.
 *
 * @param idToken The Google ID token.
 * @param auth The FirebaseAuth instance used for authentication.
 * @param onSuccess Callback invoked when the sign-in is successful.
 * @param errorText State to update with error messages.
 * @param showLoading State to update the loading status.
 * @author Thibaud Babin
 */
fun loginWithFirebase(
    idToken: String,
    auth: FirebaseAuth,
    onSuccess: () -> Unit,
    errorText: androidx.compose.runtime.MutableState<String?>,
    showLoading: androidx.compose.runtime.MutableState<Boolean>
) {
  // Create Firebase credential using the Google ID token
  val credential = GoogleAuthProvider.getCredential(idToken, null)
  auth.signInWithCredential(credential).addOnCompleteListener { task ->
    if (task.isSuccessful) {
      // Sign-in success, invoke the success callback
      showLoading.value = false
      onSuccess()
    } else {
      // Sign-in failed, update the error message
      errorText.value = "Échec de la connexion: ${task.exception?.localizedMessage}"
      showLoading.value = false
    }
  }
}
