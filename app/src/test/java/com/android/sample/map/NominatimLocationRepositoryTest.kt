package com.android.sample.map

import com.android.sample.model.map.LocationSearchException
import com.android.sample.model.map.NominatimLocationRepository
import com.android.sample.model.map.USER_AGENT_BASE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NominatimLocationRepositoryTest {

  private lateinit var mockClient: OkHttpClient
  private lateinit var mockCall: Call
  private lateinit var repository: NominatimLocationRepository

  @Before
  fun setup() {
    mockClient = mockk()
    mockCall = mockk()
    repository = NominatimLocationRepository(mockClient, "test-device-id")
  }

  @Test
  fun `search returns list of locations when response is successful`() = runTest {
    // Given
    val query = "EPFL Lausanne"
    val jsonResponse =
        """
        [
          {
            "lat": "46.5191",
            "lon": "6.5668",
            "display_name": "EPFL, Lausanne, Switzerland"
          },
          {
            "lat": "46.5200",
            "lon": "6.5700",
            "display_name": "EPFL Campus, Lausanne"
          }
        ]
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    val result = repository.search(query)

    // Then
    assertEquals(2, result.size)
    assertEquals(46.5191, result[0].latitude, 0.0001)
    assertEquals(6.5668, result[0].longitude, 0.0001)
    assertEquals("EPFL, Lausanne, Switzerland", result[0].name)
    assertEquals(46.5200, result[1].latitude, 0.0001)
    assertEquals(6.5700, result[1].longitude, 0.0001)
    assertEquals("EPFL Campus, Lausanne", result[1].name)
  }

  @Test
  fun `search returns empty list when response body is empty`() = runTest {
    // Given
    val query = "nonexistent place"
    val jsonResponse = "[]"

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    val result = repository.search(query)

    // Then
    assertTrue(result.isEmpty())
  }

  @Test(expected = LocationSearchException::class)
  fun `search throws exception when response is not successful`() = runTest {
    // Given
    val query = "test query"

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("Not Found")
            .body("".toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    repository.search(query)

    // Then - exception is thrown
  }

  @Test(expected = LocationSearchException::class)
  fun `search throws LocationSearchException when network request fails`() = runTest {
    // Given
    val query = "test query"

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } throws IOException("Network error")

    // When
    repository.search(query)

    // Then - LocationSearchException is thrown
  }

  @Test
  fun `search constructs correct URL with query parameters`() = runTest {
    // Given
    val query = "EPFL Lausanne"
    val jsonResponse = "[]"

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    repository.search(query)

    // Then
    verify {
      mockClient.newCall(
          match { request ->
            val url = request.url
            url.scheme == "https" &&
                url.host == "nominatim.openstreetmap.org" &&
                url.pathSegments.contains("search") &&
                url.queryParameter("q") == query &&
                url.queryParameter("format") == "json"
          })
    }
  }

  @Test
  fun `search includes correct User-Agent header`() = runTest {
    // Given
    val query = "test"
    val jsonResponse = "[]"

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    repository.search(query)

    // Then
    verify {
      mockClient.newCall(
          match { request ->
            request.header("User-Agent") == "$USER_AGENT_BASE device:test-device-id"
          })
    }
  }

  @Test
  fun `search includes Referer header`() = runTest {
    // Given
    val query = "test"
    val jsonResponse = "[]"

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    repository.search(query)

    // Then
    verify {
      mockClient.newCall(
          match { request ->
            request.header("Referer") == "https://github.com/CampusReachEight/CampusReach_team_08"
          })
    }
  }

  @Test
  fun `search returns cached results on second call`() = runTest {
    // Given
    val query = "EPFL"
    val jsonResponse =
        """
        [
          {
            "lat": "46.5191",
            "lon": "6.5668",
            "display_name": "EPFL, Lausanne, Switzerland"
          }
        ]
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When - First call
    val result1 = repository.search(query)

    // Second call should use cache
    val result2 = repository.search(query)

    // Then - API should only be called once
    verify(exactly = 1) { mockClient.newCall(any()) }
    assertEquals(result1, result2)
    assertEquals(1, result1.size)
  }

  @Test
  fun `search enforces rate limiting between requests`() = runTest {
    // Given
    val query1 = "Paris"
    val query2 = "London"
    val jsonResponse = "[]"

    val mockResponse1 =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    val mockResponse2 =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returnsMany listOf(mockCall, mockCall)
    every { mockCall.execute() } returnsMany listOf(mockResponse1, mockResponse2)

    // When - Make two requests quickly
    val startTime = System.currentTimeMillis()
    repository.search(query1)
    repository.search(query2)
    val endTime = System.currentTimeMillis()

    // Then - Should take at least 1 second due to rate limiting
    val duration = endTime - startTime
    assertTrue("Duration should be >= 1000ms, was ${duration}ms", duration >= 1000)
    verify(exactly = 2) { mockClient.newCall(any()) }
  }

  @Test
  fun `search parses single location correctly`() = runTest {
    // Given
    val query = "Eiffel Tower"
    val jsonResponse =
        """
        [
          {
            "lat": "48.8583701",
            "lon": "2.2944813",
            "display_name": "Tour Eiffel, Paris, France"
          }
        ]
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    val result = repository.search(query)

    // Then
    assertEquals(1, result.size)
    assertEquals(48.8583701, result[0].latitude, 0.0001)
    assertEquals(2.2944813, result[0].longitude, 0.0001)
    assertEquals("Tour Eiffel, Paris, France", result[0].name)
  }

  @Test
  fun `search handles special characters in query`() = runTest {
    // Given
    val query = "Café & Restaurant"
    val jsonResponse = "[]"

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    val result = repository.search(query)

    // Then
    assertTrue(result.isEmpty())
    verify { mockClient.newCall(match { request -> request.url.queryParameter("q") == query }) }
  }

  @Test
  fun `search handles negative coordinates correctly`() = runTest {
    // Given
    val query = "Buenos Aires"
    val jsonResponse =
        """
        [
          {
            "lat": "-34.6037",
            "lon": "-58.3816",
            "display_name": "Buenos Aires, Argentina"
          }
        ]
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    val result = repository.search(query)

    // Then
    assertEquals(1, result.size)
    assertEquals(-34.6037, result[0].latitude, 0.0001)
    assertEquals(-58.3816, result[0].longitude, 0.0001)
    assertEquals("Buenos Aires, Argentina", result[0].name)
  }

  @Test
  fun `search handles small query with many diverse results`() = runTest {
    // Given
    val query = "Main"
    val jsonResponse =
        """
        [
          {
            "lat": "50.0007",
            "lon": "8.2703",
            "display_name": "Main, Frankfurt, Germany"
          },
          {
            "lat": "43.6426",
            "lon": "-70.2568",
            "display_name": "Main Street, Portland, Maine, USA"
          },
          {
            "lat": "40.7580",
            "lon": "-73.9855",
            "display_name": "Main Street, New York, USA"
          },
          {
            "lat": "51.5074",
            "lon": "-0.1278",
            "display_name": "Main Road, London, UK"
          },
          {
            "lat": "48.8566",
            "lon": "2.3522",
            "display_name": "Main Avenue, Paris, France"
          },
          {
            "lat": "35.6762",
            "lon": "139.6503",
            "display_name": "Main Street, Tokyo, Japan"
          },
          {
            "lat": "-33.8688",
            "lon": "151.2093",
            "display_name": "Main Street, Sydney, Australia"
          },
          {
            "lat": "55.7558",
            "lon": "37.6173",
            "display_name": "Main Street, Moscow, Russia"
          },
          {
            "lat": "-23.5505",
            "lon": "-46.6333",
            "display_name": "Main Avenue, São Paulo, Brazil"
          },
          {
            "lat": "19.4326",
            "lon": "-99.1332",
            "display_name": "Main Street, Mexico City, Mexico"
          }
        ]
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    val result = repository.search(query)

    // Then
    assertEquals(10, result.size)

    // Verify first result
    assertEquals(50.0007, result[0].latitude, 0.0001)
    assertEquals(8.2703, result[0].longitude, 0.0001)
    assertEquals("Main, Frankfurt, Germany", result[0].name)

    // Verify a middle result
    assertEquals(48.8566, result[4].latitude, 0.0001)
    assertEquals(2.3522, result[4].longitude, 0.0001)
    assertEquals("Main Avenue, Paris, France", result[4].name)

    // Verify last result
    assertEquals(19.4326, result[9].latitude, 0.0001)
    assertEquals(-99.1332, result[9].longitude, 0.0001)
    assertEquals("Main Street, Mexico City, Mexico", result[9].name)

    // Verify diversity - check that all locations have different coordinates
    val uniqueCoordinates = result.map { Pair(it.latitude, it.longitude) }.toSet()
    assertEquals(10, uniqueCoordinates.size)
  }

  @Test
  fun `search uses default limit of 10 when not specified`() = runTest {
    // Given
    val query = "test"
    val jsonResponse = "[]"

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    repository.search(query)

    // Then
    verify { mockClient.newCall(match { request -> request.url.queryParameter("limit") == "10" }) }
  }

  @Test
  fun `search respects custom limit parameter`() = runTest {
    // Given
    val query = "Paris"
    val customLimit = 5
    val jsonResponse =
        """
        [
          {
            "lat": "48.8566",
            "lon": "2.3522",
            "display_name": "Paris, France"
          },
          {
            "lat": "48.8567",
            "lon": "2.3523",
            "display_name": "Paris Center, France"
          },
          {
            "lat": "48.8568",
            "lon": "2.3524",
            "display_name": "Paris Downtown, France"
          }
        ]
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    val result = repository.search(query, customLimit)

    // Then
    verify { mockClient.newCall(match { request -> request.url.queryParameter("limit") == "5" }) }
    assertEquals(3, result.size)
  }

  @Test
  fun `search with limit 1 returns only one result`() = runTest {
    // Given
    val query = "London"
    val jsonResponse =
        """
        [
          {
            "lat": "51.5074",
            "lon": "-0.1278",
            "display_name": "London, UK"
          }
        ]
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    // When
    val result = repository.search(query, limit = 1)

    // Then
    verify { mockClient.newCall(match { request -> request.url.queryParameter("limit") == "1" }) }
    assertEquals(1, result.size)
    assertEquals("London, UK", result[0].name)
  }
}
