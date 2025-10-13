package com.android.sample.map

import com.android.sample.model.map.NominatimLocationRepository
import com.android.sample.model.map.USER_AGENT
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
    repository = NominatimLocationRepository(mockClient)
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

  @Test(expected = Exception::class)
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

  @Test(expected = IOException::class)
  fun `search throws IOException when network request fails`() = runTest {
    // Given
    val query = "test query"

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } throws IOException("Network error")

    // When
    repository.search(query)

    // Then - IOException is thrown
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
    verify { mockClient.newCall(match { request -> request.header("User-Agent") == USER_AGENT }) }
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
}
