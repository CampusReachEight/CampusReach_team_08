package com.android.sample.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.request_validation.HelpReceivedConstants
import com.android.sample.ui.request_validation.HelpReceivedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiveHelpTests {

  @Test
  fun constants_haveExpectedValues() {
    assertEquals(1, HelpReceivedConstants.HELP_RECEIVED_PER_HELP)
    assertEquals(0, HelpReceivedConstants.MIN_HELP_RECEIVED)
    assertEquals(1, HelpReceivedConstants.MAX_HELP_RECEIVED_PER_TRANSACTION)
  }

  @Test
  fun invalidAmountException_containsAmountInMessage() {
    val amount = -5
    val ex = HelpReceivedException.InvalidAmount(amount)
    val message = ex.message ?: ""
    assertTrue(message.contains(amount.toString()))
  }

  @Test
  fun transactionFailedException_containsUserIdInMessage() {
    val userId = "user-123"
    val cause = Throwable("cause")
    val ex = HelpReceivedException.TransactionFailed(userId, cause)
    val message = ex.message ?: ""
    assertTrue(message.contains(userId))
  }

  @Test
  fun userNotFoundException_containsUserIdInMessage() {
    val userId = "missing-user"
    val ex = HelpReceivedException.UserNotFound(userId)
    val message = ex.message ?: ""
    assertTrue(message.contains(userId))
  }
}
