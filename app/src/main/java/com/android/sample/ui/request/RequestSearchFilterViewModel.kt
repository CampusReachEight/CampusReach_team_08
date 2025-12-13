package com.android.sample.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.search.LuceneRequestSearchEngine
import com.android.sample.model.search.SearchResult
import com.android.sample.ui.utils.RequestFacet
import com.android.sample.ui.utils.RequestFacetDefinitions
import java.util.Comparator
import kotlin.math.ceil
import kotlin.math.max
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class RequestSearchFilterViewModel(
    private val engineFactory: () -> LuceneRequestSearchEngine = { LuceneRequestSearchEngine() }
) : ViewModel() {

  private var engine: LuceneRequestSearchEngine? = null

  private val _baseRequests = MutableStateFlow<List<Request>>(emptyList())

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching

  private val _sortCriteria = MutableStateFlow(RequestSort.default())
  val sortCriteria: StateFlow<RequestSort> = _sortCriteria

  val facets: List<RequestFacet> = RequestFacetDefinitions.requests_all.map { RequestFacet(it) }

  @Volatile private var hasIndex: Boolean = false

  // Debounced, trimmed query for searching
  private val debouncedQuery: StateFlow<String> =
      _searchQuery
          .debounce(300)
          .map { it.trim() }
          .distinctUntilChanged()
          .stateIn(viewModelScope, SharingStarted.Lazily, "")

  // Search results flow: combines debounced query with base requests
  private val searchResults: StateFlow<List<SearchResult<Request>>> =
      combine(debouncedQuery, _baseRequests) { q, base ->
            if (q.isBlank()) return@combine emptyList<SearchResult<Request>>()

            _isSearching.update { true }
            try {
              val localEngine = engine
              if (!hasIndex || localEngine == null) {
                // Fallback: tokenize-based matching
                val tokens = tokenize(q)
                if (tokens.isEmpty()) return@combine emptyList<SearchResult<Request>>()

                val minShould = minShouldMatch(tokens.size)
                val lowerTokens = tokens.map { it.lowercase() }

                return@combine base
                    .mapNotNull { req ->
                      val text = req.toSearchText().lowercase()
                      val matchCount = lowerTokens.count { text.contains(it) }
                      if (matchCount >= minShould) {
                        val score = ((matchCount.toFloat() / lowerTokens.size) * 100).toInt()
                        SearchResult(item = req, score = score, matchedFields = emptyList())
                      } else null
                    }
                    .sortedByDescending { it.score }
              }

              localEngine.search(base, q)
            } catch (_: Throwable) {
              emptyList()
            } finally {
              _isSearching.update { false }
            }
          }
          .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  // Base for filtering: either search results or all requests
  private val searchBase: StateFlow<List<Request>> =
      combine(debouncedQuery, searchResults, _baseRequests) { q, results, base ->
            if (q.isBlank()) base else results.map { it.item }
          }
          .distinctUntilChanged()
          .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  private val _displayedRequests = MutableStateFlow<List<Request>>(emptyList())
  val displayedRequests: StateFlow<List<Request>> = _displayedRequests

  init {
    // For each facet, compute counts excluding other selections
    facets.forEach { f ->
      val otherSelections: List<StateFlow<Set<Enum<*>>>> =
          facets.filter { it !== f }.map { it.selected }
      val flows: List<Flow<*>> = listOf(searchBase) + otherSelections

      val countsFlow: StateFlow<Map<Enum<*>, Int>> =
          combine(flows) { arr ->
                val base = arr[0] as List<Request>
                val others = otherSelections.mapIndexed { idx, _ -> arr[idx + 1] as Set<Enum<*>> }

                val include: (Request) -> Boolean = { req ->
                  others.zip(facets.filter { it !== f }).all { (sel, facet) ->
                    sel.isEmpty() || facet.extract(req).any { it in sel }
                  }
                }

                val filtered = base.filter(include)
                val counts =
                    mutableMapOf<Enum<*>, Int>().apply { f.values.forEach { this[it] = 0 } }
                filtered.forEach { r ->
                  f.extract(r).forEach { v -> counts[v] = (counts[v] ?: 0) + 1 }
                }
                counts.toMap()
              }
              .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

      f.counts = countsFlow
    }

    // Single source of truth for displayed requests
    viewModelScope.launch {
      val selectionFlows: List<StateFlow<Set<Enum<*>>>> = facets.map { it.selected }
      combine(searchBase, combine(selectionFlows) { it.toList() }, sortCriteria) { list, sels, sort
            ->
            if (list.isEmpty()) return@combine emptyList<Request>()

            val filtered =
                list.filter { req ->
                  sels.zip(facets).all { (sel, facet) ->
                    sel.isEmpty() || facet.extract(req).any { it in sel }
                  }
                }

            applySort(filtered, sort)
          }
          .distinctUntilChanged()
          .collect { filtered -> _displayedRequests.value = filtered }
    }
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.update { query }
  }

  fun clearSearch() {
    _searchQuery.update { "" }
  }

  fun clearAllFilters() {
    facets.forEach { it.clear() }
  }

  fun initializeWithRequests(requests: List<Request>) {
    _baseRequests.value = requests

    viewModelScope.launch {
      try {
        if (engine == null) engine = engineFactory()
        engine?.indexRequests(requests)
        hasIndex = true
      } catch (_: Throwable) {
        hasIndex = false
        engine = null
      }
    }
  }

  fun setSortCriteria(criteria: RequestSort) {
    _sortCriteria.update { criteria }
  }

  private fun applySort(list: List<Request>, criteria: RequestSort): List<Request> =
      list.sortedWith(criteria.comparator)

  // Tokenization utilities for fallback search
  private fun tokenize(query: String): List<String> =
      REGEX_TOKEN.findAll(query.lowercase()).map { it.value }.filter { it.length >= 2 }.toList()

  private fun minShouldMatch(tokenCount: Int, ratio: Double = 0.6, floor: Int = 1): Int =
      if (tokenCount <= 0) 0 else max(floor, ceil(tokenCount * ratio).toInt())

  override fun onCleared() {
    try {
      engine?.close()
    } catch (_: Exception) {}
  }

  private companion object {
    // Regex pattern for tokenizing search queries (words, underscores, hyphens)
    val REGEX_TOKEN = Regex("[a-z0-9_-]+")
  }
}

// --- Sorting definitions ---
enum class RequestSort(val comparator: Comparator<Request>, val label: String) {
  NEWEST(compareByDescending<Request> { it.startTimeStamp }.thenBy { it.requestId }, "Latest"),
  OLDEST(compareBy<Request> { it.startTimeStamp }.thenBy { it.requestId }, "Oldest"),
  LAST_MINUTE(compareBy<Request> { it.expirationTime }.thenBy { it.startTimeStamp }, "Last minute"),
  MOST_PARTICIPANTS(
      Comparator<Request> { a, b ->
            val diff = b.people.size - a.people.size
            if (diff != 0) diff else a.startTimeStamp.compareTo(b.startTimeStamp)
          }
          .thenBy { it.requestId },
      "Participants (Descending)"),
  LEAST_PARTICIPANTS(
      Comparator<Request> { a, b ->
            val diff = a.people.size - b.people.size
            if (diff != 0) diff else a.startTimeStamp.compareTo(b.startTimeStamp)
          }
          .thenBy { it.requestId },
      "Participants (Ascending)"),
  TITLE_ASCENDING(
      compareBy<Request> { it.title.lowercase() }
          .thenBy { it.startTimeStamp }
          .thenBy { it.requestId },
      "Title (A-Z)"),
  TITLE_DESCENDING(
      compareByDescending<Request> { it.title.lowercase() }
          .thenBy { it.startTimeStamp }
          .thenBy { it.requestId },
      "Title (Z-A)");

  companion object {
    fun default(): RequestSort = NEWEST
  }
}

private fun statusOrderForSort(status: RequestStatus): Int =
    when (status) {
      RequestStatus.OPEN -> 0
      RequestStatus.IN_PROGRESS -> 1
      RequestStatus.COMPLETED -> 2
      RequestStatus.CANCELLED -> 3
      RequestStatus.ARCHIVED -> 4
    }
