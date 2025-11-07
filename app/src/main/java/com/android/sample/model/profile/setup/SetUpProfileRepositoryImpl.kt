package com.android.sample.model.profile.setup

import com.android.sample.ui.profile.setup.SetupProfileState
import kotlinx.coroutines.delay

class SetupProfileRepositoryImpl : SetupProfileRepository {

    // In-memory placeholder. Replace with real data source (Room, remote API, etc.)
    private var stored: SetupProfileState = SetupProfileState.Companion.default()

    override suspend fun getProfile(): SetupProfileState {
        // simulate latency
        delay(200)
        return stored
    }

    override suspend fun saveProfile(state: SetupProfileState) {
        // simulate work + possible validation
        delay(250)
        if (state.userName.isBlank()) throw IllegalArgumentException("Name cannot be empty")
        stored = state.copy(saved = true)
    }
}
