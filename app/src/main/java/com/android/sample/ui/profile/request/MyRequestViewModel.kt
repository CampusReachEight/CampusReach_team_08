package com.android.sample.ui.myrequests

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.request.Request
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class MyRequestsViewModel(
    private val loadMyRequests: suspend () -> List<Request> = { emptyList() }
) : ViewModel() {
  init {
    Log.d("MyRequestsViewModel", "ViewModel initialized")
    Log.d("MyRequestsViewModel", "loadMyRequests function: $loadMyRequests")
    refresh()
  }

  private val _state = MutableStateFlow(MyRequestState.loading())
  open val state: StateFlow<MyRequestState> = _state

  init {
    refresh()
  }

  fun refresh() {
    Log.d("MyRequestsViewModel", "refresh() called")
    _state.value = MyRequestState.loading()

    viewModelScope.launch {
      try {
        Log.d("MyRequestsViewModel", "Loading requests...")
        val requests = loadMyRequests()
        Log.d("MyRequestsViewModel", "Loaded ${requests.size} requests")
        _state.value = MyRequestState(myRequests = requests)
      } catch (e: Exception) {
        Log.e("MyRequestsViewModel", "Error loading requests", e)
        _state.value = MyRequestState.withError(e.message ?: "Failed to load requests")
      }
    }
  }
}
