package com.android.sample.model.profile

import android.content.Context
import java.io.File
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class UserProfileCache(private val context: Context) {
  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
  }
  private val cacheDir = File(context.cacheDir, "user_profiles_cache")
  private val leaderboardCacheFile = File(cacheDir, "leaderboard.json")

  init {
    // Ensure the cache directory exists
    if (!cacheDir.exists()) {
      cacheDir.mkdirs()
    }
  }

  /**
   * Saves a single user profile to the cache.
   *
   * @param profile The UserProfile to save.
   * @throws java.io.IOException if the file cannot be written.
   */
  fun saveProfile(profile: UserProfile) {
    try {
      val profileJson = json.encodeToString(UserProfile.serializer(), profile)
      val file = File(cacheDir, "${profile.id}.json")
      file.writeText(profileJson)
    } catch (e: SerializationException) {
      throw IOException("Failed to serialize profile with ID ${profile.id}", e)
    } catch (e: IOException) {
      throw IOException("Failed to write profile with ID ${profile.id} to cache", e)
    }
  }

  /**
   * Retrieves a specific user profile from the cache by its ID.
   *
   * @param profileId The ID of the profile to retrieve.
   * @return The cached UserProfile object.
   * @throws NoSuchElementException if no profile with the given ID exists in the cache.
   * @throws IOException if the file cannot be read.
   * @throws SerializationException if the JSON is corrupted or invalid.
   */
  fun getProfileById(profileId: String): UserProfile {
    val file = File(cacheDir, "$profileId.json")

    if (!file.exists()) {
      throw NoSuchElementException("Profile with ID $profileId not found in cache")
    }

    return try {
      val profileJson = file.readText()
      json.decodeFromString(UserProfile.serializer(), profileJson)
    } catch (e: Exception) {
      throw IOException("Failed to read profile with ID $profileId from cache", e)
    }
  }

  /**
   * Deletes a specific user profile from the cache.
   *
   * @param profileId The ID of the profile to delete.
   * @return true if the profile was deleted, false otherwise.
   */
  fun deleteProfile(profileId: String): Boolean {
    val file = File(cacheDir, "$profileId.json")
    return if (file.exists()) {
      file.delete()
    } else {
      false
    }
  }

  /**
   * Checks if a profile exists in the cache.
   *
   * @param profileId The ID of the profile to check.
   * @return true if the profile exists in cache, false otherwise.
   */
  fun hasProfile(profileId: String): Boolean {
    val file = File(cacheDir, "$profileId.json")
    return file.exists()
  }

  /**
   * Saves the leaderboard list to cache. All profiles are stored in a single file to preserve order
   * and ties.
   *
   * @param profiles The list of profiles to save as the leaderboard.
   */
  fun saveLeaderboard(profiles: List<UserProfile>) {
    try {
      // Strip photos so we do not cache user images for leaderboard snapshots.
      val sanitized = profiles.map { it.copy(photo = null) }
      val payload = json.encodeToString(ListSerializer(UserProfile.serializer()), sanitized)
      leaderboardCacheFile.writeText(payload)
    } catch (e: Exception) {
      // Non-fatal; just log to aid debugging
      e.printStackTrace()
    }
  }

  /**
   * Loads the cached leaderboard, or returns an empty list if unavailable or corrupted.
   *
   * @return The list of cached leaderboard profiles.
   */
  fun loadLeaderboard(): List<UserProfile> {
    if (!leaderboardCacheFile.exists()) return emptyList()
    return try {
      val payload = leaderboardCacheFile.readText()
      json.decodeFromString(ListSerializer(UserProfile.serializer()), payload)
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  /** Clears all cached profiles. */
  fun clearAll() {
    if (cacheDir.exists()) {
      cacheDir.listFiles()?.forEach { file ->
        check(file.delete()) { "Failed to delete cache file: ${file.absolutePath}" }
      }
    }
  }
}
