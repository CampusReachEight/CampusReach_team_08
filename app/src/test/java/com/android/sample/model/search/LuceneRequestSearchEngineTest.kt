package com.android.sample.model.search

import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LuceneRequestSearchEngineTest {

  private lateinit var engine: LuceneRequestSearchEngine
  private lateinit var requests: List<Request>

  @Before
  fun setUp() = runBlocking {
    engine = LuceneRequestSearchEngine()
    requests =
        listOf(
            req(
                id = "1",
                title = "Study group for calculus",
                description = "Looking for peers to solve integrals",
                locationName = "EPFL Library",
                types = listOf(RequestType.STUDY_GROUP),
                tags = listOf(Tags.GROUP_WORK),
                status = RequestStatus.OPEN),
            req(
                id = "2",
                title = "Pizza night",
                description = "Fresh pizza and soda",
                locationName = "Lausanne Center",
                types = listOf(RequestType.EATING),
                tags = listOf(Tags.INDOOR),
                status = RequestStatus.IN_PROGRESS),
            req(
                id = "3",
                title = "Lost and found: calculator",
                description = "Found a calculator near the lab",
                locationName = "CE Building",
                types = listOf(RequestType.LOST_AND_FOUND),
                tags = listOf(Tags.EASY),
                status = RequestStatus.OPEN))
    engine.indexRequests(requests)
  }

  @After
  fun tearDown() {
    engine.close()
  }

  @Test
  fun `search by title returns relevant results`() = runBlocking {
    val res = engine.search(requests, "study group")
    assertTrue(res.isNotEmpty())
    assertEquals("1", res.first().item.requestId)
  }

  @Test
  fun `search by description finds matches`() = runBlocking {
    val res = engine.search(requests, "fresh pizza")
    assertTrue(res.isNotEmpty())
    assertEquals("2", res.first().item.requestId)
  }

  @Test
  fun `search by location name works`() = runBlocking {
    val res = engine.search(requests, "library")
    assertTrue(res.isNotEmpty())
    assertEquals("1", res.first().item.requestId)
  }

  @Test
  fun `search by request types matches enum display variants`() = runBlocking {
    val res = engine.search(requests, "lost and found")
    assertTrue(res.isNotEmpty())
    assertEquals("3", res.first().item.requestId)
  }

  @Test
  fun `search by tags finds matches`() = runBlocking {
    val res = engine.search(requests, "indoor")
    assertTrue(res.isNotEmpty())
    assertEquals("2", res.first().item.requestId)
  }

  @Test
  fun `empty query returns empty`() = runBlocking {
    val res = engine.search(requests, " ")
    assertTrue(res.isEmpty())
  }

  @Test
  fun `multi word query uses AND logic`() = runBlocking {
    // Only request 1 mentions both study and group
    val res = engine.search(requests, "study group")
    assertTrue(res.isNotEmpty())
    assertEquals("1", res.first().item.requestId)
  }

  @Test
  fun `title matches score higher than description-only`() = runBlocking {
    // Query 'pizza' present in title of id=2 and maybe in description of others; ensure 2 ranks
    // first
    val res = engine.search(requests, "pizza")
    assertTrue(res.isNotEmpty())
    assertEquals("2", res.first().item.requestId)
    if (res.size >= 2) {
      assertTrue(res.first().score >= res[1].score)
    }
  }

  @Test
  fun `special characters handled gracefully`() = runBlocking {
    val outcome = runCatching { engine.search(requests, "study+(pizza) AND status:OPEN") }
    assertTrue(outcome.isSuccess)
  }

  @Test
  fun `non-indexed engine returns empty`() = runBlocking {
    val fresh = LuceneRequestSearchEngine()
    val res = fresh.search(requests, "anything")
    assertTrue(res.isEmpty())
    fresh.close()
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
          expirationTime = Date(Date().time + 3600_000),
          people = emptyList(),
          tags = tags,
          creatorId = "creator-$id")
}
