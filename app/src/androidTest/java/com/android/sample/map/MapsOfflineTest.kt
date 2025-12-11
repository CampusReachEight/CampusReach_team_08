package com.android.sample.map

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.rule.GrantPermissionRule
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.model.request.RequestRepository
import com.android.sample.ui.map.MapScreen
import com.android.sample.ui.map.MapViewModel
import com.android.sample.ui.request.FakeLocationProvider
import com.android.sample.utils.BaseEmulatorTest
import com.android.sample.utils.UI_WAIT_TIMEOUT
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Test class for MapScreen offline mode functionality. This class has its own setup and doesn't
 * inherit the default MapTest setup to avoid setContent conflicts.
 */
class MapsOfflineTest : BaseEmulatorTest() {
  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  private lateinit var profileRepository: UserProfileRepository
  private lateinit var fakeFusedLocationProvider: FakeLocationProvider

  @Before
  override fun setUp() {
    super.setUp()
    profileRepository = UserProfileRepositoryFirestore(Firebase.firestore)
    fakeFusedLocationProvider = FakeLocationProvider()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun offlineMode_displaysOfflineMessage() {
    // Create a mock repository that throws when getAllCurrentRequests is called
    runTest {
      val mockRepository = mock(RequestRepository::class.java)
      `when`(mockRepository.getAllCurrentRequests()).thenThrow(RuntimeException("No internet"))

      // Create a new ViewModel with the mock repository
      val offlineViewModel =
          MapViewModel(mockRepository, profileRepository, fakeFusedLocationProvider)

      // Set the content with the offline viewModel
      composeTestRule.setContent { MapScreen(offlineViewModel) }

      // Trigger refresh which will call getAllCurrentRequests
      offlineViewModel.refreshUIState(null)

      // Wait until offline mode is set
      composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { offlineViewModel.uiState.value.offlineMode }

      // Verify the offline message is displayed
      composeTestRule.onNodeWithText("Map is unavailable in offline mode").assertIsDisplayed()
    }
  }
}
