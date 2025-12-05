package com.android.sample.ui.utils

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
