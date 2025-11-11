package com.android.sample.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.android.sample.ui.profile.composables.EditProfileDialog
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EditProfileUiTests {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun dialog_displays_title_name_and_section() {
    composeTestRule.setContent {
      MaterialTheme {
        Box(modifier = androidx.compose.ui.Modifier.size(360.dp, 640.dp)) {
          EditProfileDialog(
              visible = true,
              initialName = "Alice",
              initialSection = "Computer Science",
              onSave = { _, _ -> },
              onCancel = {})
        }
      }
    }

    composeTestRule.onNodeWithTag(ProfileTestTags.EDIT_PROFILE_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.EDIT_PROFILE_DIALOG_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.SECTION_DROPDOWN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileTestTags.SECTION_DROPDOWN)
        .assert(hasText("Computer Science"))
  }

  @Test
  fun editing_name_and_pressing_save_invokes_onSave_with_values() {
    val saved = AtomicReference<Pair<String, String>?>(null)

    composeTestRule.setContent {
      MaterialTheme {
        Box(modifier = androidx.compose.ui.Modifier.size(360.dp, 640.dp)) {
          EditProfileDialog(
              visible = true,
              initialName = "Alice",
              initialSection = "Mathematics",
              onSave = { n, s -> saved.set(n to s) },
              onCancel = {})
        }
      }
    }

    composeTestRule
        .onNodeWithTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT)
        .performTextReplacement("Bob")
    composeTestRule.onNodeWithTag(ProfileTestTags.EDIT_PROFILE_DIALOG_SAVE_BUTTON).performClick()

    val pair = saved.get()
    assertNotNull("onSave should be called", pair)
    assertEquals("Bob", pair!!.first)
    assertEquals("Mathematics", pair.second)
  }

  @Test
  fun selecting_architecture_updates_textfield() {
    composeTestRule.setContent {
      MaterialTheme {
        Box(modifier = androidx.compose.ui.Modifier.size(360.dp, 640.dp)) {
          EditProfileDialog(
              visible = true,
              initialName = "Alice",
              initialSection = "Computer Science",
              onSave = { _, _ -> },
              onCancel = {})
        }
      }
    }

    // open dropdown
    composeTestRule.onNodeWithTag(ProfileTestTags.EDIT_PROFILE_SECTION_DROPDOWN).performClick()
    composeTestRule.waitForIdle()

    // click the Architecture option by its generated test tag
    val targetLabel = "Architecture"
    val optionTag =
        ProfileTestTags.SECTION_OPTION_PREFIX +
            targetLabel.replace(Regex("\\s+"), "_").replace(Regex("[^A-Za-z0-9_]"), "")

    composeTestRule.onNodeWithTag(optionTag).assertExists().performClick()

    // ensure UI updated
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ProfileTestTags.SECTION_DROPDOWN).assert(hasText(targetLabel))
  }

  @Test
  fun all_section_options_have_expected_tags_and_are_present_in_menu() {
    composeTestRule.setContent {
      MaterialTheme {
        Box(modifier = androidx.compose.ui.Modifier.size(360.dp, 640.dp)) {
          EditProfileDialog(
              visible = true,
              initialName = "Alice",
              initialSection = "Computer Science",
              onSave = { _, _ -> },
              onCancel = {})
        }
      }
    }

    composeTestRule.onNodeWithTag(ProfileTestTags.EDIT_PROFILE_SECTION_DROPDOWN).performClick()

    UserSections.labels().forEach { label ->
      val optionTag =
          ProfileTestTags.SECTION_OPTION_PREFIX +
              label.replace(Regex("\\s+"), "_").replace(Regex("[^A-Za-z0-9_]"), "")
      composeTestRule.onNodeWithTag(optionTag).assertExists()
    }
  }

  @Test
  fun pressing_cancel_invokes_onCancel() {
    val cancelled = AtomicBoolean(false)

    composeTestRule.setContent {
      MaterialTheme {
        Box(modifier = androidx.compose.ui.Modifier.size(360.dp, 640.dp)) {
          EditProfileDialog(
              visible = true,
              initialName = "Alice",
              initialSection = "Physics",
              onSave = { _, _ -> },
              onCancel = { cancelled.set(true) })
        }
      }
    }

    composeTestRule.onNodeWithTag(ProfileTestTags.EDIT_PROFILE_DIALOG_CANCEL_BUTTON).performClick()
    assertTrue("onCancel should be invoked", cancelled.get())
  }

  //  @Test
  //  fun editFlow_save_updates_viewModel_and_closes_dialog() {
  //    val vm = ProfileViewModel(initialState = ProfileState.default())
  //
  //    composeTestRule.setContent {
  //      ProfileScreen(viewModel = vm) // deterministic, no auth listener needed
  //    }
  //    // enter edit mode
  //    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER_EDIT_BUTTON).performClick()
  //
  //    // change name
  //    composeTestRule
  //        .onNodeWithTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT)
  //        .performTextReplacement("Bob")
  //
  //    // open section dropdown and pick "Architecture"
  //    composeTestRule.onNodeWithTag(ProfileTestTags.EDIT_PROFILE_SECTION_DROPDOWN).performClick()
  //    composeTestRule.waitForIdle()
  //    val target = "Architecture"
  //    val optionTag =
  //        ProfileTestTags.SECTION_OPTION_PREFIX +
  //            target.replace(Regex("\\s+"), "_").replace(Regex("[^A-Za-z0-9_]"), "")
  //    composeTestRule.onNodeWithTag(optionTag).assertExists().performClick()
  //    composeTestRule.waitForIdle()
  //
  //    // save
  //
  // composeTestRule.onNodeWithTag(ProfileTestTags.EDIT_PROFILE_DIALOG_SAVE_BUTTON).performClick()
  //
  //    // verify viewModel updated and dialog closed
  //    composeTestRule.runOnIdle {
  //      assertEquals("Bob", vm.state.value.userName)
  //      assertEquals(target, vm.state.value.userSection)
  //      assertFalse(vm.state.value.isEditMode)
  //    }
  //  }
}
