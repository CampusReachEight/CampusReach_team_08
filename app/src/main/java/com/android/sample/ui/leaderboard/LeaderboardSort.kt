package com.android.sample.ui.leaderboard

import com.android.sample.model.profile.UserProfile
import java.util.Comparator

/**
 * Sorting options for the leaderboard. Each option defines a comparator for ordering UserProfiles.
 *
 * The default sort is by kudos descending (highest kudos first), reflecting the "leaderboard"
 * nature of the screen.
 */
enum class LeaderboardSort(val comparator: Comparator<UserProfile>) {
  /** Sort by kudos count, highest first. Default for leaderboard ranking. */
  KUDOS_DESC(
      compareByDescending<UserProfile> { it.kudos }
          .thenBy { it.name.lowercase() }
          .thenBy { it.id }),

  /** Sort by kudos count, lowest first. */
  KUDOS_ASC(compareBy<UserProfile> { it.kudos }.thenBy { it.name.lowercase() }.thenBy { it.id }),

  /** Sort by help received count, highest first. */
  HELP_DESC(
      compareByDescending<UserProfile> { it.helpReceived }
          .thenBy { it.name.lowercase() }
          .thenBy { it.id }),

  /** Sort by help received count, lowest first. */
  HELP_ASC(
      compareBy<UserProfile> { it.helpReceived }.thenBy { it.name.lowercase() }.thenBy { it.id }),

  /** Sort by name (first name), A-Z. */
  NAME_ASC(
      compareBy<UserProfile> { it.name.lowercase() }
          .thenBy { it.lastName.lowercase() }
          .thenBy { it.id }),

  /** Sort by name (first name), Z-A. */
  NAME_DESC(
      compareByDescending<UserProfile> { it.name.lowercase() }
          .thenByDescending { it.lastName.lowercase() }
          .thenBy { it.id }),

  /** Sort by arrival date, newest first. */
  ARRIVAL_DESC(
      compareByDescending<UserProfile> { it.arrivalDate }
          .thenBy { it.name.lowercase() }
          .thenBy { it.id }),

  /** Sort by arrival date, oldest first. */
  ARRIVAL_ASC(
      compareBy<UserProfile> { it.arrivalDate }.thenBy { it.name.lowercase() }.thenBy { it.id });

  companion object {
    /** Returns the default sorting option (kudos descending for leaderboard ranking). */
    fun default(): LeaderboardSort = KUDOS_DESC
  }
}

/**
 * User-facing display label for sorting options. Transforms enum name to Title Case with
 * appropriate descriptions.
 *
 * @return Human-readable label for UI display
 */
fun LeaderboardSort.displayLabel(): String =
    when (this) {
      LeaderboardSort.KUDOS_DESC -> "Kudos (High to Low)"
      LeaderboardSort.KUDOS_ASC -> "Kudos (Low to High)"
      LeaderboardSort.HELP_DESC -> "Help Received (High to Low)"
      LeaderboardSort.HELP_ASC -> "Help Received (Low to High)"
      LeaderboardSort.NAME_ASC -> "Name (A-Z)"
      LeaderboardSort.NAME_DESC -> "Name (Z-A)"
      LeaderboardSort.ARRIVAL_DESC -> "Newest Members"
      LeaderboardSort.ARRIVAL_ASC -> "Oldest Members"
    }
