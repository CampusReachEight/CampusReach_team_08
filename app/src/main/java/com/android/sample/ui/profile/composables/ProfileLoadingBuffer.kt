package com.android.sample.ui.profile.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Minimal loading placeholder used while profile data is loading. Keeps the same test tag
 * "profile_loading" so existing tests continue to work.
 */
@Composable
fun ProfileLoadingBuffer(modifier: Modifier = Modifier, tag: String = "profile_loading") {
  Box(modifier = modifier.fillMaxSize().testTag(tag), contentAlignment = Alignment.Center) {
    LoadingIndicator()
  }
}
