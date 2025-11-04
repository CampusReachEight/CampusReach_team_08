package com.android.sample.ui.request

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel backing the Request List screen.
 *
 * Responsibility kept focused on data management: loading requests, profile icons, error handling,
 * and navigation. Filtering and search are delegated to RequestSearchFilterViewModel.
 */
class RequestListViewModel(
    val requestRepository: RequestRepository = RequestRepositoryFirestore(Firebase.firestore),
    val profileRepository: UserProfileRepository =
        UserProfileRepositoryFirestore(Firebase.firestore),
    val verboseLogging: Boolean = false
) : ViewModel() {
  private val _state = MutableStateFlow(RequestListState())
  val state: StateFlow<RequestListState> = _state

  private val _profileIcons = MutableStateFlow<Map<String, Bitmap?>>(emptyMap())
  val profileIcons: StateFlow<Map<String, Bitmap?>> = _profileIcons

  // Base list as a flow
  private val allRequests = state.map { it.requests }.distinctUntilChanged()

  /** Loads all requests and their profile icons. Leaves previous list intact on error. */
  fun loadRequests() {
    _state.update { it.copy(isLoading = true) }
    viewModelScope.launch {
      try {
        val requests = requestRepository.getAllRequests()
        _state.update { it.copy(requests = requests, isLoading = false, errorMessage = null) }
        requests.forEach { loadProfileImage(it.creatorId) }
      } catch (e: Exception) {
        if (verboseLogging) Log.e("RequestListViewModel", "Failed to load requests", e)
        val friendly =
            e.message?.takeIf { it.isNotBlank() } ?: "Failed to load requests. Please try again."
        _state.update { it.copy(isLoading = false, errorMessage = friendly) }
      }
    }
  }

  /** Loads a profile image. Failures are non-fatal: logged and stored as null. */
  fun loadProfileImage(userId: String) {
    if (_profileIcons.value.containsKey(userId)) return
    viewModelScope.launch {
      try {
        val profile = profileRepository.getUserProfile(userId)
        val uri = profile.photo
        val bmp = if (uri != null) loadBitmapFromUri(uri) else null
        _profileIcons.value = _profileIcons.value + (userId to bmp)
      } catch (e: Exception) {
        if (verboseLogging) Log.e("RequestListViewModel", "Failed to load profile for $userId", e)
        _profileIcons.value = _profileIcons.value + (userId to null)
      }
    }
  }

  private suspend fun loadBitmapFromUri(uri: Uri): Bitmap? =
      withContext(Dispatchers.IO) {
        try {
          when (uri.scheme?.lowercase()) {
            "http",
            "https" ->
                URL(uri.toString()).openStream().use { input -> BitmapFactory.decodeStream(input) }
            else ->
                null // Unsupported without a Context (e.g., content://). Could be extended later.
          }
        } catch (_: Exception) {
          null
        }
      }
  /** Clears the current error message, if any. */
  fun clearError() {
    _state.update { it.copy(errorMessage = null) }
  }

  /** Centralizes navigation decision based on current user id. */
  fun handleRequestClick(
      request: Request,
      onNavigateEdit: (String) -> Unit,
      onNavigateAccept: (String) -> Unit
  ) {
    Firebase.auth.currentUser?.uid?.let { id ->
      if (request.creatorId == id) {
        onNavigateEdit(request.requestId)
      } else {
        onNavigateAccept(request.requestId)
      }
    }
  }
}

/** UI state for the request list screen. */
data class RequestListState(
    val requests: List<Request> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
