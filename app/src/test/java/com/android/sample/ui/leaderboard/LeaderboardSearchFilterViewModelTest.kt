package com.android.sample.ui.leaderboard

import com.android.sample.model.profile.UserProfile
import com.android.sample.ui.profile.UserSections
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardSearchFilterViewModelTest {

  companion object {
    private const val ADVANCE_TIME_SEARCH_MS = 500L
  }

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var vm: LeaderboardSearchFilterViewModel
  private lateinit var profiles: List<UserProfile>

  private fun profile(
      id: String,
      name: String,
      lastName: String,
      section: UserSections,
      kudos: Int,
      help: Int
  ): UserProfile =
      UserProfile(
          id = id,
          name = name,
          lastName = lastName,
          email = "$name.$lastName@example.com".lowercase(),
          photo = null,
          kudos = kudos,
          helpReceived = help,
          section = section,
          arrivalDate = Date())

  @Before
  fun setUp() =
      runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        vm = LeaderboardSearchFilterViewModel()
        profiles =
            listOf(
                profile("1", "John", "Doe", UserSections.COMPUTER_SCIENCE, kudos = 500, help = 5),
                profile("2", "Jane", "Smith", UserSections.MATHEMATICS, kudos = 200, help = 20),
                profile("3", "Johnny", "Appleseed", UserSections.PHYSICS, kudos = 50, help = 1),
                profile(
                    "4", "Alice", "Johnson", UserSections.COMPUTER_SCIENCE, kudos = 900, help = 10),
                profile(
                    "5",
                    "Bob",
                    "Williams",
                    UserSections.CHEMISTRY_AND_CHEMICAL_ENGINEERING,
                    kudos = 100,
                    help = 0),
            )
        vm.initializeWithProfiles(profiles)
        advanceUntilIdle()
      }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun updateSearchQuery_updates_state() =
      runTest(testDispatcher) {
        vm.updateSearchQuery(" john ")
        assertEquals(" john ", vm.searchQuery.first())
      }

  @Test
  fun search_byName_returnsMatches() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("john")
        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        advanceUntilIdle()
        val displayed = vm.displayedProfiles.first()
        assertTrue(displayed.any { it.name == "John" })
        assertTrue(displayed.any { it.name == "Johnny" })
      }

  @Test
  fun clearSearch_resets_displayed_to_base_list() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("john")
        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        advanceUntilIdle()
        val searched = vm.displayedProfiles.first()
        assertTrue(searched.size < profiles.size)

        vm.clearSearch()
        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        advanceUntilIdle()
        val displayed = vm.displayedProfiles.first()
        assertEquals(profiles.size, displayed.size)
      }

  @Test
  fun sectionFacet_filters_profiles() =
      runTest(testDispatcher) {
        val sectionFacet = vm.facets.first { it.id == "section" }
        sectionFacet.toggle(UserSections.COMPUTER_SCIENCE)
        advanceUntilIdle()
        val displayed = vm.displayedProfiles.first()
        assertTrue(displayed.all { it.section == UserSections.COMPUTER_SCIENCE })
      }

  @Test
  fun kudosRange_filters_profiles() =
      runTest(testDispatcher) {
        val kudosRange = vm.rangeFacets.first { it.id == "kudos" }
        kudosRange.setRange(300..1000)
        advanceUntilIdle()
        val displayed = vm.displayedProfiles.first()
        assertTrue(displayed.all { it.kudos in 300..1000 })
      }

  @Test
  fun combined_search_and_filters() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("john")
        val kudosRange = vm.rangeFacets.first { it.id == "kudos" }
        kudosRange.setRange(400..800)
        val sectionFacet = vm.facets.first { it.id == "section" }
        sectionFacet.toggle(UserSections.COMPUTER_SCIENCE)

        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        advanceUntilIdle()

        val displayed = vm.displayedProfiles.first()
        assertEquals(1, displayed.size)
        assertEquals("John", displayed.first().name)
      }

  @Test
  fun setSortCriteria_applies_order() =
      runTest(testDispatcher) {
        vm.setSortCriteria(LeaderboardSort.NAME_ASC)
        advanceUntilIdle()
        val displayed = vm.displayedProfiles.first()
        assertTrue(displayed.size == profiles.size)
        assertEquals("Alice", displayed.first().name)
      }

  @Test
  fun clearAllFilters_resets_facets_and_ranges() =
      runTest(testDispatcher) {
        val sectionFacet = vm.facets.first { it.id == "section" }
        sectionFacet.toggle(UserSections.COMPUTER_SCIENCE)
        val kudosRange = vm.rangeFacets.first { it.id == "kudos" }
        kudosRange.setRange(400..800)

        vm.clearAllFilters()
        advanceUntilIdle()

        assertTrue(sectionFacet.selected.first().isEmpty())
        assertEquals(kudosRange.fullRange, kudosRange.currentRange.first())
      }

  @Test
  fun search_before_index_returns_empty() =
      runTest(testDispatcher) {
        val fresh = LeaderboardSearchFilterViewModel()
        fresh.updateSearchQuery("john")
        advanceTimeBy(ADVANCE_TIME_SEARCH_MS)
        advanceUntilIdle()
        assertTrue(fresh.displayedProfiles.first().isEmpty())
      }
}
