package com.android.sample.ui.profile.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette

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
      title = {
        Text(text = "Log out", modifier = Modifier.testTag(ProfileTestTags.LOG_OUT_DIALOG_TITLE))
      },
      text = {
        Text(
            text = "Are you sure you want to log out?",
            modifier = Modifier.testTag(ProfileTestTags.LOG_OUT_DIALOG_MESSAGE))
      },
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

@Composable
fun EditProfileDialog(
    visible: Boolean,
    initialName: String,
    initialSection: String,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit,
    palette: AppPalette = appPalette()
) {
    var name by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }

    LaunchedEffect(visible) {
        if (visible) {
            name = initialName
            section = initialSection
        }
    }

    if (!visible) return
    AlertDialog(
      onDismissRequest = onCancel,
      title = { Text("Edit profile", color = palette.onBackground) },
      text = {
        Column {
          ProfileOutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = "Name",
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              palette = palette)

          Spacer(modifier = Modifier.height(UiDimens.SpacingSm))

          ProfileOutlinedTextField(
              value = section,
              onValueChange = { section = it },
              label = "Section",
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              palette = palette)
        }
      },
      confirmButton = {
        TextButton(onClick = { onSave(name, section) }) { Text("Save", color = palette.text) }
      },
      dismissButton = {
        OutlinedButton(onClick = onCancel) { Text("Cancel", color = palette.text) }
      })
}

/**
 * Small reusable text field used by the profile dialogs. Kept in this file to avoid creating a new
 * file and impacting coverage.
 */
@Composable
fun ProfileOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    palette: AppPalette = appPalette()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = palette.text) },
        singleLine = singleLine,
        modifier = modifier,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text,
                cursorColor = palette.accent,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.primary.copy(alpha = 0.6f),
                focusedContainerColor = palette.surface,
                unfocusedContainerColor = palette.surface))
}
