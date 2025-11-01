package com.android.sample.ui.profile.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.sample.ui.profile.ProfileTestTags

/**
 * Reusable logout confirmation dialog used by ProfileScreen. Preserves test tags so UI tests remain
 * stable.
 */
@Composable
fun LogoutDialog(visible: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  if (!visible) return

  AlertDialog(
      modifier = Modifier.testTag(ProfileTestTags.LOG_OUT_DIALOG),
      onDismissRequest = onDismiss,
      title = { Text("Log out") },
      text = { Text("Are you sure you want to log out?") },
      confirmButton = {
        TextButton(
            onClick = onConfirm,
            modifier = Modifier.testTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM)) {
              Text("Log out")
            }
      },
      dismissButton = {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(ProfileTestTags.LOG_OUT_DIALOG_CANCEL)) {
              Text("Cancel")
            }
      })
}
