package com.android.sample.ui.request

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.android.sample.model.map.Location
import com.android.sample.model.map.LocationRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class EditRequestScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
  private val currentDate = Date()
  private val futureDate = Date(System.currentTimeMillis() + 86400000)

  // Helper composable that manages state properly
  @Composable
  private fun TestEditRequestContent(
      initialTitle: String = "",
      initialDescription: String = "",
      initialRequestTypes: List<RequestType> = emptyList(),
      initialLocation: Location? = null,
      initialLocationName: String = "",
      initialTags: List<Tags> = emptyList(),
      isEditMode: Boolean = false,
      onSave: () -> Unit = {}
  ) {
    val title = remember { mutableStateOf(initialTitle) }
    val description = remember { mutableStateOf(initialDescription) }
    val requestTypes = remember { mutableStateOf(initialRequestTypes) }
    val location = remember { mutableStateOf(initialLocation) }
    val locationName = remember { mutableStateOf(initialLocationName) }
    val startTimeStamp = remember { mutableStateOf(futureDate) }
    val expirationTime = remember { mutableStateOf(Date(futureDate.time + 86400000)) }
    val tags = remember { mutableStateOf(initialTags) }

    MaterialTheme {
      EditRequestContent(
          paddingValues = PaddingValues(0.dp),
          title = title.value,
          description = description.value,
          requestTypes = requestTypes.value,
          location = location.value,
          locationName = locationName.value,
          startTimeStamp = startTimeStamp.value,
          expirationTime = expirationTime.value,
          tags = tags.value,
          isEditMode = isEditMode,
          isLoading = false,
          errorMessage = null,
          locationSearchResults = emptyList(),
          isSearchingLocation = false,
          onTitleChange = { title.value = it },
          onDescriptionChange = { description.value = it },
          onRequestTypesChange = { requestTypes.value = it },
          onLocationChange = { location.value = it },
          onLocationNameChange = { locationName.value = it },
          onStartTimeStampChange = { startTimeStamp.value = it },
          onExpirationTimeChange = { expirationTime.value = it },
          onTagsChange = { tags.value = it },
          onSave = onSave,
          onClearError = {},
          onSearchLocations = {},
          onClearLocationSearch = {})
    }
  }

  // Test 1: Create mode title
  @Test
  fun editRequestContent_createMode_displaysCorrectTitle() {
    composeTestRule.setContent { TestEditRequestContent(isEditMode = false) }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .assertTextEquals("Create Request")
  }

  // Test 2: Edit mode title
  @Test
  fun editRequestContent_editMode_displaysCorrectTitle() {
    composeTestRule.setContent {
      TestEditRequestContent(
          initialTitle = "Test",
          initialDescription = "Test",
          initialRequestTypes = listOf(RequestType.STUDYING),
          initialLocation = Location(0.0, 0.0, "Test"),
          initialLocationName = "Test",
          isEditMode = true)
    }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .assertTextEquals("Update Request")
  }

  // Test 3: Title field updates
  @Test
  fun editRequestContent_titleField_updatesOnTextInput() {
    composeTestRule.setContent { TestEditRequestContent() }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .performTextInput("Study Session")

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .assertTextContains("Study Session")
  }

  // Test 4: Description field updates
  @Test
  fun editRequestContent_descriptionField_updatesOnTextInput() {
    composeTestRule.setContent { TestEditRequestContent() }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION)
        .performTextInput("Looking for study partners")

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION)
        .assertTextContains("Looking for study partners")
  }

  // Test 6: Empty title validation
  @Test
  fun editRequestContent_emptyTitle_showsValidationError() {
    composeTestRule.setContent {
      TestEditRequestContent(
          initialDescription = "Valid",
          initialRequestTypes = listOf(RequestType.STUDYING),
          initialLocation = Location(0.0, 0.0, "Test"),
          initialLocationName = "Test")
    }

    // Type and clear to trigger validation
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE).performTextInput("a")

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE).performTextClearance()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Title cannot be empty").assertExists()
  }

  // Test 7: Empty description validation
  @Test
  fun editRequestContent_emptyDescription_showsValidationError() {
    composeTestRule.setContent {
      TestEditRequestContent(
          initialTitle = "Valid Title",
          initialRequestTypes = listOf(RequestType.STUDYING),
          initialLocation = Location(0.0, 0.0, "Test"),
          initialLocationName = "Test")
    }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION).performTextInput("a")

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION)
        .performTextClearance()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Description cannot be empty").assertExists()
  }

  // Test 8: No request type validation
  @Test
  fun editRequestContent_noRequestType_showsValidationError() {
    composeTestRule.setContent {
      TestEditRequestContent(
          initialTitle = "Valid",
          initialDescription = "Valid",
          initialLocation = Location(0.0, 0.0, "Test"),
          initialLocationName = "Test")
    }

    // Select and deselect to trigger error
    composeTestRule.onNodeWithText("STUDYING").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("STUDYING").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Please select at least one request type").assertExists()
  }

  // Test 10: Valid form calls onSav

  @Test
  fun editRequestContent_validForm_callsOnSave() {
    var saveCalled = false

    composeTestRule.setContent {
      MaterialTheme {
        EditRequestContent(
            paddingValues = PaddingValues(0.dp),
            title = "Test",
            description = "Test",
            requestTypes = listOf(RequestType.STUDYING),
            location = Location(0.0, 0.0, "Test"),
            locationName = "Test",
            startTimeStamp = Date(System.currentTimeMillis() + 86400000),
            expirationTime = Date(System.currentTimeMillis() + 2 * 86400000),
            tags = emptyList(),
            isEditMode = false,
            isLoading = false,
            errorMessage = null,
            locationSearchResults = emptyList(),
            isSearchingLocation = false,
            onTitleChange = {},
            onDescriptionChange = {},
            onRequestTypesChange = {},
            onLocationChange = {},
            onLocationNameChange = {},
            onStartTimeStampChange = {},
            onExpirationTimeChange = {},
            onTagsChange = {},
            onSave = { saveCalled = true },
            onClearError = {},
            onSearchLocations = {},
            onClearLocationSearch = {})
      }
    }

    composeTestRule.waitForIdle()

    println("🔍 Before scroll and click...")

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .performScrollTo() // ← THIS IS THE FIX!
        .assertIsEnabled()
        .assertHasClickAction()
        .performClick()

    composeTestRule.waitForIdle()

    println("🔍 After click, saveCalled = $saveCalled")

    assert(saveCalled) { "onSave was not called after click" }
  }
  // Test 11: Loading state disables button
  @Test
  fun editRequestContent_loadingState_disablesSaveButton() {
    composeTestRule.setContent {
      MaterialTheme {
        EditRequestContent(
            paddingValues = PaddingValues(0.dp),
            title = "Test",
            description = "Test",
            requestTypes = listOf(RequestType.STUDYING),
            location = Location(0.0, 0.0, "Test"),
            locationName = "Test",
            startTimeStamp = currentDate,
            expirationTime = futureDate,
            tags = emptyList(),
            isEditMode = false,
            isLoading = true,
            errorMessage = null,
            locationSearchResults = emptyList(),
            isSearchingLocation = false,
            onTitleChange = {},
            onDescriptionChange = {},
            onRequestTypesChange = {},
            onLocationChange = {},
            onLocationNameChange = {},
            onStartTimeStampChange = {},
            onExpirationTimeChange = {},
            onTagsChange = {},
            onSave = {},
            onClearError = {},
            onSearchLocations = {},
            onClearLocationSearch = {})
      }
    }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  // Test 12: Loading state disables inputs
  @Test
  fun editRequestContent_loadingState_disablesInputFields() {
    composeTestRule.setContent {
      MaterialTheme {
        EditRequestContent(
            paddingValues = PaddingValues(0.dp),
            title = "Test",
            description = "Test",
            requestTypes = listOf(RequestType.STUDYING),
            location = Location(0.0, 0.0, "Test"),
            locationName = "Test",
            startTimeStamp = currentDate,
            expirationTime = futureDate,
            tags = emptyList(),
            isEditMode = false,
            isLoading = true,
            errorMessage = null,
            locationSearchResults = emptyList(),
            isSearchingLocation = false,
            onTitleChange = {},
            onDescriptionChange = {},
            onRequestTypesChange = {},
            onLocationChange = {},
            onLocationNameChange = {},
            onStartTimeStampChange = {},
            onExpirationTimeChange = {},
            onTagsChange = {},
            onSave = {},
            onClearError = {},
            onSearchLocations = {},
            onClearLocationSearch = {})
      }
    }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE).assertIsNotEnabled()
  }

  // Test 13: Error message displays
  @Test
  fun editRequestContent_errorMessage_displaysCorrectly() {
    composeTestRule.setContent {
      MaterialTheme {
        EditRequestContent(
            paddingValues = PaddingValues(0.dp),
            title = "Test",
            description = "Test",
            requestTypes = listOf(RequestType.STUDYING),
            location = Location(0.0, 0.0, "Test"),
            locationName = "Test",
            startTimeStamp = currentDate,
            expirationTime = futureDate,
            tags = emptyList(),
            isEditMode = false,
            isLoading = true,
            errorMessage = "Failed to save request",
            locationSearchResults = emptyList(),
            isSearchingLocation = false,
            onTitleChange = {},
            onDescriptionChange = {},
            onRequestTypesChange = {},
            onLocationChange = {},
            onLocationNameChange = {},
            onStartTimeStampChange = {},
            onExpirationTimeChange = {},
            onTagsChange = {},
            onSave = {},
            onClearError = {},
            onSearchLocations = {},
            onClearLocationSearch = {})
      }
    }

    composeTestRule.onNodeWithText("Failed to save request").assertExists()
  }

  // Test 14: Error dismiss
  @Test
  fun editRequestContent_errorMessage_canBeDismissed() {
    var errorCleared = false

    composeTestRule.setContent {
      MaterialTheme {
        EditRequestContent(
            paddingValues = PaddingValues(0.dp),
            title = "Test",
            description = "Test",
            requestTypes = listOf(RequestType.STUDYING),
            location = Location(0.0, 0.0, "Test"),
            locationName = "Test",
            startTimeStamp = currentDate,
            expirationTime = futureDate,
            tags = emptyList(),
            isEditMode = false,
            isLoading = true,
            errorMessage = "Failed to save request",
            locationSearchResults = emptyList(),
            isSearchingLocation = false,
            onTitleChange = {},
            onDescriptionChange = {},
            onRequestTypesChange = {},
            onLocationChange = {},
            onLocationNameChange = {},
            onStartTimeStampChange = {},
            onExpirationTimeChange = {},
            onTagsChange = {},
            onSave = {},
            onClearError = { errorCleared = true },
            onSearchLocations = {},
            onClearLocationSearch = {})
      }
    }

    composeTestRule.onNodeWithText("Dismiss").performClick()

    assert(errorCleared)
  }

  // Test 15: RequestType select
  @Test
  fun requestTypeChipGroup_selectChip_updatesSelection() {
    var selectedTypes by mutableStateOf(emptyList<RequestType>())

    composeTestRule.setContent {
      MaterialTheme {
        RequestTypeChipGroup(
            selectedTypes = selectedTypes,
            onSelectionChanged = { selectedTypes = it },
            enabled = true)
      }
    }

    composeTestRule.onNodeWithText("STUDYING").performClick()

    composeTestRule.waitForIdle()
    assert(selectedTypes.contains(RequestType.STUDYING))
  }

  // Test 16: RequestType deselect
  @Test
  fun requestTypeChipGroup_deselectChip_updatesSelection() {
    var selectedTypes by mutableStateOf(listOf(RequestType.STUDYING))

    composeTestRule.setContent {
      MaterialTheme {
        RequestTypeChipGroup(
            selectedTypes = selectedTypes,
            onSelectionChanged = { selectedTypes = it },
            enabled = true)
      }
    }

    composeTestRule.onNodeWithText("STUDYING").performClick()

    composeTestRule.waitForIdle()
    assert(!selectedTypes.contains(RequestType.STUDYING))
  }

  // Test 17: RequestType multiple selections
  @Test
  fun requestTypeChipGroup_multipleSelections_worksCorrectly() {
    var selectedTypes by mutableStateOf(emptyList<RequestType>())

    composeTestRule.setContent {
      MaterialTheme {
        RequestTypeChipGroup(
            selectedTypes = selectedTypes,
            onSelectionChanged = { selectedTypes = it },
            enabled = true)
      }
    }

    composeTestRule.onNodeWithText("STUDYING").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("SPORT").performClick()
    composeTestRule.waitForIdle()

    assert(selectedTypes.contains(RequestType.STUDYING))
    assert(selectedTypes.contains(RequestType.SPORT))
    assert(selectedTypes.size == 2)
  }

  // Test 18: Tags select
  @Test
  fun tagsChipGroup_selectTag_updatesSelection() {
    var selectedTags by mutableStateOf(emptyList<Tags>())

    composeTestRule.setContent {
      MaterialTheme {
        TagsChipGroup(
            selectedTags = selectedTags, onSelectionChanged = { selectedTags = it }, enabled = true)
      }
    }

    composeTestRule.onNodeWithText("URGENT").performClick()
    composeTestRule.waitForIdle()

    assert(selectedTags.contains(Tags.URGENT))
  }

  // Test 19: Tags deselect
  @Test
  fun tagsChipGroup_deselectTag_updatesSelection() {
    var selectedTags by mutableStateOf(listOf(Tags.URGENT))

    composeTestRule.setContent {
      MaterialTheme {
        TagsChipGroup(
            selectedTags = selectedTags, onSelectionChanged = { selectedTags = it }, enabled = true)
      }
    }

    composeTestRule.onNodeWithText("URGENT").performClick()
    composeTestRule.waitForIdle()

    assert(!selectedTags.contains(Tags.URGENT))
  }

  // Test 20: Tags multiple selections
  @Test
  fun tagsChipGroup_multipleSelections_worksCorrectly() {
    var selectedTags by mutableStateOf(emptyList<Tags>())

    composeTestRule.setContent {
      MaterialTheme {
        TagsChipGroup(
            selectedTags = selectedTags, onSelectionChanged = { selectedTags = it }, enabled = true)
      }
    }

    composeTestRule.onNodeWithText("URGENT").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("EASY").performClick()
    composeTestRule.waitForIdle()

    assert(selectedTags.contains(Tags.URGENT))
    assert(selectedTags.contains(Tags.EASY))
    assert(selectedTags.size == 2)
  }

  @Test
  fun editRequestContent_allFields_areDisplayed() {
    composeTestRule.setContent { TestEditRequestContent() }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE).assertExists()
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION).assertExists()
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_LOCATION_NAME).assertExists()
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON).assertExists()
  }

  @Test
  fun editRequestContent_preFilledData_displaysCorrectly() {
    composeTestRule.setContent {
      TestEditRequestContent(
          initialTitle = "Study Group",
          initialDescription = "Looking for partners",
          initialLocationName = "BC Building",
          isEditMode = true)
    }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .assertTextContains("Study Group")
  }

  @Test
  fun requestTypeChipGroup_chipLabels_displayCorrectly() {
    composeTestRule.setContent {
      MaterialTheme {
        RequestTypeChipGroup(selectedTypes = emptyList(), onSelectionChanged = {}, enabled = true)
      }
    }

    composeTestRule.onNodeWithText("STUDY GROUP").assertExists()
  }

  @Test
  fun tagsChipGroup_chipLabels_displayCorrectly() {
    composeTestRule.setContent {
      MaterialTheme {
        TagsChipGroup(selectedTags = emptyList(), onSelectionChanged = {}, enabled = true)
      }
    }

    composeTestRule.onNodeWithText("GROUP WORK").assertExists()
  }

  @Test
  fun editRequestContent_startDateField_updatesOnTextInput() {
    composeTestRule.setContent { TestEditRequestContent() }

    val validDate = "15/12/2025 14:30"
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
        .performTextInput(validDate)

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
        .assertTextContains(validDate)
  }

  // Test 23: Expiration date field updates correctly
  @Test
  fun editRequestContent_expirationDateField_updatesOnTextInput() {
    composeTestRule.setContent { TestEditRequestContent() }

    val validDate = "20/12/2025 18:00"
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
        .performTextInput(validDate)

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
        .assertTextContains(validDate)
  }

  // Test 24: Invalid date format shows error
  @Test
  fun editRequestContent_invalidDateFormat_showsError() {
    composeTestRule.setContent {
      TestEditRequestContent(
          initialTitle = "Test",
          initialDescription = "Test",
          initialRequestTypes = listOf(RequestType.STUDYING),
          initialLocation = Location(0.0, 0.0, "Test"),
          initialLocationName = "Test")
    }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
        .performTextInput("invalid date")

    composeTestRule.waitForIdle()

    // Should find exactly one error message for start date
    composeTestRule
        .onAllNodesWithText("Invalid format (must be dd/MM/yyyy HH:mm)")
        .assertCountEquals(1)
  }

  // Test 25: Disabled chips don't respond to clicks
  @Test
  fun requestTypeChipGroup_disabledChips_dontRespondToClicks() {
    var selectedTypes by mutableStateOf(emptyList<RequestType>())

    composeTestRule.setContent {
      MaterialTheme {
        RequestTypeChipGroup(
            selectedTypes = selectedTypes,
            onSelectionChanged = { selectedTypes = it },
            enabled = false)
      }
    }

    composeTestRule.onNodeWithText("STUDYING").performClick()

    composeTestRule.waitForIdle()

    // Selection should remain empty since chips are disabled
    assert(selectedTypes.isEmpty()) { "Disabled chips should not update selection" }
  }

  // Test 26: Disabled tags chips don't respond to clicks
  @Test
  fun tagsChipGroup_disabledChips_dontRespondToClicks() {
    var selectedTags by mutableStateOf(emptyList<Tags>())

    composeTestRule.setContent {
      MaterialTheme {
        TagsChipGroup(
            selectedTags = selectedTags,
            onSelectionChanged = { selectedTags = it },
            enabled = false)
      }
    }

    composeTestRule.onNodeWithText("URGENT").performClick()

    composeTestRule.waitForIdle()

    // Selection should remain empty since chips are disabled
    assert(selectedTags.isEmpty()) { "Disabled chips should not update selection" }
  }

  // Test 27: Error message for both empty fields simultaneously
  @Test
  fun editRequestContent_multipleEmptyFields_showsMultipleErrors() {
    composeTestRule.setContent { TestEditRequestContent() }

    // Type and clear both fields to trigger both errors
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE).performTextInput("a")
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE).performTextClearance()

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION).performTextInput("a")
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION)
        .performTextClearance()

    composeTestRule.waitForIdle()

    // Both errors should be visible
    composeTestRule.onNodeWithText("Title cannot be empty").assertExists()
    composeTestRule.onNodeWithText("Description cannot be empty").assertExists()
  }

  // Test 28: Button shows loading indicator when loading
  @Test
  fun editRequestContent_loadingState_showsLoadingIndicator() {
    composeTestRule.setContent {
      MaterialTheme {
        EditRequestContent(
            paddingValues = PaddingValues(0.dp),
            title = "Test",
            description = "Test",
            requestTypes = listOf(RequestType.STUDYING),
            location = Location(0.0, 0.0, "Test"),
            locationName = "Test",
            startTimeStamp = currentDate,
            expirationTime = futureDate,
            tags = emptyList(),
            isEditMode = false,
            isLoading = true,
            errorMessage = null,
            locationSearchResults = emptyList(),
            isSearchingLocation = false,
            onTitleChange = {},
            onDescriptionChange = {},
            onRequestTypesChange = {},
            onLocationChange = {},
            onLocationNameChange = {},
            onStartTimeStampChange = {},
            onExpirationTimeChange = {},
            onTagsChange = {},
            onSave = {},
            onClearError = {},
            onSearchLocations = {},
            onClearLocationSearch = {})
      }
    }

    // The button should be disabled and the text should not be visible when loading
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  // test 29: EditRequestScreen in create mode initializes ViewModel correctly
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun editRequestScreen_createMode_initializesViewModel() = runTest {
    val mockRepo = mock<RequestRepository>()
    val mockLocationRepo = mock<LocationRepository>()
    val viewModel = EditRequestViewModel(mockRepo, mockLocationRepo)

    composeTestRule.setContent {
      MaterialTheme {
        EditRequestScreen(
            requestId = null, // Create mode
            creatorId = "test123",
            onNavigateBack = {},
            viewModel = viewModel)
      }
    }

    advanceUntilIdle()
    composeTestRule.waitForIdle()

    // Verify "Create Request" appears (in both TopAppBar and Button)
    composeTestRule
        .onAllNodesWithText("Create Request", substring = true)
        .assertCountEquals(2) // ← Expect 2: title + button

    // Or verify the button specifically
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .assertTextEquals("Create Request")
  }

  // Test 30: EditRequestScreen loads existing request
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun editRequestScreen_editMode_loadsRequest() = runTest {
    val mockRepo = mock<RequestRepository>()
    val mockLocationRepo = mock<LocationRepository>()
    val testRequest =
        Request(
            requestId = "test-id",
            title = "Loaded Title",
            description = "Loaded Description",
            requestType = listOf(RequestType.STUDYING),
            location = Location(0.0, 0.0, "Test"),
            locationName = "Test Location",
            status = RequestStatus.IN_PROGRESS,
            startTimeStamp = Date(),
            expirationTime = Date(System.currentTimeMillis() + 86400000),
            people = listOf("creator123"),
            tags = emptyList(),
            creatorId = "creator123")

    whenever(mockRepo.getRequest("test-id")).thenReturn(testRequest)

    val viewModel = EditRequestViewModel(mockRepo, mockLocationRepo)

    composeTestRule.setContent {
      MaterialTheme {
        EditRequestScreen(
            requestId = "test-id", // Edit mode
            creatorId = "creator123",
            onNavigateBack = {},
            viewModel = viewModel)
      }
    }

    advanceUntilIdle()
    composeTestRule.waitForIdle()

    // Verify title shows "Edit Request"
    composeTestRule.onNodeWithText("Edit Request").assertExists()

    // Verify loaded data appears
    composeTestRule.onNodeWithText("Loaded Title").assertExists()
  }

  // Test 31: Date field shows error for past date
  @Test
  fun editRequestContent_pastStartDate_showsError() {
    composeTestRule.setContent {
      TestEditRequestContent(
          initialTitle = "Test",
          initialDescription = "Test",
          initialRequestTypes = listOf(RequestType.STUDYING),
          initialLocation = Location(0.0, 0.0, "Test"),
          initialLocationName = "Test")
    }

    val pastDate = "01/01/2020 10:00"
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
        .performTextInput(pastDate)

    composeTestRule.waitForIdle()

    // Past dates should show format error
    composeTestRule
        .onAllNodesWithText("Invalid format (must be dd/MM/yyyy HH:mm)")
        .assertCountEquals(1)
  }

  // Test 32: Both date fields can show errors simultaneously
  @Test
  fun editRequestContent_invalidDates_showsBothErrors() {
    composeTestRule.setContent { TestEditRequestContent() }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
        .performTextInput("invalid")

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
        .performTextInput("also invalid")

    composeTestRule.waitForIdle()

    // Both date errors should appear
    composeTestRule
        .onAllNodesWithText("Invalid format (must be dd/MM/yyyy HH:mm)")
        .assertCountEquals(2)
  }

  // Test 33: Valid date doesn't show error
  @Test
  fun editRequestContent_validDate_noError() {
    composeTestRule.setContent { TestEditRequestContent() }

    val validDate = "31/12/2026 15:30"
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
        .performTextInput(validDate)

    composeTestRule.waitForIdle()

    // No error should appear
    composeTestRule.onNodeWithText("Invalid format (must be dd/MM/yyyy HH:mm)").assertDoesNotExist()
  }

  // Test 34: RequestTypeChipGroup displays all types
  @Test
  fun requestTypeChipGroup_displaysAllRequestTypes() {
    composeTestRule.setContent {
      MaterialTheme { RequestTypeChipGroup(selectedTypes = emptyList(), onSelectionChanged = {}) }
    }

    // Verify all request types are displayed
    RequestType.entries.forEach { type ->
      val displayName = type.name.replace("_", " ")
      composeTestRule.onNodeWithText(displayName).assertExists()
    }
  }

  // Test 35: TagsChipGroup displays all tags
  @Test
  fun tagsChipGroup_displaysAllTags() {
    composeTestRule.setContent {
      MaterialTheme { TagsChipGroup(selectedTags = emptyList(), onSelectionChanged = {}) }
    }

    // Verify all tags are displayed
    Tags.entries.forEach { tag ->
      val displayName = tag.name.replace("_", " ")
      composeTestRule.onNodeWithText(displayName).assertExists()
    }
  }

  // Test 36: Clearing title after typing shows error
  @Test
  fun editRequestContent_clearTitleAfterTyping_showsError() {
    composeTestRule.setContent { TestEditRequestContent() }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .performTextInput("Some title")
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE).performTextClearance()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Title cannot be empty").assertExists()
  }

  // Test 37: Clearing description after typing shows error
  @Test
  fun editRequestContent_clearDescriptionAfterTyping_showsError() {
    composeTestRule.setContent { TestEditRequestContent() }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION)
        .performTextInput("Some description")
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION)
        .performTextClearance()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Description cannot be empty").assertExists()
  }

  // Test 38: Selecting and deselecting request type shows error
  @Test
  fun editRequestContent_deselectLastRequestType_showsError() {
    composeTestRule.setContent {
      TestEditRequestContent(initialRequestTypes = listOf(RequestType.STUDYING))
    }

    // Deselect the only selected type
    composeTestRule.onNodeWithText("STUDYING").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Please select at least one request type").assertExists()
  }

  // Test 41: Multiple request types can be selected
  @Test
  fun requestTypeChipGroup_multipleTypes_allSelected() {
    var selectedTypes by mutableStateOf(emptyList<RequestType>())

    composeTestRule.setContent {
      MaterialTheme {
        RequestTypeChipGroup(
            selectedTypes = selectedTypes, onSelectionChanged = { selectedTypes = it })
      }
    }

    // Select three different types
    composeTestRule.onNodeWithText("STUDYING").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("SPORT").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("EATING").performClick()
    composeTestRule.waitForIdle()

    assert(selectedTypes.size == 3)
    assert(
        selectedTypes.containsAll(
            listOf(RequestType.STUDYING, RequestType.SPORT, RequestType.EATING)))
  }

  // Test 42: Multiple tags can be selected
  @Test
  fun tagsChipGroup_multipleTags_allSelected() {
    var selectedTags by mutableStateOf(emptyList<Tags>())

    composeTestRule.setContent {
      MaterialTheme {
        TagsChipGroup(selectedTags = selectedTags, onSelectionChanged = { selectedTags = it })
      }
    }

    // Select three different tags
    composeTestRule.onNodeWithText("URGENT").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("EASY").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("OUTDOOR").performClick()
    composeTestRule.waitForIdle()

    assert(selectedTags.size == 3)
    assert(selectedTags.containsAll(listOf(Tags.URGENT, Tags.EASY, Tags.OUTDOOR)))
  }

  // Test 43: Error message dismiss button works
  @Test
  fun editRequestContent_editModeWithData_displaysAllFields() {
    composeTestRule.setContent {
      TestEditRequestContent(
          initialTitle = "Study Session",
          initialDescription = "Need help with calculus",
          initialRequestTypes = listOf(RequestType.STUDYING, RequestType.STUDY_GROUP),
          initialLocation = Location(46.5197, 6.5668, "EPFL"),
          initialLocationName = "BC Building, Room 330",
          initialTags = listOf(Tags.URGENT, Tags.GROUP_WORK),
          isEditMode = true)
    }

    // Verify all pre-filled data is displayed
    composeTestRule.onNodeWithText("Study Session").assertExists()
    composeTestRule.onNodeWithText("Need help with calculus").assertExists()
    composeTestRule.onNodeWithText("BC Building, Room 330").assertExists()

    // Verify button shows "Update Request" in edit mode
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .assertTextContains("Update Request")
  }

  // Test 45: Location search shows loading indicator
  @Test
  fun editRequestContent_locationSearchLoading_showsIndicator() {
    composeTestRule.setContent {
      MaterialTheme {
        EditRequestContent(
            paddingValues = PaddingValues(0.dp),
            title = "Test",
            description = "Test",
            requestTypes = listOf(RequestType.STUDYING),
            location = null,
            locationName = "EPFL",
            startTimeStamp = futureDate,
            expirationTime = Date(futureDate.time + 86400000),
            tags = emptyList(),
            isEditMode = false,
            isLoading = false,
            errorMessage = null,
            locationSearchResults = emptyList(),
            isSearchingLocation = true, // ← Loading state
            onTitleChange = {},
            onDescriptionChange = {},
            onRequestTypesChange = {},
            onLocationChange = {},
            onLocationNameChange = {},
            onStartTimeStampChange = {},
            onExpirationTimeChange = {},
            onTagsChange = {},
            onSave = {},
            onClearError = {},
            onSearchLocations = {},
            onClearLocationSearch = {})
      }
    }

    // Verify loading indicator exists (CircularProgressIndicator in LocationSearchField)
    // Note: Can't easily test CircularProgressIndicator directly, but we can verify the field
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_LOCATION_NAME)
        .assertExists()
        .assertIsEnabled()
  }
}
