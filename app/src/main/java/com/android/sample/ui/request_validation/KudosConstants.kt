package com.android.sample.ui.request_validation

/**
 * Constants for the kudos system.
 *
 * This file centralizes all kudos-related configuration values to ensure consistency across the
 * application and facilitate future changes.
 */
object KudosConstants {
  /** Number of kudos awarded to each selected helper when a request is closed. */
  const val KUDOS_PER_HELPER = 1

  /** Number of kudos awarded to the request creator for successfully resolving their request. */
  const val KUDOS_FOR_CREATOR_RESOLUTION = 1

  /** Minimum number of helpers that must be selected to close a request (0 = optional). */
  const val MIN_HELPERS_TO_SELECT = 0

  /** Maximum number of kudos that can be awarded in a single transaction (safety limit). */
  const val MAX_KUDOS_PER_TRANSACTION = 1000
}

/** Exception thrown when kudos-related operations fail. */
sealed class KudosException(message: String, cause: Throwable? = null) : Exception(message, cause) {
  class InvalidAmount(amount: Int) :
      KudosException("Invalid kudos amount: $amount. Must be positive and within limits.")

  class TransactionFailed(userId: String, cause: Throwable? = null) :
      KudosException("Failed to award kudos to user: $userId", cause)

  class UserNotFound(userId: String) : KudosException("User profile not found: $userId")
}
