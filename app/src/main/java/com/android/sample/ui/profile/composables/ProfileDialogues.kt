package com.android.sample.ui.profile.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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

          SectionDropDown(
              selected = section,
              onSelectedChange = { section = it },
              options = com.android.sample.ui.profile.UserSections.labels(),
              modifier = Modifier.fillMaxWidth(),
              palette = palette
          )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDropDown (
    selected: String,
    onSelectedChange: (String) -> Unit,
    options: List<String>,
    modifier: Modifier = Modifier,
    palette: AppPalette = appPalette()
) {
    // Implementation of dropdown menu for selecting section
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = { },
            label = { Text("Section", color = palette.text) },
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text,
                cursorColor = palette.accent,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.primary.copy(alpha = 0.6f),
                focusedContainerColor = palette.surface,
                unfocusedContainerColor = palette.surface
            ),
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = palette.text) },
                    onClick = {
                        onSelectedChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
