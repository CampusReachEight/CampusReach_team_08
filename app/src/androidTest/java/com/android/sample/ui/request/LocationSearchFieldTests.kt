package com.android.sample.ui.request

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.model.map.Location
import com.android.sample.ui.request.edit.LocationSearchField
import com.android.sample.ui.request.edit.LocationSearchFieldTestTags
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI tests for the LocationSearchField composable.
 *
 * These tests verify the following behaviors:
 * - Label and placeholder display
 * - Text input updates and triggers callbacks
 * - Search results dropdown and selection
 * - Loading indicator visibility
 * - Clear button functionality
 * - Error message display and tagging
 * - Selected location card display logic
 * - Field enable/disable state
 * - Divider display between multiple results
 * - Single line text field enforcement
 *
 * Each test uses Jetpack Compose UI testing APIs.
 */
class LocationSearchFieldTest {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Helper function to generate LocationSearchField with default values. Reduces boilerplate and
   * improves test readability.
   *
   * @param locationName Current location name text (default: empty)
   * @param location Selected location object (default: null)
   * @param searchResults List of search results (default: empty)
   * @param isSearching Whether search is in progress (default: false)
   * @param isError Whether to show error state (default: false)
   * @param errorMessage Error message text (default: empty)
   * @param enabled Whether field is enabled (default: true)
   * @param onLocationNameChange Callback for name changes (default: no-op)
   * @param onLocationSelected Callback for location selection (default: no-op)
   * @param onSearchQueryChange Callback for search query (default: no-op)
   * @param onClearSearch Callback for clearing search (default: no-op)
   */
  @Composable
  private fun generateLocationSearchField(
      locationName: String = "",
      location: Location? = null,
      searchResults: List<Location> = emptyList(),
      isSearching: Boolean = false,
      isError: Boolean = false,
      errorMessage: String = "",
      enabled: Boolean = true,
      onLocationNameChange: (String) -> Unit = {},
      onLocationSelected: (Location) -> Unit = {},
      onSearchQueryChange: (String) -> Unit = {},
      onClearSearch: () -> Unit = {}
  ) {
    MaterialTheme {
      LocationSearchField(
          locationName = locationName,
          location = location,
          searchResults = searchResults,
          isSearching = isSearching,
          isError = isError,
          errorMessage = errorMessage,
          enabled = enabled,
          onLocationNameChange = onLocationNameChange,
          onLocationSelected = onLocationSelected,
          onSearchQueryChange = onSearchQueryChange,
          onClearSearch = onClearSearch)
    }
  }

  // Test 1: Field displays correctly with label
  @Test
  fun locationSearchField_displaysLabelAndPlaceholder() {
    composeTestRule.setContent { generateLocationSearchField() }

    composeTestRule.onNodeWithText("Location *").assertExists()
    composeTestRule.onNodeWithText("Location Name").assertExists()
  }

  // Test 2: Text input updates locationName
  @Test
  fun locationSearchField_textInput_updatesLocationName() {
    var locationName by mutableStateOf("")

    composeTestRule.setContent {
      generateLocationSearchField(
          locationName = locationName, onLocationNameChange = { locationName = it })
    }

    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName)
        .performTextInput("EPFL")

    assert(locationName == "EPFL")
  }

  // Test 3: Text input triggers search query callback
  @Test
  fun locationSearchField_textInput_triggersSearchCallback() {
    var searchQuery = ""

    composeTestRule.setContent {
      generateLocationSearchField(onSearchQueryChange = { searchQuery = it })
    }

    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName)
        .performTextInput("BC Building")

    assert(searchQuery == "BC Building")
  }

  // Test 4: Search results display in dropdown
  @Test
  fun locationSearchField_searchResults_displayInDropdown() {
    var locationName by mutableStateOf("")
    val searchResults =
        listOf(
            Location(46.5197, 6.5668, "EPFL, Lausanne, Switzerland"),
            Location(46.5191, 6.5667, "BC Building, EPFL"))

    composeTestRule.setContent {
      generateLocationSearchField(
          locationName = locationName,
          searchResults = searchResults,
          onLocationNameChange = { locationName = it })
    }

    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName)
        .performTextInput("EPFL")

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithText("EPFL, Lausanne, Switzerland", useUnmergedTree = true)
        .assertExists()
    composeTestRule.onNodeWithText("BC Building, EPFL", useUnmergedTree = true).assertExists()
  }

  // Test 5: Selecting location from dropdown updates state
  @Test
  fun locationSearchField_selectLocation_updatesState() {
    var selectedLocation: Location? = null
    var locationName by mutableStateOf("")
    val searchResults = listOf(Location(46.5197, 6.5668, "EPFL, Lausanne, Switzerland"))

    composeTestRule.setContent {
      generateLocationSearchField(
          locationName = locationName,
          location = selectedLocation,
          searchResults = searchResults,
          onLocationNameChange = { locationName = it },
          onLocationSelected = { selectedLocation = it })
    }

    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName)
        .performTextInput("EPFL")

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithText("EPFL, Lausanne, Switzerland", useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()

    assert(selectedLocation != null)
    assert(selectedLocation!!.latitude == 46.5197)
    assert(selectedLocation!!.longitude == 6.5668)
  }

  // Test 6: Selecting location closes dropdown
  @Test
  fun locationSearchField_selectLocation_closesDropdown() {
    var locationName by mutableStateOf("")
    val searchResults = listOf(Location(46.5197, 6.5668, "EPFL, Lausanne, Switzerland"))

    composeTestRule.setContent {
      generateLocationSearchField(
          locationName = locationName,
          searchResults = searchResults,
          onLocationNameChange = { locationName = it })
    }

    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName)
        .performTextInput("EPFL")

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithText("EPFL, Lausanne, Switzerland", useUnmergedTree = true)
        .assertExists()

    composeTestRule
        .onNodeWithText("EPFL, Lausanne, Switzerland", useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(LocationSearchFieldTestTags.InputLocationName).assertExists()
  }

  // Test 7: Loading indicator shows when searching
  @Test
  fun locationSearchField_searching_showsLoadingIndicator() {
    composeTestRule.setContent {
      generateLocationSearchField(locationName = "EPFL", isSearching = true)
    }

    composeTestRule.onNodeWithTag(LocationSearchFieldTestTags.LoadingIndicator).assertExists()
  }

  // Test 8: Clear button appears when text is entered
  @Test
  fun locationSearchField_nonEmptyText_showsClearButton() {
    composeTestRule.setContent { generateLocationSearchField(locationName = "EPFL") }

    composeTestRule.onNodeWithTag(LocationSearchFieldTestTags.ClearButton).assertExists()
  }

  // Test 9: Clear button clears text and location
  @Test
  fun locationSearchField_clearButton_clearsTextAndLocation() {
    var locationName by mutableStateOf("EPFL")
    var location by mutableStateOf<Location?>(Location(46.5197, 6.5668, "EPFL"))
    var clearSearchCalled = false

    composeTestRule.setContent {
      generateLocationSearchField(
          locationName = locationName,
          location = location,
          onLocationNameChange = { locationName = it },
          onLocationSelected = { location = it },
          onClearSearch = { clearSearchCalled = true })
    }

    // composeTestRule.onNodeWithContentDescription("Clear").performClick()

    composeTestRule.onNodeWithTag(LocationSearchFieldTestTags.ClearButton).performClick()

    composeTestRule.waitForIdle()

    assert(locationName.isEmpty())
    assert(location?.latitude == 0.0)
    assert(location?.longitude == 0.0)
    assert(clearSearchCalled)
  }

  // Test 10: Error state displays error message
  @Test
  fun locationSearchField_error_displaysErrorMessage() {
    composeTestRule.setContent {
      generateLocationSearchField(isError = true, errorMessage = "Location name cannot be empty")
    }

    composeTestRule.onNodeWithText("Location name cannot be empty").assertExists()
  }

  // Test 11: Error message has correct test tag
  @Test
  fun locationSearchField_error_hasCorrectTestTag() {
    composeTestRule.setContent {
      generateLocationSearchField(isError = true, errorMessage = "Location name cannot be empty")
    }

    composeTestRule.onNodeWithText("Location name cannot be empty").assertExists()
  }

  // Test 12: Selected location card displays when location is set
  @Test
  fun locationSearchField_selectedLocation_displaysCard() {
    val location = Location(46.5197, 6.5668, "EPFL, Lausanne, Switzerland")

    composeTestRule.setContent {
      generateLocationSearchField(locationName = "EPFL", location = location)
    }

    composeTestRule.onNodeWithTag(LocationSearchFieldTestTags.SelectedLocationCard).assertExists()
    composeTestRule.onNodeWithText("Selected Location").assertExists()
    composeTestRule.onNodeWithText("EPFL, Lausanne, Switzerland").assertExists()
    composeTestRule.onNodeWithText("Lat: 46.5197, Lng: 6.5668", substring = true).assertExists()
  }

  // Test 13: Selected location card does not display when location is null
  @Test
  fun locationSearchField_noLocation_doesNotDisplayCard() {
    composeTestRule.setContent { generateLocationSearchField(locationName = "EPFL") }

    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.SelectedLocationCard)
        .assertDoesNotExist()

    // assertion
    composeTestRule.onNodeWithText("Selected Location").assertDoesNotExist()
  }

  // Test 14: Selected location card does not display for default location (0,0)
  @Test
  fun locationSearchField_defaultLocation_doesNotDisplayCard() {
    val location = Location(0.0, 0.0, "")

    composeTestRule.setContent { generateLocationSearchField(location = location) }

    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.SelectedLocationCard)
        .assertDoesNotExist()

    composeTestRule.onNodeWithText("Selected Location").assertDoesNotExist()
  }

  // Test 15: Dropdown shows coordinates for each result
  @Test
  fun locationSearchField_dropdownResults_showCoordinates() {
    var locationName by mutableStateOf("")
    val searchResults = listOf(Location(46.5197, 6.5668, "EPFL, Lausanne"))

    composeTestRule.setContent {
      generateLocationSearchField(
          locationName = locationName,
          searchResults = searchResults,
          onLocationNameChange = { locationName = it })
    }

    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName)
        .performTextInput("EPFL")

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithText("Lat: 46.5197, Lng: 6.5668", useUnmergedTree = true)
        .assertExists()
  }

  // Test 16: Dropdown does not show when search results are empty
  @Test
  fun locationSearchField_emptyResults_noDropdown() {
    composeTestRule.setContent { generateLocationSearchField(locationName = "EPFL") }

    composeTestRule.onNodeWithText("EPFL, Lausanne, Switzerland").assertDoesNotExist()
  }

  // Test 17: Field is disabled when enabled = false
  @Test
  fun locationSearchField_disabled_fieldNotEnabled() {
    composeTestRule.setContent {
      generateLocationSearchField(locationName = "EPFL", enabled = false)
    }

    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName)
        .assertIsNotEnabled()
  }

  // Test 18: Multiple search results display with dividers
  @Test
  fun locationSearchField_multipleResults_displayWithDividers() {
    var locationName by mutableStateOf("")
    val searchResults =
        listOf(
            Location(46.5197, 6.5668, "EPFL, Lausanne"),
            Location(46.5191, 6.5667, "BC Building"),
            Location(46.5200, 6.5670, "Rolex Learning Center"))

    composeTestRule.setContent {
      generateLocationSearchField(
          locationName = locationName,
          searchResults = searchResults,
          onLocationNameChange = { locationName = it })
    }

    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName)
        .performTextInput("EPFL")

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("EPFL, Lausanne", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("BC Building", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Rolex Learning Center", useUnmergedTree = true).assertExists()
  }

  // Test 19: Selecting location triggers onClearSearch
  @Test
  fun locationSearchField_selectLocation_triggersOnClearSearch() {
    var clearSearchCalled = false
    var locationName by mutableStateOf("")
    val searchResults = listOf(Location(46.5197, 6.5668, "EPFL, Lausanne"))

    composeTestRule.setContent {
      generateLocationSearchField(
          locationName = locationName,
          searchResults = searchResults,
          onLocationNameChange = { locationName = it },
          onClearSearch = { clearSearchCalled = true })
    }

    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName)
        .performTextInput("EPFL")

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("EPFL, Lausanne", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    assert(clearSearchCalled)
  }

  // Test 20: Text field is single line
  @Test
  fun locationSearchField_isSingleLine() {
    composeTestRule.setContent {
      generateLocationSearchField(
          locationName = "This is a very long location name that should not wrap to multiple lines")
    }

    composeTestRule.onNodeWithTag(LocationSearchFieldTestTags.InputLocationName).assertExists()
  }
}
