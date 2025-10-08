package com.android.sample.ui.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class ProfileViewModel : ViewModel() {
    // Current state of the profile screen (start with mock data)
    private val _state = mutableStateOf(
        ProfileState(
            userName = "John Doe",
            userEmail = "john.doe@epfl.ch",
            profileId = "360553",
            kudosReceived = 100,
            helpReceived = 24,
            followers = 2,
            following = 3,
            arrivalDate = "01/01/2026",
            section = "Informatics",
            isLoading = false
        )
    )
    // Expose as immutable State (best practice)
    val state: State<ProfileState> = _state
}
