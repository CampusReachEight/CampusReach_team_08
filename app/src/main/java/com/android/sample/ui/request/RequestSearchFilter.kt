package com.android.sample.ui.request

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.android.sample.ui.getTextFieldColors
import com.android.sample.ui.theme.appPalette
import com.android.sample.ui.utils.EnumFilterButtonSimple
import com.android.sample.ui.utils.EnumFilterPanelSimple

/** Centralized filter UI consuming dynamic facets from ViewModel. */
@Composable
internal fun FiltersSection(
    searchFilterViewModel: RequestSearchFilterViewModel,
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit
) {
  val sortCriteria by searchFilterViewModel.sortCriteria.collectAsState()
  val facets = searchFilterViewModel.facets
  val selectedSets = facets.map { it.selected.collectAsState() }
  var openFacetId: String? by remember { mutableStateOf<String?>(null) }

  RequestSearchBar(
      query = query,
      onQueryChange = onQueryChange,
      onClear = onClearQuery,
      isSearching = isSearching,
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = ConstantRequestList.PaddingLarge)
              .testTag(RequestListTestTags.REQUEST_SEARCH_BAR))

  Spacer(modifier = Modifier.height(ConstantRequestList.PaddingMedium))

  // Horizontal scrollable bar of buttons: sort first, then filters
  LazyRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = ConstantRequestList.PaddingLarge),
      horizontalArrangement = Arrangement.spacedBy(ConstantRequestList.RowSpacing)) {
        // Sorting button first
        item {
          SortCriteriaButton(
              current = sortCriteria,
              onSelect = { searchFilterViewModel.setSortCriteria(it) },
              modifier = Modifier.height(ConstantRequestList.FilterButtonHeight))
        }
        facets.forEachIndexed { index, facet ->
          val selectedCount = selectedSets[index].value.size
          item(key = facet.id) {
            EnumFilterButtonSimple(
                title = facet.title,
                selectedCount = selectedCount,
                testTag = facet.dropdownButtonTag,
                onClick = { openFacetId = if (openFacetId == facet.id) null else facet.id },
                modifier = Modifier.height(ConstantRequestList.FilterButtonHeight))
          }
        }
      }

  facets
      .find { it.id == openFacetId }
      ?.let { openFacet ->
        val countsState = openFacet.counts.collectAsState()
        EnumFilterPanelSimple(
            values = openFacet.values,
            selected = openFacet.selected.collectAsState().value,
            counts = countsState.value,
            labelOf = { openFacet.labelOf(it) },
            onToggle = { openFacet.toggle(it) },
            dropdownSearchBarTestTag = openFacet.searchBarTag,
            rowTestTagOf = { openFacet.rowTagOf(it) },
            horizontalPadding = ConstantRequestList.PaddingLarge,
            maxHeight = ConstantRequestList.DropdownMaxHeight)
      }
}

/** Global search bar bound to SearchFilterViewModel's state. */
@Composable
private fun RequestSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
  OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier = modifier,
      singleLine = true,
      colors = getTextFieldColors(),
      placeholder = { Text("Search") },
      trailingIcon = {
        if (query.isNotEmpty()) {
          TextButton(onClick = onClear) { Text("Clear") }
        } else if (isSearching) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
      })
}

// --- Sorting UI ---
object RequestSearchFilterTestTags {
  const val SORT_BUTTON = "requestSortButton"
  const val SORT_MENU = "requestSortMenu"
}

/**
 * Button that shows current sort criteria and opens a dropdown to pick another one. Positioned by
 * caller (RequestListScreen) typically at top-end over the list area.
 */
@Composable
internal fun SortCriteriaButton(
    current: RequestSort,
    onSelect: (RequestSort) -> Unit,
    modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  // User-facing label: transform enum name to Title Case
  fun labelOf(sort: RequestSort) = sort.label

  Box(modifier = modifier) {
    FilledTonalButton(
        onClick = { expanded = true },
        modifier = Modifier.testTag(RequestSearchFilterTestTags.SORT_BUTTON),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
          Text(labelOf(current))
          Spacer(Modifier.width(4.dp))
          Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        properties = PopupProperties(focusable = false),
        containerColor = appPalette().surface,
        modifier = Modifier.testTag(RequestSearchFilterTestTags.SORT_MENU)) {
          RequestSort.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(labelOf(option)) },
                onClick = {
                  expanded = false
                  if (option != current) onSelect(option)
                },
                leadingIcon =
                    if (option == current) {
                      { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
                    } else null)
          }
        }
  }
}
