package com.android.sample.ui.authentication

import android.app.Activity
import android.util.Log
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

object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_TITLE = "loginTitle"
  const val LOGIN_BUTTON = "loginButton"
}

@Composable
fun SignInScreen(
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignInSuccess: () -> Unit = {}
    /**
     * You can add parameters with default values here, e.g., onClick: () -> Unit = {}, modifier:
     * Modifier = Modifier, credentialManager: CredentialManager =
     * CredentialManager.create(LocalContext.current)
     *
     * Note: Parameters with default values do not mean that you should use the default value in
     * your implementation. They ensure that we can still use your code in our tests.
     */
) {
  val appContext = LocalContext.current
  val scope = rememberCoroutineScope()
  val firebaseAuth = Firebase.auth

  val showLoading = remember { mutableStateOf(false) }
  val errorText = remember { mutableStateOf<String?>(null) }

  fun startGoogleLogin() {
    val clientId = appContext.getString(R.string.default_web_client_id)

    Log.d("SignIn", "Package: ${appContext.packageName}")
    Log.d("SignIn", "Client ID: $clientId")

    showLoading.value = true
    errorText.value = null

    scope.launch {
      try {
        val googleIdOption = GetGoogleIdOption.Builder()
        googleIdOption.setServerClientId(clientId)
        googleIdOption.setFilterByAuthorizedAccounts(false)
        val builtOption = googleIdOption.build()

        val requestBuilder = GetCredentialRequest.Builder()
        requestBuilder.addCredentialOption(builtOption)
        val builtRequest = requestBuilder.build()

        val activity = appContext as Activity
        val result =
            credentialManager.getCredential(
                activity, builtRequest) // Fixed: context first, then request
        val userCredential = result.credential

        handleCredential(userCredential, firebaseAuth, onSignInSuccess, errorText, showLoading)
      } catch (e: GetCredentialException) {
        if (e is GetCredentialCancellationException) {
          errorText.value = "Connexion annulée"
        } else if (e is NoCredentialException) {
          errorText.value = "Aucun compte Google trouvé"
        } else {
          errorText.value = "Erreur de connexion: ${e.localizedMessage}"
        }
        showLoading.value = false
      }
    }
  }

  Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "App Logo",
            modifier = Modifier.size(80.dp).testTag(SignInScreenTestTags.APP_LOGO),
            tint = MaterialTheme.colorScheme.primary)

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
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
              } else {
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

fun handleCredential(
    credential: Credential,
    auth: FirebaseAuth,
    onSuccess: () -> Unit,
    errorText: androidx.compose.runtime.MutableState<String?>,
    showLoading: androidx.compose.runtime.MutableState<Boolean>
) {
  val isCustom = credential is CustomCredential

  if (isCustom) {
    val customCred = credential as CustomCredential
    val isGoogleType = customCred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL

    if (isGoogleType) {
      try {
        val googleCred = GoogleIdTokenCredential.createFrom(customCred.data)
        val token = googleCred.idToken
        loginWithFirebase(token, auth, onSuccess, errorText, showLoading)
      } catch (e: GoogleIdTokenParsingException) {
        errorText.value = "Erreur lors de la création du token Google: ${e.localizedMessage}"
        showLoading.value = false
      }
    } else {
      errorText.value = "Le credential n'est pas de type Google ID!"
      showLoading.value = false
    }
  } else {
    errorText.value = "Le credential n'est pas de type Google ID!"
    showLoading.value = false
  }
}

fun loginWithFirebase(
    idToken: String,
    auth: FirebaseAuth,
    onSuccess: () -> Unit,
    errorText: androidx.compose.runtime.MutableState<String?>,
    showLoading: androidx.compose.runtime.MutableState<Boolean>
) {
  val credential = GoogleAuthProvider.getCredential(idToken, null)

  auth.signInWithCredential(credential).addOnCompleteListener { task ->
    if (task.isSuccessful) {
      // Log.d("SignIn", "signInWithCredential:success")
      showLoading.value = false
      onSuccess()
    } else {
      // Log.w("SignIn", "signInWithCredential:failure", task.exception)
      errorText.value = "Échec de la connexion: ${task.exception?.localizedMessage}"
      showLoading.value = false
    }
  }
}
