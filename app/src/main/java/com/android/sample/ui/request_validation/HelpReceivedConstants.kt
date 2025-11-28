package com.android.sample.ui.request_validation

/**
 * Constants and exceptions for help\-received operations.
 *
 * Mirrors the style of `KudosConstants.kt` to centralize limits and error types used by
 * `receiveHelp` and test fixtures.
 */
object HelpReceivedConstants {
    /** Number of help units recorded per single helper action. */
    const val HELP_RECEIVED_PER_HELP = 1

    /** Minimum allowed help amount (inclusive). */
    const val MIN_HELP_RECEIVED = 0

    /** Safety limit: maximum help units that can be recorded in a single transaction. */
    const val MAX_HELP_RECEIVED_PER_TRANSACTION = 1000
}

/** Exceptions specific to help\-received operations. */
sealed class HelpReceivedException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class InvalidAmount(amount: Int) :
        HelpReceivedException("Invalid help amount: $amount. Must be positive and within limits.")

    class TransactionFailed(userId: String, cause: Throwable? = null) :
        HelpReceivedException("Failed to record help for user: $userId", cause)

    class UserNotFound(userId: String) :
        HelpReceivedException("User profile not found: $userId")
}
