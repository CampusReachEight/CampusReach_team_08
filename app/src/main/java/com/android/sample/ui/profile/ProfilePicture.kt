package com.android.sample.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.request.ConstantRequestList
import com.android.sample.ui.theme.AppColors.SecondaryDark
import com.android.sample.ui.theme.AppColors.WhiteColor
import com.android.sample.ui.theme.appPalette
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProfilePictureTestTags {
  const val PROFILE_PICTURE = "profile_picture"
  const val PROFILE_PICTURE_LOADING = "profile_picture_loading"
  const val PROFILE_PICTURE_DEFAULT = "profile_picture_default"
  const val PROFILE_PICTURE_NAME = "profile_picture_name"
}

object ProfileIconCache {
  private val cache = mutableMapOf<String, Bitmap?>()

  fun get(uri: String): Bitmap? = cache[uri]

  fun put(uri: String, bitmap: Bitmap?) {
    cache[uri] = bitmap
  }

  fun clear() {
    cache.clear()
  }
}

object ProfileCache {
  private val cache = mutableMapOf<String, UserProfile?>()

  fun get(profileId: String): UserProfile? = cache[profileId]

  fun put(profileId: String, profile: UserProfile?) {
    cache[profileId] = profile
  }

  fun clear() {
    cache.clear()
  }
}

const val PictureNameRatio = 0.8f

private const val PADDING = 1f

@Composable
fun ProfilePicture(
    profileRepository: UserProfileRepository = UserProfileRepositoryFirestore(Firebase.firestore),
    profileId: String,
    onClick: (String) -> Unit = {},
    navigationActions: NavigationActions? = null,
    modifier: Modifier = Modifier,
    withName: Boolean = false
) {
  var loading: Boolean by remember { mutableStateOf(true) }
  var bitmap: Bitmap? by remember { mutableStateOf(null) }
  var name: String by remember { mutableStateOf("") }

  LaunchedEffect(profileId) {
    loading = true

    val cachedProfile = ProfileCache.get(profileId)
    val profile: UserProfile? =
        cachedProfile
            ?: try {
              val p = profileRepository.getUserProfile(profileId)
              ProfileCache.put(profileId, p)
              p
            } catch (e: Exception) {
              // couldn't fetch profile
              ProfileCache.put(profileId, null)
              null
            }

    if (profile == null) {
      bitmap = null
      loading = false
      return@LaunchedEffect
    }

    val photoUriObj: Uri? = profile.photo
    name = profile.name

    if (photoUriObj != null && photoUriObj.toString().isNotBlank()) {
      val uri = photoUriObj
      val key = photoUriObj.toString()

      // Check cache first (keyed by uri string)
      val cachedBitmap =
          try {
            ProfileIconCache.get(key)
          } catch (_: Exception) {
            null
          }
      if (cachedBitmap != null) {
        bitmap = cachedBitmap
      } else {
        val loadedBitmap =
            try {
              loadBitmapFromUri(uri)
            } catch (_: Exception) {
              null
            }
        ProfileIconCache.put(key, loadedBitmap)
        bitmap = loadedBitmap
      }
    } else {
      bitmap = null
    }

    loading = false
  }

  Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = modifier.fillMaxSize()) {
        val sizeMod = Modifier.weight(PictureNameRatio, fill = true)

        Surface(
            modifier =
                sizeMod.aspectRatio(PADDING).clip(CircleShape).clickable() {
                  if (navigationActions != null) {
                    val currentUserId = profileRepository.getCurrentUserId()
                    if (profileId == currentUserId) {
                      navigationActions.navigateTo(Screen.Profile(profileId))
                    } else {
                      navigationActions.navigateTo(Screen.PublicProfile(profileId))
                    }
                  } else {
                    onClick(profileId)
                  }
                },
            shape = CircleShape,
        ) {
          if (loading) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(SecondaryDark.copy(alpha = 0.3f))
                        .testTag(ProfilePictureTestTags.PROFILE_PICTURE_LOADING),
            )
            return@Surface
          }

          // Error loading image fallback
          if (bitmap == null) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(appPalette().secondary)
                        .testTag(ProfilePictureTestTags.PROFILE_PICTURE_DEFAULT),
                contentAlignment = Alignment.Center) {
                  Icon(
                      imageVector = Icons.Default.Person,
                      contentDescription = "Default profile picture",
                      modifier = Modifier.fillMaxSize(0.6f),
                      tint = WhiteColor)
                }
            return@Surface
          }

          Image(
              bitmap = bitmap!!.asImageBitmap(),
              contentDescription = "Profile picture",
              Modifier.fillMaxSize().testTag(ProfilePictureTestTags.PROFILE_PICTURE))
        }

        if (withName && name.isNotBlank()) {
          Text(
              text = name,
              fontSize = ConstantRequestList.RequestItemNameFontSize,
              lineHeight = ConstantRequestList.RequestItemNameFontSize,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
              color = appPalette().text.copy(alpha = 0.8f),
              modifier =
                  Modifier.wrapContentHeight()
                      .padding(top = ConstantRequestList.RequestItemNameTopPadding)
                      .testTag(ProfilePictureTestTags.PROFILE_PICTURE_NAME))
        }
      }
}

private suspend fun loadBitmapFromUri(uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
      try {
        when (uri.scheme?.lowercase()) {
          "http",
          "https" ->
              URL(uri.toString()).openStream().use { input -> BitmapFactory.decodeStream(input) }
          else -> null
        }
      } catch (_: Exception) {
        null
      }
    }
