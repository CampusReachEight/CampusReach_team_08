package com.android.sample.ui.authentication

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that handles Google Sign-In and Firebase authentication logic.
 *
 * Responsible for:
 * - Handling credential parsing and validation
 * - Authenticating with Firebase
 * - Exposing UI state for loading and error handling
 *
 * @author Thibaud
 */
class SignInViewModel : ViewModel() {

  private val _loading = MutableStateFlow(false)
  val loading = _loading.asStateFlow()

  private val _errorText = MutableStateFlow<String?>(null)
  val errorText = _errorText.asStateFlow()

  private val firebaseAuth = FirebaseAuth.getInstance()

  /**
   * Initiates sign-in with a Google credential. Called by the Composable after receiving a
   * credential from CredentialManager.
   *
   * @param credential Credential obtained from the Google Sign-In process
   * @param onSuccess Callback invoked on successful authentication
   */
  fun signInWithGoogle(credential: Credential, onSuccess: () -> Unit) {
    viewModelScope.launch {
      _loading.value = true
      _errorText.value = null

      if (credential is CustomCredential &&
          credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        try {
          val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
          val token = googleCred.idToken
          loginWithFirebase(token, onSuccess)
        } catch (e: GoogleIdTokenParsingException) {
          _errorText.value = "Error parsing Google credentials: ${e.localizedMessage}"
          _loading.value = false
        }
      } else {
        _errorText.value = "Invalid Google credential"
        _loading.value = false
      }
    }
  }

  /** Authenticates the user with Firebase using the Google ID token. */
  private fun loginWithFirebase(idToken: String, onSuccess: () -> Unit) {
    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

    firebaseAuth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
      _loading.value = false
      if (task.isSuccessful) {
        onSuccess()
      } else {
        _errorText.value = "Firebase sign-in failed: ${task.exception?.localizedMessage}"
      }
    }
  }

  fun clearError() {
    _errorText.value = null
  }

  fun setError(message: String) {
    _errorText.value = message
    _loading.value = false
  }

  fun setLoading(isLoading: Boolean) {
    _loading.value = isLoading
  }
}
