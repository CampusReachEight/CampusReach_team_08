package com.android.sample.ui.profile.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
    modifier: Modifier = Modifier,
    onLogoutRequested: () -> Unit = {},
    onMyRequestAction: () -> Unit,
    onAcceptedRequestsAction: () -> Unit = {},
    onEditRequested: () -> Unit = {}
) {
  val scrollState = rememberScrollState()
  LazyColumn(modifier = modifier) {
    if (state.errorMessage != null) {
      item {
        ErrorBanner(state.errorMessage)
        Spacer(Modifier.height(ProfileDimens.Vertical))
      }
    }

    item {
      ProfileHeader(state = state, onEditRequested = onEditRequested)
      Spacer(Modifier.height(ProfileDimens.Horizontal))
    }

    item {
      ProfileStats(state)
      Spacer(Modifier.height(ProfileDimens.Horizontal))
    }

    item {
      ProfileInformation(state)
      Spacer(Modifier.height(ProfileDimens.Horizontal))
    }

    item {
      ProfileActions(
          onLogoutClick = onLogoutRequested,
          onMyRequestClick = onMyRequestAction,
          onAcceptedRequestsClick = onAcceptedRequestsAction)
    }
  }
}
