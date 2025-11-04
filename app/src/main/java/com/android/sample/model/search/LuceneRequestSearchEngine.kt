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
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.lucene.store.Directory

/**
 * Lucene-based in-memory search engine for Request objects.
 * - Uses ByteBuffersDirectory (no disk IO)
 * - Uses StandardAnalyzer for tokenization/normalization
 * - Indexes: title, description, locationName, requestTypes, tags, status (+ timestamps as
 *   LongPoint)
 * - Stores: requestId, creatorId, timestamps (stored for retrieval)
 */
class LuceneRequestSearchEngine(private val maxResults: Int = 100) :
    SearchStrategy<Request>, Closeable {

  // Lucene components
  private val directory: Directory = ByteBuffersDirectory()
  private val analyzer: Analyzer = StandardAnalyzer()
  private var writer: IndexWriter? = null
  private var reader: DirectoryReader? = null
  private var searcher: IndexSearcher? = null

  // Keep a copy to reconstruct full Request on hits
  private val indexedById = HashMap<String, Request>()

  /** Index or re-index the provided requests. Clears previous index content. */
  suspend fun indexRequests(requests: List<Request>) =
      withContext(Dispatchers.IO) {
        try {
          // Reset writer each time to ensure a clean re-index
          writer?.close()
          val cfg = IndexWriterConfig(analyzer)
          writer = IndexWriter(directory, cfg)

          // Clear previous in-memory map
          indexedById.clear()

          // Add documents
          requests.forEach { req ->
            val doc = Document()

            val map = req.toMap()
            // Store ids for retrieval
            val requestId = (map[FIELD_REQUEST_ID] as? String) ?: req.requestId
            val creatorId = (map[FIELD_CREATOR_ID] as? String) ?: req.creatorId
            doc.add(StoredField(FIELD_REQUEST_ID, requestId))
            doc.add(StoredField(FIELD_CREATOR_ID, creatorId))

            // Helper to add a TextField if present and non-blank
            fun addText(name: String, value: String?) {
              val v = value?.trim().orEmpty()
              if (v.isNotEmpty()) doc.add(TextField(name, v, Field.Store.NO))
            }
            fun joinList(values: Any?): String {
              return when (values) {
                is List<*> -> values.filterNotNull().joinToString(" ") { it.toString() }
                else -> values?.toString() ?: ""
              }
            }

            // Index primary searchable text fields from the map
            addText(FIELD_TITLE, map[FIELD_TITLE] as? String)
            addText(FIELD_DESCRIPTION, map[FIELD_DESCRIPTION] as? String)
            addText(FIELD_LOCATION_NAME, map[FIELD_LOCATION_NAME] as? String)

            // Index types/tags/status as text for search
            // We also include a human-friendly variant by replacing '_' with space to match display
            // strings
            val types = joinList(map[FIELD_TYPES])
            val tags = joinList(map[FIELD_TAGS])
            val status = (map[FIELD_STATUS] as? String) ?: ""
            fun withDisplayVariants(text: String): String =
                if (text.isBlank()) text
                else
                    buildString {
                      append(text)
                      append(' ')
                      append(text.replace('_', ' '))
                    }
            addText(FIELD_TYPES, withDisplayVariants(types))
            addText(FIELD_TAGS, withDisplayVariants(tags))
            addText(FIELD_STATUS, withDisplayVariants(status))

            // Numeric timestamps for potential date range queries
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
            // Also store them for optional retrieval
            doc.add(StoredField(FIELD_START_TS, startTsMs))
            doc.add(StoredField(FIELD_EXPIRATION_TS, endTsMs))

            writer!!.addDocument(doc)
            indexedById[requestId] = req
          }

          writer!!.commit()

          // Refresh searcher
          reader?.close()
          reader = DirectoryReader.open(writer)
          searcher = IndexSearcher(reader)
        } catch (e: Exception) {
          // If anything goes wrong, ensure index is not half-initialized
          try {
            writer?.rollback()
          } catch (_: Exception) {}
          throw e
        }
      }

  override suspend fun search(items: List<Request>, query: String): List<SearchResult<Request>> =
      withContext(Dispatchers.IO) {
        // Guard: blank query or not ready
        if (query.isBlank()) return@withContext emptyList()
        val localSearcher = searcher ?: return@withContext emptyList()

        // Escape special characters to avoid parse errors
        val safeQuery =
            try {
              QueryParserBase.escape(query)
            } catch (_: Exception) {
              query
            }

        // Multi-field parsing with boosts according to requirements
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
                FIELD_TITLE to 3.0f,
                FIELD_DESCRIPTION to 1.5f,
                FIELD_LOCATION_NAME to 2.0f,
                FIELD_TYPES to 1.2f,
                FIELD_TAGS to 1.2f,
                FIELD_STATUS to 1.0f,
            )

        val parser = MultiFieldQueryParser(fields, analyzer, boosts)
        parser.defaultOperator = QueryParser.Operator.AND

        val luceneQuery =
            try {
              parser.parse(safeQuery)
            } catch (_: Exception) {
              // If parsing still fails for some reason, try a more permissive fallback:
              // term-escaped query
              parser.parse(QueryParserBase.escape(safeQuery))
            }

        val top: TopDocs = localSearcher.search(luceneQuery, maxResults)
        val hits: Array<ScoreDoc> = top.scoreDocs
        if (hits.isEmpty()) return@withContext emptyList()

        // Normalize scores 0..100 based on top score
        val topScore = hits.maxOf { it.score }.takeIf { it > 0f } ?: 1f
        hits.mapNotNull { sd ->
          val doc = localSearcher.storedFields().document(sd.doc)
          val id = doc.get(FIELD_REQUEST_ID) ?: return@mapNotNull null
          val item = indexedById[id] ?: return@mapNotNull null
          val normalizedScore = ((sd.score / topScore) * 100f).toInt().coerceIn(0, 100)
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
    const val FIELD_TYPES = "requestType" // align with toMap keys
    const val FIELD_TAGS = "tags"
    const val FIELD_STATUS = "status"
    const val FIELD_START_TS = "startTimeStamp"
    const val FIELD_EXPIRATION_TS = "expirationTime"
  }
}
