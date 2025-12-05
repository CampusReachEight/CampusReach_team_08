package com.android.sample.model.search

import com.android.sample.model.profile.UserProfile
import com.android.sample.ui.profile.UserSections
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [LuceneProfileSearchEngine].
 *
 * This search engine uses prefix matching to support partial name searches:
 * - "Jo" matches "John", "Johnny", "Johnson"
 * - "John Sm" matches "John Smith" (both terms must match)
 * - Search is case-insensitive
 * - Only first name and last name are indexed (via [UserProfile.toSearchText])
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LuceneProfileSearchEngineTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var engine: LuceneProfileSearchEngine

  private val testProfiles =
      listOf(
          createProfile("1", "John", "Doe", UserSections.COMPUTER_SCIENCE, kudos = 100),
          createProfile("2", "Jane", "Smith", UserSections.MATHEMATICS, kudos = 250),
          createProfile("3", "Johnny", "Appleseed", UserSections.PHYSICS, kudos = 50),
          createProfile("4", "Alice", "Johnson", UserSections.COMPUTER_SCIENCE, kudos = 300),
          createProfile(
              "5", "Bob", "Williams", UserSections.CHEMISTRY_AND_CHEMICAL_ENGINEERING, kudos = 75),
      )

  private fun createProfile(
      id: String,
      name: String,
      lastName: String,
      section: UserSections,
      kudos: Int = 0,
      helpReceived: Int = 0
  ): UserProfile {
    return UserProfile(
        id = id,
        name = name,
        lastName = lastName,
        email = "$name.$lastName@example.com".lowercase(),
        photo = null,
        kudos = kudos,
        helpReceived = helpReceived,
        section = section,
        arrivalDate = Date())
  }

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    engine = LuceneProfileSearchEngine()
  }

  @After
  fun tearDown() {
    engine.close()
    Dispatchers.resetMain()
  }

  // ========== Basic search tests ==========

  @Test
  fun indexProfiles_and_search_returnsMatchingResults() = runTest {
    engine.indexProfiles(testProfiles)

    val results = engine.search(emptyList(), "John")

    assertTrue("Expected to find profiles matching 'John'", results.isNotEmpty())
    assertTrue(
        "Expected to find John Doe",
        results.any { it.item.name == "John" && it.item.lastName == "Doe" })
  }

  @Test
  fun search_byFirstName_returnsMatchingProfiles() = runTest {
    engine.indexProfiles(testProfiles)

    val results = engine.search(emptyList(), "Jane")

    assertEquals(1, results.size)
    assertEquals("Jane", results[0].item.name)
    assertEquals("Smith", results[0].item.lastName)
  }

  @Test
  fun search_byLastName_returnsMatchingProfiles() = runTest {
    engine.indexProfiles(testProfiles)

    val results = engine.search(emptyList(), "Williams")

    assertEquals(1, results.size)
    assertEquals("Bob", results[0].item.name)
    assertEquals("Williams", results[0].item.lastName)
  }

  // ========== Prefix/partial matching tests ==========

  @Test
  fun search_partialFirstName_matchesPrefix() = runTest {
    engine.indexProfiles(testProfiles)

    // "jo" should match "John", "Johnny" (first names starting with "jo")
    val results = engine.search(emptyList(), "jo")

    assertTrue("Expected at least 2 results for prefix 'jo'", results.size >= 2)
    val firstNames = results.map { it.item.name }
    assertTrue("Expected John in results", firstNames.contains("John"))
    assertTrue("Expected Johnny in results", firstNames.contains("Johnny"))
  }

  @Test
  fun search_partialLastName_matchesPrefix() = runTest {
    engine.indexProfiles(testProfiles)

    // "smi" should match "Smith"
    val results = engine.search(emptyList(), "smi")

    assertTrue("Expected results for prefix 'smi'", results.isNotEmpty())
    assertTrue("Expected Smith in results", results.any { it.item.lastName == "Smith" })
  }

  @Test
  fun search_singleCharacter_matchesPrefix() = runTest {
    engine.indexProfiles(testProfiles)

    // "j" should match John, Jane, Johnny, Johnson (anyone with j in name)
    val results = engine.search(emptyList(), "j")

    assertTrue("Expected multiple results for single char 'j'", results.size >= 3)
  }

  @Test
  fun search_fullName_matchesExactly() = runTest {
    engine.indexProfiles(testProfiles)

    // "John" should match John Doe, Johnny Appleseed, and Alice Johnson
    val results = engine.search(emptyList(), "John")

    assertTrue("Expected at least 2 results for 'John'", results.size >= 2)
    val names = results.map { "${it.item.name} ${it.item.lastName}" }
    assertTrue("Expected John Doe in results", names.any { it.contains("John Doe") })
    assertTrue(
        "Expected Johnny or Johnson in results",
        names.any { it.contains("Johnny") || it.contains("Johnson") })
  }

  // ========== Multi-term query tests ==========

  @Test
  fun search_twoTerms_matchesBothFirstAndLastName() = runTest {
    engine.indexProfiles(testProfiles)

    // "John Doe" should primarily match John Doe (both terms must match)
    val results = engine.search(emptyList(), "John Doe")

    assertTrue("Expected results for 'John Doe'", results.isNotEmpty())
    assertTrue(
        "Expected John Doe to be in results",
        results.any { it.item.name == "John" && it.item.lastName == "Doe" })
  }

  @Test
  fun search_partialFirstAndLastName_matches() = runTest {
    engine.indexProfiles(testProfiles)

    // "ja sm" should match "Jane Smith" (prefix match on both terms)
    val results = engine.search(emptyList(), "ja sm")

    assertTrue("Expected results for 'ja sm'", results.isNotEmpty())
    assertTrue(
        "Expected Jane Smith in results",
        results.any { it.item.name == "Jane" && it.item.lastName == "Smith" })
  }

  @Test
  fun search_multipleTerms_allMustMatch() = runTest {
    engine.indexProfiles(testProfiles)

    // "John Williams" should NOT match anyone (no one has both John and Williams)
    val results = engine.search(emptyList(), "John Williams")

    assertTrue("Expected no results for 'John Williams' - no one has both", results.isEmpty())
  }

  // ========== Case insensitivity tests ==========

  @Test
  fun search_caseInsensitive_lowercase() = runTest {
    engine.indexProfiles(testProfiles)

    val results = engine.search(emptyList(), "jane")

    assertEquals("Lowercase search should find Jane", 1, results.size)
    assertEquals("Jane", results[0].item.name)
  }

  @Test
  fun search_caseInsensitive_uppercase() = runTest {
    engine.indexProfiles(testProfiles)

    val results = engine.search(emptyList(), "JANE")

    assertEquals("Uppercase search should find Jane", 1, results.size)
    assertEquals("Jane", results[0].item.name)
  }

  @Test
  fun search_caseInsensitive_mixedCase() = runTest {
    engine.indexProfiles(testProfiles)

    val results = engine.search(emptyList(), "JaNe")

    assertEquals("Mixed case search should find Jane", 1, results.size)
    assertEquals("Jane", results[0].item.name)
  }

  // ========== Edge cases ==========

  @Test
  fun search_emptyQuery_returnsEmptyList() = runTest {
    engine.indexProfiles(testProfiles)

    val results = engine.search(emptyList(), "")

    assertTrue("Empty query should return empty results", results.isEmpty())
  }

  @Test
  fun search_blankQuery_returnsEmptyList() = runTest {
    engine.indexProfiles(testProfiles)

    val results = engine.search(emptyList(), "   ")

    assertTrue("Blank query should return empty results", results.isEmpty())
  }

  @Test
  fun search_noMatch_returnsEmptyList() = runTest {
    engine.indexProfiles(testProfiles)

    val results = engine.search(emptyList(), "Zzyzzyxx")

    assertTrue("Non-matching query should return empty results", results.isEmpty())
  }

  @Test
  fun search_beforeIndex_returnsEmptyList() = runTest {
    // Don't index profiles
    val results = engine.search(emptyList(), "John")

    assertTrue("Search before indexing should return empty results", results.isEmpty())
  }

  // ========== Scoring tests ==========

  @Test
  fun search_resultsHaveScores() = runTest {
    engine.indexProfiles(testProfiles)

    val results = engine.search(emptyList(), "John")

    assertTrue("Expected results", results.isNotEmpty())
    results.forEach { result ->
      assertTrue("Score should be between 0 and 100", result.score in 0..100)
    }
  }

  @Test
  fun search_resultsAreSortedByRelevance() = runTest {
    engine.indexProfiles(testProfiles)

    val results = engine.search(emptyList(), "John Doe")

    if (results.size > 1) {
      for (i in 0 until results.size - 1) {
        assertTrue(
            "Results should be sorted by score descending",
            results[i].score >= results[i + 1].score)
      }
    }
  }

  // ========== Re-indexing tests ==========

  @Test
  fun indexProfiles_replacesExistingIndex() = runTest {
    // Index initial profiles
    engine.indexProfiles(testProfiles)
    val initialResults = engine.search(emptyList(), "John")
    assertTrue("Initial search should find John", initialResults.isNotEmpty())

    // Re-index with different profiles
    val newProfiles =
        listOf(
            createProfile("10", "Michael", "Brown", UserSections.PHYSICS),
            createProfile("11", "Sarah", "Davis", UserSections.MATHEMATICS),
        )
    engine.indexProfiles(newProfiles)

    // Old profiles should not be found
    val oldResults = engine.search(emptyList(), "John")
    assertTrue("Old profiles should not be found after re-indexing", oldResults.isEmpty())

    // New profiles should be found
    val newResults = engine.search(emptyList(), "Michael")
    assertEquals("New profiles should be found", 1, newResults.size)
    assertEquals("Michael", newResults[0].item.name)
  }

  // ========== Special characters tests ==========

  @Test
  fun search_specialCharacters_handledSafely() = runTest {
    engine.indexProfiles(testProfiles)

    // These should not throw exceptions - special chars are escaped
    val results1 = engine.search(emptyList(), "John*")
    val results2 = engine.search(emptyList(), "John?")
    val results3 = engine.search(emptyList(), "John+Doe")
    val results4 = engine.search(emptyList(), "(John)")

    // Just verify no exceptions were thrown
    assertNotNull(results1)
    assertNotNull(results2)
    assertNotNull(results3)
    assertNotNull(results4)
  }

  // ========== Lifecycle tests ==========

  @Test
  fun close_canBeCalledMultipleTimes() {
    // Should not throw
    engine.close()
    engine.close()
  }

  @Test
  fun search_afterClose_returnsEmptyList() = runTest {
    engine.indexProfiles(testProfiles)
    engine.close()

    val results = engine.search(emptyList(), "John")

    // After close, searcher is null, so should return empty
    assertTrue("Search after close should return empty", results.isEmpty())
  }

  // ========== toSearchText integration tests ==========

  @Test
  fun userProfile_toSearchText_containsFirstAndLastName() {
    val profile = createProfile("1", "John", "Doe", UserSections.COMPUTER_SCIENCE)

    val searchText = profile.toSearchText()

    assertTrue("Search text should contain first name", searchText.contains("John"))
    assertTrue("Search text should contain last name", searchText.contains("Doe"))
  }

  @Test
  fun userProfile_toSearchText_doesNotContainSection() {
    val profile = createProfile("1", "John", "Doe", UserSections.COMPUTER_SCIENCE)

    val searchText = profile.toSearchText()

    assertFalse("Search text should NOT contain section", searchText.contains("COMPUTER"))
    assertFalse(
        "Search text should NOT contain section label", searchText.contains("Computer Science"))
  }
}
