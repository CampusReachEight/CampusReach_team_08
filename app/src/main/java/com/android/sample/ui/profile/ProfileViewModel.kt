package com.android.sample.ui.profile

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.Section
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
  private val repository: UserProfileRepository = UserProfileRepositoryFirestore(FirebaseFirestore.getInstance()),
  initialState: ProfileState = ProfileState.default()
) : ViewModel() {
  private val _state = MutableStateFlow(initialState)
  val state: StateFlow<ProfileState> = _state

  fun updateProfile(name: String, section: String) {
    _state.value = _state.value.copy(isLoading = true)
    viewModelScope.launch {
      try {
        val userId = repository.getNewUid()
        val currentProfile = repository.getUserProfile(userId)
        val updatedProfile = currentProfile.copy(
          name = name,
          section = Section.valueOf(section.replace(" ", "_").uppercase())
        )
        repository.updateUserProfile(userId, updatedProfile)
        _state.value = updatedProfile.toProfileState().copy(isEditMode = false, isLoading = false, errorMessage = null)
      } catch (e: Exception) {
        _state.value = _state.value.copy(isLoading = false, errorMessage = "Failed to save profile")
      }
    }
  }

  @SuppressLint("SimpleDateFormat")
  fun ProfileState.toUserProfile(): UserProfile = UserProfile(
    id = profileId,
    name = userName,
    lastName = "", // Add lastName field if needed
    email = userEmail,
    photo = null, // Handle photo if used
    kudos = kudosReceived,
    section = Section.valueOf(section.replace(" ", "_").uppercase()),
    arrivalDate = SimpleDateFormat("dd/MM/yyyy").parse(arrivalDate)
  )

  @SuppressLint("SimpleDateFormat")
  fun UserProfile.toProfileState(): ProfileState = ProfileState(
    userName = name,
    userEmail = email ?: "",
    profileId = id,
    kudosReceived = kudos,
    // TODO: Add helpReceived, followers, and following fields to UserProfile data class and Firestore.
    // Currently defaulted to 0 because these fields do not exist in UserProfile yet.
    helpReceived = 0,
    followers = 0,
    following = 0,
    arrivalDate = SimpleDateFormat("dd/MM/yyyy").format(arrivalDate),
    section = section.name.replace("_", " ").capitalize()
  )

}

