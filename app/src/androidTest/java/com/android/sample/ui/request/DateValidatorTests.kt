package com.android.sample.ui.request

import com.android.sample.ui.request.edit.DateFormats
import com.android.sample.ui.request.edit.DateValidator
import java.text.SimpleDateFormat
import java.util.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DateValidatorTest {

  private lateinit var validator: DateValidator
  private lateinit var dateFormat: SimpleDateFormat

  @Before
  fun setup() {
    dateFormat = SimpleDateFormat(DateFormats.DATE_TIME_FORMAT, Locale.getDefault())
    validator = DateValidator(dateFormat)
  }

  @Test
  fun isValidFormat_withValidFutureDate_returnsTrue() {
    val futureDate = Date(System.currentTimeMillis() + 86400000)
    val dateString = dateFormat.format(futureDate)

    assertTrue(validator.isValidFormat(dateString))
  }

  @Test
  fun isValidFormat_withPastDate_returnsFalse() {
    val pastDate = Date(System.currentTimeMillis() - 86400000)
    val dateString = dateFormat.format(pastDate)

    assertFalse(validator.isValidFormat(dateString))
  }

  @Test
  fun isValidFormat_withPastDateAndAllowPast_returnsTrue() {
    val pastDate = Date(System.currentTimeMillis() - 86400000)
    val dateString = dateFormat.format(pastDate)

    assertTrue(validator.isValidFormat(dateString, allowPast = true))
  }

  @Test
  fun isValidFormat_withInvalidFormat_returnsFalse() {
    assertFalse(validator.isValidFormat("invalid date"))
  }

  @Test
  fun isValidFormat_withBlankString_returnsFalse() {
    assertFalse(validator.isValidFormat(""))
  }

  @Test
  fun parseDate_withValidDate_returnsDate() {
    val dateString = "15/10/2025 14:30"
    val result = validator.parseDate(dateString)

    assertNotNull(result)
  }

  @Test
  fun parseDate_withInvalidDate_returnsNull() {
    val result = validator.parseDate("invalid")

    assertNull(result)
  }

  @Test
  fun isExpirationAfterStart_withValidOrder_returnsTrue() {
    val start = Date(System.currentTimeMillis())
    val expiration = Date(System.currentTimeMillis() + 86400000)

    assertTrue(validator.isExpirationAfterStart(expiration, start))
  }

  @Test
  fun isExpirationAfterStart_withInvalidOrder_returnsFalse() {
    val start = Date(System.currentTimeMillis() + 86400000)
    val expiration = Date(System.currentTimeMillis())

    assertFalse(validator.isExpirationAfterStart(expiration, start))
  }
}
