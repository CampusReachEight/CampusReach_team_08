package com.android.sample.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.search.LuceneRequestSearchEngine
import com.android.sample.model.search.SearchResult
import java.util.Comparator
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

  private inline fun <reified T> Flow<T>.stateInEager(): StateFlow<T> {
    val initial: T =
        when {
          List::class.java.isAssignableFrom(T::class.java) -> emptyList<Any?>() as T
          Map::class.java.isAssignableFrom(T::class.java) -> emptyMap<Any?, Any?>() as T
          else -> throw IllegalArgumentException("Unsupported type for stateInEager: ${T::class}")
        }
    return this.stateIn(viewModelScope, SharingStarted.Eagerly, initial)
  }

  private val _baseRequests = MutableStateFlow<List<Request>>(emptyList())

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching

  private val _sortCriteria = MutableStateFlow(RequestSort.default())
  val sortCriteria: StateFlow<RequestSort> = _sortCriteria

  // Build facets from single-source definitions
  val facets: List<RequestFacet> = RequestFacetDefinitions.all.map { RequestFacet(it) }

  @Volatile private var hasIndex: Boolean = false

  private val searchResults: StateFlow<List<SearchResult<Request>>> =
      _searchQuery
          .debounce(300)
          .map { it.trim() }
          .distinctUntilChanged()
          .let { debounced ->
            debounced
                .combine(_baseRequests) { q, base -> q to base }
                .map { (q, base) ->
                  if (q.isBlank()) return@map emptyList<SearchResult<Request>>()
                  _isSearching.update { true }
                  try {
                    val localEngine = engine
                    if (!hasIndex || localEngine == null) {
                      val lower = q.lowercase()
                      return@map base
                          .filter { req -> req.toSearchText().lowercase().contains(lower) }
                          .map { SearchResult(item = it, score = 100, matchedFields = emptyList()) }
                    }
                    localEngine.search(base, q)
                  } catch (_: Throwable) {
                    emptyList()
                  } finally {
                    _isSearching.update { false }
                  }
                }
          }
          .stateInEager()

  private val searchBase: StateFlow<List<Request>> =
      combine(_baseRequests, searchResults, _searchQuery) { base, results, q ->
            if (q.isBlank()) base else results.map { it.item }
          }
          .distinctUntilChanged()
          .stateInEager()

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
              .stateInEager()
      f.counts = countsFlow
    }

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

  private fun recomputeDisplayed() {
    val base = searchBase.value
    val sels = facets.map { it.selected.value }
    val sort = _sortCriteria.value
    _displayedRequests.value =
        applySort(
            base.filter { req ->
              sels.zip(facets).all { (sel, facet) ->
                sel.isEmpty() || facet.extract(req).any { it in sel }
              }
            },
            sort)
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.update { query.trim() }
  }

  fun clearSearch() {
    _searchQuery.update { "" }
    recomputeDisplayed()
  }

  fun clearAllFilters() {
    facets.forEach { it.clear() }
    recomputeDisplayed()
  }

  fun initializeWithRequests(requests: List<Request>) {
    _baseRequests.value = requests
    recomputeDisplayed()
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
    recomputeDisplayed()
  }

  private fun applySort(list: List<Request>, criteria: RequestSort): List<Request> =
      list.sortedWith(criteria.comparator)

  override fun onCleared() {
    try {
      engine?.close()
    } catch (_: Exception) {}
  }
}

// Sorting stays as-is
enum class RequestSort(val comparator: Comparator<Request>) {
  NEWEST(compareByDescending<Request> { it.startTimeStamp }.thenBy { it.requestId }),
  OLDEST(compareBy<Request> { it.startTimeStamp }.thenBy { it.requestId }),
  LAST_MINUTE(compareBy<Request> { it.expirationTime }.thenBy { it.startTimeStamp }),
  STATUS(
      Comparator<Request> { a, b ->
            val diff = statusOrderForSort(a.status) - statusOrderForSort(b.status)
            if (diff != 0) diff else a.startTimeStamp.compareTo(b.startTimeStamp)
          }
          .thenBy { it.requestId }),
  MOST_PARTICIPANTS(
      Comparator<Request> { a, b ->
            val diff = b.people.size - a.people.size
            if (diff != 0) diff else a.startTimeStamp.compareTo(b.startTimeStamp)
          }
          .thenBy { it.requestId }),
  LEAST_PARTICIPANTS(
      Comparator<Request> { a, b ->
            val diff = a.people.size - b.people.size
            if (diff != 0) diff else a.startTimeStamp.compareTo(b.startTimeStamp)
          }
          .thenBy { it.requestId }),
  TITLE_ASCENDING(
      compareBy<Request> { it.title.lowercase() }
          .thenBy { it.startTimeStamp }
          .thenBy { it.requestId }),
  TITLE_DESCENDING(
      compareByDescending<Request> { it.title.lowercase() }
          .thenBy { it.startTimeStamp }
          .thenBy { it.requestId });

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
