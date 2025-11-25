package com.android.sample.ui.profile.publicProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PublicProfileUiState(
    val isLoading: Boolean = false,
    val profile: PublicProfile? = null,
    val errorMessage: String? = null
)

class PublicProfileViewModel(
    private val repository: UserProfileRepository =
        UserProfileRepositoryFirestore(FirebaseFirestore.getInstance())
) : ViewModel() {

  private val _uiState = MutableStateFlow(PublicProfileUiState())
  val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

  fun loadPublicProfile(profileId: String) {
    // Implementation to load public profile from repository
    // and update _uiState accordingly
    viewModelScope.launch {
      _uiState.value = PublicProfileUiState(isLoading = true, errorMessage = null)
      try {
        val up = withContext(Dispatchers.IO) { repository.getUserProfile(profileId) }
        val public = userProfileToPublic(up)
        _uiState.value =
            _uiState.value.copy(isLoading = false, profile = public, errorMessage = null)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                profile = null,
                errorMessage = e.message ?: "Failed to load profile")
      }
    }
  }

  fun refresh(profileId: String) = loadPublicProfile(profileId)
}
