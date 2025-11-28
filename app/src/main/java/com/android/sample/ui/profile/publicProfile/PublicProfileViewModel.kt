package com.android.sample.ui.profile.publicProfile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PublicProfileUiState(
    val isLoading: Boolean = false,
    val profile: PublicProfile? = null,
    val error: String? = null
)

/**
 * UI-only, read\-only ViewModel for the public profile screen.
 * - No repos, no network, no coroutines.
 * - `loadPublicProfile` produces a deterministic fake profile for a non-blank id.
 * - `setLoading` / `setError` / `clear` helpers for tests/previews.
 */
class PublicProfileViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(PublicProfileUiState())
  val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

  /**
   * Load a public profile in a UI-only manner. Does not contact any backend. Produces a
   * deterministic fake for non-blank ids; sets an error for blank ids.
   */
  fun loadPublicProfile(profileId: String) {
    if (profileId.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false, profile = null, error = PublicProfileErrors.EMPTY_PROFILE_ID)
      return
    }

    val fake =
        PublicProfile(
            userId = profileId,
            name = "User $profileId",
            section = PublicProfileDefaults.DEFAULT_SECTION,
            arrivalDate = null,
            pictureUriString = null,
            kudosReceived = 0,
            helpReceived = 0,
            followers = 0,
            following = 0)

    _uiState.value = _uiState.value.copy(isLoading = false, profile = fake, error = null)
  }

  /** Toggle loading state (useful in tests/previews). */
  fun setLoading(loading: Boolean) {
    _uiState.value = _uiState.value.copy(isLoading = loading)
  }

  /** Set or clear an error message (useful in tests/previews). */
  fun setError(message: String?) {
    _uiState.value = _uiState.value.copy(error = message)
  }

  /** Clear the currently shown profile and error. */
  fun clear() {
    _uiState.value = PublicProfileUiState()
  }
}
