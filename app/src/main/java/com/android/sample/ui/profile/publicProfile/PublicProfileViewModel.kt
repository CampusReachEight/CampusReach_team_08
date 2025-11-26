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
    val error: PublicProfileError? = null
)

sealed class PublicProfileError {
  object EmptyProfileId : PublicProfileError()

  object ProfileNotFound : PublicProfileError()

  data class Network(val message: String?) : PublicProfileError()

  data class Unexpected(val message: String?) : PublicProfileError()
}

class PublicProfileViewModel(private val repository: UserProfileRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(PublicProfileUiState())
  val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

  private var lastProfileId: String? = null
  private var loadJob: Job? = null

  fun loadPublicProfile(profileId: String) {
    if (profileId.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false, profile = null, error = PublicProfileError.EmptyProfileId)
      return
    }

    lastProfileId = profileId
    loadJob?.cancel()
    loadJob =
        viewModelScope.launch {
          _uiState.value = _uiState.value.copy(isLoading = true, error = null)
          try {
            // Bound the call to avoid indefinite hangs; repository should ideally be non-blocking.
            val up = withTimeoutOrNull(15_000) { repository.getUserProfile(profileId) }
            if (up == null) {
              _uiState.value =
                  _uiState.value.copy(
                      profile = null, error = PublicProfileError.Network("Timeout or not found"))
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
                  _uiState.value.copy(
                      profile = null,
                      error = PublicProfileError.Unexpected("Malformed profile data"))
            } else {
              _uiState.value = _uiState.value.copy(profile = publicProfile, error = null)
            }
          } catch (ce: CancellationException) {
            // propagate cancellation so callers / coroutine system can handle it
            throw ce
          } catch (e: Exception) {
            _uiState.value =
                _uiState.value.copy(profile = null, error = PublicProfileError.Network(e.message))
          } finally {
            _uiState.value = _uiState.value.copy(isLoading = false)
          }
        }
  }

  fun refresh() {
    val id = lastProfileId
    if (id.isNullOrBlank()) {
      _uiState.value = _uiState.value.copy(error = PublicProfileError.EmptyProfileId)
      return
    }
    loadPublicProfile(id)
  }
}
