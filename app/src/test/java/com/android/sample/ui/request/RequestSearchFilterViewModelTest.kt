package com.android.sample.ui.request

import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RequestSearchFilterViewModelTest {
  companion object {
    const val ADVANCE_TIME_SHORT_MS = 10L
    const val ADVANCE_TIME_SEARCH_MS = 350L
    const val ONE_HOUR_MS = 3_600_000L
    const val COUNT_ONE = 1
  }

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var vm: RequestSearchFilterViewModel
  private lateinit var requests: List<Request>

  private fun facet(id: String) = vm.facets.first { it.id == id }

  @Before
  fun setUp() =
      runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        vm = RequestSearchFilterViewModel()
        requests =
            listOf(
                req(
                    "1",
                    "Study group",
                    "Calculus",
                    "Library",
                    listOf(RequestType.STUDY_GROUP),
                    listOf(Tags.GROUP_WORK),
                    RequestStatus.OPEN),
                req(
                    "2",
                    "Pizza night",
                    "Fresh pizza",
                    "Cafeteria",
                    listOf(RequestType.EATING),
                    listOf(Tags.INDOOR),
                    RequestStatus.IN_PROGRESS),
                req(
                    "3",
                    "Football",
                    "Outdoor sport",
                    "Stadium",
                    listOf(RequestType.SPORT),
                    listOf(Tags.OUTDOOR),
                    RequestStatus.OPEN),
            )
        vm.initializeWithRequests(requests)
        advanceTimeBy(ADVANCE_TIME_SHORT_MS)
      }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun updateSearchQuery_updates_state() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("  pizza  ")
        assertEquals("pizza", vm.searchQuery.first())
      }

  @Test
  fun clearSearch_resets_query() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("pizza")
        vm.clearSearch()
        assertEquals("", vm.searchQuery.first())
      }

  @Test
  fun debounced_search_triggers_after_300ms() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("pizza")
        var displayed = vm.displayedRequests.first()
        assertTrue(displayed.isEmpty() || displayed == requests)
        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        displayed = vm.displayedRequests.first()
        assertTrue(displayed.any { it.title.contains("Pizza", ignoreCase = true) })
      }

  @Test
  fun facet_filters_AND_with_search() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("pizza")
        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        val statusFacet = facet("status")
        statusFacet.toggle(RequestStatus.OPEN)
        val displayed = vm.displayedRequests.first()
        assertTrue(displayed.isEmpty())
      }

  @Test
  fun toggle_methods_update_state() =
      runTest(testDispatcher) {
        val typeFacet = facet("type")
        val statusFacet = facet("status")
        val tagFacet = facet("tags")
        typeFacet.toggle(RequestType.EATING)
        assertTrue(typeFacet.selected.first().contains(RequestType.EATING))
        statusFacet.toggle(RequestStatus.OPEN)
        assertTrue(statusFacet.selected.first().contains(RequestStatus.OPEN))
        tagFacet.toggle(Tags.INDOOR)
        assertTrue(tagFacet.selected.first().contains(Tags.INDOOR))
      }

  @Test
  fun clearAllFilters_resets_all() =
      runTest(testDispatcher) {
        val typeFacet = facet("type")
        val statusFacet = facet("status")
        val tagFacet = facet("tags")
        typeFacet.toggle(RequestType.EATING)
        statusFacet.toggle(RequestStatus.OPEN)
        tagFacet.toggle(Tags.INDOOR)
        vm.clearAllFilters()
        assertTrue(typeFacet.selected.first().isEmpty())
        assertTrue(statusFacet.selected.first().isEmpty())
        assertTrue(tagFacet.selected.first().isEmpty())
      }

  @Test
  fun initializeWithRequests_indexes_and_searches() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("football")
        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        val displayed = vm.displayedRequests.first()
        assertTrue(displayed.any { it.title.contains("football", ignoreCase = true) })
      }

  @Test
  fun combined_flow_filters_correctly() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("pizza")
        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        val tagFacet = facet("tags")
        tagFacet.toggle(Tags.INDOOR)
        val displayed = vm.displayedRequests.first()
        assertEquals(COUNT_ONE, displayed.size)
        assertEquals("2", displayed.first().requestId)
      }

  @Test
  fun search_fallback_when_engine_unavailable_does_not_crash() =
      runTest(testDispatcher) {
        val fresh = RequestSearchFilterViewModel()
        fresh.updateSearchQuery("anything")
        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        assertTrue(fresh.displayedRequests.first().isEmpty())
      }

  @Test
  fun clearSearch_resets_displayed_to_base_list() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("pizza")
        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        val searched = vm.displayedRequests.first()
        assertTrue(searched.size < requests.size)
        vm.clearSearch()
        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        val displayed = vm.displayedRequests.first()
        assertEquals(requests.size, displayed.size)
      }

  private fun req(
      id: String,
      title: String,
      description: String,
      locationName: String,
      types: List<RequestType>,
      tags: List<Tags>,
      status: RequestStatus
  ): Request =
      Request(
          requestId = id,
          title = title,
          description = description,
          requestType = types,
          location = Location(0.0, 0.0, locationName),
          locationName = locationName,
          status = status,
          startTimeStamp = Date(),
          expirationTime = Date(Date().time + ONE_HOUR_MS),
          people = emptyList(),
          tags = tags,
          creatorId = "creator-$id")
}
