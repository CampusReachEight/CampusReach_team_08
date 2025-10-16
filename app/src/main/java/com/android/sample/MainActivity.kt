package com.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.android.sample.resources.C
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationScreen
import com.android.sample.ui.theme.SampleAppTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099) // Use Firebase Auth emulator

    setContent {
      SampleAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          AppNavigation()
        }
      }
    }
  }
}

@Composable
fun AppNavigation() {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController = navController)
  NavigationScreen(
      navigationActions = navigationActions,
      navController = navController,
      modifier = Modifier.fillMaxSize())
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
