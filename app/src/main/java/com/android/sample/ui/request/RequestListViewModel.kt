package com.android.sample.ui.request

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RequestListViewModel(
    val requestRepository: RequestRepository = RequestRepositoryFirestore(Firebase.firestore),
    val profileRepository: UserProfileRepository =
        UserProfileRepositoryFirestore(Firebase.firestore),
) : ViewModel() {
  private val _state = MutableStateFlow(RequestListState())
  val state: StateFlow<RequestListState> = _state

  private val _profileIcons = MutableStateFlow<Map<String, Bitmap?>>(emptyMap())
  val profileIcons: StateFlow<Map<String, Bitmap?>> = _profileIcons

  fun loadRequests() {
    _state.value = RequestListState(isLoading = true)
    viewModelScope.launch {
      try {
        val requests = requestRepository.getAllRequests()
        _state.value = _state.value.copy(requests = requests, isLoading = false)
        requests.forEach { loadProfileImage(it.creatorId) }
      } catch (e: Exception) {
        _state.value = RequestListState(errorMessage = e.message)
      }
    }
  }

  fun loadProfileImage(userId: String) {
    if (_profileIcons.value.containsKey(userId)) return
    viewModelScope.launch {
      try {
        val profile = profileRepository.getUserProfile(userId)
        val uri = profile.photo
        val bmp = if (uri != null) loadBitmapFromUri(uri) else null
        _profileIcons.value = _profileIcons.value + (userId to bmp)
      } catch (e: Exception) {
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
}

data class RequestListState(
    val requests: List<Request> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
