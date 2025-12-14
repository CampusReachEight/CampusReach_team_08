package com.android.sample.ui.leaderboard

import com.android.sample.model.profile.UserProfile

/**
 * Utility for calculating user positions in the leaderboard based on kudos.
 *
 * Positions are calculated using the same sorting logic as [LeaderboardSort.KUDOS_DESC], ensuring
 * consistency with the leaderboard ranking system.
 */
object LeaderboardPositionCalculator {

  /**
   * Calculates the position of each user based on their kudos count.
   *
   * Positions are 1-indexed (1st place, 2nd place, etc.) and are determined by:
   * 1. Kudos count (descending)
   * 2. Help received (ascending)
   * 3. User ID - final tie-breaker
   *
   * This matches the sorting logic in [LeaderboardSort.KUDOS_DESC].
   *
   * @param profiles List of all user profiles to rank
   * @return Map of user ID to position (1-indexed)
   */
  fun calculatePositions(profiles: List<UserProfile>): Map<String, Int> {
    if (profiles.isEmpty()) return emptyMap()

    val sorted = profiles.sortedWith(LeaderboardSort.KUDOS_DESC.comparator)

    // Assign positions (1-indexed)
    val positionMap = mutableMapOf<String, Int>()
    sorted.forEachIndexed { index, profile -> positionMap[profile.id] = index + 1 }

    return positionMap
  }
}
