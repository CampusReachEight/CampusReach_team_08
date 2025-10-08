package com.android.sample.ui.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class ProfileViewModel(initialState: ProfileState = ProfileState.default()) : ViewModel() {
  private val _state = mutableStateOf(initialState)
  val state: State<ProfileState> = _state
}
