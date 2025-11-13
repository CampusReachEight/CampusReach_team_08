package com.android.sample.ui.request

import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.model.request.displayString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single-source facet configuration. Add a new Request enum attribute by:
 * 1. Adding the enum field to Request.
 * 2. Adding a FacetDefinition entry below. Everything else (UI + ViewModel) updates automatically.
 */
object RequestFacetDefinitions {
  data class FacetDefinition(
      val id: String,
      val title: String,
      val values: List<Enum<*>>, // full set of enum values (List for structural equality)
      val extract: (Request) -> List<Enum<*>>, // how to read facet values from a Request
      val dropdownButtonTag: String,
      val searchBarTag: String,
      val rowTagOf: (Enum<*>) -> String,
      val labelOf: (Enum<*>) -> String = { it.name },
  )

  // Edit only this list to add facets.
  val all: List<FacetDefinition> =
      listOf(
          FacetDefinition(
              id = "type",
              title = RequestType.toString(),
              values = RequestType.entries.toList(),
              extract = { it.requestType },
              dropdownButtonTag = RequestListTestTags.REQUEST_TYPE_FILTER_DROPDOWN_BUTTON,
              searchBarTag = RequestListTestTags.REQUEST_TYPE_FILTER_SEARCH_BAR,
              rowTagOf = { v -> RequestListTestTags.getRequestTypeFilterTag(v as RequestType) },
              labelOf = { (it as RequestType).displayString() },
          ),
          FacetDefinition(
              id = "status",
              title = RequestStatus.toString(),
              values = RequestStatus.entries.toList(),
              extract = { listOf(it.status) },
              dropdownButtonTag = RequestListTestTags.REQUEST_STATUS_FILTER_DROPDOWN_BUTTON,
              searchBarTag = RequestListTestTags.REQUEST_STATUS_FILTER_SEARCH_BAR,
              rowTagOf = { v -> RequestListTestTags.getRequestStatusFilterTag(v as RequestStatus) },
              labelOf = { (it as RequestStatus).displayString() },
          ),
          FacetDefinition(
              id = "tags",
              title = Tags.toString(),
              values = Tags.entries.toList(),
              extract = { it.tags },
              dropdownButtonTag = RequestListTestTags.REQUEST_TAG_FILTER_DROPDOWN_BUTTON,
              searchBarTag = RequestListTestTags.REQUEST_TAG_FILTER_SEARCH_BAR,
              rowTagOf = { v -> RequestListTestTags.getRequestTagFilterTag(v as Tags) },
              labelOf = { (it as Tags).displayString() },
          ),
      )
}

class RequestFacet internal constructor(val def: RequestFacetDefinitions.FacetDefinition) {
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

  val extract: (Request) -> List<Enum<*>>
    get() = def.extract

  internal val _selected = MutableStateFlow<Set<Enum<*>>>(emptySet())
  val selected: StateFlow<Set<Enum<*>>>
    get() = _selected

  internal lateinit var counts: StateFlow<Map<Enum<*>, Int>>

  fun toggle(value: Enum<*>) {
    _selected.value =
        if (value in _selected.value) _selected.value - value else _selected.value + value
  }

  fun clear() {
    _selected.value = emptySet()
  }
}
