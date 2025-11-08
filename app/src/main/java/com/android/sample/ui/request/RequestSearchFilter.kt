package com.android.sample.ui.request

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.displayString

/**
 * FilterKind: binds UI filter categories directly to Request model enums for clarity/extensibility.
 */
internal sealed interface FilterKind<T : Enum<T>> {
  val title: String
  val entries: Array<T>

  fun labelOf(item: T): String
}

internal object KindRequestType : FilterKind<RequestType> {
  override val title: String = RequestType.toString()
  override val entries: Array<RequestType> = RequestType.entries.toTypedArray()

  override fun labelOf(item: RequestType): String = item.displayString()
}

internal object KindRequestStatus : FilterKind<RequestStatus> {
  override val title: String = RequestStatus.toString()
  override val entries: Array<RequestStatus> = RequestStatus.entries.toTypedArray()

  override fun labelOf(item: RequestStatus): String = item.displayString()
}

internal object KindRequestTags : FilterKind<com.android.sample.model.request.Tags> {
  override val title: String = com.android.sample.model.request.Tags.toString()
  override val entries: Array<com.android.sample.model.request.Tags> =
      com.android.sample.model.request.Tags.entries.toTypedArray()

  override fun labelOf(item: com.android.sample.model.request.Tags): String = item.displayString()
}

/** Lightweight config used to render filter buttons in a compact, reusable way. */
internal data class FilterCfg<T : Enum<T>>(
    val kind: FilterKind<T>,
    val selectedCount: Int,
    val testTag: String,
)

/**
 * All composables related to searching, filtering and sorting of Requests. Moved out of
 * RequestList.kt to reduce cognitive load and keep RequestList file focused on high-level screen
 * composition and list rendering.
 */

// --- Filter infrastructure ---
/** Public entry point grouping the global search bar, filter buttons, and expanded panel. */
@Composable
internal fun FiltersSection(
    searchFilterViewModel: RequestSearchFilterViewModel,
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit
) {
  val selectedTypes by searchFilterViewModel.selectedTypes.collectAsState()
  val selectedStatuses by searchFilterViewModel.selectedStatuses.collectAsState()
  val selectedTags by searchFilterViewModel.selectedTags.collectAsState()
  val typeCounts by searchFilterViewModel.typeCounts.collectAsState()
  val statusCounts by searchFilterViewModel.statusCounts.collectAsState()
  val tagCounts by searchFilterViewModel.tagCounts.collectAsState()
  val sortCriteria by searchFilterViewModel.sortCriteria.collectAsState()

  var openMenu: FilterKind<*>? by remember { mutableStateOf<FilterKind<*>?>(null) }

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

        // Compact configs for the three filters
        val cfgs: List<FilterCfg<out Enum<*>>> =
            listOf(
                FilterCfg(
                    KindRequestType,
                    selectedTypes.size,
                    RequestListTestTags.REQUEST_TYPE_FILTER_DROPDOWN_BUTTON),
                FilterCfg(
                    KindRequestStatus,
                    selectedStatuses.size,
                    RequestListTestTags.REQUEST_STATUS_FILTER_DROPDOWN_BUTTON),
                FilterCfg(
                    KindRequestTags,
                    selectedTags.size,
                    RequestListTestTags.REQUEST_TAG_FILTER_DROPDOWN_BUTTON),
            )

        cfgs.forEach { anyCfg ->
          item {
            // Use star-projection for generic neutrality; only metadata used here
            FilterMenuButton(
                title = anyCfg.kind.title,
                selectedCount = anyCfg.selectedCount,
                testTag = anyCfg.testTag,
                onClick = { openMenu = if (openMenu == anyCfg.kind) null else anyCfg.kind },
                modifier = Modifier.height(ConstantRequestList.FilterButtonHeight))
          }
        }
      }

  when {
    openMenu === KindRequestType -> {
      FilterMenuPanel(
          values = KindRequestType.entries,
          selected = selectedTypes,
          counts = typeCounts,
          labelOf = { KindRequestType.labelOf(it) },
          onToggle = { searchFilterViewModel.toggleType(it) },
          dropdownSearchBarTestTag = RequestListTestTags.REQUEST_TYPE_FILTER_SEARCH_BAR,
          rowTestTagOf = { RequestListTestTags.getRequestTypeFilterTag(it) })
    }
    openMenu === KindRequestStatus -> {
      FilterMenuPanel(
          values = KindRequestStatus.entries,
          selected = selectedStatuses,
          counts = statusCounts,
          labelOf = { KindRequestStatus.labelOf(it) },
          onToggle = { searchFilterViewModel.toggleStatus(it) },
          dropdownSearchBarTestTag = RequestListTestTags.REQUEST_STATUS_FILTER_SEARCH_BAR,
          rowTestTagOf = { RequestListTestTags.getRequestStatusFilterTag(it.displayString()) })
    }
    openMenu === KindRequestTags -> {
      FilterMenuPanel(
          values = KindRequestTags.entries,
          selected = selectedTags,
          counts = tagCounts,
          labelOf = { KindRequestTags.labelOf(it) },
          onToggle = { searchFilterViewModel.toggleTag(it) },
          dropdownSearchBarTestTag = RequestListTestTags.REQUEST_TAG_FILTER_SEARCH_BAR,
          rowTestTagOf = { RequestListTestTags.getRequestTagFilterTag(it.displayString()) })
    }
    else -> Unit
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
  OutlinedButton(
      onClick = onClick,
      // In a LazyRow, prefer wrap content width (no fillMaxWidth) to avoid unbounded weight issues
      modifier = modifier.testTag(testTag)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Text("$title ($selectedCount)")
              Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
            }
      }
}

@Composable
private fun <E : Enum<E>> FilterMenuPanel(
    values: Array<E>,
    selected: Set<E>,
    counts: Map<E, Int>,
    labelOf: (E) -> String,
    onToggle: (E) -> Unit,
    dropdownSearchBarTestTag: String,
    rowTestTagOf: (E) -> String
) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = ConstantRequestList.PaddingLarge)) {
        Spacer(modifier = Modifier.height(ConstantRequestList.PaddingSmall))
        Surface(
            shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp, shadowElevation = 2.dp) {
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
      placeholder = { Text("Search options") })
}

@Composable
private fun <E : Enum<E>> FilterMenuValuesList(
    values: Array<E>,
    selected: Set<E>,
    counts: Map<E, Int>,
    labelOf: (E) -> String,
    onToggle: (E) -> Unit,
    localQuery: String,
    rowTestTagOf: (E) -> String
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
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
          Text(labelOf(current))
          Spacer(Modifier.width(4.dp))
          Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
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
