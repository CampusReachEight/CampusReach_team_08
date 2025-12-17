package com.android.sample.ui.authentication

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileCache
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.ui.profile.UserSections
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.firestore
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
class SignInViewModel(
    val profileRepository: UserProfileRepository =
        UserProfileRepositoryFirestore(Firebase.firestore),
    val profileCache: UserProfileCache? = null
) : ViewModel() {

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
        addUserToDataBase()
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

  fun addUserToDataBase() {
    viewModelScope.launch {
      val user = firebaseAuth.currentUser ?: return@launch

      try {
        val profile = profileRepository.getUserProfile(user.uid)
        profileCache?.deleteProfile(user.uid)
        profileCache?.saveProfile(profile)
        return@launch
      } catch (_: NoSuchElementException) {
        val profile: UserProfile =
            UserProfile(
                id = user.uid,
                name = user.displayName?.split(" ")?.getOrNull(0) ?: "",
                lastName = user.displayName?.split(" ")?.getOrNull(1) ?: "",
                email = user.email,
                photo = user.photoUrl,
                kudos = 0,
                helpReceived = 0,
                section = UserSections.NONE,
                arrivalDate = java.util.Date())
        profileRepository.addUserProfile(profile)
        profileCache?.deleteProfile(user.uid)
        profileCache?.saveProfile(profile)
      } catch (e: Exception) { // Catch other exceptions
        setError("Failed to retrieve or add user to database: ${e.localizedMessage}")
      }
    }
  }

  /**
   * Signs in anonymously with Firebase for demo/testing purposes.
   * Creates a demo user profile with fixed data.
   */
  fun signInAnonymously(onSuccess: () -> Unit) {
    viewModelScope.launch {
      _loading.value = true
      _errorText.value = null

      try {
        firebaseAuth.signInAnonymously().addOnCompleteListener { task ->
          _loading.value = false
          if (task.isSuccessful) {
            createDemoUserProfile()
            onSuccess()
          } else {
            _errorText.value = "Demo sign-in failed: ${task.exception?.localizedMessage}"
          }
        }
      } catch (e: Exception) {
        _errorText.value = "Demo sign-in failed: ${e.localizedMessage}"
        _loading.value = false
      }
    }
  }

  /**
   * Creates a demo user profile in Firestore with fixed data.
   */
  private fun createDemoUserProfile() {
    viewModelScope.launch {
      try {
        val user = firebaseAuth.currentUser ?: return@launch

        // Check if profile already exists
        try {
          val existingProfile = profileRepository.getUserProfile(user.uid)
          profileCache?.deleteProfile(user.uid)
          profileCache?.saveProfile(existingProfile)
          return@launch
        } catch (_: NoSuchElementException) {
          // Profile doesn't exist, create it
        }

        val demoProfile = UserProfile(
          id = user.uid,
          name = "Demo",
          lastName = "User",
          email = "demo@campusreach.com",
          photo = null,
          kudos = 0,
          helpReceived = 0,
          section = UserSections.NONE,
          arrivalDate = java.util.Date())

        profileRepository.addUserProfile(demoProfile)
        profileCache?.deleteProfile(user.uid)
        profileCache?.saveProfile(demoProfile)
      } catch (e: Exception) {
        setError("Failed to create demo profile: ${e.localizedMessage}")
      }
    }
  }
}

class SignInViewModelFactory(
    private val profileRepository: UserProfileRepository,
    private val profileCache: UserProfileCache?
) : androidx.lifecycle.ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(SignInViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST") return SignInViewModel(profileRepository, profileCache) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
