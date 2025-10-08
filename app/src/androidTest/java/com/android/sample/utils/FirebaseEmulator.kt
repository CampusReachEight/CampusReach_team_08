package com.android.sample.utils

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * An object to manage the connection to Firebase Emulators for Android tests.
 *
 * This object will automatically use the emulators if they are running when the tests start.
 */
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

  private fun areEmulatorsRunning(): Boolean =
      runCatching {
            val request = Request.Builder().url("http://$HOST:$FIRESTORE_PORT").build()
            httpClient.newCall(request).execute().isSuccessful
          }
          .getOrNull() == true

  val isRunning = areEmulatorsRunning()

  init {
    if (isRunning) {
      auth.useEmulator(HOST, AUTH_PORT)
      firestore.useEmulator(HOST, FIRESTORE_PORT)
      assert(Firebase.firestore.firestoreSettings.host.contains(HOST)) {
        "Failed to connect to Firebase Firestore Emulator."
      }
      Log.d(
          "FirebaseEmulator", "Connected to emulators (Auth:$AUTH_PORT, Firestore:$FIRESTORE_PORT)")
    } else {
      Log.w("FirebaseEmulator", "Emulators are not running. Tests will use production Firebase.")
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
          firestorePort = emulators.getJSONObject("firestore").getInt("port"))
    } catch (e: Exception) {
      Log.w("FirebaseEmulator", "Failed to read firebase.json, using defaults: ${e.message}")
      EmulatorConfig(authPort = 9099, firestorePort = 8080)
    }
  }

  private fun clearEmulator(endpoint: String) {
    val request = Request.Builder().url(endpoint).delete().build()
    val response = httpClient.newCall(request).execute()
    assert(response.isSuccessful) { "Failed to clear emulator at $endpoint" }
  }

  fun clearAuthEmulator() {
    clearEmulator(authEndpoint)
  }

  fun clearFirestoreEmulator() {
    clearEmulator(firestoreEndpoint)
  }

  suspend fun signInTestUser(email: String = "test@example.com", password: String = "test123456") {
    try {
      auth.signInWithEmailAndPassword(email, password).await()
    } catch (e: Exception) {
      // User doesn't exist, create it
      auth.createUserWithEmailAndPassword(email, password).await()
    }
    Log.d("FirebaseEmulator", "Signed in as ${auth.currentUser?.uid}")
  }

  fun signOut() {
    auth.signOut()
  }

  private data class EmulatorConfig(val authPort: Int, val firestorePort: Int)
}
