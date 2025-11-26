package com.android.sample.ui.profile.publicProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class PublicProfileUiState(
    val isLoading: Boolean = false,
    val profile: PublicProfile? = null,
    val error: String? = null
)

private const val TIME_OUT = 15_000L

class PublicProfileViewModel(private val repository: UserProfileRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(PublicProfileUiState())
  val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

  private var lastProfileId: String? = null
  private var loadJob: Job? = null

  fun loadPublicProfile(profileId: String) {
    if (profileId.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false, profile = null, error = PublicProfileErrors.EMPTY_PROFILE_ID)
      return
    }

    lastProfileId = profileId
    loadJob?.cancel()
    loadJob =
        viewModelScope.launch {
          _uiState.value = _uiState.value.copy(isLoading = true, error = null)
          try {
            // keep the timeout literal per request
            val up = withTimeoutOrNull(TIME_OUT) { repository.getUserProfile(profileId) }
            if (up == null) {
              _uiState.value =
                  _uiState.value.copy(profile = null, error = PublicProfileErrors.FAILED_TO_LOAD)
              return@launch
            }

            val publicProfile =
                try {
                  userProfileToPublic(up)
                } catch (_: Exception) {
                  null
                }

            if (publicProfile == null) {
              _uiState.value =
                  _uiState.value.copy(profile = null, error = PublicProfileErrors.FAILED_TO_LOAD)
            } else {
              _uiState.value = _uiState.value.copy(profile = publicProfile, error = null)
            }
          } catch (ce: CancellationException) {
            throw ce
          } catch (e: Exception) {
            _uiState.value =
                _uiState.value.copy(
                    profile = null, error = e.message ?: PublicProfileErrors.FAILED_TO_LOAD)
          } finally {
            _uiState.value = _uiState.value.copy(isLoading = false)
          }
        }
  }

  fun refresh() {
    val id = lastProfileId
    if (id.isNullOrBlank()) {
      _uiState.value = _uiState.value.copy(error = PublicProfileErrors.EMPTY_PROFILE_ID)
      return
    }
    loadPublicProfile(id)
  }
}
