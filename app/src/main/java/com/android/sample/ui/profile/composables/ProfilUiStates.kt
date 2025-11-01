package com.android.sample.ui.profile.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfileTestTags

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
  CircularProgressIndicator(modifier = modifier.testTag(ProfileTestTags.LOADING_INDICATOR))
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
  Text(
      text = message,
      color = MaterialTheme.colorScheme.error,
      modifier = modifier.fillMaxWidth().padding(ProfileDimens.Horizontal).testTag("profile_error"),
      textAlign = TextAlign.Center)
}
