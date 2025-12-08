package com.android.sample.ui.leaderboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

/** Simple add-on overlay definition for leaderboard profile images. */
data class ProfileAddon(
    val image: ImageVector,
    val size: Dp = 24.dp,
)

/** Registry of reusable add-ons (crowns, badges, etc.). */
object LeaderboardAddOns {
  val crown: ProfileAddon = ProfileAddon(image = CrownIcon, size = 20.dp)
  val cutiePatootie: ProfileAddon = ProfileAddon(image = CutiePatootieIcon, size = 56.dp)
}

/** Eligible hashed user ids for CutiePatootie overlay. Currently empty placeholder. */
object AddonEligibility {
  val cutiePatootieHashes: Set<String> =
      setOf(
          "73eec23b48744023c605dd11894c173ad9d42edfbc21c28910345a7b242b533c",
          "5cf30eb45c89cc436143614234f50ef9d3bb082ee1c037be553be70c8fde9621",
      )
}

private val CrownIcon: ImageVector by lazy {
  ImageVector.Builder(
          name = "Crown",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f)
      .apply {
        // Symmetric three-peak crown inspired by provided SVG (scaled to 24x24 viewport)
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
          moveTo(4.8f, 19.2f)
          lineTo(19.2f, 19.2f)
          lineTo(21.6f, 8.4f)
          lineTo(16.8f, 13.2f)
          lineTo(12f, 4.8f)
          lineTo(7.2f, 13.2f)
          lineTo(2.4f, 8.4f)
          close()
        }
      }
      .build()
}

private val CutiePatootieIcon: ImageVector by lazy {
  ImageVector.Builder(
          name = "CutiePatootie",
          defaultWidth = 512.dp,
          defaultHeight = 512.dp,
          viewportWidth = 512f,
          viewportHeight = 512f)
      .apply {
        // Left whiskers (nudged closer to nose)
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color(0xFF231F20)),
            strokeLineWidth = 12f,
            pathFillType = PathFillType.NonZero) {
              moveTo(190f, 280f)
              lineTo(130f, 262f)
              moveTo(185f, 320f)
              lineTo(125f, 320f)
              moveTo(190f, 360f)
              lineTo(130f, 378f)
            }
        // Right whiskers (nudged closer to nose)
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color(0xFF231F20)),
            strokeLineWidth = 12f,
            pathFillType = PathFillType.NonZero) {
              moveTo(322f, 280f)
              lineTo(382f, 262f)
              moveTo(327f, 320f)
              lineTo(387f, 320f)
              moveTo(322f, 360f)
              lineTo(382f, 378f)
            }
        // Nose
        path(
            fill = SolidColor(Color(0xFFFFD200)),
            stroke = SolidColor(Color(0xFF231F20)),
            strokeLineWidth = 6f,
            pathFillType = PathFillType.NonZero) {
              moveTo(256f, 290f)
              curveTo(272.568f, 290f, 286f, 298.954f, 286f, 310f)
              curveTo(286f, 321.046f, 272.568f, 330f, 256f, 330f)
              curveTo(239.432f, 330f, 226f, 321.046f, 226f, 310f)
              curveTo(226f, 298.954f, 239.432f, 290f, 256f, 290f)
              close()
            }
        // Bow group shifted up/right
        // Left loop (pink)
        path(
            fill = SolidColor(Color(0xFFEA4C89)),
            stroke = SolidColor(Color(0xFF231F20)),
            strokeLineWidth = 8f,
            pathFillType = PathFillType.NonZero) {
              moveTo(400f, 80f)
              quadTo(375f, 40f, 360f, 55f)
              quadTo(350f, 80f, 360f, 105f)
              quadTo(375f, 130f, 400f, 80f)
              close()
            }
        // Right loop (pink)
        path(
            fill = SolidColor(Color(0xFFEA4C89)),
            stroke = SolidColor(Color(0xFF231F20)),
            strokeLineWidth = 8f,
            pathFillType = PathFillType.NonZero) {
              moveTo(440f, 80f)
              quadTo(465f, 40f, 480f, 55f)
              quadTo(490f, 80f, 480f, 105f)
              quadTo(465f, 130f, 440f, 80f)
              close()
            }
        // Center knot (pink)
        path(
            fill = SolidColor(Color(0xFFEA4C89)),
            stroke = SolidColor(Color(0xFF231F20)),
            strokeLineWidth = 8f,
            pathFillType = PathFillType.NonZero) {
              moveTo(430f, 80f)
              arcToRelative(22f, 22f, 0f, true, true, -44f, 0f)
              arcToRelative(22f, 22f, 0f, false, true, 44f, 0f)
              close()
            }
        // Bow creases
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color(0xFF231F20)),
            strokeLineWidth = 3f,
            pathFillType = PathFillType.NonZero) {
              moveTo(415f, 75f)
              quadTo(408f, 80f, 415f, 85f)
              moveTo(445f, 75f)
              quadTo(452f, 80f, 445f, 85f)
            }
      }
      .build()
}

/** Simple SHA-256 hex helper for id hashing. */
fun hashIdSha256(id: String): String {
  val digest = MessageDigest.getInstance("SHA-256").digest(id.toByteArray())
  return digest.joinToString("") { byte -> "%02x".format(byte) }
}
