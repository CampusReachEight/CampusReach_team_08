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
 * Tests cover: prefix matching, multi-term queries, case insensitivity, edge cases, and lifecycle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LuceneProfileSearchEngineTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var engine: LuceneProfileSearchEngine

  private val testProfiles =
      listOf(
          createProfile("1", "John", "Doe", UserSections.COMPUTER_SCIENCE),
          createProfile("2", "Jane", "Smith", UserSections.MATHEMATICS),
          createProfile("3", "Johnny", "Appleseed", UserSections.PHYSICS),
          createProfile("4", "Alice", "Johnson", UserSections.COMPUTER_SCIENCE),
          createProfile("5", "Bob", "Williams", UserSections.CHEMISTRY_AND_CHEMICAL_ENGINEERING),
      )

  private fun createProfile(
      id: String,
      name: String,
      lastName: String,
      section: UserSections
  ): UserProfile =
      UserProfile(
          id = id,
          name = name,
          lastName = lastName,
          email = "$name.$lastName@example.com".lowercase(),
          photo = null,
          kudos = 0,
          helpReceived = 0,
          section = section,
          arrivalDate = Date())

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

  @Test
  fun search_byExactFirstName_returnsMatch() = runTest {
    engine.indexProfiles(testProfiles)
    val results = engine.search(emptyList(), "Jane")
    assertEquals(1, results.size)
    assertEquals("Jane", results[0].item.name)
  }

  @Test
  fun search_byExactLastName_returnsMatch() = runTest {
    engine.indexProfiles(testProfiles)
    val results = engine.search(emptyList(), "Williams")
    assertEquals(1, results.size)
    assertEquals("Bob", results[0].item.name)
  }

  @Test
  fun search_prefixMatch_returnsMultipleResults() = runTest {
    engine.indexProfiles(testProfiles)
    // "jo" prefix should match John, Johnny (first names starting with jo)
    val results = engine.search(emptyList(), "jo")
    assertTrue("Expected at least 2 results for prefix 'jo'", results.size >= 2)
    assertTrue(results.any { it.item.name == "John" })
    assertTrue(results.any { it.item.name == "Johnny" })
  }

  @Test
  fun search_multiTermQuery_requiresAllTerms() = runTest {
    engine.indexProfiles(testProfiles)
    // "John Doe" should match only John Doe (both terms required)
    val results = engine.search(emptyList(), "John Doe")
    assertTrue(results.isNotEmpty())
    assertTrue(results.any { it.item.name == "John" && it.item.lastName == "Doe" })

    // "John Williams" should not match anyone (no profile has both)
    val noMatch = engine.search(emptyList(), "John Williams")
    assertTrue(noMatch.isEmpty())
  }

  @Test
  fun search_caseInsensitive_findsResults() = runTest {
    engine.indexProfiles(testProfiles)
    val lower = engine.search(emptyList(), "jane")
    val upper = engine.search(emptyList(), "JANE")
    assertEquals(1, lower.size)
    assertEquals(1, upper.size)
    assertEquals("Jane", lower[0].item.name)
  }

  @Test
  fun search_emptyOrBlankQuery_returnsEmpty() = runTest {
    engine.indexProfiles(testProfiles)
    assertTrue(engine.search(emptyList(), "").isEmpty())
    assertTrue(engine.search(emptyList(), "   ").isEmpty())
  }

  @Test
  fun search_noMatch_returnsEmpty() = runTest {
    engine.indexProfiles(testProfiles)
    assertTrue(engine.search(emptyList(), "Zzyzzyxx").isEmpty())
  }

  @Test
  fun search_beforeIndexing_returnsEmpty() = runTest {
    // Don't call indexProfiles
    assertTrue(engine.search(emptyList(), "John").isEmpty())
  }

  @Test
  fun reindex_replacesOldData() = runTest {
    engine.indexProfiles(testProfiles)
    assertTrue(engine.search(emptyList(), "John").isNotEmpty())

    // Re-index with new profiles
    engine.indexProfiles(listOf(createProfile("10", "Michael", "Brown", UserSections.PHYSICS)))

    assertTrue(engine.search(emptyList(), "John").isEmpty())
    assertEquals(1, engine.search(emptyList(), "Michael").size)
  }

  @Test
  fun search_afterClose_returnsEmpty() = runTest {
    engine.indexProfiles(testProfiles)
    engine.close()
    assertTrue(engine.search(emptyList(), "John").isEmpty())
  }
}
