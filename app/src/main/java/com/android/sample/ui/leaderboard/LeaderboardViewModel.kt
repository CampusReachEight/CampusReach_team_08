package com.android.sample.ui.leaderboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileCache
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

/**
 * ViewModel for the Leaderboard screen.
 *
 * Responsibilities:
 * - Loading all user profiles from the repository
 * - Managing offline mode with cache fallback
 * - Error handling and loading state
 *
 * Search/filter/sort logic is delegated to [LeaderboardSearchFilterViewModel].
 *
 * This follows the same pattern as [RequestListViewModel] for consistency.
 */
class LeaderboardViewModel(
    val profileRepository: UserProfileRepository =
        UserProfileRepositoryFirestore(Firebase.firestore),
    private val profileCache: UserProfileCache? = null,
    private val verboseLogging: Boolean = false
) : ViewModel() {

  private val _state = MutableStateFlow(LeaderboardState())
  val state: StateFlow<LeaderboardState> = _state

  /**
   * Loads all user profiles from the repository. On failure, attempts to load from cache if
   * available.
   */
  fun loadProfiles() {
    _state.update { it.copy(isLoading = true) }
    viewModelScope.launch {
      try {
        val profiles = profileRepository.getAllUserProfiles()
        val positions = LeaderboardPositionCalculator.calculatePositions(profiles)
        _state.update {
          it.copy(
              profiles = profiles,
              positions = positions,
              offlineMode = false,
              isLoading = false,
              errorMessage = null)
        }

        // Save profiles to cache if available
        profileCache?.saveLeaderboard(profiles, FirebaseAuth.getInstance().currentUser?.uid)
      } catch (e: Exception) {
        if (verboseLogging) Log.e(LOG_TAG, "Failed to load profiles", e)

        // Try to load from cache if there's an error (e.g., no internet)
        val cachedProfiles = loadFromCache()
        if (cachedProfiles.isNotEmpty()) {
          val positions = LeaderboardPositionCalculator.calculatePositions(cachedProfiles)
          _state.update {
            it.copy(
                profiles = cachedProfiles,
                positions = positions,
                offlineMode = true,
                isLoading = false,
                errorMessage = null)
          }
          if (verboseLogging) Log.i(LOG_TAG, "Loaded ${cachedProfiles.size} profiles from cache")
        } else {
          // No cached data available
          val friendly =
              e.message?.takeIf { it.isNotBlank() } ?: "Failed to load profiles. Please try again."
          _state.update { it.copy(isLoading = false, errorMessage = friendly) }
        }
      }
    }
  }

  /**
   * Attempts to load profiles from cache.
   *
   * Uses the profile cache to retrieve the last saved leaderboard snapshot.
   */
  private fun loadFromCache(): List<UserProfile> {
    return profileCache?.loadLeaderboard() ?: emptyList()
  }

  /** Clears the current error message, if any. */
  fun clearError() {
    _state.update { it.copy(errorMessage = null) }
  }

  /** Refreshes the profile list from the server. */
  fun refresh() {
    loadProfiles()
  }

  @VisibleForTesting
  internal fun setOfflineMode(offline: Boolean) {
    _state.update { it.copy(offlineMode = offline) }
  }

  @VisibleForTesting
  internal fun setProfiles(profiles: List<UserProfile>) {
    val positions = LeaderboardPositionCalculator.calculatePositions(profiles)
    _state.update { it.copy(profiles = profiles, positions = positions) }
  }

  companion object {
    private const val LOG_TAG = "LeaderboardViewModel"
  }
}

/**
 * Factory for creating [LeaderboardViewModel] instances with custom dependencies.
 *
 * @param profileCache Optional cache for offline support
 */
class LeaderboardViewModelFactory(
    private val profileCache: UserProfileCache? = null,
    private val profileRepository: UserProfileRepository? = null
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return if (profileRepository != null) {
        LeaderboardViewModel(profileRepository = profileRepository, profileCache = profileCache)
            as T
      } else {
        LeaderboardViewModel(profileCache = profileCache) as T
      }
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}

/**
 * UI state for the leaderboard screen.
 *
 * @property profiles List of all loaded user profiles
 * @property positions Map of user ID to position (1-indexed) based on kudos ranking
 * @property isLoading Whether profiles are currently being loaded
 * @property errorMessage User-facing error message, if any
 * @property offlineMode Whether the data is from cache due to network issues
 */
data class LeaderboardState(
    val profiles: List<UserProfile> = emptyList(),
    val positions: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val offlineMode: Boolean = false
)
