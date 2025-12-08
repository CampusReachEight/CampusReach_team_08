package com.android.sample.ui.utils

import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.model.request.displayString
import com.android.sample.ui.request.RequestListTestTags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Generic facet configuration for enum-based filtering. This utility allows any screen (Requests,
 * Leaderboard, etc.) to define filterable enum attributes with consistent behavior.
 *
 * Usage:
 * 1. Define a `FacetDefinition<T>` for your data type T (e.g., Request, UserProfile)
 * 2. Create an `EnumFacet<T>` instance from the definition
 * 3. Use `EnumFacet.selected` StateFlow to observe/filter, `toggle()` to update selection
 *
 * @param T The type of item being filtered (e.g., Request, UserProfile)
 */
object EnumFacetDefinitions {

  /**
   * Defines a single filterable enum attribute for items of type T.
   *
   * @param T The type of item being filtered
   * @property id Unique identifier for this facet (e.g., "type", "status", "section")
   * @property title Display title for the filter button (e.g., "Type", "Section")
   * @property values All possible enum values for this facet
   * @property extract Function to extract the facet value(s) from an item of type T
   * @property dropdownButtonTag Test tag for the dropdown button
   * @property searchBarTag Test tag for the search bar within the dropdown
   * @property rowTagOf Function to generate test tag for each filter row
   * @property labelOf Function to convert enum value to display label
   */
  data class FacetDefinition<T>(
      val id: String,
      val title: String,
      val values: List<Enum<*>>,
      val extract: (T) -> List<Enum<*>>,
      val dropdownButtonTag: String,
      val searchBarTag: String,
      val rowTagOf: (Enum<*>) -> String,
      val labelOf: (Enum<*>) -> String = { it.name },
  )
}

/**
 * Runtime facet state holder for enum-based filtering.
 *
 * Manages the selected values and provides reactive state for UI binding. The `counts` property
 * must be initialized by the ViewModel after construction to enable count display.
 *
 * @param T The type of item being filtered
 * @property def The facet definition this instance is based on
 */
class EnumFacet<T>(val def: EnumFacetDefinitions.FacetDefinition<T>) {
  val id: String
    get() = def.id

  val title: String
    get() = def.title

  val values: List<Enum<*>>
    get() = def.values

  val dropdownButtonTag: String
    get() = def.dropdownButtonTag

  val searchBarTag: String
    get() = def.searchBarTag

  val labelOf: (Enum<*>) -> String
    get() = def.labelOf

  val rowTagOf: (Enum<*>) -> String
    get() = def.rowTagOf

  val extract: (T) -> List<Enum<*>>
    get() = def.extract

  internal val _selected = MutableStateFlow<Set<Enum<*>>>(emptySet())

  /** Observable set of currently selected enum values for this facet. */
  val selected: StateFlow<Set<Enum<*>>>
    get() = _selected

  /**
   * Count of items matching each enum value. Must be initialized by the ViewModel after
   * construction to enable dynamic count updates based on other filter selections.
   */
  lateinit var counts: StateFlow<Map<Enum<*>, Int>>

  /** Toggles the selection state of the given enum value. */
  fun toggle(value: Enum<*>) {
    _selected.value =
        if (value in _selected.value) _selected.value - value else _selected.value + value
  }

  /** Clears all selected values for this facet. */
  fun clear() {
    _selected.value = emptySet()
  }
}

/**
 * Request-specific facet definitions.
 *
 * To add a new Request enum attribute:
 * 1. Add the enum field to Request.
 * 2. Add a FacetDefinition entry to `requests_all` below.
 * 3. Everything else (UI + ViewModel) updates automatically.
 */
object RequestFacetDefinitions {

  /** All facet definitions for Request filtering. */
  val requests_all: List<EnumFacetDefinitions.FacetDefinition<Request>> =
      listOf(
          EnumFacetDefinitions.FacetDefinition(
              id = "type",
              title = RequestType.toString(),
              values = RequestType.entries.toList(),
              extract = { it.requestType },
              dropdownButtonTag = RequestListTestTags.REQUEST_TYPE_FILTER_DROPDOWN_BUTTON,
              searchBarTag = RequestListTestTags.REQUEST_TYPE_FILTER_SEARCH_BAR,
              rowTagOf = { v -> RequestListTestTags.getRequestTypeFilterTag(v as RequestType) },
              labelOf = { (it as RequestType).displayString() },
          ),
          EnumFacetDefinitions.FacetDefinition(
              id = "status",
              title = RequestStatus.toString(),
              values = listOf(RequestStatus.OPEN, RequestStatus.IN_PROGRESS),
              extract = { listOf(it.status) },
              dropdownButtonTag = RequestListTestTags.REQUEST_STATUS_FILTER_DROPDOWN_BUTTON,
              searchBarTag = RequestListTestTags.REQUEST_STATUS_FILTER_SEARCH_BAR,
              rowTagOf = { v ->
                RequestListTestTags.getRequestStatusFilterTag((v as RequestStatus).displayString())
              },
              labelOf = { (it as RequestStatus).displayString() },
          ),
          EnumFacetDefinitions.FacetDefinition(
              id = "tags",
              title = Tags.toString(),
              values = Tags.entries.toList(),
              extract = { it.tags },
              dropdownButtonTag = RequestListTestTags.REQUEST_TAG_FILTER_DROPDOWN_BUTTON,
              searchBarTag = RequestListTestTags.REQUEST_TAG_FILTER_SEARCH_BAR,
              rowTagOf = { v ->
                RequestListTestTags.getRequestTagFilterTag((v as Tags).displayString())
              },
              labelOf = { (it as Tags).displayString() },
          ),
      )
}

/**
 * Type alias for Request-specific EnumFacet. Provides backwards compatibility while leveraging the
 * generic EnumFacet implementation.
 */
typealias RequestFacet = EnumFacet<Request>
