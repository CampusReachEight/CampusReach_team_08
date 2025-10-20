package com.android.sample.ui.request.edit

import java.text.SimpleDateFormat
import java.util.*

/** Validator for date strings and date comparisons. */
class DateValidator(private val dateFormat: SimpleDateFormat) {

  fun isValidFormat(dateString: String, allowPast: Boolean = false): Boolean {
    if (dateString.isBlank()) return false
    return try {
      val parsedDate = dateFormat.parse(dateString) ?: return false
      if (!allowPast) {
        parsedDate.after(Date()) || parsedDate == Date()
      } else {
        true
      }
    } catch (e: Exception) {
      false
    }
  }

  fun parseDate(dateString: String): Date? {
    return try {
      dateFormat.parse(dateString)
    } catch (e: Exception) {
      null
    }
  }

  fun isExpirationAfterStart(expiration: Date, start: Date): Boolean {
    return !expiration.before(start)
  }

  companion object {
    fun create(): DateValidator {
      val dateFormat = SimpleDateFormat(DateFormats.DATE_TIME_FORMAT, Locale.getDefault())
      return DateValidator(dateFormat)
    }
  }
}
