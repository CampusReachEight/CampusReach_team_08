package com.android.sample.ui.leaderboard

import com.android.sample.model.profile.UserProfile
import com.android.sample.model.search.LuceneProfileSearchEngine
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
        vm =
            LeaderboardSearchFilterViewModel(
                engineFactory = { LuceneProfileSearchEngine(dispatcher = testDispatcher) })
        profiles =
            listOf(
                profile("1", "John", "Doe", UserSections.COMPUTER_SCIENCE, kudos = 90, help = 5),
                profile("2", "Jane", "Smith", UserSections.MATHEMATICS, kudos = 80, help = 20),
                profile("3", "Johnny", "Appleseed", UserSections.PHYSICS, kudos = 50, help = 1),
                profile(
                    "4", "Alice", "Johnson", UserSections.COMPUTER_SCIENCE, kudos = 95, help = 10),
                profile(
                    "5",
                    "Bob",
                    "Williams",
                    UserSections.CHEMISTRY_AND_CHEMICAL_ENGINEERING,
                    kudos = 10,
                    help = 0),
            )
        vm.initializeWithProfiles(profiles)
        advanceUntilIdle()
        // Wait for initial state to settle
        advanceTimeBy(1000)
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
        advanceTimeBy(1000)
        advanceUntilIdle()
        val displayed = vm.displayedProfiles.value
        assertTrue(displayed.any { it.name == "John" })
        assertTrue(displayed.any { it.name == "Johnny" })
      }

  @Test
  fun clearSearch_resets_displayed_to_base_list() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("john")
        advanceTimeBy(1000)
        advanceUntilIdle()

        vm.clearSearch()
        advanceTimeBy(1000)
        advanceUntilIdle()

        assertEquals(profiles.size, vm.displayedProfiles.value.size)
      }

  @Test
  fun facet_selection_filters_profiles() =
      runTest(testDispatcher) {
        val sectionFacet = vm.facets.first { it.id == "section" }
        sectionFacet.toggle(UserSections.COMPUTER_SCIENCE)
        advanceUntilIdle()

        val displayed = vm.displayedProfiles.value
        assertTrue(displayed.all { it.section == UserSections.COMPUTER_SCIENCE })
        assertEquals(2, displayed.size)
      }

  @Test
  fun kudosRange_filters_profiles() =
      runTest(testDispatcher) {
        val kudosFacet = vm.rangeFacets.first { it.id == "kudos" }
        kudosFacet.setRange(85..100)
        advanceUntilIdle()

        val displayed = vm.displayedProfiles.value
        assertTrue(displayed.all { it.kudos in 85..100 })
        assertEquals(2, displayed.size) // John (90) and Alice (95)
      }

  @Test
  fun combined_search_and_filters() =
      runTest(testDispatcher) {
        vm.updateSearchQuery("john")
        advanceTimeBy(1000)
        advanceUntilIdle()

        val sectionFacet = vm.facets.first { it.id == "section" }
        sectionFacet.toggle(UserSections.COMPUTER_SCIENCE)
        advanceUntilIdle()

        val displayed = vm.displayedProfiles.value
        // Should match "John" (CS) but not "Johnny" (Physics) or "Alice" (CS, but name doesn't
        // match "john" - wait, Alice Johnson matches "john" via last name?)
        // "Alice Johnson" -> "Johnson" contains "john".
        // So Alice is a match for "john".
        // John Doe (CS) -> Match
        // Johnny Appleseed (Physics) -> Match name, but not section
        // Alice Johnson (CS) -> Match name (Johnson), Match section

        assertTrue(displayed.any { it.name == "John" })
        assertTrue(displayed.any { it.name == "Alice" })
        assertTrue(displayed.none { it.name == "Johnny" })
      }

  @Test
  fun setSortCriteria_applies_order() =
      runTest(testDispatcher) {
        vm.setSortCriteria(LeaderboardSort.KUDOS_DESC)
        advanceUntilIdle()

        val displayed = vm.displayedProfiles.value
        val kudos = displayed.map { it.kudos }
        assertEquals(kudos.sortedDescending(), kudos)
      }

  @Test
  fun clearAllFilters_resets_facets_and_ranges() =
      runTest(testDispatcher) {
        val sectionFacet = vm.facets.first { it.id == "section" }
        sectionFacet.toggle(UserSections.PHYSICS)

        val kudosFacet = vm.rangeFacets.first { it.id == "kudos" }
        kudosFacet.setRange(0..10)

        advanceUntilIdle()
        assertTrue(vm.displayedProfiles.value.size < profiles.size)

        vm.clearAllFilters()
        advanceUntilIdle()

        assertEquals(profiles.size, vm.displayedProfiles.value.size)
        assertTrue(sectionFacet.selected.value.isEmpty())
        assertEquals(
            kudosFacet.def.minBound..kudosFacet.def.maxBound, kudosFacet.currentRange.value)
      }
}
