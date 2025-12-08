package com.android.sample.ui.profile.publicProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileCache
import com.android.sample.model.profile.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PublicProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null,
    val offlineMode: Boolean = false
)

class PublicProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    val profileCache: UserProfileCache? = null
) : ViewModel() {

  private val _uiState = MutableStateFlow(PublicProfileUiState())
  val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

  fun loadPublicProfile(profileId: String) {
    if (profileId.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false, profile = null, error = PublicProfileErrors.EMPTY_PROFILE_ID)
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, error = null)

      try {
        val userProfile = userProfileRepository.getUserProfile(profileId)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, profile = userProfile, error = null, offlineMode = false)
      } catch (e: Exception) {
        try {
          val userProfile = profileCache!!.getProfileById(profileId)
          _uiState.value =
              _uiState.value.copy(
                  isLoading = false, profile = userProfile, error = null, offlineMode = true)
        } catch (_: Exception) {
          _uiState.value =
              _uiState.value.copy(
                  isLoading = false, profile = null, error = "Failed to load profile: ${e.message}")
        }
      }
    }
  }

  fun setLoading(loading: Boolean) {
    _uiState.value = _uiState.value.copy(isLoading = loading)
  }

  fun setError(message: String?) {
    _uiState.value = _uiState.value.copy(error = message)
  }

  fun clear() {
    _uiState.value = PublicProfileUiState()
  }
}

class PublicProfileViewModelFactory(
    private val userProfileRepository: UserProfileRepository,
    val profileCache: UserProfileCache? = null
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(PublicProfileViewModel::class.java)) {
      return PublicProfileViewModel(userProfileRepository, profileCache = profileCache) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
