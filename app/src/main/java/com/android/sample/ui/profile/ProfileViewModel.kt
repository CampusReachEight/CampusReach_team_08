package com.android.sample.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.firebase.auth.FirebaseAuth

class ProfileViewModel(
  initialState: ProfileState = ProfileState.default(),
  private val fireBaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
  private val onLogout: (() -> Unit)? = null // Optional callback for navigation
) : ViewModel() {
  private val _state = MutableStateFlow(initialState)
  val state: StateFlow<ProfileState> = _state

  fun setEditMode(enabled: Boolean) {
      _state.value = _state.value.copy(isEditMode = enabled)
  }

  fun setError(message: String?) {
      _state.value = _state.value.copy(errorMessage = message)
  }

  fun setLoading(loading: Boolean) {
      _state.value = _state.value.copy(isLoading = loading)
  }

  fun updateUserName(newName: String) {
      _state.value = _state.value.copy(userName = newName)
  }

  fun showLogoutDialog() {
    _state.value = _state.value.copy(isLoggingOut = true)
  }

  fun hideLogoutDialog() {
    _state.value = _state.value.copy(isLoggingOut = false)
  }

  fun logout() {
    _state.value = _state.value.copy(isLoggingOut = false, isLoading = true)
    fireBaseAuth.signOut() // Log out user
    _state.value = ProfileState.empty()
    onLogout?.invoke()
  }
}
