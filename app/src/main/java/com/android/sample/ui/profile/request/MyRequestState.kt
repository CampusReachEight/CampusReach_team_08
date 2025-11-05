package com.android.sample.ui.myrequests

import com.android.sample.model.request.Request

data class MyRequestState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val myRequests: List<Request> = emptyList()
) {
    companion object {
        fun empty() = MyRequestState()
        fun loading() = MyRequestState(isLoading = true)
        fun withError(msg: String) = MyRequestState(errorMessage = msg)
    }
}