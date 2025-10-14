package com.android.sample.model.request

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.android.sample.model.map.Location
import com.android.sample.model.map.EMPTY_LOCATION
import com.android.sample.model.map.isValid
import com.android.sample.ui.theme.LocationSearchFieldDimensions

object LocationSearchFieldTestTags {
    const val InputLocationName = "input_location_name"
    const val ErrorMessage = "location_error_message"
    const val LoadingIndicator = "location_loading_indicator"
    const val ClearButton = "location_clear_button"
    const val SelectedLocationCard = "selected_location_card"
}

/**
 * Composable for searching and selecting a location using Nominatim autocomplete.
 *
 * Displays a text field for location input, a dropdown for search results,
 * and a card for the selected location. Handles loading, error, and clear states.
 *
 * @param locationName Current input value for location name.
 * @param location Currently selected location, or null if none.
 * @param searchResults List of locations returned from search.
 * @param isSearching True if search is in progress.
 * @param isError True if an error should be shown.
 * @param errorMessage Error message to display.
 * @param enabled Whether the field is enabled.
 * @param onLocationNameChange Called when input value changes.
 * @param onLocationSelected Called when a location is selected.
 * @param onSearchQueryChange Called to trigger a search.
 * @param onClearSearch Called to clear search results.
 * @param modifier Modifier for the container.
 */
@Composable
fun LocationSearchField(
    locationName: String,
    location: Location?,
    searchResults: List<Location>,
    isSearching: Boolean,
    isError: Boolean,
    errorMessage: String,
    enabled: Boolean = true,
    onLocationNameChange: (String) -> Unit,
    onLocationSelected: (Location) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Controls visibility of the dropdown menu
    var showDropdown by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        // Field label
        Text(
            text = stringResource(R.string.location_field_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = LocationSearchFieldDimensions.LabelBottomPadding)
        )

        // Container for search field and dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            // Location input field
            OutlinedTextField(
                value = locationName,
                onValueChange = { newValue ->
                    onLocationNameChange(newValue) // Update input value
                    onSearchQueryChange(newValue)  // Trigger search
                    showDropdown = newValue.isNotEmpty() // Show dropdown if not empty
                },
                label = { Text(stringResource(R.string.location_field_name)) },
                placeholder = { Text(stringResource(R.string.location_field_placeholder)) },
                isError = isError,
                supportingText = {
                    // Show error message if needed
                    if (isError) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(LocationSearchFieldTestTags.ErrorMessage)
                        )
                    }
                },
                trailingIcon = {
                    // Show loading indicator or clear button
                    when {
                        isSearching -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(LocationSearchFieldDimensions.IconSize)
                                    .testTag(LocationSearchFieldTestTags.LoadingIndicator),
                                strokeWidth = LocationSearchFieldDimensions.LoadingIndicatorStroke
                            )
                        }
                        locationName.isNotEmpty() -> {
                            IconButton(
                                onClick = {
                                    onLocationNameChange("") // Clear input
                                    onLocationSelected(EMPTY_LOCATION)  // Reset selection
                                    onClearSearch() // Clear search results
                                    showDropdown = false // Hide dropdown
                                },
                                modifier = Modifier.testTag(LocationSearchFieldTestTags.ClearButton)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    stringResource(R.string.location_field_clear_description)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(LocationSearchFieldTestTags.InputLocationName),
                enabled = enabled,
                singleLine = true
            )

            // Dropdown menu for search results
            DropdownMenu(
                expanded = showDropdown && searchResults.isNotEmpty(),
                onDismissRequest = { showDropdown = false },
                modifier = Modifier
                    .fillMaxWidth(LocationSearchFieldDimensions.DropdownWidthFraction)
                    .heightIn(max = LocationSearchFieldDimensions.DropdownMaxHeight)
            ) {
                // List each search result as a menu item
                searchResults.forEach { searchLocation ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                // Location name
                                Text(
                                    text = searchLocation.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // Latitude and longitude
                                Text(
                                    text = "Lat: ${searchLocation.latitude}, " +
                                            "Lng: ${searchLocation.longitude}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onLocationSelected(searchLocation) // Select location
                            onLocationNameChange(searchLocation.name) // Update input
                            showDropdown = false // Hide dropdown
                            onClearSearch() // Clear search results
                        },
                        modifier = Modifier.testTag("locationResult_${searchLocation.name}")
                    )
                    // Divider between items except after last
                    if (searchLocation != searchResults.last()) {
                        HorizontalDivider()
                    }
                }
            }
        }

        // Display selected location card if valid location is chosen
        if (location != null && location.isValid()) {
            Spacer(modifier = Modifier.height(LocationSearchFieldDimensions.CardSpacing))
            SelectedLocationCard(
                location = location,
                modifier = Modifier.testTag(LocationSearchFieldTestTags.SelectedLocationCard)
            )
        }
    }
}
/**
 * Displays a card with details of the selected location.
 *
 * Shows the location name, latitude, and longitude in a styled card.
 * Includes a location icon and uses Material 3 theming.
 *
 * @param location The selected Location object to display.
 * @param modifier Modifier for customizing the card layout.
 */
@Composable
private fun SelectedLocationCard(location: Location, modifier: Modifier = Modifier) {
    // Card container for location details
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        // Row for icon and location info
        Row(
            modifier = Modifier.padding(LocationSearchFieldDimensions.CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location icon
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = stringResource(R.string.location_field_name_simple),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(LocationSearchFieldDimensions.LocationIconSize)
            )
            Spacer(modifier = Modifier.width(LocationSearchFieldDimensions.CardSpacing))

            // Column for location text details
            Column {
                // Title label
                Text(
                    text = stringResource(R.string.location_selected_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // Location name
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // Latitude and longitude
                Text(
                    text = "Lat: ${String.format("%.4f", location.latitude)}, " +
                            "Lng: ${String.format("%.4f", location.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

}
