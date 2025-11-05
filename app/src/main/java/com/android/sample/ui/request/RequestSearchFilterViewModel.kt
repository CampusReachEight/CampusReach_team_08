package com.android.sample.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.model.search.LuceneRequestSearchEngine
import com.android.sample.model.search.SearchResult
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

/**
 * ViewModel responsible for full-text search (Lucene) and facet filtering for Requests.
 *
 * Responsibilities:
 * - Manage query and searching state
 * - Debounced search using Lucene (300ms)
 * - Manage facet selections (types, statuses, tags)
 * - Expose counts and the final displayedRequests
 */
class RequestSearchFilterViewModel(
    private val engineFactory: () -> LuceneRequestSearchEngine = { LuceneRequestSearchEngine() }
) : ViewModel() {

  // Lazily-created Lucene engine to avoid classloading or initialization issues
  private var engine: LuceneRequestSearchEngine? = null

  // Replace overloaded helpers with a single inline reified version to avoid JVM clash
  private inline fun <reified T> kotlinx.coroutines.flow.Flow<T>.stateInEager(): StateFlow<T> {
    val initial: T =
        when {
          List::class.java.isAssignableFrom(T::class.java) -> emptyList<Any?>() as T
          Map::class.java.isAssignableFrom(T::class.java) -> emptyMap<Any?, Any?>() as T
          else -> throw IllegalArgumentException("Unsupported type for stateInEager: ${T::class}")
        }
    return this.stateIn(viewModelScope, SharingStarted.Eagerly, initial)
  }

  // Base requests set by UI after loading via RequestListViewModel
  private val _baseRequests = MutableStateFlow<List<Request>>(emptyList())

  // Public query/searching state
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching

  // Facet selections
  private val _selectedTypes = MutableStateFlow<Set<RequestType>>(emptySet())
  val selectedTypes: StateFlow<Set<RequestType>> = _selectedTypes

  private val _selectedStatuses = MutableStateFlow<Set<RequestStatus>>(emptySet())
  val selectedStatuses: StateFlow<Set<RequestStatus>> = _selectedStatuses

  private val _selectedTags = MutableStateFlow<Set<Tags>>(emptySet())
  val selectedTags: StateFlow<Set<Tags>> = _selectedTags

  @Volatile private var hasIndex: Boolean = false

  // Debounced Lucene search results
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
                    // If engine isn't ready, fallback to substring search
                    val localEngine = engine
                    if (!hasIndex || localEngine == null) {
                      val lower = q.lowercase()
                      return@map base
                          .filter { req ->
                            val haystack = req.toSearchText().lowercase()
                            haystack.contains(lower)
                          }
                          .map { SearchResult(item = it, score = 100, matchedFields = emptyList()) }
                    }
                    localEngine.search(base, q)
                  } catch (_: Throwable) {
                    // Any Lucene failure should not crash; return empty to be safe
                    emptyList()
                  } finally {
                    _isSearching.update { false }
                  }
                }
          }
          .stateInEager()

  // Determine the base collection for counts and filtering: either search results or all
  private val searchBase: StateFlow<List<Request>> =
      combine(_baseRequests, searchResults, _searchQuery) { base, results, q ->
            if (q.isBlank()) base else results.map { it.item }
          }
          .distinctUntilChanged()
          .stateInEager()

  // Backing state for displayed list to allow synchronous recomputation after toggles/clears
  private val _displayedRequests = MutableStateFlow<List<Request>>(emptyList())
  val displayedRequests: StateFlow<List<Request>> = _displayedRequests

  init {
    // Keep displayedRequests reactive to all upstream changes
    viewModelScope.launch {
      combine(searchBase, selectedTypes, selectedStatuses, selectedTags) {
              list,
              types,
              statuses,
              tags ->
            if (list.isEmpty()) return@combine emptyList<Request>()
            list.filter { req ->
              val typeOk = types.isEmpty() || req.requestType.any { it in types }
              val statusOk = statuses.isEmpty() || req.status in statuses
              val tagsOk = tags.isEmpty() || req.tags.any { it in tags }
              typeOk && statusOk && tagsOk
            }
          }
          .distinctUntilChanged()
          .collect { filtered -> _displayedRequests.value = filtered }
    }
  }

  private fun recomputeDisplayed() {
    val base = searchBase.value
    val types = _selectedTypes.value
    val statuses = _selectedStatuses.value
    val tags = _selectedTags.value
    _displayedRequests.value =
        base.filter { req ->
          val typeOk = types.isEmpty() || req.requestType.any { it in types }
          val statusOk = statuses.isEmpty() || req.status in statuses
          val tagsOk = tags.isEmpty() || req.tags.any { it in tags }
          typeOk && statusOk && tagsOk
        }
  }

  // Facet counts with self-exclusion across the other active facets, computed over searchBase
  val typeCounts: StateFlow<Map<RequestType, Int>> =
      combine(searchBase, selectedStatuses, selectedTags) { list, statuses, tags ->
            val base =
                list.filter { req ->
                  val statusOk = statuses.isEmpty() || req.status in statuses
                  val tagsOk = tags.isEmpty() || req.tags.any { it in tags }
                  statusOk && tagsOk
                }
            val counts =
                mutableMapOf<RequestType, Int>().apply {
                  RequestType.entries.forEach { this[it] = 0 }
                }
            base.forEach { req -> req.requestType.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
            counts.toMap()
          }
          .distinctUntilChanged()
          .stateInEager()

  val statusCounts: StateFlow<Map<RequestStatus, Int>> =
      combine(searchBase, selectedTypes, selectedTags) { list, types, tags ->
            val base =
                list.filter { req ->
                  val typeOk = types.isEmpty() || req.requestType.any { it in types }
                  val tagsOk = tags.isEmpty() || req.tags.any { it in tags }
                  typeOk && tagsOk
                }
            val counts =
                mutableMapOf<RequestStatus, Int>().apply {
                  RequestStatus.entries.forEach { this[it] = 0 }
                }
            base.forEach { req -> counts[req.status] = (counts[req.status] ?: 0) + 1 }
            counts.toMap()
          }
          .distinctUntilChanged()
          .stateInEager()

  val tagCounts: StateFlow<Map<Tags, Int>> =
      combine(searchBase, selectedTypes, selectedStatuses) { list, types, statuses ->
            val base =
                list.filter { req ->
                  val typeOk = types.isEmpty() || req.requestType.any { it in types }
                  val statusOk = statuses.isEmpty() || req.status in statuses
                  typeOk && statusOk
                }
            val counts = mutableMapOf<Tags, Int>().apply { Tags.entries.forEach { this[it] = 0 } }
            base.forEach { req -> req.tags.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
            counts.toMap()
          }
          .distinctUntilChanged()
          .stateInEager()

  // API
  fun updateSearchQuery(query: String) {
    _searchQuery.update { query.trim() }
    // No need to force recompute here; debounce/searchResults will drive updates
  }

  fun clearSearch() {
    _searchQuery.update { "" }
    recomputeDisplayed()
  }

  fun toggleType(type: RequestType) {
    _selectedTypes.update { cur -> if (type in cur) cur - type else cur + type }
    recomputeDisplayed()
  }

  fun toggleStatus(status: RequestStatus) {
    _selectedStatuses.update { cur -> if (status in cur) cur - status else cur + status }
    recomputeDisplayed()
  }

  fun toggleTag(tag: Tags) {
    _selectedTags.update { cur -> if (tag in cur) cur - tag else cur + tag }
    recomputeDisplayed()
  }

  fun clearAllFilters() {
    _selectedTypes.update { emptySet() }
    _selectedStatuses.update { emptySet() }
    _selectedTags.update { emptySet() }
    recomputeDisplayed()
  }

  fun initializeWithRequests(requests: List<Request>) {
    _baseRequests.value = requests
    recomputeDisplayed()
    // Index in background tied to the ViewModel scope dispatcher (test dispatcher in tests)
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

  override fun onCleared() {
    super.onCleared()
    try {
      engine?.close()
    } catch (_: Exception) {}
  }
}
