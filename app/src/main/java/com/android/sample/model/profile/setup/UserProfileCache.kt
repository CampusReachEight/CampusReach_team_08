package com.android.sample.model.profile.setup

import android.content.Context
import com.android.sample.model.profile.UserProfile
import java.io.File
import kotlinx.serialization.json.Json

class UserProfileCache(private val context: Context) {
  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
  }
  private val cacheDir = File(context.cacheDir, "user_profiles_cache")

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
   */
  fun saveProfile(profile: UserProfile) {
    try {
      val profileJson = json.encodeToString(UserProfile.serializer(), profile)
      val file = File(cacheDir, "${profile.id}.json")
      file.writeText(profileJson)
    } catch (e: Exception) {
      // Log the error or handle it as needed
      e.printStackTrace()
    }
  }

  /**
   * Loads a specific user profile from the cache by its ID.
   *
   * @param profileId The ID of the profile to load.
   * @return The cached UserProfile object, or null if not found or fails to load.
   */
  fun loadProfile(profileId: String): UserProfile? {
    val file = File(cacheDir, "$profileId.json")

    if (!file.exists()) {
      return null
    }

    return try {
      val profileJson = file.readText()
      json.decodeFromString(UserProfile.serializer(), profileJson)
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Loads all cached user profiles from disk.
   *
   * @return A list of cached UserProfile objects. Returns an empty list if cache is empty or fails.
   */
  fun loadAllProfiles(): List<UserProfile> {
    if (!cacheDir.exists()) return emptyList()

    return cacheDir
        .listFiles { _, name -> name.endsWith(".json") }
        ?.mapNotNull { file ->
          try {
            val profileJson = file.readText()
            json.decodeFromString(UserProfile.serializer(), profileJson)
          } catch (e: Exception) {
            e.printStackTrace()
            null
          }
        } ?: emptyList()
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

  /** Clears all cached profiles. */
  fun clearAll() {
    if (cacheDir.exists()) {
      cacheDir.listFiles()?.forEach { file ->
        check(file.delete()) { "Failed to delete cache file: ${file.absolutePath}" }
      }
    }
  }
}