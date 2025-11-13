package com.android.sample.ui.request

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.PopupProperties

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
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = ConstantRequestList.PaddingLarge)
              .testTag(RequestListTestTags.FILTER_BAR),
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
            FilterMenuButton(
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
        FilterMenuPanel(
            values = openFacet.values,
            selected = openFacet.selected.collectAsState().value,
            counts = countsState.value,
            labelOf = { openFacet.labelOf(it) },
            onToggle = { openFacet.toggle(it) },
            dropdownSearchBarTestTag = openFacet.searchBarTag,
            rowTestTagOf = { openFacet.rowTagOf(it) })
      }
}

@Composable
private fun FilterMenuButton(
    title: String,
    selectedCount: Int,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  OutlinedButton(onClick = onClick, modifier = modifier.testTag(testTag)) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ConstantRequestList.RowSpacing),
        verticalAlignment = Alignment.CenterVertically) {
          val label = if (selectedCount > 0) "$title ($selectedCount)" else title
          Text(label)
          androidx.compose.material3.Icon(
              imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
        }
  }
}

@Composable
private fun FilterMenuPanel(
    values: List<Enum<*>>,
    selected: Set<Enum<*>>,
    counts: Map<Enum<*>, Int>,
    labelOf: (Enum<*>) -> String,
    onToggle: (Enum<*>) -> Unit,
    dropdownSearchBarTestTag: String,
    rowTestTagOf: (Enum<*>) -> String
) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = ConstantRequestList.PaddingLarge)) {
        Surface(shape = MaterialTheme.shapes.medium) {
          Column(modifier = Modifier.fillMaxWidth().padding(ConstantRequestList.PaddingMedium)) {
            var localQuery by rememberSaveable { mutableStateOf("") }
            FilterMenuSearchBar(
                query = localQuery,
                onQueryChange = { localQuery = it },
                dropdownSearchBarTestTag = dropdownSearchBarTestTag)
            Spacer(modifier = Modifier.height(ConstantRequestList.PaddingSmall))
            FilterMenuValuesList(
                values = values,
                selected = selected,
                counts = counts,
                labelOf = labelOf,
                onToggle = onToggle,
                localQuery = localQuery,
                rowTestTagOf = rowTestTagOf)
          }
        }
      }
}

@Composable
private fun FilterMenuSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    dropdownSearchBarTestTag: String
) {
  OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier = Modifier.fillMaxWidth().testTag(dropdownSearchBarTestTag),
      singleLine = true,
      placeholder = { Text("Search options") })
}

@Composable
private fun FilterMenuValuesList(
    values: List<Enum<*>>,
    selected: Set<Enum<*>>,
    counts: Map<Enum<*>, Int>,
    labelOf: (Enum<*>) -> String,
    onToggle: (Enum<*>) -> Unit,
    localQuery: String,
    rowTestTagOf: (Enum<*>) -> String
) {
  val filteredValues =
      remember(localQuery, values, counts) {
        values
            .filter { labelOf(it).contains(localQuery, ignoreCase = true) }
            .sortedByDescending { counts[it] ?: 0 }
      }
  Box(modifier = Modifier.heightIn(max = ConstantRequestList.DropdownMaxHeight)) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
      filteredValues.forEach { v ->
        val isChecked = selected.contains(v)
        val count = counts[v] ?: 0
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable { onToggle(v) }
                    .padding(horizontal = ConstantRequestList.FilterRowHorizontalPadding)
                    .testTag(rowTestTagOf(v))
                    .height(ConstantRequestList.FilterRowHeight),
            horizontalArrangement = Arrangement.Start) {
              Checkbox(checked = isChecked, onCheckedChange = null)
              Spacer(modifier = Modifier.width(ConstantRequestList.RowSpacing))
              Text(text = labelOf(v))
              Spacer(modifier = Modifier.weight(1f))
              Text(text = count.toString())
            }
      }
    }
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
      placeholder = { Text("Search") },
      trailingIcon = {
        when {
          query.isNotEmpty() -> {
            TextButton(
                onClick = onClear,
                modifier = Modifier.testTag(RequestListTestTags.CLEAR_SEARCH_BUTTON)) {
                  Text("Clear")
                }
          }
          isSearching -> {
            CircularProgressIndicator(
                modifier =
                    Modifier.size(ConstantRequestList.SearchBarSize)
                        .testTag(RequestListTestTags.REQUEST_SEARCH_PROGRESS),
                strokeWidth = ConstantRequestList.SearchBarStrokeWidth)
          }
        }
      })
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
  fun labelOf(sort: RequestSort) =
      sort.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

  Box(modifier = modifier) {
    FilledTonalButton(
        onClick = { expanded = true },
        modifier = Modifier.testTag(RequestListTestTags.SORT_BUTTON),
        contentPadding =
            PaddingValues(
                horizontal = ConstantRequestList.PaddingMedium,
                vertical = ConstantRequestList.PaddingSmall)) {
          Text(labelOf(current))
          Spacer(Modifier.width(ConstantRequestList.RowSpacing))
          androidx.compose.material3.Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        properties = PopupProperties(focusable = false),
        modifier = Modifier.testTag(RequestListTestTags.SORT_MENU)) {
          RequestSort.entries.forEach { option ->
            DropdownMenuItem(
                modifier = Modifier.testTag(RequestListTestTags.getSortOptionTag(option)),
                text = { Text(labelOf(option)) },
                onClick = {
                  expanded = false
                  if (option != current) onSelect(option)
                },
                leadingIcon =
                    if (option == current) {
                      {
                        androidx.compose.material3.Icon(
                            Icons.Filled.ArrowDropDown, contentDescription = null)
                      }
                    } else null)
          }
        }
  }
}
