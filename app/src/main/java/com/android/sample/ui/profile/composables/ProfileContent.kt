package com.android.sample.ui.profile.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfileState

/**
 * Main column content of the Profile screen.
 *
 * Accepts callbacks and the screen state so it can be previewed / tested independently.
 */
@Composable
fun ProfileContent(
    state: ProfileState,
    onLogoutRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        state.errorMessage?.let {
            ErrorBanner(it)
            Spacer(modifier = Modifier.height(ProfileDimens.Vertical))
        }

        ProfileHeader(state = state)
        Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
        ProfileStats(state = state)
        Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
        ProfileInformation(state = state)
        Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
        ProfileActions(onLogoutClick = onLogoutRequested)
    }
}