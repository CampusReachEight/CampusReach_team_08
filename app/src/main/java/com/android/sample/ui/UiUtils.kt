package com.android.sample.ui

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.appPalette

class UiUtils {

    /**
     * Truncates a given text to fit within a specified maximum length, optionally preserving a suffix
     * (e.g., domain in an email) and appending an ellipsis ("...") to indicate truncation.
     *
     * @param text The input string to be truncated.
     * @param maxLength The maximum allowed length of the resulting string, including the ellipsis.
     *   Must be greater than or equal to 1.
     * @param keepSuffixLength The number of characters to preserve at the end of the string. Useful
     *   for keeping domains or other important suffixes. Defaults to 0.
     * @param ellipsis The string to append to indicate truncation. Defaults to "...".
     * @return The truncated string with the ellipsis and optional suffix preserved.
     * @throws IllegalArgumentException if `maxLength` is less than 1.
     */
    fun ellipsizeWithMiddle(
        text: String,
        maxLength: Int,
        keepSuffixLength: Int = 0,
        ellipsis: String = "..."
    ): String {
        require(maxLength >= 1) { "maxLength must be >= 1" }
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
