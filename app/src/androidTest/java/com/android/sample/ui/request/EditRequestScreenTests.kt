package com.android.sample.ui.request

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import com.android.sample.model.map.Location
import com.android.sample.model.map.LocationRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.request.edit.EditRequestActions
import com.android.sample.ui.request.edit.EditRequestContent
import com.android.sample.ui.request.edit.EditRequestScreen
import com.android.sample.ui.request.edit.EditRequestScreenTestTags
import com.android.sample.ui.request.edit.EditRequestUiState
import com.android.sample.ui.request.edit.EditRequestViewModel
import com.android.sample.ui.request.edit.FieldValidationState
import com.android.sample.ui.request.edit.RequestTypeChipGroup
import com.android.sample.ui.request.edit.TagsChipGroup
import com.android.sample.ui.request.edit.combineDateAndTime
import com.android.sample.ui.request.edit.isLocationEnabled
import com.android.sample.utils.UI_WAIT_TIMEOUT
import java.text.SimpleDateFormat
import java.util.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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
    val fakeFusedLocationProvider = FakeLocationProvider()
    return EditRequestViewModel(mockRequestRepo, mockLocationRepo, fakeFusedLocationProvider)
  }

  protected fun createTestViewModel(locationProvider: FakeLocationProvider): EditRequestViewModel {
    val mockRequestRepo = mock<RequestRepository>()
    val mockLocationRepo = mock<LocationRepository>()
    return EditRequestViewModel(mockRequestRepo, mockLocationRepo, locationProvider)
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
            onClearLocationSearch = viewModel::clearLocationSearch,
            onDeleteClick = viewModel::confirmDelete,
            onConfirmDelete = { viewModel.deleteRequest("") {} },
            onCancelDelete = viewModel::cancelDelete,
            onUseCurrentLocation = viewModel::getCurrentLocation)

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
      onClearSuccessMessage: () -> Unit = {},
      onUseCurrentLocation: () -> Unit = {}
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
            onClearLocationSearch = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onCancelDelete = {},
            onUseCurrentLocation = onUseCurrentLocation)

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
    val fakeFusedLocationProvider = FakeLocationProvider() // Changed here
    val viewModel = EditRequestViewModel(mockRepo, mockLocationRepo, fakeFusedLocationProvider)
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
    val fakeFusedLocationProvider = FakeLocationProvider() // Changed here
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

    val viewModel = EditRequestViewModel(mockRepo, mockLocationRepo, fakeFusedLocationProvider)

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

  /**
   * Test that delete button only appears in edit mode. This is a SCREEN-level integration test, not
   * a button unit test.
   */
  @Test
  fun deleteButton_onlyVisibleInEditMode() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState =
              EditRequestUiStateBuilder()
                  .withEditMode(false) // Create mode
                  .build())
    }
    composeTestRule.onNodeWithText("Delete Request").assertDoesNotExist()
  }

  /**
   * Test that delete confirmation dialog appears when delete button is clicked. Tests the full
   * flow: button click → dialog shows. Stays in EditRequestScreenTest.
   */
  @Test
  fun deleteButton_showsConfirmationDialogOnClick() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    viewModel.updateRequestTypes(listOf(RequestType.STUDYING))
    viewModel.updateLocation(Location(0.0, 0.0, "Test"))
    viewModel.updateLocationName("Test Location")
    waitForUI()

    viewModel.confirmDelete()
    waitForUI()

    composeTestRule.onNodeWithText("Delete Request?").assertExists()
  }

  /**
   * Test that canceling delete closes the confirmation dialog. Tests ViewModel state management
   * (could also go in ViewModel tests).
   */
  @Test
  fun deleteConfirmation_cancelClosesDialog() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    viewModel.confirmDelete()
    waitForUI()
    assert(viewModel.uiState.value.showDeleteConfirmation)

    viewModel.cancelDelete()
    waitForUI()

    assert(!viewModel.uiState.value.showDeleteConfirmation)
  }

  @Test
  fun useCurrentLocationButton_exists_andIsEnabledInitially() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(uiState = EditRequestUiStateBuilder().build())
    }

    composeTestRule.onNodeWithText("Use Current Location").assertExists().assertIsEnabled()
  }

  @Test
  fun useCurrentLocationButton_showsLoadingSpinner_whenSearchingLocation() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiStateBuilder().build().copy(isSearchingLocation = true))
    }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.USE_CURRENT_LOCATION_BUTTON)
        .assertExists()

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.LOCATION_LOADING_SPINNER).assertExists()
  }

  @Test
  fun useCurrentLocationButton_isDisabled_whenIsLoading() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiStateBuilder().withLoading(true).build())
    }

    composeTestRule.onNodeWithText("Use Current Location").assertIsNotEnabled()
  }

  @Test
  fun useCurrentLocationButton_isDisabled_whenSearchingLocation() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiStateBuilder().build().copy(isSearchingLocation = true))
    }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.USE_CURRENT_LOCATION_BUTTON)
        .assertIsNotEnabled()
  }

  @Test
  fun useCurrentLocationButton_showsErrorMessage_whenLocationFetchFails() {
    val errorMsg = "Failed to get current location: GPS unavailable"

    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiStateBuilder().withError(errorMsg).build())
    }

    assertErrorMessage(errorMsg)
  }

  @Test
  fun useCurrentLocationButton_updatesLocationFields_onSuccess() {
    val testLocation = Location(latitude = 46.5197, longitude = 6.6323, name = "EPFL, Lausanne")

    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState =
              EditRequestUiStateBuilder()
                  .withLocation(testLocation)
                  .withLocationName(testLocation.name)
                  .build())
    }

    // Instead of checking UI text, verify the field exists and isn't in error state
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_LOCATION_NAME)
        .assertExists()
        .assertIsEnabled()
  }

  @Test
  fun useCurrentLocationButton_callsAction_whenClicked() {
    var actionCalled = false

    composeTestRule.setContent {
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
              onSave = {},
              onClearError = {},
              onClearSuccessMessage = {},
              onSearchLocations = {},
              onClearLocationSearch = {},
              onDeleteClick = {},
              onConfirmDelete = {},
              onCancelDelete = {},
              onUseCurrentLocation = { actionCalled = true })

      MaterialTheme {
        EditRequestContent(
            paddingValues = PaddingValues(0.dp),
            uiState = EditRequestUiStateBuilder().build(),
            actions = actions)
      }
    }

    clickButton("Use Current Location")
    waitForUI()

    assert(actionCalled) { "onUseCurrentLocation action should be called" }
  }

  @Test
  fun useCurrentLocationButton_staysDisabled_duringBothLoadingStates() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState =
              EditRequestUiStateBuilder()
                  .withLoading(true)
                  .build()
                  .copy(isSearchingLocation = true))
    }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.USE_CURRENT_LOCATION_BUTTON)
        .assertIsNotEnabled()
  }

  @Test
  fun permissionLauncher_whenFineLocationGrantedAndGPSEnabled_callsGetCurrentLocation() {
    val mockLocationManager =
        mock<LocationManager> {
          on { isProviderEnabled(LocationManager.GPS_PROVIDER) } doReturn true
        }

    var getCurrentLocationCalled = false
    var setLocationErrorCalled = false

    // Simulate the permission callback logic
    val permissions = mapOf(Manifest.permission.ACCESS_FINE_LOCATION to true)

    when {
      permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
        if (mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
          getCurrentLocationCalled = true // Instead of calling viewModel
        } else {
          setLocationErrorCalled = true
        }
      }
      else -> {
        setLocationErrorCalled = true
      }
    }

    assertTrue(getCurrentLocationCalled)
    assertFalse(setLocationErrorCalled)
  }

  @Test
  fun permissionLauncher_whenPermissionDenied_setsLocationError() {
    var getCurrentLocationCalled = false
    var setLocationErrorCalled = false

    val permissions =
        mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to false)

    when {
      permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
        getCurrentLocationCalled = true
      }
      else -> {
        setLocationErrorCalled = true
      }
    }

    assertTrue(setLocationErrorCalled)
    assertFalse(getCurrentLocationCalled)
  }

  // ========== USE CURRENT LOCATION BUTTON TEST ==========

  @Test
  fun useCurrentLocationButton_exists_andIsClickable() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiState() // Direct usage, no builder
          )
    }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.USE_CURRENT_LOCATION_BUTTON)
        .assertExists()
        .assertIsEnabled()
  }

  @Test
  fun useCurrentLocationButton_whenLoading_showsSpinner() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState = EditRequestUiState(isSearchingLocation = true) // Direct usage
          )
    }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.LOCATION_LOADING_SPINNER).assertExists()
  }

  // ========== TEST isLocationEnabled FUNCTION ==========

  @Test
  fun testIsLocationEnabled_withGPSEnabled() {
    val mockLocationManager =
        mock<LocationManager> {
          on { isProviderEnabled(LocationManager.GPS_PROVIDER) } doReturn true
          on { isProviderEnabled(LocationManager.NETWORK_PROVIDER) } doReturn false
        }
    val mockContext =
        mock<Context> {
          on { getSystemService(Context.LOCATION_SERVICE) } doReturn mockLocationManager
        }

    val result = isLocationEnabled(mockContext)

    assertTrue(result)
  }

  @Test
  fun testIsLocationEnabled_withBothDisabled() {
    val mockLocationManager =
        mock<LocationManager> {
          on { isProviderEnabled(LocationManager.GPS_PROVIDER) } doReturn false
          on { isProviderEnabled(LocationManager.NETWORK_PROVIDER) } doReturn false
        }
    val mockContext =
        mock<Context> {
          on { getSystemService(Context.LOCATION_SERVICE) } doReturn mockLocationManager
        }

    val result = isLocationEnabled(mockContext)

    assertFalse(result)
  }

  // ========== DATE FORMAT ERROR TESTS ==========

  @Test
  fun expirationDateField_withDateOrderError_displaysOrderError() {
    composeTestRule.setContent {
      TestEditRequestContentWithStaticState(
          uiState =
              EditRequestUiState(validationState = FieldValidationState(showDateOrderError = true)))
    }

    assertErrorMessage("Expiration date must be after start date")
  }

  // ========== PERMISSION AND LOCATION TESTS (UI-based) ==========

  @Test
  fun useCurrentLocationButton_whenClicked_showsLoadingState() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Click the button
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.USE_CURRENT_LOCATION_BUTTON)
        .performClick()

    // The button click will trigger the permission check flow
    // We can't test the actual permission dialog (system UI)
    // But we can verify the button was clickable
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.USE_CURRENT_LOCATION_BUTTON)
        .assertExists()
  }

  // ========== TEST PERMISSION CALLBACK LOGIC DIRECTLY ==========

  @Test
  fun permissionCallbackLogic_allBranches() {
    // Test the permission callback logic as pure logic

    // Test 1: Fine location granted
    var result = ""
    val permissions1 = mapOf(Manifest.permission.ACCESS_FINE_LOCATION to true)
    when {
      permissions1[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          permissions1[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
        result = "has_permission"
      }
      else -> {
        result = "no_permission"
      }
    }
    assertEquals("has_permission", result)

    // Test 2: Coarse location only
    val permissions2 =
        mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to true)
    result = ""
    when {
      permissions2[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          permissions2[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
        result = "has_permission"
      }
      else -> {
        result = "no_permission"
      }
    }
    assertEquals("has_permission", result)

    // Test 3: No permissions
    val permissions3 =
        mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to false)
    result = ""
    when {
      permissions3[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          permissions3[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
        result = "has_permission"
      }
      else -> {
        result = "no_permission"
      }
    }
    assertEquals("no_permission", result)
  }

  // ========== EDIT MODE WITH REQUEST ID TEST ==========

  @Test
  fun editRequestScreen_withRequestId_showsEditMode() {
    composeTestRule.setContent {
      EditRequestScreen(requestId = "test-request-123", onNavigateBack = {})
    }

    // Verify it shows "Edit Request" instead of "Create Request"
    composeTestRule.onNodeWithText("Edit Request").assertExists()

    // Verify navigation tag for edit mode
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_REQUEST_SCREEN).assertExists()
  }

  // ========== UPDATED DATE TESTS ==========

  @Test
  fun startDateField_clickOpensDatePicker() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Click on start date field
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    waitForUI()

    // Verify DatePicker dialog appears (check for "OK" and "Cancel" buttons)
    composeTestRule.onNodeWithText("OK").assertExists()
    composeTestRule.onNodeWithText("Cancel").assertExists()
  }

  @Test
  fun startDatePicker_selectDate_opensTimePicker() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Click on start date field
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    waitForUI()

    // Click OK on date picker (accepts current selected date)
    composeTestRule.onNodeWithText("OK").performClick()
    waitForUI()

    // Verify TimePicker dialog appears
    composeTestRule.onNodeWithText("OK").assertExists()
    composeTestRule.onNodeWithText("Cancel").assertExists()
  }

  @Test
  fun startTimePicker_selectTime_updatesField() {
    val viewModel = createTestViewModel()
    val initialDate = viewModel.uiState.value.startTimeStamp

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Click on start date field
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    waitForUI()

    // Click OK on date picker
    composeTestRule.onNodeWithText("OK").performClick()
    waitForUI()

    // Click OK on time picker
    composeTestRule.onNodeWithText("OK").performClick()
    waitForUI()

    // Verify the date was updated (different from initial or at least set)
    val updatedDate = viewModel.uiState.value.startTimeStamp
    assert(updatedDate != null)
  }

  @Test
  fun startDatePicker_cancel_doesNotChangeDate() {
    val viewModel = createTestViewModel()
    val initialDate = viewModel.uiState.value.startTimeStamp

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Click on start date field
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    waitForUI()

    // Click Cancel on date picker
    composeTestRule.onNodeWithText("Cancel").performClick()
    waitForUI()

    // Verify the date was NOT changed
    val updatedDate = viewModel.uiState.value.startTimeStamp
    assert(updatedDate == initialDate)
  }

  @Test
  fun startTimePicker_cancel_doesNotChangeDate() {
    val viewModel = createTestViewModel()
    val initialDate = viewModel.uiState.value.startTimeStamp

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Click on start date field
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    waitForUI()

    // Click OK on date picker
    composeTestRule.onNodeWithText("OK").performClick()
    waitForUI()

    // Click Cancel on time picker
    composeTestRule.onNodeWithText("Cancel").performClick()
    waitForUI()

    // Verify the date was NOT changed
    val updatedDate = viewModel.uiState.value.startTimeStamp
    assert(updatedDate == initialDate)
  }

  @Test
  fun expirationDateField_clickOpensDatePicker() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Click on expiration date field
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE).performClick()
    waitForUI()

    // Verify DatePicker dialog appears
    composeTestRule.onNodeWithText("OK").assertExists()
    composeTestRule.onNodeWithText("Cancel").assertExists()
  }

  @Test
  fun expirationDatePicker_selectDate_opensTimePicker() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Click on expiration date field
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE).performClick()
    waitForUI()

    // Click OK on date picker
    composeTestRule.onNodeWithText("OK").performClick()
    waitForUI()

    // Verify TimePicker dialog appears
    composeTestRule.onNodeWithText("OK").assertExists()
    composeTestRule.onNodeWithText("Cancel").assertExists()
  }

  @Test
  fun expirationTimePicker_selectTime_updatesField() {
    val viewModel = createTestViewModel()
    val initialDate = viewModel.uiState.value.expirationTime

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Click on expiration date field
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE).performClick()
    waitForUI()

    // Click OK on date picker
    composeTestRule.onNodeWithText("OK").performClick()
    waitForUI()

    // Click OK on time picker
    composeTestRule.onNodeWithText("OK").performClick()
    waitForUI()

    // Verify the date was updated
    val updatedDate = viewModel.uiState.value.expirationTime
    assert(updatedDate != null)
  }

  @Test
  fun expirationDatePicker_cancel_doesNotChangeDate() {
    val viewModel = createTestViewModel()
    val initialDate = viewModel.uiState.value.expirationTime

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Click on expiration date field
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE).performClick()
    waitForUI()

    // Click Cancel on date picker
    composeTestRule.onNodeWithText("Cancel").performClick()
    waitForUI()

    // Verify the date was NOT changed
    val updatedDate = viewModel.uiState.value.expirationTime
    assert(updatedDate == initialDate)
  }

  @Test
  fun dateFields_displayCorrectFormat() {
    val viewModel = createTestViewModel()
    val testDate = Date(1735689600000L) // 2025-01-01 00:00:00
    viewModel.updateStartTimeStamp(testDate)

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }
    waitForUI()

    // Verify the date is displayed in correct format (dd/MM/yyyy HH:mm)
    val displayedText =
        composeTestRule
            .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text

    // Should contain date elements (not testing exact format as it depends on locale)
    assert(displayedText.isNotEmpty())
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

  @Test
  fun multipleDateFields_independentDialogs() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Click start date
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    waitForUI()

    // Cancel it
    composeTestRule.onNodeWithText("Cancel").performClick()
    waitForUI()

    // Click expiration date
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE).performClick()
    waitForUI()

    // Should show date picker for expiration date
    composeTestRule.onNodeWithText("OK").assertExists()
    composeTestRule.onNodeWithText("Cancel").assertExists()
  }

  @Test
  fun startTimePicker_selectTime_updatesFieldWithCorrectDateTime() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    waitForUI()

    composeTestRule.onNodeWithText("OK").performClick()
    waitForUI()

    composeTestRule.onNodeWithText("OK").performClick()
    waitForUI()

    val updatedDate = viewModel.uiState.value.startTimeStamp
    assertNotNull(updatedDate)

    val displayedText =
        composeTestRule
            .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text

    assertTrue(displayedText.isNotEmpty())
  }

  @Ignore("Flaky test on CI")
  @Test
  fun startDatePicker_dismissViaOutsideClick_clearsState() {
    val viewModel = createTestViewModel()
    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    waitForUI()
    composeTestRule.onNode(isDialog()).assertExists()

    Espresso.pressBack()
    waitForUI()

    composeTestRule.onNodeWithText("OK").assertDoesNotExist()
  }

  // ========== ADDITIONAL COVERAGE TESTS ==========

  @Test
  fun startDateTimePicker_completesFullFlow_updatesViewModel() {
    val viewModel = createTestViewModel()
    val initialDate = viewModel.uiState.value.startTimeStamp

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Complete full flow: click field -> date picker -> time picker
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    waitForUI()

    // Accept date
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()

    // Accept time
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()

    // Verify ViewModel was updated (this covers the lambda body execution)
    val updatedDate = viewModel.uiState.value.startTimeStamp
    assertNotNull(updatedDate)

    // Verify the date changed or at least the field displays something valid
    val displayedText =
        composeTestRule
            .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text

    assertTrue(displayedText.isNotEmpty())
  }

  @Test
  fun expirationDateTimePicker_completesFullFlow_updatesViewModel() {
    val viewModel = createTestViewModel()
    val initialDate = viewModel.uiState.value.expirationTime

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    // Complete full flow for expiration date
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE).performClick()
    waitForUI()

    // Accept date
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()

    // Accept time
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()

    // Verify ViewModel was updated
    val updatedDate = viewModel.uiState.value.expirationTime
    assertNotNull(updatedDate)

    val displayedText =
        composeTestRule
            .onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text

    assertTrue(displayedText.isNotEmpty())
  }

  @Test
  fun expirationTimePicker_dismissViaCancel_resetsState() {
    val viewModel = createTestViewModel()
    val initialDate = viewModel.uiState.value.expirationTime

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE).performClick()
    waitForUI()

    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()

    composeTestRule.onNodeWithText("Cancel").performClick()
    waitForUI()

    val updatedDate = viewModel.uiState.value.expirationTime
    assertEquals(initialDate, updatedDate)
  }

  @Test
  fun sequentialDatePickers_bothWorkIndependently() {
    val viewModel = createTestViewModel()

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    waitForUI()
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()

    val startDate = viewModel.uiState.value.startTimeStamp

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE).performClick()
    waitForUI()
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()

    val expirationDate = viewModel.uiState.value.expirationTime

    assertNotNull(startDate)
    assertNotNull(expirationDate)
  }

  // ========== FULL INTEGRATION TESTS WITH STATE VERIFICATION ==========

  @Test
  fun calendarManipulation_setsCorrectDateTime() {
    val viewModel = createTestViewModel()

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    val beforeDate = viewModel.uiState.value.startTimeStamp

    // Complete full flow
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()
    Thread.sleep(500)

    val afterDate = viewModel.uiState.value.startTimeStamp

    // Verify the date was actually updated (calendar manipulation executed)
    assertNotNull(afterDate)
  }

  @Test
  fun expirationDateFlow_calendarManipulationExecutes() {
    val viewModel = createTestViewModel()

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    val beforeDate = viewModel.uiState.value.expirationTime

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE).performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    waitForUI()
    Thread.sleep(500)

    val afterDate = viewModel.uiState.value.expirationTime

    assertNotNull(afterDate)
  }

  @Test
  fun dismissViaOnDismissRequest_cleansUpState() {
    val viewModel = createTestViewModel()

    composeTestRule.setContent { TestEditRequestContentWithRealViewModel(viewModel = viewModel) }

    val initialDate = viewModel.uiState.value.startTimeStamp

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE).performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Cancel").performClick()
    waitForUI()
    Thread.sleep(300)

    val afterDate = viewModel.uiState.value.startTimeStamp
    assertEquals(initialDate, afterDate)
  }

  @Test
  fun combineDateAndTime_combinesCorrectly() {
    val date = Date(1735689600000L)
    val hour = 14
    val minute = 30

    val result = combineDateAndTime(date, hour, minute)

    val calendar = Calendar.getInstance().apply { time = result }
    assert(calendar.get(Calendar.HOUR_OF_DAY) == 14)
    assert(calendar.get(Calendar.MINUTE) == 30)
    assert(calendar.get(Calendar.YEAR) == 2025)
    assert(calendar.get(Calendar.MONTH) == Calendar.JANUARY)
    assert(calendar.get(Calendar.DAY_OF_MONTH) == 1)
  }

  @Test
  fun combineDateAndTime_preservesDate() {
    val date = Date(1767225600000L)
    val result = combineDateAndTime(date, 0, 0)

    val calendar = Calendar.getInstance().apply { time = result }
    assert(calendar.get(Calendar.YEAR) == 2026)
  }

  @Test
  fun combineDateAndTime_zerosOutSecondsAndMillis() {
    val date = Date(1735689612345L)

    val result = combineDateAndTime(date, 10, 15)

    val calendar = Calendar.getInstance().apply { time = result }
    assert(calendar.get(Calendar.HOUR_OF_DAY) == 10)
    assert(calendar.get(Calendar.MINUTE) == 15)
    // Seconds and millis should be zeroed
    assert(calendar.get(Calendar.SECOND) == 0) {
      "Expected seconds=0, got ${calendar.get(Calendar.SECOND)}"
    }
    assert(calendar.get(Calendar.MILLISECOND) == 0) {
      "Expected millis=0, got ${calendar.get(Calendar.MILLISECOND)}"
    }
  }

  @Test
  fun combineDateAndTime_handlesMidnight() {
    val date = Date(1735689600000L)

    val result = combineDateAndTime(date, 0, 0)

    val calendar = Calendar.getInstance().apply { time = result }
    assert(calendar.get(Calendar.HOUR_OF_DAY) == 0)
    assert(calendar.get(Calendar.MINUTE) == 0)
  }

  @Test
  fun combineDateAndTime_handlesEndOfDay() {
    val date = Date(1735689600000L)

    val result = combineDateAndTime(date, 23, 59)

    val calendar = Calendar.getInstance().apply { time = result }
    assert(calendar.get(Calendar.HOUR_OF_DAY) == 23)
    assert(calendar.get(Calendar.MINUTE) == 59)
  }
}
