package com.android.sample.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.appPalette

// Extracted constants for magic values used in this file
private const val DEFAULT_ELLIPSIS = "\u2026"
private const val FALLBACK_MIDDLE_ELLIPSIS = "..."
private const val DEFAULT_MAX_LINES = 2
private const val SECOND_LINE_INDEX = 1
private const val MIN_ALLOWED_MAX_LENGTH = 1
private const val SPACE_CHAR = ' '

class UiUtils {

    /**
     * Truncates a given text to fit within a specified maximum length, optionally preserving a suffix
     * (e.g., domain in an email) and appending an ellipsis to indicate truncation.
     *
     * @param text The input string to be truncated.
     * @param maxLength The maximum allowed length of the resulting string, including the ellipsis.
     *   Must be greater than or equal to 1.
     * @param keepSuffixLength The number of characters to preserve at the end of the string. Useful
     *   for keeping domains or other important suffixes. Defaults to 0.
     * @param ellipsis The string to append to indicate truncation. Defaults to "..." (fallback).
     * @return The truncated string with the ellipsis and optional suffix preserved.
     * @throws IllegalArgumentException if `maxLength` is less than 1.
     */
    fun ellipsizeWithMiddle(
        text: String,
        maxLength: Int,
        keepSuffixLength: Int = 0,
        ellipsis: String = FALLBACK_MIDDLE_ELLIPSIS
    ): String {
        require(maxLength >= MIN_ALLOWED_MAX_LENGTH) { "maxLength must be >= $MIN_ALLOWED_MAX_LENGTH" }
        if (text.length <= maxLength) return text
        val suffix =
            if (keepSuffixLength > 0 && keepSuffixLength < text.length) text.takeLast(keepSuffixLength)
            else ""
        val keepStart = maxLength - suffix.length - ellipsis.length
        return if (keepStart > 0) {
            text.take(keepStart) + ellipsis + suffix
        } else {
            // not enough room to keep suffix + ellipsis, do a simple truncation
            text.take(maxLength - ellipsis.length) + ellipsis
        }
    }
}

@Composable
fun getTextFieldColors(palette: AppPalette = appPalette()): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.onSurface,
        unfocusedTextColor = palette.onSurface,
        cursorColor = palette.accent,
        focusedBorderColor = palette.accent,
        unfocusedBorderColor = palette.onSurface,
        focusedContainerColor = palette.transparent,
        unfocusedContainerColor = palette.transparent,
        unfocusedLabelColor = palette.onSurface,
        focusedLabelColor = palette.accent)
}

@Composable
fun getFilterAndSortButtonColors(palette: AppPalette = appPalette()): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = palette.accent,
        contentColor = palette.onAccent,
        disabledContainerColor = palette.secondary,
        disabledContentColor = palette.onPrimary
    )
}
