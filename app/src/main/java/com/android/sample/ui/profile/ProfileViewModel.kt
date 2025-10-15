package com.android.sample.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(initialState: ProfileState = ProfileState.default()) : ViewModel() {
  private val _state = MutableStateFlow(initialState)
  val state: StateFlow<ProfileState> = _state

  fun enterEditMode() {
    _state.value = _state.value.copy(isEditMode = true)
  }

  fun exitEditMode() {
    _state.value = _state.value.copy(isEditMode = false)
  }

  fun updateProfile(name: String, section: String) {
    _state.value = _state.value.copy(
      userName = name,
      section = section
    )
    exitEditMode()
  }
}
