package com.android.sample.model.search

import com.android.sample.model.request.Request
import com.google.firebase.Timestamp
import java.io.Closeable
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
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

const val DEFAULT_MAX_SEARCH_RESULTS = 100

/**
 * A search engine for requests using Apache Lucene. Pros:
 * - Powerful full-text search capabilities
 * - Flexible querying with support for multiple fields and boosting
 * - Fast search performance on indexed data Cons:
 * - /!\ Requires indexing step before searching
 * - Additional dependency on Lucene library
 * - Increased app size due to Lucene binaries
 */
class LuceneRequestSearchEngine(private val maxResults: Int = DEFAULT_MAX_SEARCH_RESULTS) :
    SearchStrategy<Request>, Closeable {

  private val dispatcher = Dispatchers.IO
  private val directory: Directory = ByteBuffersDirectory()
  private val analyzer: Analyzer = StandardAnalyzer()
  private var writer: IndexWriter? = null
  private var reader: DirectoryReader? = null
  private var searcher: IndexSearcher? = null

  private val indexedById = HashMap<String, Request>()

  /** Indexes the provided list of requests, replacing any previously indexed data. */
  suspend fun indexRequests(requests: List<Request>) =
      withContext(dispatcher) {
        try {
          writer?.close()
          val cfg = IndexWriterConfig(analyzer)
          writer = IndexWriter(directory, cfg)
          indexedById.clear()

          requests.forEach { req ->
            val doc = Document()
            val map = req.toMap()
            val requestId = (map[FIELD_REQUEST_ID] as? String) ?: req.requestId
            val creatorId = (map[FIELD_CREATOR_ID] as? String) ?: req.creatorId
            doc.add(StoredField(FIELD_REQUEST_ID, requestId))
            doc.add(StoredField(FIELD_CREATOR_ID, creatorId))

            fun addText(name: String, value: String?) {
              val v = value?.trim().orEmpty()
              if (v.isNotEmpty()) doc.add(TextField(name, v, Field.Store.NO))
            }

            fun joinList(values: Any?): String {
              return when (values) {
                is List<*> ->
                    values.filterNotNull().joinToString(TEXT_JOIN_SEPARATOR) { it.toString() }
                else -> values?.toString() ?: ""
              }
            }

            addText(FIELD_TITLE, map[FIELD_TITLE] as? String)
            addText(FIELD_DESCRIPTION, map[FIELD_DESCRIPTION] as? String)
            addText(FIELD_LOCATION_NAME, map[FIELD_LOCATION_NAME] as? String)

            val types = joinList(map[FIELD_TYPES])
            val tags = joinList(map[FIELD_TAGS])
            val status = (map[FIELD_STATUS] as? String) ?: ""

            fun withDisplayVariants(text: String): String =
                if (text.isBlank()) text
                else
                    buildString {
                      append(text)
                      append(SPACE_CHAR)
                      append(text.replace(UNDERSCORE_CHAR, SPACE_CHAR))
                    }

            addText(FIELD_TYPES, withDisplayVariants(types))
            addText(FIELD_TAGS, withDisplayVariants(tags))
            addText(FIELD_STATUS, withDisplayVariants(status))

            val startTsMs =
                when (val ts = map[FIELD_START_TS]) {
                  is Timestamp -> ts.toDate().time
                  is Number -> ts.toLong()
                  else -> req.startTimeStamp.time
                }
            val endTsMs =
                when (val ts = map[FIELD_EXPIRATION_TS]) {
                  is Timestamp -> ts.toDate().time
                  is Number -> ts.toLong()
                  else -> req.expirationTime.time
                }
            doc.add(LongPoint(FIELD_START_TS, startTsMs))
            doc.add(LongPoint(FIELD_EXPIRATION_TS, endTsMs))
            doc.add(StoredField(FIELD_START_TS, startTsMs))
            doc.add(StoredField(FIELD_EXPIRATION_TS, endTsMs))

            writer?.addDocument(doc)
            indexedById[requestId] = req
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
   * Searches the indexed requests for the given query string and returns a list of matching results
   * with their scores.
   *
   * @param items Not used in this implementation - the search is performed on the data indexed by
   *   [indexRequests], assumed to be called by the view model.
   * @param query The search query string.
   * @return A list of [SearchResult] containing matching requests and their scores.
   */
  override suspend fun search(items: List<Request>, query: String): List<SearchResult<Request>> =
      withContext(dispatcher) {
        if (query.isBlank()) return@withContext emptyList()
        val localSearcher = searcher ?: return@withContext emptyList()

        val safeQuery = QueryParserBase.escape(query)

        val fields =
            arrayOf(
                FIELD_TITLE,
                FIELD_DESCRIPTION,
                FIELD_LOCATION_NAME,
                FIELD_TYPES,
                FIELD_TAGS,
                FIELD_STATUS,
            )
        val boosts =
            mapOf(
                FIELD_TITLE to BOOST_TITLE,
                FIELD_DESCRIPTION to BOOST_DESCRIPTION,
                FIELD_LOCATION_NAME to BOOST_LOCATION_NAME,
                FIELD_TYPES to BOOST_TYPES,
                FIELD_TAGS to BOOST_TAGS,
                FIELD_STATUS to BOOST_STATUS,
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
          val id = doc.get(FIELD_REQUEST_ID) ?: return@mapNotNull null
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
    const val FIELD_REQUEST_ID = "requestId"
    const val FIELD_CREATOR_ID = "creatorId"
    const val FIELD_TITLE = "title"
    const val FIELD_DESCRIPTION = "description"
    const val FIELD_LOCATION_NAME = "locationName"
    const val FIELD_TYPES = "requestType"
    const val FIELD_TAGS = "tags"
    const val FIELD_STATUS = "status"
    const val FIELD_START_TS = "startTimeStamp"
    const val FIELD_EXPIRATION_TS = "expirationTime"
    const val MIN_SHOULD_MATCH_RATIO = 0.6

    // Boost factors for individual fields
    const val BOOST_TITLE = 3.0f
    const val BOOST_DESCRIPTION = 1.5f
    const val BOOST_LOCATION_NAME = 2.0f
    const val BOOST_TYPES = 1.2f
    const val BOOST_TAGS = 1.2f
    const val BOOST_STATUS = 1.0f

    // Thresholds & scaling constants
    const val SINGLE_TERM_SIZE_THRESHOLD = 1
    const val MIN_TOKEN_COUNT = 0
    const val MIN_SHOULD_MATCH_BASE = 1
    const val MIN_TOP_SCORE = 0f
    const val FALLBACK_TOP_SCORE = 1f
    const val SCORE_PERCENT_SCALE = 100f
    const val SCORE_PERCENT_MIN = 0

    // Query / parsing related constants
    const val WHITESPACE_REGEX = "\\s+"
    const val QUERY_MATCH_ALL = "*"

    // String building helpers
    const val SPACE_CHAR = ' '
    const val UNDERSCORE_CHAR = '_'
    const val TEXT_JOIN_SEPARATOR = " "
  }
}
