package com.android.sample.ui.myrequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.request.Request
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class MyRequestsViewModel( private val loadMyRequests: suspend () -> List<Request> ) : ViewModel()
{ private val _state = MutableStateFlow(MyRequestState.loading())
    open val state: StateFlow<MyRequestState> = _state
    init { refresh() } fun refresh() { _state.value = MyRequestState.loading()
    viewModelScope.launch { try { val requests = loadMyRequests()
        _state.value = MyRequestState(myRequests = requests) } catch (e: Exception) { _state.value = MyRequestState.withError(e.message ?: "Failed to load requests")
            }
        }
    }
}