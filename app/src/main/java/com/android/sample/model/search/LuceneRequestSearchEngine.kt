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

class LuceneRequestSearchEngine(private val maxResults: Int = 100) :
    SearchStrategy<Request>, Closeable {

  private val directory: Directory = ByteBuffersDirectory()
  private val analyzer: Analyzer = StandardAnalyzer()
  private var writer: IndexWriter? = null
  private var reader: DirectoryReader? = null
  private var searcher: IndexSearcher? = null
  private val indexedById = HashMap<String, Request>()

  suspend fun indexRequests(requests: List<Request>) =
      withContext(Dispatchers.IO) {
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
                is List<*> -> values.filterNotNull().joinToString(" ") { it.toString() }
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
                      append(' ')
                      append(text.replace('_', ' '))
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

  override suspend fun search(items: List<Request>, query: String): List<SearchResult<Request>> =
      withContext(Dispatchers.IO) {
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
                FIELD_TITLE to 3.0f,
                FIELD_DESCRIPTION to 1.5f,
                FIELD_LOCATION_NAME to 2.0f,
                FIELD_TYPES to 1.2f,
                FIELD_TAGS to 1.2f,
                FIELD_STATUS to 1.0f,
            )

        val parser = MultiFieldQueryParser(fields, analyzer, boosts)
        parser.defaultOperator = QueryParser.Operator.OR

        val terms = safeQuery.split("\\s+".toRegex()).mapNotNull { t -> t.trim().ifEmpty { null } }

        val luceneQuery =
            if (terms.size <= 1) {
              try {
                parser.parse(safeQuery)
              } catch (_: Exception) {
                parser.parse("*")
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

              if (tokenCount > 0) {
                val msm =
                    kotlin.math.max(
                        1, kotlin.math.ceil(tokenCount * MIN_SHOULD_MATCH_RATIO).toInt())
                val finalBuilder = BooleanQuery.Builder()
                builtQuery.clauses().forEach { finalBuilder.add(it) }
                finalBuilder.setMinimumNumberShouldMatch(msm)
                finalBuilder.build()
              } else {
                try {
                  parser.parse(safeQuery)
                } catch (_: Exception) {
                  parser.parse("*")
                }
              }
            }

        val top: TopDocs = localSearcher.search(luceneQuery, maxResults)
        val hits: Array<ScoreDoc> = top.scoreDocs
        if (hits.isEmpty()) return@withContext emptyList()

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
    const val FIELD_TYPES = "requestType"
    const val FIELD_TAGS = "tags"
    const val FIELD_STATUS = "status"
    const val FIELD_START_TS = "startTimeStamp"
    const val FIELD_EXPIRATION_TS = "expirationTime"
    const val MIN_SHOULD_MATCH_RATIO = 0.6
  }
}
