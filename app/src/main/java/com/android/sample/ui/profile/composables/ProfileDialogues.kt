package com.android.sample.ui.profile.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.sample.ui.getTextFieldColors
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.profile.UserSections
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette


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
      containerColor = palette.surface,
      modifier = Modifier.testTag(ProfileTestTags.EDIT_PROFILE_DIALOG),
      onDismissRequest = onCancel,
      title = {
        Text(
            "Edit profile",
            color = palette.onBackground,
            modifier = Modifier.testTag(ProfileTestTags.EDIT_PROFILE_DIALOG_TITLE),
        )
      },
      text = {
        Column {
          ProfileOutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = "Name",
              modifier = Modifier.fillMaxWidth().testTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT),
              singleLine = true,
              palette = palette)

          Spacer(modifier = Modifier.height(UiDimens.SpacingSm))

          SectionDropDown(
              selected = section,
              onSelectedChange = { section = it },
              options = UserSections.labels(),
              modifier =
                  Modifier.fillMaxWidth().testTag(ProfileTestTags.EDIT_PROFILE_SECTION_DROPDOWN),
              palette = palette)
        }
      },
      confirmButton = {
        TextButton(
            onClick = { onSave(name, section) },
            modifier = Modifier.testTag(ProfileTestTags.EDIT_PROFILE_DIALOG_SAVE_BUTTON)) {
              Text("Save", color = palette.text)
            }
      },
      dismissButton = {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.testTag(ProfileTestTags.EDIT_PROFILE_DIALOG_CANCEL_BUTTON)) {
              Text("Cancel", color = palette.text)
            }
      })
}

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
      colors = getTextFieldColors(palette)
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDropDown(
    selected: String,
    onSelectedChange: (String) -> Unit,
    options: List<String>,
    modifier: Modifier = Modifier,
    palette: AppPalette = appPalette()
) {
  var expanded by remember { mutableStateOf(false) }
  var showSheet by remember { mutableStateOf(false) }

  // When option count is large, prefer a bottom sheet for better UX on small screens.
  val sheetThreshold = 6
  val useSheet = options.size > sheetThreshold

  // Compute safe max height for dropdown popup (50% of screen).
  val configuration = LocalConfiguration.current
  val screenHeightDp = configuration.screenHeightDp
  val maxMenuHeight: Dp = with(LocalDensity.current) { (screenHeightDp.dp * 0.5f) }

  ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = {
        if (useSheet) {
          // open sheet instead of popup when list is long
          showSheet = true
        } else {
          expanded = !expanded
        }
      },
      modifier = modifier // preserve caller test tag (e.g. EDIT_PROFILE_SECTION_DROPDOWN)
      ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text("Section", color = palette.text) },
            readOnly = true,
            singleLine = true,
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded || showSheet)
            },
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.text,
                    unfocusedTextColor = palette.text,
                    cursorColor = palette.accent,
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.primary.copy(alpha = 0.6f),
                    focusedContainerColor = palette.surface,
                    unfocusedContainerColor = palette.surface),
            modifier = Modifier.menuAnchor().testTag(ProfileTestTags.SECTION_DROPDOWN))

        // Standard popup dropdown (scrollable)
        if (!useSheet) {
          ExposedDropdownMenu(
              expanded = expanded,
              onDismissRequest = { expanded = false },
              modifier = Modifier.heightIn(max = maxMenuHeight)) {
                LazyColumn(modifier = Modifier.heightIn(max = maxMenuHeight)) {
                  items(options) { option ->
                    val optionTag =
                        ProfileTestTags.SECTION_OPTION_PREFIX +
                            option.replace(Regex("\\s+"), "_").replace(Regex("[^A-Za-z0-9_]"), "")

                    DropdownMenuItem(
                        text = {
                          Box(
                              Modifier.fillMaxWidth()
                                  .padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(option, color = palette.text)
                              }
                        },
                        onClick = {
                          onSelectedChange(option)
                          expanded = false
                        },
                        modifier = Modifier.testTag(optionTag))
                  }
                }
              }
        }
      }

  // Modal bottom sheet for long lists (keeps content scrollable and visible)
  if (useSheet) {
    if (showSheet) {
      ModalBottomSheet(
          onDismissRequest = { showSheet = false },
          modifier = Modifier.testTag(ProfileTestTags.EDIT_PROFILE_SECTION_DROPDOWN)) {
            LazyColumn(
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(max = maxMenuHeight * 2) // allow a taller sheet if needed
                        .padding(vertical = 8.dp)) {
                  items(options) { option ->
                    val optionTag =
                        ProfileTestTags.SECTION_OPTION_PREFIX +
                            option.replace(Regex("\\s+"), "_").replace(Regex("[^A-Za-z0-9_]"), "")

                    Box(
                        Modifier.fillMaxWidth()
                            .clickable {
                              onSelectedChange(option)
                              showSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag(optionTag)) {
                          Text(option, color = palette.text)
                        }
                  }
                }
          }
    }
  }
}

@Preview(showBackground = true, name = "EditProfileDialog Preview")
@Composable
fun EditProfileDialogPreview() {
  EditProfileDialog(
      visible = true,
      initialName = "Alice",
      initialSection = "Computer Science",
      onSave = { _, _ -> },
      onCancel = {})
}
