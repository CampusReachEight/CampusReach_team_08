package com.android.sample.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class EmulatorTestRunner : AndroidJUnitRunner() {

  override fun onStart() {
    // Instrumentation is registered at this point.
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    if (FirebaseApp.getApps(ctx).isEmpty()) {
      FirebaseApp.initializeApp(ctx)
    }

    // Bind default app to local emulators BEFORE any test/activity touches Firebase.
    FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)

    FirebaseFirestore.getInstance().apply {
      useEmulator("10.0.2.2", 8080)
      firestoreSettings =
          FirebaseFirestoreSettings.Builder()
              .setPersistenceEnabled(false) // avoid cache interference across tests
              .build()
    }

    super.onStart()
  }
}
