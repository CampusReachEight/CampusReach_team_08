package com.android.sample.model.request

import android.content.Context
import java.io.File
import kotlin.io.path.exists
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class RequestCache(private val context: Context) {
  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
  }
  private val cacheDir = File(context.cacheDir, "requests_cache")

  init {
    // Ensure the cache directory exists
    if (!cacheDir.exists()) {
      cacheDir.mkdirs()
    }
  }

  /** Saves a list of requests to the cache, overwriting any existing cache. */
  fun saveRequests(requests: List<Request>) {
    clearAll()
    requests.forEach { request ->
      try {
        val requestJson = json.encodeToString(Request.serializer(), request)

        val file = File(cacheDir, "${request.requestId}.json")
        file.writeText(requestJson)
      } catch (e: Exception) {
        // Log the error or handle it as needed
        e.printStackTrace()
      }
    }
  }

  /**
   * Loads all cached requests from disk.
   *
   * @return A list of cached Request objects. Returns an empty list if cache is empty or fails.
   */
  fun loadRequests(): List<Request> {
    if (!cacheDir.exists()) return emptyList()

    return cacheDir
        .listFiles { _, name -> name.endsWith(".json") }
        ?.mapNotNull { file ->
          try {
            val requestJson = file.readText()
            json.decodeFromString(Request.serializer(), requestJson)
          } catch (e: Exception) {
            e.printStackTrace()
            null
          }
        } ?: emptyList()
  }

  /**
   * Retrieves a specific request from the cache by its ID.
   *
   * @param requestId The ID of the request to retrieve.
   * @return The cached Request object.
   * @throws IllegalArgumentException if no request with the given ID exists in the cache.
   */
  fun getRequestById(requestId: String): Request {
    val file = File(cacheDir, "$requestId.json")

    if (!file.exists()) {
      throw IllegalArgumentException("Request with ID $requestId not found in cache")
    }

    return try {
      val requestJson = file.readText()
      json.decodeFromString(Request.serializer(), requestJson)
    } catch (e: Exception) {
      throw IllegalArgumentException("Failed to load request with ID $requestId from cache", e)
    }
  }

  fun clearAll() {
    if (cacheDir.exists()) {
      cacheDir.listFiles()?.forEach { it.delete() }
    }
  }
}
