package com.android.sample.model.profile.setup

import com.android.sample.ui.profile.setup.SetupProfileState

interface SetupProfileRepository {
  suspend fun getProfile(): SetupProfileState

  suspend fun saveProfile(state: SetupProfileState)
}
