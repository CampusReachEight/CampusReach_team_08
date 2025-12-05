package com.android.sample.model.search

import com.android.sample.model.profile.UserProfile
import java.io.Closeable
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.IntPoint
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParserBase
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.PrefixQuery
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.lucene.store.Directory

/**
 * A search engine for UserProfiles using Apache Lucene.
 *
 * This engine provides full-text search across user profile fields (name, lastName, section) with
 * boosted relevance scoring. It mirrors the architecture of [LuceneRequestSearchEngine] for
 * consistency across the codebase.
 *
 * Why use local Lucene instead of server-side `searchUserProfiles()`?
 * - Server-side Firestore prefix queries cannot be combined with client-side range/facet filters
 *   without over-fetching or missing results (the "limit mismatch" problem)
 * - Local Lucene provides instant search latency and full offline support
 * - Consistent filtering: search + section filter + kudos range all applied in one pass
 *
 * Usage:
 * 1. Call [indexProfiles] with all profiles to build the search index
 * 2. Call [search] with a query string to get matching results
 * 3. Call [close] when done to release resources
 *
 * @param maxResults Maximum number of search results to return
 */
class LuceneProfileSearchEngine(private val maxResults: Int = DEFAULT_MAX_SEARCH_RESULTS) :
    SearchStrategy<UserProfile>, Closeable {

  private val dispatcher = Dispatchers.IO
  private val directory: Directory = ByteBuffersDirectory()
  private val analyzer: Analyzer = StandardAnalyzer()
  private var writer: IndexWriter? = null
  private var reader: DirectoryReader? = null
  private var searcher: IndexSearcher? = null

  private val indexedById = HashMap<String, UserProfile>()

  /**
   * Indexes the provided list of user profiles, replacing any previously indexed data.
   *
   * @param profiles The list of UserProfile objects to index
   */
  suspend fun indexProfiles(profiles: List<UserProfile>) =
      withContext(dispatcher) {
        try {
          writer?.close()
          val cfg = IndexWriterConfig(analyzer)
          writer = IndexWriter(directory, cfg)
          indexedById.clear()

          profiles.forEach { profile ->
            val doc = Document()

            // Stored fields for retrieval
            doc.add(StoredField(FIELD_PROFILE_ID, profile.id))

            // Primary search field: combined first + last name from toSearchText()
            // This is the main field we query against for name searches
            val searchText = profile.toSearchText()
            if (searchText.isNotBlank()) {
              doc.add(TextField(FIELD_SEARCH_TEXT, searchText, Field.Store.NO))
              // Also store lowercased version for prefix matching
              doc.add(TextField(FIELD_SEARCH_TEXT_LOWER, searchText.lowercase(), Field.Store.NO))
            }

            // Individual name fields for exact/boosted matching
            fun addText(fieldName: String, value: String?) {
              val v = value?.trim().orEmpty()
              if (v.isNotEmpty()) doc.add(TextField(fieldName, v, Field.Store.NO))
            }

            addText(FIELD_NAME, profile.name)
            addText(FIELD_LAST_NAME, profile.lastName)
            addText(FIELD_NAME_LOWER, profile.nameLowercase)
            addText(FIELD_LAST_NAME_LOWER, profile.lastNameLowercase)

            // Numeric fields for potential range queries (stored for reference)
            doc.add(IntPoint(FIELD_KUDOS, profile.kudos))
            doc.add(StoredField(FIELD_KUDOS, profile.kudos))
            doc.add(IntPoint(FIELD_HELP_RECEIVED, profile.helpReceived))
            doc.add(StoredField(FIELD_HELP_RECEIVED, profile.helpReceived))

            writer?.addDocument(doc)
            indexedById[profile.id] = profile
          }

          writer?.commit()
          reader?.close()
          reader = DirectoryReader.open(writer)
          searcher = IndexSearcher(reader)
        } catch (e: Exception) {
          try {
            writer?.rollback()
          } catch (_: Exception) {}
          throw e
        }
      }

  /**
   * Searches the indexed profiles for the given query string.
   *
   * This implementation supports partial/prefix matching for user names. For example, typing "Jo"
   * will match "John", "Johnny", "Johnson", etc. This is essential for a good UX when searching for
   * people by name.
   *
   * Query behavior:
   * - Single short term (< 3 chars): Prefix match only (e.g., "Jo" -> "John*")
   * - Single longer term: Prefix + exact match combined
   * - Multiple terms: Each term is prefix-matched, all should match ("John Sm" -> John* AND Sm*)
   *
   * @param items Not used - search uses indexed data from [indexProfiles]
   * @param query The search query (typically a name or partial name)
   * @return Matching profiles with relevance scores (0-100)
   */
  override suspend fun search(
      items: List<UserProfile>,
      query: String
  ): List<SearchResult<UserProfile>> =
      withContext(dispatcher) {
        if (query.isBlank()) return@withContext emptyList()
        val localSearcher = searcher ?: return@withContext emptyList()

        val normalizedQuery = query.trim().lowercase()
        val terms =
            normalizedQuery.split(WHITESPACE_REGEX).mapNotNull { t ->
              val cleaned = t.trim()
              if (cleaned.isEmpty()) null else QueryParserBase.escape(cleaned)
            }

        if (terms.isEmpty()) return@withContext emptyList()

        val luceneQuery = buildNameQuery(terms)

        val top: TopDocs = localSearcher.search(luceneQuery, maxResults)
        val hits: Array<ScoreDoc> = top.scoreDocs
        if (hits.isEmpty()) return@withContext emptyList()

        val topScore = hits.maxOf { it.score }.takeIf { it > MIN_TOP_SCORE } ?: FALLBACK_TOP_SCORE
        hits.mapNotNull { sd ->
          val doc = localSearcher.storedFields().document(sd.doc)
          val id = doc.get(FIELD_PROFILE_ID) ?: return@mapNotNull null
          val item = indexedById[id] ?: return@mapNotNull null
          val normalizedScore =
              ((sd.score / topScore) * SCORE_PERCENT_SCALE)
                  .toInt()
                  .coerceIn(SCORE_PERCENT_MIN, SCORE_PERCENT_SCALE.toInt())
          SearchResult(item = item, score = normalizedScore, matchedFields = emptyList())
        }
      }

  /**
   * Builds a Lucene query optimized for name searching with prefix support.
   *
   * For a single term like "jo":
   * - Creates prefix queries on name fields (jo*) to match John, Johnny, Johnson, etc.
   *
   * For multiple terms like "john sm":
   * - Each term becomes a prefix query
   * - All terms must match (AND logic) to ensure "john sm" matches "John Smith" but not just "John"
   */
  private fun buildNameQuery(terms: List<String>): BooleanQuery {
    val mainBuilder = BooleanQuery.Builder()

    terms.forEach { term ->
      // For each term, create a sub-query that matches any name field
      val termBuilder = BooleanQuery.Builder()

      // Prefix queries for partial matching (e.g., "jo" matches "john")
      termBuilder.add(PrefixQuery(Term(FIELD_NAME_LOWER, term)), BooleanClause.Occur.SHOULD)
      termBuilder.add(PrefixQuery(Term(FIELD_LAST_NAME_LOWER, term)), BooleanClause.Occur.SHOULD)
      termBuilder.add(PrefixQuery(Term(FIELD_SEARCH_TEXT_LOWER, term)), BooleanClause.Occur.SHOULD)

      // Also add exact term queries for full word matches (higher relevance)
      if (term.length >= MIN_EXACT_MATCH_LENGTH) {
        termBuilder.add(TermQuery(Term(FIELD_NAME_LOWER, term)), BooleanClause.Occur.SHOULD)
        termBuilder.add(TermQuery(Term(FIELD_LAST_NAME_LOWER, term)), BooleanClause.Occur.SHOULD)
      }

      val termQuery = termBuilder.build()
      // Each term MUST match at least one field (AND between terms)
      mainBuilder.add(termQuery, BooleanClause.Occur.MUST)
    }

    return mainBuilder.build()
  }

  override fun close() {
    // Null out searcher first so concurrent searches will return empty
    searcher = null
    try {
      reader?.close()
    } catch (_: IOException) {}
    reader = null
    try {
      writer?.close()
    } catch (_: IOException) {}
    writer = null
    try {
      directory.close()
    } catch (_: IOException) {}
    indexedById.clear()
  }

  private companion object {
    // Field names for indexing
    const val FIELD_PROFILE_ID = "profileId"
    const val FIELD_NAME = "name"
    const val FIELD_LAST_NAME = "lastName"
    const val FIELD_NAME_LOWER = "nameLower"
    const val FIELD_LAST_NAME_LOWER = "lastNameLower"
    const val FIELD_SEARCH_TEXT = "searchText"
    const val FIELD_SEARCH_TEXT_LOWER = "searchTextLower"
    const val FIELD_KUDOS = "kudos"
    const val FIELD_HELP_RECEIVED = "helpReceived"

    // Thresholds & scaling constants
    const val MIN_EXACT_MATCH_LENGTH = 3 // Minimum term length for exact match queries
    const val MIN_TOP_SCORE = 0f
    const val FALLBACK_TOP_SCORE = 1f
    const val SCORE_PERCENT_SCALE = 100f
    const val SCORE_PERCENT_MIN = 0

    // Query parsing
    val WHITESPACE_REGEX = "\\s+".toRegex()
  }
}
