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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.sample.model.map.Location

object LocationSearchFieldTestTags {
  const val INPUT_LOCATION_NAME = "input_location_name"
  const val ERROR_MESSAGE = "location_error_message"
  const val LOADING_INDICATOR = "location_loading_indicator"
  const val CLEAR_BUTTON = "location_clear_button"
  const val SELECTED_LOCATION_CARD = "selected_location_card"
}
/**
 * Location search field with Nominatim autocomplete dropdown.
 *
 * @param locationName Current location name text
 * @param location Selected Location object (null if not selected)
 * @param searchResults List of location search results from Nominatim
 * @param isSearching Whether a search is in progress
 * @param isError Whether to show error state
 * @param errorMessage Error message to display
 * @param enabled Whether the field is enabled
 * @param onLocationNameChange Callback when location name text changes
 * @param onLocationSelected Callback when user selects a location from dropdown
 * @param onSearchQueryChange Callback to trigger search with new query
 * @param onClearSearch Callback to clear search results
 * @param modifier Modifier for the container
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
  var showDropdown by remember { mutableStateOf(false) }

  Column(modifier = modifier) {
    // Label
    Text(
        text = "Location *",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 8.dp))

    // Search field with dropdown
    Box(modifier = Modifier.fillMaxWidth()) {
      OutlinedTextField(
          value = locationName,
          onValueChange = { newValue ->
            onLocationNameChange(newValue)
            onSearchQueryChange(newValue)
            showDropdown = newValue.isNotEmpty()
          },
          label = { Text("Location Name") },
          placeholder = { Text("e.g., BC Building, EPFL, Lausanne") },
          isError = isError,
          supportingText = {
            if (isError) {
              Text(
                  text = errorMessage,
                  color = MaterialTheme.colorScheme.error,
                  modifier = Modifier.testTag(LocationSearchFieldTestTags.ERROR_MESSAGE))
            }
          },
          trailingIcon = {
            when {
              isSearching -> {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp).testTag(LocationSearchFieldTestTags.LOADING_INDICATOR),
                    strokeWidth = 2.dp)
              }
              locationName.isNotEmpty() -> {
                IconButton(
                    onClick = {
                      onLocationNameChange("")
                      onLocationSelected(Location(0.0, 0.0, ""))
                      onClearSearch()
                      showDropdown = false
                    },
                    modifier = Modifier.testTag(LocationSearchFieldTestTags.CLEAR_BUTTON)) {
                      Icon(Icons.Default.Clear, "Clear")
                    }
              }
            }
          },
          modifier =
              Modifier.fillMaxWidth().testTag(LocationSearchFieldTestTags.INPUT_LOCATION_NAME),
          enabled = enabled,
          singleLine = true)

      // Dropdown Menu
      DropdownMenu(
          expanded = showDropdown && searchResults.isNotEmpty(),
          onDismissRequest = { showDropdown = false },
          modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 300.dp)) {
            searchResults.forEach { searchLocation ->
              DropdownMenuItem(
                  text = {
                    Column {
                      Text(
                          text = searchLocation.name,
                          style = MaterialTheme.typography.bodyMedium,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis)
                      Text(
                          text =
                              "Lat: ${searchLocation.latitude}, " +
                                  "Lng: ${searchLocation.longitude}",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  },
                  onClick = {
                    onLocationSelected(searchLocation)
                    onLocationNameChange(searchLocation.name)
                    showDropdown = false
                    onClearSearch()
                  },
                  modifier = Modifier.testTag("locationResult_${searchLocation.name}"))
              if (searchLocation != searchResults.last()) {
                HorizontalDivider()
              }
            }
          }
    }

    // Selected Location Display
    if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
      Spacer(modifier = Modifier.height(8.dp))
      SelectedLocationCard(
          location = location,
          modifier = Modifier.testTag(LocationSearchFieldTestTags.SELECTED_LOCATION_CARD))
    }
  }
}

/**
 * Card displaying the selected location details.
 *
 * @param location The selected Location object
 * @param modifier Modifier for the card
 */
@Composable
private fun SelectedLocationCard(location: Location, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier.fillMaxWidth(),
      colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
      Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "Location",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
              Text(
                  text = "Selected Location",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                  text = location.name,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
              )
              Text(
                  text =
                      "Lat: ${String.format("%.4f", location.latitude)}, " +
                              "Lng: ${String.format("%.4f", location.longitude)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
              )
          }
      }
  }
}
@Preview(showBackground = true, name = "Empty State")
@Composable
private fun LocationSearchFieldPreview_Empty() {
    MaterialTheme {
        LocationSearchField(
            locationName = "",
            location = null,
            searchResults = emptyList(),
            isSearching = false,
            isError = false,
            errorMessage = "",
            onLocationNameChange = {},
            onLocationSelected = {},
            onSearchQueryChange = {},
            onClearSearch = {})
    }
}

@Preview(showBackground = true, name = "With Text")
@Composable
private fun LocationSearchFieldPreview_WithText() {
    MaterialTheme {
        LocationSearchField(
            locationName = "EPFL Lausanne",
            location = null,
            searchResults = emptyList(),
            isSearching = false,
            isError = false,
            errorMessage = "",
            onLocationNameChange = {},
            onLocationSelected = {},
            onSearchQueryChange = {},
            onClearSearch = {})
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
private fun LocationSearchFieldPreview_Loading() {
    MaterialTheme {
        LocationSearchField(
            locationName = "EPFL",
            location = null,
            searchResults = emptyList(),
            isSearching = true,
            isError = false,
            errorMessage = "",
            onLocationNameChange = {},
            onLocationSelected = {},
            onSearchQueryChange = {},
            onClearSearch = {})
    }
}

@Preview(showBackground = true, name = "With Search Results")
@Composable
private fun LocationSearchFieldPreview_WithResults() {
    MaterialTheme {
        LocationSearchField(
            locationName = "EPFL",
            location = null,
            searchResults = listOf(
                Location(46.5197, 6.5668, "EPFL, Lausanne, Switzerland"),
                Location(46.5191, 6.5667, "BC Building, EPFL"),
                Location(46.5200, 6.5670, "Rolex Learning Center")
            ),
            isSearching = false,
            isError = false,
            errorMessage = "",
            onLocationNameChange = {},
            onLocationSelected = {},
            onSearchQueryChange = {},
            onClearSearch = {})
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
private fun LocationSearchFieldPreview_Error() {
    MaterialTheme {
        LocationSearchField(
            locationName = "",
            location = null,
            searchResults = emptyList(),
            isSearching = false,
            isError = true,
            errorMessage = "Location name cannot be empty",
            onLocationNameChange = {},
            onLocationSelected = {},
            onSearchQueryChange = {},
            onClearSearch = {})
    }
}

@Preview(showBackground = true, name = "Selected Location")
@Composable
private fun LocationSearchFieldPreview_SelectedLocation() {
    MaterialTheme {
        LocationSearchField(
            locationName = "EPFL, Lausanne, Switzerland",
            location = Location(46.5197, 6.5668, "EPFL, Lausanne, Switzerland"),
            searchResults = emptyList(),
            isSearching = false,
            isError = false,
            errorMessage = "",
            onLocationNameChange = {},
            onLocationSelected = {},
            onSearchQueryChange = {},
            onClearSearch = {})
    }
}

@Preview(showBackground = true, name = "Disabled State")
@Composable
private fun LocationSearchFieldPreview_Disabled() {
    MaterialTheme {
        LocationSearchField(
            locationName = "EPFL",
            location = null,
            searchResults = emptyList(),
            isSearching = false,
            isError = false,
            errorMessage = "",
            enabled = false,
            onLocationNameChange = {},
            onLocationSelected = {},
            onSearchQueryChange = {},
            onClearSearch = {})
    }
}
