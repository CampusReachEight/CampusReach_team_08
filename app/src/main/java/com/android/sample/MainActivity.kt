package com.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.navigation.compose.rememberNavController
import com.android.sample.resources.C
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationScreen
import com.android.sample.ui.theme.SampleAppTheme
import com.android.sample.ui.theme.SynchronizedShimmerProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val settings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
    Firebase.firestore.firestoreSettings = settings

    // Sign out any existing user to force demo login screen
    FirebaseAuth.getInstance().signOut()

    setContent {
      SampleAppTheme {
        // provide a synchronized shimmer effect across the app with: shimmerBrush()
        SynchronizedShimmerProvider {
          Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppNavigation()
          }
        }
      }
    }
  }
}

@Composable
fun AppNavigation(
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current)
) {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController = navController)
  NavigationScreen(
      navigationActions = navigationActions,
      navController = navController,
      modifier = Modifier.fillMaxSize(),
      credentialManager = credentialManager)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier.semantics { testTag = C.Tag.greeting })
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  SampleAppTheme { Greeting("Android") }
}
