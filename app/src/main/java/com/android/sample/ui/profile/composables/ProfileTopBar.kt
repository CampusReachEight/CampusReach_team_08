package com.android.sample.ui.profile.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.sample.ui.profile.ProfileTestTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(onBackClick: () -> Unit) {
  var isClicked by remember { mutableStateOf(false) }

  TopAppBar(
      title = { Text("Profile") },
      navigationIcon = {
        IconButton(
            onClick = {
              if (!isClicked) {
                isClicked = true
                onBackClick()
              }
            },
            enabled = !isClicked,
            modifier = Modifier.testTag(ProfileTestTags.PROFILE_TOP_BAR_BACK_BUTTON)) {
              Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
      })
}
