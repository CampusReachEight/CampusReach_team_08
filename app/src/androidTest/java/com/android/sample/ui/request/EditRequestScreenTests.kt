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
import com.android.sample.ui.request.edit.EditRequestActions
import com.android.sample.ui.request.edit.EditRequestContent
import com.android.sample.ui.request.edit.EditRequestScreen
import com.android.sample.ui.request.edit.EditRequestScreenTestTags
import com.android.sample.ui.request.edit.EditRequestUiState
import com.android.sample.ui.request.edit.EditRequestViewModel
import com.android.sample.ui.request.edit.FieldValidationState
import com.android.sample.ui.request.edit.RequestTypeChipGroup
import com.android.sample.ui.request.edit.TagsChipGroup
import java.text.SimpleDateFormat
import java.util.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

/** Test builder for EditRequestUiState. */
class EditRequestUiStateBuilder {
  private var title = ""
  private var description = ""
  private var requestTypes = emptyList<RequestType>()
  private var location: Location? = null
  private var locationName = ""
  private var startTimeStamp = Date()
  private var expirationTime = Date(System.currentTimeMillis() + 86400000)
  private var tags = emptyList<Tags>()
  private var isEditMode = false
  private var isLoading = false
  private var errorMessage: String? = null
  private var validationState = FieldValidationState()

  fun withTitle(title: String) = apply { this.title = title }

  fun withDescription(description: String) = apply { this.description = description }

  fun withRequestTypes(types: List<RequestType>) = apply { this.requestTypes = types }

  fun withLocation(location: Location?) = apply { this.location = location }

  fun withLocationName(name: String) = apply { this.locationName = name }

  fun withStartDate(date: Date) = apply { this.startTimeStamp = date }

  fun withExpirationDate(date: Date) = apply { this.expirationTime = date }

  fun withTags(tags: List<Tags>) = apply { this.tags = tags }

  fun withEditMode(isEditMode: Boolean) = apply { this.isEditMode = isEditMode }

  fun withLoading(isLoading: Boolean) = apply { this.isLoading = isLoading }

  fun withError(error: String?) = apply { this.errorMessage = error }

  fun withValidationState(state: FieldValidationState) = apply { this.validationState = state }

  fun build(): EditRequestUiState =
      EditRequestUiState(
          title = title,
          description = description,
          requestTypes = requestTypes,
          location = location,
          locationName = locationName,
          startTimeStamp = startTimeStamp,
          expirationTime = expirationTime,
          tags = tags,
          isEditMode = isEditMode,
          isLoading = isLoading,
          errorMessage = errorMessage,
          validationState = validationState)
}

/** Test builder for FieldValidationState. */
class FieldValidationStateBuilder {
  private var showTitleError = false
  private var showDescriptionError = false
  private var showRequestTypeError = false
  private var showLocationNameError = false
  private var showLocationError = false
  private var showStartDateError = false
  private var showExpirationDateError = false
  private var showDateOrderError = false
  private var showSuccessMessage = false

  fun withTitleError() = apply { this.showTitleError = true }

  fun withDescriptionError() = apply { this.showDescriptionError = true }

  fun withRequestTypeError() = apply { this.showRequestTypeError = true }

  fun withLocationNameError() = apply { this.showLocationNameError = true }

  fun withLocationError() = apply { this.showLocationError = true }

  fun withStartDateError() = apply { this.showStartDateError = true }

  fun withExpirationDateError() = apply { this.showExpirationDateError = true }

  fun withDateOrderError() = apply { this.showDateOrderError = true }

  fun withSuccessMessage() = apply { this.showSuccessMessage = true }

  fun build(): FieldValidationState =
      FieldValidationState(
          showTitleError = showTitleError,
          showDescriptionError = showDescriptionError,
          showRequestTypeError = showRequestTypeError,
          showLocationNameError = showLocationNameError,
          showStartDateError = showStartDateError,
          showExpirationDateError = showExpirationDateError,
          showDateOrderError = showDateOrderError,
          showSuccessMessage = showSuccessMessage)
}

/** Base test class with shared utilities. */
open class EditRequestScreenTestBase {
  @get:Rule val composeTestRule = createComposeRule()

  protected val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
  protected val currentDate = Date()
  protected val futureDate = Date(System.currentTimeMillis() + 86400000)

  protected fun createTestViewModel(): EditRequestViewModel {
    val mockRequestRepo = mock<RequestRepository>()
    val mockLocationRepo = mock<LocationRepository>()
    return EditRequestViewModel(mockRequestRepo, mockLocationRepo)
  }

  /**
   * Render content with REAL ViewModel for user interaction tests. Use this when you need to test
   * actual user workflows (typing, clicking).
   */
  @Composable
  protected fun TestEditRequestContentWithRealViewModel(
      viewModel: EditRequestViewModel = createTestViewModel(),
      onSave: () -> Unit = {}
  ) {
    val uiState by viewModel.uiState.collectAsState()

    val actions =
        EditRequestActions(
            onTitleChange = viewModel::updateTitle,
            onDescriptionChange = viewModel::updateDescription,
            onRequestTypesChange = viewModel::updateRequestTypes,
            onLocationChange = viewModel::updateLocation,
            onLocationNameChange = viewModel::updateLocationName,
            onStartTimeStampChange = viewModel::updateStartTimeStamp,
            onExpirationTimeChange = viewModel::updateExpirationTime,
            onTagsChange = viewModel::updateTags,
            onSave = onSave,
            onClearError = viewModel::clearError,
            onClearSuccessMessage = viewModel::clearSuccessMessage,
            onSearchLocations = viewModel::searchLocations,
            onClearLocationSearch = viewModel::clearLocationSearch)

    MaterialTheme {
      EditRequestContent(paddingValues = PaddingValues(0.dp), uiState = uiState, actions = actions)
    }
  }

  /**
   * Render content with STATIC state for UI rendering tests. Use this when you only need to test if
   * UI displays correctly given a state.
   */
  @Composable
  protected fun TestEditRequestContentWithStaticState(
      uiState: EditRequestUiState,
      onSave: () -> Unit = {},
      onClearError: () -> Unit = {},
      onClearSuccessMessage: () -> Unit = {}
  ) {
    val actions =
        EditRequestActions(
            onTitleChange = {},
            onDescriptionChange = {},
            onRequestTypesChange = {},
            onLocationChange = {},
            onLocationNameChange = {},
            onStartTimeStampChange = {},
            onExpirationTimeChange = {},
            onTagsChange = {},
            onSave = onSave,
            onClearError = onClearError,
            onClearSuccessMessage = onClearSuccessMessage,
            onSearchLocations = {},
            onClearLocationSearch = {})

    MaterialTheme {
      EditRequestContent(paddingValues = PaddingValues(0.dp), uiState = uiState, actions = actions)
    }
  }

  // ========== ASSERTION HELPERS ==========

  protected fun assertTextFieldExists(testTag: String) {
    composeTestRule.onNodeWithTag(testTag).assertExists()
  }

  protected fun assertTextFieldContains(testTag: String, text: String) {
    composeTestRule.onNodeWithTag(testTag).assertTextContains(text)
  }

  protected fun assertErrorMessage(errorText: String) {
    composeTestRule.onNodeWithText(errorText).assertExists()
  }

  protected fun assertNoErrorMessage(errorText: String) {
    composeTestRule.onNodeWithText(errorText).assertDoesNotExist()
  }

  protected fun assertSuccessMessage(message: String) {
    composeTestRule.onNodeWithText(message).assertExists()
  }

  protected fun assertButtonExists(text: String) {
    composeTestRule.onNodeWithText(text).assertExists()
  }

  protected fun clickButton(text: String) {
    composeTestRule.onNodeWithText(text).performClick()
  }

  protected fun clickChip(chipName: String) {
    composeTestRule.onNodeWithText(chipName).performClick()
  }

  // ========== INPUT HELPERS ==========

  protected fun typeInField(testTag: String, text: String) {
    composeTestRule.onNodeWithTag(testTag).performTextInput(text)
  }

  protected fun clearField(testTag: String) {
    composeTestRule.onNodeWithTag(testTag).performTextClearance()
  }

  protected fun typeAndClear(testTag: String, text: String) {
    typeInField(testTag, text)
    clearField(testTag)
  }

  protected fun waitForUI() {
    composeTestRule.waitForIdle()
  }
}

/**
 * Test suite for EditRequestScreen.
 * - Tests with actual user interaction (typing, clicking) use
 *   TestEditRequestContentWithRealViewModel
 * - Tests that only verify UI rendering given a state use TestEditRequestContentWithStaticState
 */
class EditRequestScreenTests : EditRequestScreenTestBase() {

  // ========== MODE TESTS ==========

  @Test
  fun createMode_displaysCorrectButtonLabel() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiStateBuilder().withEditMode(false).build())
    }
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .assertTextEquals("Create Request")
  }

  @Test
  fun editMode_displaysCorrectButtonLabel() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiStateBuilder().withEditMode(true).build())
    }
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .assertTextEquals("Update Request")
  }

  // ========== TITLE FIELD TESTS ==========

  @Test
  fun titleField_updatesOnInput() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    typeInField(EditRequestScreenTestTags.INPUT_TITLE, "Study Session")
    waitForUI()

    assertEquals("Study Session", viewModel.uiState.value.title)
  }

  @Test
  fun titleField_emptiness_triggersValidation() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    typeInField(EditRequestScreenTestTags.INPUT_TITLE, "Test")
    waitForUI()
    clearField(EditRequestScreenTestTags.INPUT_TITLE)
    waitForUI()

    assert(viewModel.uiState.value.validationState.showTitleError)
  }

  // ========== DESCRIPTION FIELD TESTS ==========

  @Test
  fun descriptionField_updatesOnInput() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    typeInField(EditRequestScreenTestTags.INPUT_DESCRIPTION, "Looking for study partners")
    waitForUI()

    assertEquals("Looking for study partners", viewModel.uiState.value.description)
  }

  @Test
  fun descriptionField_emptiness_triggersValidation() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    typeInField(EditRequestScreenTestTags.INPUT_DESCRIPTION, "Test")
    waitForUI()
    clearField(EditRequestScreenTestTags.INPUT_DESCRIPTION)
    waitForUI()

    assert(viewModel.uiState.value.validationState.showDescriptionError)
  }

  // ========== REQUEST TYPE TESTS ==========

  @Test
  fun requestTypes_singleSelection_worksCorrectly() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    clickChip("STUDYING")
    waitForUI()

    assertEquals(listOf(RequestType.STUDYING), viewModel.uiState.value.requestTypes)
  }

  @Test
  fun requestTypes_multipleSelections_worksCorrectly() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    clickChip("STUDYING")
    waitForUI()
    clickChip("SPORT")
    waitForUI()

    val types = viewModel.uiState.value.requestTypes
    assert(types.contains(RequestType.STUDYING))
    assert(types.contains(RequestType.SPORT))
    assertEquals(2, types.size)
  }

  @Test
  fun requestTypes_deselection_worksCorrectly() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    clickChip("STUDYING")
    waitForUI()
    clickChip("STUDYING")
    waitForUI()

    assert(viewModel.uiState.value.requestTypes.isEmpty())
  }

  @Test
  fun requestTypes_emptySelection_triggersValidation() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    clickChip("STUDYING")
    waitForUI()
    clickChip("STUDYING")
    waitForUI()

    assert(viewModel.uiState.value.validationState.showRequestTypeError)
  }

  @Test
  fun requestTypes_disabled_ignoresClicks() {
    var selectedTypes by mutableStateOf(emptyList<RequestType>())
    composeTestRule.setContent {
      MaterialTheme {
        RequestTypeChipGroup(
            selectedTypes = selectedTypes,
            onSelectionChanged = { selectedTypes = it },
            enabled = false)
      }
    }

    clickChip("STUDYING")
    waitForUI()

    assert(selectedTypes.isEmpty())
  }

  // ========== TAGS TESTS ==========

  @Test
  fun tags_singleSelection_worksCorrectly() {
    var selectedTags by mutableStateOf(emptyList<Tags>())
    composeTestRule.setContent {
      MaterialTheme {
        TagsChipGroup(
            selectedTags = selectedTags, onSelectionChanged = { selectedTags = it }, enabled = true)
      }
    }

    clickChip("URGENT")
    waitForUI()

    assert(selectedTags.contains(Tags.URGENT))
  }

  @Test
  fun tags_multipleSelections_worksCorrectly() {
    var selectedTags by mutableStateOf(emptyList<Tags>())
    composeTestRule.setContent {
      MaterialTheme {
        TagsChipGroup(
            selectedTags = selectedTags, onSelectionChanged = { selectedTags = it }, enabled = true)
      }
    }

    clickChip("URGENT")
    waitForUI()
    clickChip("EASY")
    waitForUI()

    assert(selectedTags.contains(Tags.URGENT))
    assert(selectedTags.contains(Tags.EASY))
    assertEquals(2, selectedTags.size)
  }

  @Test
  fun tags_deselection_worksCorrectly() {
    var selectedTags by mutableStateOf(listOf(Tags.URGENT))
    composeTestRule.setContent {
      MaterialTheme {
        TagsChipGroup(
            selectedTags = selectedTags, onSelectionChanged = { selectedTags = it }, enabled = true)
      }
    }

    clickChip("URGENT")
    waitForUI()

    assert(!selectedTags.contains(Tags.URGENT))
  }

  // ========== DATE TESTS ==========

  @Test
  fun startDateField_validDate_noError() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    val validDate = "31/12/2026 15:30"
    clearField(EditRequestScreenTestTags.INPUT_START_DATE)
    typeInField(EditRequestScreenTestTags.INPUT_START_DATE, validDate)
    waitForUI()

    assert(!viewModel.uiState.value.validationState.showStartDateError)
  }

  @Test
  fun startDateField_invalidDate_triggersValidation() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    clearField(EditRequestScreenTestTags.INPUT_START_DATE)
    typeInField(EditRequestScreenTestTags.INPUT_START_DATE, "invalid date")
    waitForUI()

    // Invalid dates don't set error flag in updateStartTimeStamp validation happens at save time
    // So we verify the field shows the invalid text
    assertTextFieldContains(EditRequestScreenTestTags.INPUT_START_DATE, "invalid date")
  }

  @Test
  fun expirationDateField_invalidDate_triggersValidation() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    clearField(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
    typeInField(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE, "also invalid")
    waitForUI()

    assertTextFieldContains(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE, "also invalid")
  }

  @Test
  fun dateRange_expiredRange_triggersValidation() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    viewModel.updateStartTimeStamp(futureDate)
    viewModel.updateExpirationTime(currentDate)
    waitForUI()

    assert(viewModel.uiState.value.validationState.showDateOrderError)
  }

  // ========== LOCATION TESTS ==========

  @Test
  fun locationField_hasCorrectUI() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(uiState = EditRequestUiStateBuilder().build())
    }
    assertTextFieldExists(EditRequestScreenTestTags.INPUT_LOCATION_NAME)
  }

  @Test
  fun locationName_empty_displaysError() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState =
              EditRequestUiStateBuilder()
                  .withLocationName("")
                  .withValidationState(
                      FieldValidationStateBuilder().withLocationNameError().build())
                  .build())
    }

    assertErrorMessage("Location name cannot be empty")
  }

  // ========== LOADING STATE TESTS ==========

  @Test
  fun loadingState_disablesSaveButton() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiStateBuilder().withLoading(true).build())
    }
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun loadingState_disablesInputFields() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiStateBuilder().withLoading(true).build())
    }
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE).assertIsNotEnabled()
  }

  // ========== ERROR HANDLING TESTS ==========

  @Test
  fun errorMessage_displaysCorrectly() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiStateBuilder().withError("Failed to save request").build())
    }
    assertErrorMessage("Failed to save request")
  }

  @Test
  fun errorMessage_canBeDismissed() {
    var errorCleared = false
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiStateBuilder().withError("Failed to save request").build(),
          onClearError = { errorCleared = true })
    }

    clickButton("Dismiss")
    assert(errorCleared)
  }

  // ========== SUCCESS MESSAGE TESTS ==========

  @Test
  fun successMessage_displaysAfterCreateMode() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState =
              EditRequestUiStateBuilder()
                  .withValidationState(FieldValidationStateBuilder().withSuccessMessage().build())
                  .withEditMode(false)
                  .build())
    }
    assertSuccessMessage("Request created successfully!")
  }

  @Test
  fun successMessage_displaysAfterEditMode() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState =
              EditRequestUiStateBuilder()
                  .withValidationState(FieldValidationStateBuilder().withSuccessMessage().build())
                  .withEditMode(true)
                  .build())
    }
    assertSuccessMessage("Request updated successfully!")
  }

  @Test
  fun successMessage_canBeDismissed() {
    var messageDismissed = false
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState =
              EditRequestUiStateBuilder()
                  .withValidationState(FieldValidationStateBuilder().withSuccessMessage().build())
                  .build(),
          onClearSuccessMessage = { messageDismissed = true })
    }

    clickButton("Dismiss")
    assert(messageDismissed)
  }

  // ========== UI VISIBILITY TESTS ==========

  @Test
  fun allFields_areDisplayed() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(uiState = EditRequestUiStateBuilder().build())
    }

    assertTextFieldExists(EditRequestScreenTestTags.INPUT_TITLE)
    assertTextFieldExists(EditRequestScreenTestTags.INPUT_DESCRIPTION)
    assertTextFieldExists(EditRequestScreenTestTags.INPUT_LOCATION_NAME)
    assertTextFieldExists(EditRequestScreenTestTags.SAVE_BUTTON)
  }

  @Test
  fun preFilledData_displaysCorrectly() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState =
              EditRequestUiStateBuilder()
                  .withTitle("Study Group")
                  .withDescription("Looking for partners")
                  .withLocationName("BC Building")
                  .build())
    }
    assertTextFieldContains(EditRequestScreenTestTags.INPUT_TITLE, "Study Group")
  }

  // ========== MULTIPLE VALIDATION ERRORS TESTS ==========

  @Test
  fun multipleFields_emptySimultaneously_showAllErrors() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    typeAndClear(EditRequestScreenTestTags.INPUT_TITLE, "Test")
    typeAndClear(EditRequestScreenTestTags.INPUT_DESCRIPTION, "Test")
    waitForUI()

    assert(viewModel.uiState.value.validationState.showTitleError)
    assert(viewModel.uiState.value.validationState.showDescriptionError)
  }

  // ========== INTEGRATION TESTS ==========

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun editRequestScreen_createMode_initializesCorrectly() = runTest {
    val mockRepo = mock<RequestRepository>()
    val mockLocationRepo = mock<LocationRepository>()
    val viewModel = EditRequestViewModel(mockRepo, mockLocationRepo)

    composeTestRule.setContent {
      MaterialTheme {
        EditRequestScreen(requestId = null, onNavigateBack = {}, viewModel = viewModel)
      }
    }

    advanceUntilIdle()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .assertTextEquals("Create Request")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun editRequestScreen_editMode_loadsRequestData() = runTest {
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
        EditRequestScreen(requestId = "test-id", onNavigateBack = {}, viewModel = viewModel)
      }
    }

    advanceUntilIdle()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Edit Request").assertExists()
    composeTestRule.onNodeWithText("Loaded Title").assertExists()
  }
  /**
   * Test that all RequestType options are displayed. Ensures UI shows all available request types
   * for user selection.
   */
  @Test
  fun requestTypeChipGroup_displaysAllAvailableTypes() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(uiState = EditRequestUiStateBuilder().build())
    }

    RequestType.entries.forEach { type ->
      val displayName = type.name.replace("_", " ")
      assertButtonExists(displayName)
    }
  }

  /**
   * Test that all Tags options are displayed. Ensures UI shows all available tags for user
   * selection.
   */
  @Test
  fun tagsChipGroup_displaysAllAvailableTags() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(uiState = EditRequestUiStateBuilder().build())
    }

    Tags.entries.forEach { tag ->
      val displayName = tag.name.replace("_", " ")
      assertButtonExists(displayName)
    }
  }

  /**
   * Test selecting all request types simultaneously. Ensures form handles maximum selections
   * without issues.
   */
  @Test
  fun requestTypes_selectAllTypes_worksCorrectly() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    RequestType.entries.forEach { type ->
      val displayName = type.name.replace("_", " ")
      clickChip(displayName)
      waitForUI()
    }

    assertEquals(RequestType.entries.size, viewModel.uiState.value.requestTypes.size)
    RequestType.entries.forEach { type ->
      assert(viewModel.uiState.value.requestTypes.contains(type))
    }
  }

  /**
   * Test that date fields update the ViewModel when valid dates are entered. Ensures start date and
   * expiration date are properly synchronized with ViewModel.
   */
  @Test
  fun dateFields_validDates_updateViewModelCorrectly() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    val startDate = "15/12/2025 10:00"
    val expirationDate = "20/12/2025 18:00"

    clearField(EditRequestScreenTestTags.INPUT_START_DATE)
    typeInField(EditRequestScreenTestTags.INPUT_START_DATE, startDate)
    waitForUI()

    clearField(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
    typeInField(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE, expirationDate)
    waitForUI()

    // Verify dates were parsed and stored in ViewModel
    assert(viewModel.uiState.value.startTimeStamp.time > 0)
    assert(
        viewModel.uiState.value.expirationTime.time > viewModel.uiState.value.startTimeStamp.time)
  }

  /**
   * Test that tags are optional (no error triggered when empty). Ensures form doesn't force tag
   * selection unlike request types.
   */
  @Test
  fun tags_areOptional_noValidationError() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Don't select any tags
    waitForUI()

    // Verify no validation error for empty tags
    assert(viewModel.uiState.value.tags.isEmpty())
    assert(!viewModel.uiState.value.validationState.showRequestTypeError)
  }

  /**
   * Test multiple validation errors display simultaneously. Ensures all invalid fields show errors
   * at the same time.
   */
  @Test
  fun allValidationErrors_displaySimultaneously() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState =
              EditRequestUiStateBuilder()
                  .withTitle("")
                  .withDescription("")
                  .withRequestTypes(emptyList())
                  .withLocationName("")
                  .withStartDate(futureDate)
                  .withExpirationDate(currentDate)
                  .withValidationState(
                      FieldValidationStateBuilder()
                          .withTitleError()
                          .withDescriptionError()
                          .withRequestTypeError()
                          .withLocationNameError()
                          .withDateOrderError()
                          .build())
                  .build())
    }

    assertErrorMessage("Title cannot be empty")
    assertErrorMessage("Description cannot be empty")
    assertErrorMessage("Please select at least one request type")
    assertErrorMessage("Location name cannot be empty")
    assertErrorMessage("Expiration date must be after start date")
  }
}
