package com.android.sample.ui.request

import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.model.request.displayString
import com.android.sample.ui.utils.EnumFacet
import com.android.sample.ui.utils.EnumFacetDefinitions

/**
 * Request-specific facet definitions using the generic EnumFacetDefinitions system.
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

  /** Legacy accessor for backwards compatibility. Prefer using `requests_all` for new code. */
  @Deprecated("Use requests_all instead", ReplaceWith("requests_all"))
  val all: List<EnumFacetDefinitions.FacetDefinition<Request>>
    get() = requests_all
}

/**
 * Type alias for Request-specific EnumFacet. Provides backwards compatibility while leveraging the
 * generic EnumFacet implementation.
 */
typealias RequestFacet = EnumFacet<Request>
