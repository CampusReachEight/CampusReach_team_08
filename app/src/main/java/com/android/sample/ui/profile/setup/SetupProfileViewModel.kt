package com.android.sample.ui.profile.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.setup.SetupProfileRepository
import com.android.sample.model.profile.setup.SetupProfileRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SetupProfileViewModel(
    private val repository: SetupProfileRepository = SetupProfileRepositoryImpl()
) : ViewModel() {

  private val _state = MutableStateFlow(SetupProfileState.empty())
  val state: StateFlow<SetupProfileState> = _state.asStateFlow()

  init {
    loadProfile()
  }

  fun onEvent(event: SetupProfileEvent) {
    when (event) {
      is SetupProfileEvent.Load -> loadProfile()
      is SetupProfileEvent.Save -> saveProfile()
    }
  }

  private fun loadProfile() {
    viewModelScope.launch {
      _state.value = _state.value.copy(isLoading = true, errorMessage = null)
      try {
        val profile = repository.getProfile()
        _state.value = profile.copy(isLoading = false, errorMessage = null)
      } catch (t: Throwable) {
        _state.value = _state.value.copy(isLoading = false, errorMessage = t.message)
      }
    }
  }

  private fun saveProfile() {
    viewModelScope.launch {
      _state.value = _state.value.copy(isSaving = true, saveError = null, saved = false)
      try {
        val toSave = _state.value
        repository.saveProfile(toSave)
        _state.value = _state.value.copy(isSaving = false, saved = true)
      } catch (t: Throwable) {
        _state.value = _state.value.copy(isSaving = false, saveError = t.message)
      }
    }
  }
}
