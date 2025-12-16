package com.android.sample.utils

import java.util.regex.Pattern

object UrlUtils {
  // Regex for validating HTTPS URLs
  private const val HTTPS_URL_PATTERN = "^https://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$"

  // Regex for extracting URLs from text (simplified)
  private const val URL_EXTRACTION_PATTERN = "https?://\\S+"

  fun isValidHttpsUrl(url: String): Boolean {
    return Pattern.matches(HTTPS_URL_PATTERN, url)
  }

  fun extractUrls(text: String): List<String> {
    val pattern = Pattern.compile(URL_EXTRACTION_PATTERN)
    val matcher = pattern.matcher(text)
    val urls = mutableListOf<String>()
    while (matcher.find()) {
      urls.add(matcher.group())
    }
    return urls
  }
}
