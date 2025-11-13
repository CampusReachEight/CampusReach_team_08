package com.android.sample.ui.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.ui.profile.UserSections
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel(
    val profileRepository: UserProfileRepository =
        UserProfileRepositoryFirestore(Firebase.firestore)
) : ViewModel() {

  private val _loading = MutableStateFlow(false)
  val loading = _loading.asStateFlow()

  private val _errorText = MutableStateFlow<String?>(null)
  val errorText = _errorText.asStateFlow()

  private val firebaseAuth = FirebaseAuth.getInstance()

  fun signInAnonymously(onSuccess: () -> Unit) {
    viewModelScope.launch {
      _loading.value = true
      _errorText.value = null

      firebaseAuth.signInAnonymously().addOnCompleteListener { task ->
        if (task.isSuccessful) {
          addUserToDataBase()
          _loading.value = false
          onSuccess()
        } else {
          _errorText.value = "Sign-in failed: ${task.exception?.localizedMessage}"
          _loading.value = false
        }
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

  private fun addUserToDataBase() {
    viewModelScope.launch {
      val user = firebaseAuth.currentUser ?: return@launch
      try {
        profileRepository.getUserProfile(user.uid)
        return@launch
      } catch (_: NoSuchElementException) {
        profileRepository.addUserProfile(
            UserProfile(
                id = user.uid,
                name = "Test",
                lastName = "User",
                email = "testuser@example.com",
                photo = null,
                kudos = 0,
                section = UserSections.COMPUTER_SCIENCE,
                arrivalDate = java.util.Date()))
      }
    }
  }
}
