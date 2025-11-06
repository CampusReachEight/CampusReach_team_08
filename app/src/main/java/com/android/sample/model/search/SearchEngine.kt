package com.android.sample.model.search

/**
 * Represents a generic search result.
 *
 * @param T the type of the item returned by the search
 * @property item the result item
 * @property score relevance score normalized to 0..100
 * @property matchedFields which fields matched (if known)
 */
data class SearchResult<T>(
    val item: T,
    val score: Int,
    val matchedFields: List<String> = emptyList()
)

/**
 * Strategy interface for searching among a collection of items. Implementations may use simple
 * in-memory filtering or advanced engines.
 */
interface SearchStrategy<T> {
  /**
   * Executes a search over the provided items.
   *
   * Implementations should be thread-friendly and may switch to IO dispatcher if needed.
   *
   * @param items the items to search within
   * @param query the text query
   * @return a list of [SearchResult] sorted by relevance
   */
  suspend fun search(items: List<T>, query: String): List<SearchResult<T>>
}

/**
 * Configuration for search engines. This is a simple holder to be passed to implementations.
 *
 * @property minQueryLength minimum characters required to start searching
 * @property maxResults maximum number of results to return
 * @property enableFuzzyMatch enable fuzzy matching if supported by the engine
 * @property caseSensitive whether matching is case sensitive
 */
data class SearchConfig(
    val minQueryLength: Int = 2,
    val maxResults: Int = 100,
    val enableFuzzyMatch: Boolean = false,
    val caseSensitive: Boolean = false
)
