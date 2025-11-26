import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.request.RequestStatus
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationScreen
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.request_validation.ValidateRequestConstants
import com.android.sample.utils.BaseEmulatorTest
import com.android.sample.utils.FirebaseEmulator
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ValidateRequestNavigationTest : BaseEmulatorTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions
  private lateinit var navController: NavHostController

  private val testRequestId = "test-request-123"

  @Before
  override fun setUp() = runTest {
    super.setUp()
    FirebaseEmulator.signInTestUser()

    // Add test request
    FirebaseEmulator.addTestRequest(requestId = testRequestId, status = RequestStatus.IN_PROGRESS)

    composeTestRule.setContent {
      navController = rememberNavController()
      navigationActions = NavigationActions(navController)
      NavigationScreen(navController = navController, navigationActions = navigationActions)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertExists()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun canNavigateToValidateRequestScreen() {
    // When
    composeTestRule.runOnUiThread {
      navController.navigate(Screen.ValidateRequest(testRequestId).route)
    }
    composeTestRule.waitForIdle()

    // Then
    composeTestRule.onNodeWithTag(NavigationTestTags.VALIDATE_REQUEST_SCREEN).assertIsDisplayed()
  }

  @Test
  fun validateRequestScreen_backButtonNavigatesBack() {
    // Given
    composeTestRule.runOnUiThread {
      navController.navigate(Screen.ValidateRequest(testRequestId).route)
    }
    composeTestRule.waitForIdle()

    // When - Use the specific ValidateRequest back button (first one)
    composeTestRule.onAllNodesWithTag(ValidateRequestConstants.TAG_BACK_BUTTON)[0].performClick()
    composeTestRule.waitForIdle()

    // Then
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun onRequestClosed_navigatesBack() {
    // Given - Navigate to validate screen
    composeTestRule.runOnUiThread {
      navController.navigate(Screen.ValidateRequest(testRequestId).route)
    }
    composeTestRule.waitForIdle()

    // When - Navigate back programmatically (simulating onRequestClosed callback)
    composeTestRule.runOnUiThread { navController.popBackStack() }
    composeTestRule.waitForIdle()

    // Then - Should be back at requests screen
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }
}
