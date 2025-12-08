package com.android.sample.model.leaderboard

import android.content.Context
import com.android.sample.model.profile.UserProfile
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Simple file-backed cache for leaderboard profiles. Stores the entire leaderboard list as a single
 * JSON file so order and ties remain intact.
 */
class LeaderboardCache(private val context: Context) {
  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
  }
  private val cacheDir = File(context.cacheDir, "leaderboard_cache")
  private val cacheFile = File(cacheDir, "leaderboard.json")

  init {
    if (!cacheDir.exists()) {
      cacheDir.mkdirs()
    }
  }

  /** Overwrites the cached leaderboard with the provided profiles. */
  fun saveLeaderboard(profiles: List<UserProfile>) {
    try {
      // Strip photos so we do not cache user images for leaderboard snapshots.
      val sanitized = profiles.map { it.copy(photo = null) }
      val payload = json.encodeToString(ListSerializer(UserProfile.serializer()), sanitized)
      cacheFile.writeText(payload)
    } catch (e: Exception) {
      // Non-fatal; just log to aid debugging
      e.printStackTrace()
    }
  }

  /** Loads the cached leaderboard, or returns an empty list if unavailable or corrupted. */
  fun loadLeaderboard(): List<UserProfile> {
    if (!cacheFile.exists()) return emptyList()
    return try {
      val payload = cacheFile.readText()
      json.decodeFromString(ListSerializer(UserProfile.serializer()), payload)
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  /** Clears the leaderboard cache. */
  fun clearAll() {
    if (cacheDir.exists()) {
      cacheDir.listFiles()?.forEach { file ->
        check(file.delete()) { "Failed to delete cache file: ${file.absolutePath}" }
      }
    }
  }
}
