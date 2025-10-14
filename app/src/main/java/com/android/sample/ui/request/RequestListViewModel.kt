package com.android.sample.ui.request

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RequestListViewModel(
    val requestRepository: RequestRepository = RequestRepositoryFirestore(Firebase.firestore),
    val profileRepository: UserProfileRepository = UserProfileRepositoryFirestore(Firebase.firestore),
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
                val bmp = profile.photo
                _profileIcons.value = _profileIcons.value + (userId to bmp)
            } catch (e: Exception) {
                _profileIcons.value = _profileIcons.value + (userId to null)
            }
        }
    }
}


data class RequestListState(
    val requests: List<Request> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)