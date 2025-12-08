package com.android.sample.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.search.LuceneProfileSearchEngine
import com.android.sample.model.search.SearchResult
import com.android.sample.ui.utils.EnumFacet
import com.android.sample.ui.utils.RangeFacet
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
class LeaderboardSearchFilterViewModel(
    private val engineFactory: () -> LuceneProfileSearchEngine = { LuceneProfileSearchEngine() }
) : ViewModel() {

  private var engine: LuceneProfileSearchEngine? = null
  private var hasIndex: Boolean = false

  private val _baseProfiles = MutableStateFlow<List<UserProfile>>(emptyList())

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching

  private val _sortCriteria = MutableStateFlow(LeaderboardSort.default())
  val sortCriteria: StateFlow<LeaderboardSort> = _sortCriteria

  val facets: List<LeaderboardFacet> = LeaderboardFacetDefinitions.users_all.map { EnumFacet(it) }
  val rangeFacets: List<LeaderboardRangeFacet> = LeaderboardRangeFilters.all.map { RangeFacet(it) }

  // Debounced, trimmed query for searching
  private val debouncedQuery: StateFlow<String> =
      _searchQuery
          .debounce(SEARCH_DEBOUNCE_MS)
          .map { it.trim() }
          .distinctUntilChanged()
          .stateIn(viewModelScope, SharingStarted.Lazily, "")

  // Search results via Lucene (or fallback if engine unavailable)
  private val searchResults: StateFlow<List<SearchResult<UserProfile>>> =
      combine(debouncedQuery, _baseProfiles) { q, base ->
            if (q.isBlank()) return@combine emptyList<SearchResult<UserProfile>>()

            _isSearching.update { true }
            try {
              val localEngine = engine
              if (localEngine == null || !hasIndex) {
                // Fallback: simple token matching on name/lastName
                val tokens = tokenize(q)
                if (tokens.isEmpty()) return@combine emptyList<SearchResult<UserProfile>>()

                val minShould = minShouldMatch(tokens.size)
                val lowerTokens = tokens.map { it.lowercase() }

                return@combine base
                    .mapNotNull { profile ->
                      val text = profile.toSearchText().lowercase()
                      val matchCount = lowerTokens.count { text.contains(it) }
                      if (matchCount >= minShould) {
                        val score =
                            ((matchCount.toFloat() / lowerTokens.size) * SCORE_PERCENT_SCALE)
                                .toInt()
                        SearchResult(item = profile, score = score, matchedFields = emptyList())
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

  // Base list for filtering: either search results or all profiles
  private val searchBase: StateFlow<List<UserProfile>> =
      combine(debouncedQuery, searchResults, _baseProfiles) { q, results, base ->
            if (q.isBlank()) base else results.map { it.item }
          }
          .distinctUntilChanged()
          .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  private val _displayedProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
  val displayedProfiles: StateFlow<List<UserProfile>> = _displayedProfiles

  init {
    // Initialize counts for enum facets, respecting other selections and range filters
    facets.forEach { facet ->
      val otherSelections: List<StateFlow<Set<Enum<*>>>> =
          facets.filter { it !== facet }.map { it.selected }
      val rangeFlows: List<StateFlow<IntRange>> = rangeFacets.map { it.currentRange }
      val flows: List<Flow<*>> = listOf(searchBase) + otherSelections + rangeFlows

      val countsFlow: StateFlow<Map<Enum<*>, Int>> =
          combine(flows) { arr ->
                val base = arr[BASE_INDEX] as List<UserProfile>
                val otherSelected =
                    otherSelections.mapIndexed { idx, _ ->
                      arr[idx + NEXT_INDEX_OFFSET] as Set<Enum<*>>
                    }
                val rangesOffset = NEXT_INDEX_OFFSET + otherSelections.size
                val ranges =
                    rangeFacets.mapIndexed { idx, rf ->
                      rf.copyRange(arr[rangesOffset + idx] as IntRange)
                    }

                val include: (UserProfile) -> Boolean = { profile ->
                  val enumOk =
                      otherSelected.zip(facets.filter { it !== facet }).all { (sel, f) ->
                        sel.isEmpty() || f.extract(profile).any { it in sel }
                      }
                  val rangeOk = ranges.all { r -> r.def.extract(profile) in r.range }
                  enumOk && rangeOk
                }

                val filtered = base.filter(include)
                val counts =
                    mutableMapOf<Enum<*>, Int>().apply {
                      facet.values.forEach { this[it] = ZERO_COUNT }
                    }
                filtered.forEach { p ->
                  facet.extract(p).forEach { v -> counts[v] = (counts[v] ?: ZERO_COUNT) + 1 }
                }
                counts.toMap()
              }
              .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

      facet.counts = countsFlow
    }

    // Primary displayed profiles stream: apply facets, ranges, and sort
    viewModelScope.launch {
      val selectionFlows: List<StateFlow<Set<Enum<*>>>> = facets.map { it.selected }
      val rangeFlows: List<StateFlow<IntRange>> = rangeFacets.map { it.currentRange }
      combine(
              searchBase,
              combine(selectionFlows) { it.toList() },
              combine(rangeFlows) { it.toList() },
              sortCriteria) { list, sels, ranges, sort ->
                if (list.isEmpty()) return@combine emptyList<UserProfile>()

                val filtered =
                    list.filter { profile ->
                      val enumsOk =
                          sels.zip(facets).all { (sel, facet) ->
                            sel.isEmpty() || facet.extract(profile).any { it in sel }
                          }
                      val rangesOk =
                          ranges.zip(rangeFacets).all { (range, rf) ->
                            val value = rf.extract(profile)
                            value in range
                          }
                      enumsOk && rangesOk
                    }

                applySort(filtered, sort)
              }
          .distinctUntilChanged()
          .collect { filtered -> _displayedProfiles.value = filtered }
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
    rangeFacets.forEach { it.reset() }
  }

  fun initializeWithProfiles(profiles: List<UserProfile>) {
    _baseProfiles.value = profiles
    _searchQuery.value = "" // Ensure search is cleared
    // For tests: immediately set displayed profiles since the flow might not emit fast enough
    _displayedProfiles.value = applySort(profiles, _sortCriteria.value)

    viewModelScope.launch {
      try {
        if (engine == null) engine = engineFactory()
        engine?.indexProfiles(profiles)
        hasIndex = true
      } catch (_: Throwable) {
        hasIndex = false
        engine = null
      }
    }
  }

  fun setSortCriteria(criteria: LeaderboardSort) {
    _sortCriteria.update { criteria }
  }

  private fun applySort(list: List<UserProfile>, criteria: LeaderboardSort): List<UserProfile> =
      list.sortedWith(criteria.comparator)

  // Tokenization utilities for fallback search
  private fun tokenize(query: String): List<String> =
      REGEX_TOKEN.findAll(query.lowercase())
          .map { it.value }
          .filter { it.length >= MIN_TOKEN_LENGTH }
          .toList()

  private fun minShouldMatch(
      tokenCount: Int,
      ratio: Double = MIN_SHOULD_RATIO,
      floor: Int = MIN_SHOULD_FLOOR
  ): Int =
      if (tokenCount <= ZERO_COUNT) ZERO_COUNT else max(floor, ceil(tokenCount * ratio).toInt())

  override fun onCleared() {
    try {
      engine?.close()
    } catch (_: Exception) {}
  }

  private data class RangeSnapshot(val def: RangeFacet<UserProfile>, val range: IntRange) {
    fun matches(profile: UserProfile): Boolean = def.extract(profile) in range
  }

  // Helper to bind a RangeFacet with an externally provided range
  private fun RangeFacet<UserProfile>.copyRange(range: IntRange): RangeSnapshot =
      RangeSnapshot(this, range)

  private companion object {
    // Regex pattern for tokenizing search queries (words, underscores, hyphens)
    const val SEARCH_DEBOUNCE_MS = 300L
    const val SCORE_PERCENT_SCALE = 100
    const val BASE_INDEX = 0
    const val NEXT_INDEX_OFFSET = 1
    const val ZERO_COUNT = 0
    const val MIN_TOKEN_LENGTH = 2
    const val MIN_SHOULD_FLOOR = 1
    const val MIN_SHOULD_RATIO = 0.6
    val REGEX_TOKEN = Regex("[a-z0-9_-]+")
  }
}
