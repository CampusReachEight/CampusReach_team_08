package com.android.sample.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FollowListState(
    val users: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class FollowListType {
  FOLLOWERS,
  FOLLOWING
}

open class FollowListViewModel(
    private val userId: String,
    private val listType: FollowListType,
    private val repository: UserProfileRepository =
        UserProfileRepositoryFirestore(FirebaseFirestore.getInstance())
) : ViewModel() {

  private val _state = MutableStateFlow(FollowListState())
  open val state: StateFlow<FollowListState> = _state.asStateFlow()

  open fun loadUsers() {
    _state.update { it.copy(isLoading = true, errorMessage = null) }

    viewModelScope.launch {
      try {
        // Get list of user IDs
        val userIds =
            if (listType == FollowListType.FOLLOWERS) {
              repository.getFollowerIds(userId)
            } else {
              repository.getFollowingIds(userId)
            }

        // Load each user's profile
        val users =
            userIds.mapNotNull { id ->
              try {
                repository.getUserProfile(id)
              } catch (e: Exception) {
                null // Skip users that can't be loaded
              }
            }

        _state.update { it.copy(users = users, isLoading = false, errorMessage = null) }
      } catch (e: Exception) {
        _state.update {
          it.copy(isLoading = false, errorMessage = "Failed to load users: ${e.message}")
        }
      }
    }
  }
}

class FollowListViewModelFactory(
    private val userId: String,
    private val listType: FollowListType,
    private val repository: UserProfileRepository =
        UserProfileRepositoryFirestore(FirebaseFirestore.getInstance())
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(FollowListViewModel::class.java)) {
      return FollowListViewModel(userId, listType, repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
