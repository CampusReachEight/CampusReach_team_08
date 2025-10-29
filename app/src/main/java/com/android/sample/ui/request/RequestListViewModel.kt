package com.android.sample.ui.request

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel backing the Request List screen.
 *
 * Semantics:
 * - Filters: OR within a facet; AND across facets. Facet counts are self-excluding against other
 *   active facets.
 * - Errors: exposed as `errorMessage` and can be cleared with [clearError]. Non-critical image load
 *   errors are logged only.
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

  // Facet selection state
  private val _selectedTypes = MutableStateFlow<Set<RequestType>>(emptySet())
  val selectedTypes: StateFlow<Set<RequestType>> = _selectedTypes

  private val _selectedStatuses = MutableStateFlow<Set<RequestStatus>>(emptySet())
  val selectedStatuses: StateFlow<Set<RequestStatus>> = _selectedStatuses

  private val _selectedTags = MutableStateFlow<Set<Tags>>(emptySet())
  val selectedTags: StateFlow<Set<Tags>> = _selectedTags

  // Base list as a flow
  private val allRequests = state.map { it.requests }.distinctUntilChanged()

  /** Filtered list applying AND across facets (OR within each facet). */
  val filteredRequests: StateFlow<List<Request>> =
      combine(allRequests, selectedTypes, selectedStatuses, selectedTags) {
              requests,
              types,
              statuses,
              tags ->
            if (requests.isEmpty()) return@combine emptyList()
            requests.filter { req ->
              val typeOk = types.isEmpty() || req.requestType.any { it in types }
              val statusOk = statuses.isEmpty() || req.status in statuses
              val tagsOk = tags.isEmpty() || req.tags.any { it in tags }
              typeOk && statusOk && tagsOk
            }
          }
          .distinctUntilChanged()
          .stateInEager(viewModelScope, emptyList())

  /** Facet counts with self-exclusion across other active facets. */
  val typeCounts: StateFlow<Map<RequestType, Int>> =
      combine(allRequests, selectedStatuses, selectedTags) { requests, statuses, tags ->
            val base =
                requests.filter { req ->
                  val statusOk = statuses.isEmpty() || req.status in statuses
                  val tagsOk = tags.isEmpty() || req.tags.any { it in tags }
                  statusOk && tagsOk
                }
            val counts = mutableMapOf<RequestType, Int>()
            // initialize all enum values with 0 to keep visibility
            RequestType.entries.forEach { counts[it] = 0 }
            base.forEach { req ->
              req.requestType.forEach { rt -> counts[rt] = (counts[rt] ?: 0) + 1 }
            }
            counts.toMap()
          }
          .distinctUntilChanged()
          .stateInEager(viewModelScope, emptyMap())

  val statusCounts: StateFlow<Map<RequestStatus, Int>> =
      combine(allRequests, selectedTypes, selectedTags) { requests, types, tags ->
            val base =
                requests.filter { req ->
                  val typeOk = types.isEmpty() || req.requestType.any { it in types }
                  val tagsOk = tags.isEmpty() || req.tags.any { it in tags }
                  typeOk && tagsOk
                }
            val counts = mutableMapOf<RequestStatus, Int>()
            RequestStatus.entries.forEach { counts[it] = 0 }
            base.forEach { req -> counts[req.status] = (counts[req.status] ?: 0) + 1 }
            counts.toMap()
          }
          .distinctUntilChanged()
          .stateInEager(viewModelScope, emptyMap())

  val tagCounts: StateFlow<Map<Tags, Int>> =
      combine(allRequests, selectedTypes, selectedStatuses) { requests, types, statuses ->
            val base =
                requests.filter { req ->
                  val typeOk = types.isEmpty() || req.requestType.any { it in types }
                  val statusOk = statuses.isEmpty() || req.status in statuses
                  typeOk && statusOk
                }
            val counts = mutableMapOf<Tags, Int>()
            Tags.entries.forEach { counts[it] = 0 }
            base.forEach { req -> req.tags.forEach { tag -> counts[tag] = (counts[tag] ?: 0) + 1 } }
            counts.toMap()
          }
          .distinctUntilChanged()
          .stateInEager(viewModelScope, emptyMap())

  /** Toggle a request type filter. */
  fun toggleType(type: RequestType) {
    _selectedTypes.update { current -> if (type in current) current - type else current + type }
  }

  /** Toggle a request status filter. */
  fun toggleStatus(status: RequestStatus) {
    _selectedStatuses.update { current ->
      if (status in current) current - status else current + status
    }
  }

  /** Toggle a tag filter. */
  fun toggleTag(tag: Tags) {
    _selectedTags.update { current -> if (tag in current) current - tag else current + tag }
  }

  /** Clears all active filters. */
  fun clearAllFilters() {
    _selectedTypes.update { emptySet() }
    _selectedStatuses.update { emptySet() }
    _selectedTags.update { emptySet() }
  }

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
        val bmp = profile.photo
        _profileIcons.value = _profileIcons.value + (userId to bmp)
      } catch (e: Exception) {
        if (verboseLogging) Log.e("RequestListViewModel", "Failed to load profile for $userId", e)
        _profileIcons.value = _profileIcons.value + (userId to null)
      }
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

/**
 * Collect this Flow eagerly into a StateFlow with the given [scope] and [initial] value.
 *
 * This avoids repeating `stateIn(scope, SharingStarted.Eagerly, initial)`.
 */
fun <T> Flow<T>.stateInEager(scope: CoroutineScope, initial: T): StateFlow<T> =
    this.stateIn(scope, SharingStarted.Eagerly, initial)
