package com.android.sample.utils

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.sample.model.request.RequestStatus
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** Helper for tests. No emulator wiring here; the runner does that. */
object FirebaseEmulator {
  val auth
    get() = Firebase.auth

  val firestore
    get() = Firebase.firestore

  const val HOST = "10.0.2.2"

  private val config: EmulatorConfig by lazy { readFirebaseConfig() }
  private val FIRESTORE_PORT
    get() = config.firestorePort

  private val AUTH_PORT
    get() = config.authPort

  val projectID by lazy { FirebaseApp.getInstance().options.projectId }
  private val httpClient = OkHttpClient()

  private val firestoreEndpoint by lazy {
    "http://$HOST:$FIRESTORE_PORT/emulator/v1/projects/$projectID/databases/(default)/documents"
  }
  private val authEndpoint by lazy {
    "http://$HOST:$AUTH_PORT/emulator/v1/projects/$projectID/accounts"
  }

  /** Check if Firebase emulators are running by attempting to connect to the auth emulator. */
  val isRunning: Boolean by lazy {
    try {
      val request = Request.Builder().url("http://$HOST:$AUTH_PORT/").head().build()
      val response = httpClient.newCall(request).execute()
      response.isSuccessful || response.code in 400..499
    } catch (e: Exception) {
      Log.w("FirebaseEmulator", "Emulators not running: ${e.message}")
      false
    }
  }

  private fun readFirebaseConfig(): EmulatorConfig {
    return try {
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      val jsonString = context.assets.open("firebase.json").bufferedReader().use { it.readText() }
      val json = JSONObject(jsonString)
      val emulators = json.getJSONObject("emulators")
      EmulatorConfig(
          authPort = emulators.getJSONObject("auth").getInt("port"),
          firestorePort = emulators.getJSONObject("firestore").getInt("port"),
      )
    } catch (e: Exception) {
      Log.w("FirebaseEmulator", "Failed to read firebase.json, using defaults: ${e.message}")
      EmulatorConfig(authPort = 9099, firestorePort = 8080)
    }
  }

  private fun clearEmulator(endpoint: String) {
    val request = Request.Builder().url(endpoint).delete().build()
    val response = httpClient.newCall(request).execute()
    check(response.isSuccessful) { "Failed to clear emulator at $endpoint" }
  }

  fun clearAuthEmulator() = clearEmulator(authEndpoint)

  fun clearFirestoreEmulator() = clearEmulator(firestoreEndpoint)

  suspend fun signInTestUser(email: String = "test@example.com", password: String = "test123456") {
    try {
      auth.signInWithEmailAndPassword(email, password).await()
    } catch (_: Exception) {
      auth.createUserWithEmailAndPassword(email, password).await()
    }
    Log.d("FirebaseEmulator", "Signed in as ${auth.currentUser?.uid}")
  }

  /** Add a test request to Firestore for testing purposes. */
  suspend fun addTestRequest(
      requestId: String,
      creatorId: String = auth.currentUser?.uid ?: "test-creator",
      status: RequestStatus = RequestStatus.IN_PROGRESS,
      title: String = "Test Request",
      description: String = "Test Description"
  ) {
    val request =
        hashMapOf(
            "requestId" to requestId,
            "title" to title,
            "description" to description,
            "status" to status.name,
            "creatorId" to creatorId,
            "requestType" to emptyList<String>(),
            "people" to emptyList<String>(),
            "tags" to emptyList<String>(),
            "locationName" to "Test Location",
            "startTimeStamp" to System.currentTimeMillis(),
            "expirationTime" to System.currentTimeMillis() + 86400000 // +1 day
            )

    firestore.collection("requests").document(requestId).set(request).await()

    Log.d("FirebaseEmulator", "Added test request: $requestId")
  }

  fun signOut() = auth.signOut()

  private data class EmulatorConfig(val authPort: Int, val firestorePort: Int)
}
