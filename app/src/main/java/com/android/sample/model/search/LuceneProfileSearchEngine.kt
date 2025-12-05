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
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.queryparser.classic.QueryParserBase
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.ScoreDoc
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

            // Text fields for searching
            fun addText(name: String, value: String?) {
              val v = value?.trim().orEmpty()
              if (v.isNotEmpty()) doc.add(TextField(name, v, Field.Store.NO))
            }

            addText(FIELD_NAME, profile.name)
            addText(FIELD_LAST_NAME, profile.lastName)
            addText(FIELD_NAME_LOWERCASE, profile.nameLowercase)
            addText(FIELD_LAST_NAME_LOWERCASE, profile.lastNameLowercase)

            // Section with display variants (e.g., "COMPUTER_SCIENCE" and "Computer Science")
            val sectionName = profile.section.name
            val sectionLabel = profile.section.label
            val sectionText =
                buildString {
                      append(sectionName)
                      append(SPACE_CHAR)
                      append(sectionName.replace(UNDERSCORE_CHAR, SPACE_CHAR))
                      append(SPACE_CHAR)
                      append(sectionLabel)
                    }
                    .trim()
            addText(FIELD_SECTION, sectionText)

            // Full name combined field for better phrase matching
            val fullName = "${profile.name} ${profile.lastName}".trim()
            addText(FIELD_FULL_NAME, fullName)

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
   * @param items Not used in this implementation - search is performed on indexed data from
   *   [indexProfiles]. This parameter exists to satisfy the [SearchStrategy] interface.
   * @param query The search query string (typically a name or part of a name)
   * @return A list of [SearchResult] containing matching profiles and their relevance scores
   */
  override suspend fun search(
      items: List<UserProfile>,
      query: String
  ): List<SearchResult<UserProfile>> =
      withContext(dispatcher) {
        if (query.isBlank()) return@withContext emptyList()
        val localSearcher = searcher ?: return@withContext emptyList()

        val safeQuery = QueryParserBase.escape(query)

        val fields =
            arrayOf(
                FIELD_NAME,
                FIELD_LAST_NAME,
                FIELD_NAME_LOWERCASE,
                FIELD_LAST_NAME_LOWERCASE,
                FIELD_FULL_NAME,
                FIELD_SECTION,
            )
        val boosts =
            mapOf(
                FIELD_NAME to BOOST_NAME,
                FIELD_LAST_NAME to BOOST_LAST_NAME,
                FIELD_NAME_LOWERCASE to BOOST_NAME_LOWERCASE,
                FIELD_LAST_NAME_LOWERCASE to BOOST_LAST_NAME_LOWERCASE,
                FIELD_FULL_NAME to BOOST_FULL_NAME,
                FIELD_SECTION to BOOST_SECTION,
            )

        val parser = MultiFieldQueryParser(fields, analyzer, boosts)
        parser.defaultOperator = QueryParser.Operator.OR

        val terms = safeQuery.split(WHITESPACE_REGEX).mapNotNull { t -> t.trim().ifEmpty { null } }

        val luceneQuery =
            if (terms.size <= SINGLE_TERM_SIZE_THRESHOLD) {
              try {
                parser.parse(safeQuery)
              } catch (_: Exception) {
                parser.parse(QUERY_MATCH_ALL)
              }
            } else {
              val builder = BooleanQuery.Builder()
              terms.forEach { term ->
                try {
                  val sub = parser.parse(term)
                  builder.add(sub, BooleanClause.Occur.SHOULD)
                } catch (_: Exception) {
                  // Skip unparseable terms
                }
              }

              val builtQuery = builder.build()
              val tokenCount = builtQuery.clauses().size

              if (tokenCount > MIN_TOKEN_COUNT) {
                val msm =
                    kotlin.math.max(
                        MIN_SHOULD_MATCH_BASE,
                        kotlin.math.ceil(tokenCount * MIN_SHOULD_MATCH_RATIO).toInt())
                val finalBuilder = BooleanQuery.Builder()
                builtQuery.clauses().forEach { finalBuilder.add(it) }
                finalBuilder.setMinimumNumberShouldMatch(msm)
                finalBuilder.build()
              } else {
                try {
                  parser.parse(safeQuery)
                } catch (_: Exception) {
                  parser.parse(QUERY_MATCH_ALL)
                }
              }
            }

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

  override fun close() {
    try {
      reader?.close()
    } catch (_: IOException) {}
    try {
      writer?.close()
    } catch (_: IOException) {}
    try {
      directory.close()
    } catch (_: IOException) {}
  }

  private companion object {
    // Field names for indexing
    const val FIELD_PROFILE_ID = "profileId"
    const val FIELD_NAME = "name"
    const val FIELD_LAST_NAME = "lastName"
    const val FIELD_NAME_LOWERCASE = "nameLowercase"
    const val FIELD_LAST_NAME_LOWERCASE = "lastNameLowercase"
    const val FIELD_FULL_NAME = "fullName"
    const val FIELD_SECTION = "section"
    const val FIELD_KUDOS = "kudos"
    const val FIELD_HELP_RECEIVED = "helpReceived"

    // Boost factors - prioritize exact name matches
    const val BOOST_NAME = 3.0f
    const val BOOST_LAST_NAME = 3.0f
    const val BOOST_NAME_LOWERCASE = 2.5f
    const val BOOST_LAST_NAME_LOWERCASE = 2.5f
    const val BOOST_FULL_NAME = 2.0f
    const val BOOST_SECTION = 1.0f

    // Minimum should match ratio for multi-term queries
    const val MIN_SHOULD_MATCH_RATIO = 0.6

    // Thresholds & scaling constants
    const val SINGLE_TERM_SIZE_THRESHOLD = 1
    const val MIN_TOKEN_COUNT = 0
    const val MIN_SHOULD_MATCH_BASE = 1
    const val MIN_TOP_SCORE = 0f
    const val FALLBACK_TOP_SCORE = 1f
    const val SCORE_PERCENT_SCALE = 100f
    const val SCORE_PERCENT_MIN = 0

    // Query / parsing related constants
    val WHITESPACE_REGEX = "\\s+".toRegex()
    const val QUERY_MATCH_ALL = "*"

    // String building helpers
    const val SPACE_CHAR = ' '
    const val UNDERSCORE_CHAR = '_'
  }
}
