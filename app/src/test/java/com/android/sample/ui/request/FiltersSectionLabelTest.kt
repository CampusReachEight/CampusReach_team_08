package com.android.sample.ui.request

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import java.util.Date
import org.junit.Rule
import org.junit.Test

class FiltersSectionLabelTest {
  @get:Rule val composeRule = createComposeRule()

  private fun sampleRequests(): List<Request> =
      listOf(
          Request(
              requestId = "1",
              title = "A",
              description = "d",
              requestType = listOf(RequestType.STUDY_GROUP),
              location = Location(0.0, 0.0, "loc"),
              locationName = "loc",
              status = RequestStatus.OPEN,
              startTimeStamp = Date(),
              expirationTime = Date(),
              people = emptyList(),
              tags = listOf(Tags.GROUP_WORK),
              creatorId = "c1"))

  @Test
  fun filter_button_hides_zero_count() {
    val vm = RequestSearchFilterViewModel()
    vm.initializeWithRequests(sampleRequests())

    composeRule.setContent {
      FiltersSection(
          searchFilterViewModel = vm,
          query = "",
          isSearching = false,
          onQueryChange = {},
          onClearQuery = {})
    }

    // Button should show just title e.g. "Type" not "Type (0)"
    composeRule.onNodeWithText("Type").assertIsDisplayed()
  }

  @Test
  fun filter_button_shows_nonzero_count() {
    val vm = RequestSearchFilterViewModel()
    vm.initializeWithRequests(sampleRequests())
    // Select one type
    val typeFacet = vm.facets.first { it.id == "type" }
    typeFacet.toggle(RequestType.STUDY_GROUP)

    composeRule.setContent {
      FiltersSection(
          searchFilterViewModel = vm,
          query = "",
          isSearching = false,
          onQueryChange = {},
          onClearQuery = {})
    }

    composeRule.onNodeWithText("Type (1)").assertIsDisplayed()
  }
}
