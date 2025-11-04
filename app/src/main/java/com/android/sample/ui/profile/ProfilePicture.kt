package com.android.sample.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.theme.AppColors.SecondaryColor
import com.android.sample.ui.theme.AppColors.SecondaryDark
import com.android.sample.ui.theme.AppColors.WhiteColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object ProfilePictureTestTags {
    const val PROFILE_PICTURE = "profile_picture"
    const val PROFILE_PICTURE_LOADING = "profile_picture_loading"
    const val PROFILE_PICTURE_DEFAULT = "profile_picture_default"
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


@Composable
fun ProfilePicture(
    profileRepository: UserProfileRepository,
    profileId : String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loading : Boolean by remember {
        mutableStateOf(true)
    }
    var bitmap: Bitmap? by remember {
        mutableStateOf(null)
    }

    LaunchedEffect(profileId) {
        loading = true

        val cachedProfile = ProfileCache.get(profileId)
        val profile: UserProfile? = if (cachedProfile != null) {
            cachedProfile
        } else {
            try {
                val p = profileRepository.getUserProfile(profileId)
                ProfileCache.put(profileId, p)
                p
            } catch (e: Exception) {
                // couldn't fetch profile
                ProfileCache.put(profileId, null)
                null
            }
        }

        if (profile == null) {
            bitmap = null
            loading = false
            return@LaunchedEffect
        }

        val photoUriObj: Uri? = profile.photo

        if (photoUriObj != null && photoUriObj.toString().isNotBlank()) {
            val uri = photoUriObj
            val key = photoUriObj.toString()

            // Check cache first (keyed by uri string)
            val cachedBitmap = try { ProfileIconCache.get(key) } catch (_: Exception) { null }
            if (cachedBitmap != null) {
                bitmap = cachedBitmap
            } else {
                val loadedBitmap = try { loadBitmapFromUri(uri) } catch (_: Exception) { null }
                ProfileIconCache.put(key, loadedBitmap)
                bitmap = loadedBitmap
            }
        } else {
            bitmap = null
        }

        loading = false
    }

    Surface(
        modifier = modifier.clip(CircleShape).clickable() { onClick() },
        shape = CircleShape,
    ) {
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SecondaryDark.copy(alpha = 0.3f))
                    .testTag(ProfilePictureTestTags.PROFILE_PICTURE_LOADING),
            )
            return@Surface
        }

        // Error loading image fallback
        if (bitmap == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SecondaryColor)
                    .testTag(ProfilePictureTestTags.PROFILE_PICTURE_DEFAULT),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default profile picture",
                    modifier = Modifier.fillMaxSize(0.6f),
                    tint = WhiteColor
                )
            }
            return@Surface
        }

        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Profile picture",
            modifier.fillMaxSize().testTag(ProfilePictureTestTags.PROFILE_PICTURE)
        )

    }
}

private suspend fun loadBitmapFromUri(uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            when (uri.scheme?.lowercase()) {
                "http",
                "https" ->
                    URL(uri.toString()).openStream().use { input -> BitmapFactory.decodeStream(input) }
                else ->
                    null
            }
        } catch (_: Exception) {
            null
        }
    }