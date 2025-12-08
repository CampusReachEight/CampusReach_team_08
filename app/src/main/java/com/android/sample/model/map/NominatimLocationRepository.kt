package com.android.sample.model.map

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

// The email was created to comply with nominatim's usage policy.
const val USER_AGENT_BASE = "CampusReach/1.0 (campusreachteam08@gmail.com)"

const val MAX_LIMIT = 40 // Nominatim allows up to 40 results per request in version 5.X

class NominatimLocationRepository(
    private val client: OkHttpClient,
    private val deviceId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous",
    private val verbose: Boolean = false, // Enable detailed logging for debugging
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocationRepository {

  private val userAgent = "$USER_AGENT_BASE device:$deviceId"

  // Rate limiting: minimum 1 second between requests
  private var lastRequestTime = 0L
  private val minRequestIntervalMs = 1000L

  // Cache: stores query results with timestamp
  private val cache = mutableMapOf<String, Pair<Long, List<Location>>>()
  private val cacheExpirationMs = 3600000L // 1 hour

  private fun parseBody(body: String): List<Location> {
    return try {
      val jsonArray = JSONArray(body)

      List(jsonArray.length()) { i ->
        val jsonObject = jsonArray.getJSONObject(i)
        val lat = jsonObject.getDouble("lat")
        val lon = jsonObject.getDouble("lon")
        val name = jsonObject.getString("display_name")
        Location(lat, lon, name)
      }
    } catch (e: Exception) {
      if (verbose) {
        Log.e("NominatimLocationRepository", "Failed to parse JSON response: $body", e)
      }
      emptyList()
    }
  }

  override suspend fun search(query: String, limit: Int): List<Location> =
      withContext(dispatcher) {
        // Validate parameters
        require(!(query.isBlank())) { "Query must not be blank" }
        require(limit > 0 || limit <= MAX_LIMIT) { "Limit must be between 1 and $MAX_LIMIT" }

        // Check cache first
        val cacheKey = "$query-$limit"
        cache[cacheKey]?.let { (timestamp, locations) ->
          if (System.currentTimeMillis() - timestamp < cacheExpirationMs) {
            if (verbose) {
              Log.d("NominatimLocationRepository", "Cache hit for query: $query")
            }
            return@withContext locations
          }
        }

        // Rate limiting: ensure at least 1 second between requests
        val now = System.currentTimeMillis()
        val timeSinceLastRequest = now - lastRequestTime
        if (timeSinceLastRequest < minRequestIntervalMs) {
          val delayTime = minRequestIntervalMs - timeSinceLastRequest
          if (verbose) {
            Log.d("NominatimLocationRepository", "Rate limiting: delaying for ${delayTime}ms")
          }
          delay(delayTime)
        }
        lastRequestTime = System.currentTimeMillis()

        // Using HttpUrl.Builder to properly construct the URL with query parameters.
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .addPathSegment("search")
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json")
                .addQueryParameter("limit", limit.toString())
                .build()

        // Create the request with a custom User-Agent and optional Referer
        val request =
            Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Referer", "https://github.com/CampusReachEight/CampusReach_team_08")
                .build()

        try {
          val response = client.newCall(request).execute()
          response.use {
            if (!response.isSuccessful) {
              if (verbose) {
                Log.d("NominatimLocationRepository", "Unexpected code $response")
              }
              throw LocationSearchException("HTTP error: ${response.code} ${response.message}")
            }

            val body = response.body?.string()
            return@withContext if (body != null) {
              if (verbose) {
                Log.d("NominatimLocationRepository", "Body: $body")
              }
              val results = parseBody(body)

              // Store in cache
              cache[cacheKey] = System.currentTimeMillis() to results

              results
            } else {
              if (verbose) {
                Log.d("NominatimLocationRepository", "Empty body")
              }
              emptyList()
            }
          }
        } catch (e: IOException) {
          if (verbose) {
            Log.e("NominatimLocationRepository", "Failed to execute request", e)
          }
          throw LocationSearchException("Network request failed: ${e.message}", e)
        }
      }
}
