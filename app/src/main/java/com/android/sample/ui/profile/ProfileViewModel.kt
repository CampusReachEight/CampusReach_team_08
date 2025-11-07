package com.android.sample.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.Section
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileViewModel(
  initialState: ProfileState = ProfileState.default(),
  private val fireBaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
  private val repository: UserProfileRepository =
    UserProfileRepositoryFirestore(FirebaseFirestore.getInstance()),
  private val onLogout: (() -> Unit)? = null
) : ViewModel() {
  private val _state = MutableStateFlow(initialState)
  val state: StateFlow<ProfileState> = _state.asStateFlow()

  private var loadedProfile: UserProfile? = null

  private val authListener = FirebaseAuth.AuthStateListener {
    auth -> viewModelScope.launch { handleAuthUser(auth.currentUser) }
  }

  init {
    fireBaseAuth.addAuthStateListener(authListener)
    viewModelScope.launch { handleAuthUser(fireBaseAuth.currentUser) }
  }

  override fun onCleared() {
    super.onCleared()
    fireBaseAuth.removeAuthStateListener(authListener)
  }

  private suspend fun handleAuthUser(user: FirebaseUser?) {
    if (user == null) {
      loadedProfile = null
      _state.value = ProfileState.empty()
      return
    }

    setLoading(true)
    setError(null)

    try {
      // Try load private profile (owner)
      val profile = try {
        repository.getUserProfile(user.uid)
      } catch (e: Exception) {
        // Create minimal profile using the display name as-is (no splitting/formatting)
        val new = UserProfile(
          id = repository.getNewUid(),
          name = user.displayName.orEmpty(), // keep full name at face value
          lastName = "", // don't force a format — leave empty
          email = user.email,
          photo = null,
          kudos = 0,
          section = Section.OTHER,
          arrivalDate = Date()
        )
        repository.addUserProfile(new)
        new
      }

      loadedProfile = profile
      _state.value = mapProfileToState(profile)
    } catch (e: Exception) {
      setError("Failed to load profile")
    } finally {
      setLoading(false)
    }
  }

  private fun mapProfileToState(profile: UserProfile): ProfileState {
    val displayName =
      if (profile.lastName.isBlank()) profile.name else "${profile.name} ${profile.lastName}"
    return ProfileState(
      userName = displayName,
      userEmail = profile.email.orEmpty(),
      profileId = profile.id,
      kudosReceived = profile.kudos,
      helpReceived = 0,
      followers = 0,
      following = 0,
      arrivalDate = formatDate(profile.arrivalDate),
      userSection = if (profile.section == Section.OTHER) "None" else profile.section.name.replace("_", " "),
      isLoading = false,
      errorMessage = null,
      isEditMode = false,
      profilePictureUrl = profile.photo?.toString(),
      isLoggingOut = false
    )
  }

  private fun formatDate(date: Date): String {
    return try {
      SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) {
      "00/00/0000"
    }
  }

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

  fun updateSection(newSection: String) {
    _state.value = _state.value.copy(userSection = newSection)
  }
}
