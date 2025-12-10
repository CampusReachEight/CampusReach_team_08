package com.android.sample.ui.request

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@Composable
fun FilterMenuButton(
    title: String,
    selectedCount: Int,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  OutlinedButton(
      onClick = onClick,
      modifier = modifier.testTag(testTag),
      colors =
          ButtonDefaults.outlinedButtonColors(
              containerColor = appPalette().accent, contentColor = appPalette().onAccent),
      border = BorderStroke(1.dp, appPalette().accent)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              val label = if (selectedCount > 0) "$title ($selectedCount)" else title
              Text(label)
              Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
            }
      }
}

@Composable
fun FilterMenuPanel(
    values: List<Enum<*>>,
    selected: Set<Enum<*>>,
    counts: Map<Enum<*>, Int>,
    labelOf: (Enum<*>) -> String,
    onToggle: (Enum<*>) -> Unit,
    dropdownSearchBarTestTag: String,
    rowTestTagOf: (Enum<*>) -> String
) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = ConstantRequestList.PaddingLarge)
              .background(color = appPalette().surface)) {
        Spacer(modifier = Modifier.height(ConstantRequestList.PaddingSmall))
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            color = appPalette().surface) {
              Column(
                  modifier = Modifier.fillMaxWidth().padding(ConstantRequestList.PaddingMedium)) {
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
      placeholder = { Text("Search options") },
      colors = getTextFieldColors())
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
  Box(
      modifier =
          Modifier.heightIn(max = ConstantRequestList.DropdownMaxHeight)
              .background(color = appPalette().surface)) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .background(color = appPalette().surface)) {
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
                      Checkbox(
                          checked = isChecked,
                          onCheckedChange = null,
                          colors =
                              CheckboxDefaults.colors(
                                  checkedColor = appPalette().accent,
                                  uncheckedColor = appPalette().secondary,
                                  checkmarkColor = appPalette().onAccent))
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
  fun labelOf(sort: RequestSort) =
      sort.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

  Box(modifier = modifier) {
    FilledTonalButton(
        onClick = { expanded = true },
        modifier = Modifier.testTag(RequestSearchFilterTestTags.SORT_BUTTON),
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = appPalette().accent, contentColor = appPalette().onAccent),
        border = BorderStroke(1.dp, appPalette().accent),
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
