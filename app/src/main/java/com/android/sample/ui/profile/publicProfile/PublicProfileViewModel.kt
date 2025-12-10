package com.android.sample.ui.profile.publicProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PublicProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null,
    val isFollowing: Boolean = false
)

class PublicProfileViewModel(val userProfileRepository: UserProfileRepository) : ViewModel() {

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
        _uiState.value = _uiState.value.copy(isLoading = false, profile = userProfile, error = null)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, profile = null, error = "Failed to load profile: ${e.message}")
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

  // Add after the clear() function:

  fun toggleFollow(targetUserId: String) {
    viewModelScope.launch {
      try {
        val currentUserId = userProfileRepository.getCurrentUserId()

        if (currentUserId.isBlank()) {
          _uiState.value = _uiState.value.copy(error = "You must be logged in to follow users")
          return@launch
        }

        val isCurrentlyFollowing = _uiState.value.isFollowing
        val currentProfile = _uiState.value.profile

        if (isCurrentlyFollowing) {
          userProfileRepository.unfollowUser(currentUserId, targetUserId)
          // Update state immediately without reloading
          _uiState.value =
              _uiState.value.copy(
                  isFollowing = false,
                  profile = currentProfile?.copy(followerCount = currentProfile.followerCount - 1))
        } else {
          userProfileRepository.followUser(currentUserId, targetUserId)
          // Update state immediately without reloading
          _uiState.value =
              _uiState.value.copy(
                  isFollowing = true,
                  profile = currentProfile?.copy(followerCount = currentProfile.followerCount + 1))
        }
      } catch (e: IllegalArgumentException) {
        _uiState.value = _uiState.value.copy(error = e.message)
      } catch (e: IllegalStateException) {
        _uiState.value = _uiState.value.copy(error = e.message)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(error = "Failed to update follow status: ${e.message}")
      }
    }
  }

  suspend fun checkFollowingStatus(currentUserId: String, targetUserId: String) {
    if (currentUserId.isBlank() || targetUserId.isBlank()) return

    try {
      val isFollowing = userProfileRepository.isFollowing(currentUserId, targetUserId)
      _uiState.value = _uiState.value.copy(isFollowing = isFollowing)
    } catch (e: Exception) {
      // Silently fail - following status is not critical
    }
  }
}

class PublicProfileViewModelFactory(private val userProfileRepository: UserProfileRepository) :
    ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(PublicProfileViewModel::class.java)) {
      return PublicProfileViewModel(userProfileRepository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
