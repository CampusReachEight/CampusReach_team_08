package com.android.sample.map

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import com.android.sample.ui.map.ConstantMap
import com.android.sample.ui.map.createMarkerBitmap
import com.android.sample.ui.map.createTextPaint
import com.android.sample.ui.map.getTextSizeForCount
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapClusterImageTest {
  private val verySmallSizeText = 5
  private val smallSizeText = 9
  private val midSmallSizeText = 10
  private val midSizeText = 99
  private val bigSmallSizeText = 100
  private val bigSizeText = 999
  private val one = 1
  private val fifty = 50

  @Test
  fun getTextSizeForCount_returns_correct_size_for_single_digit() {
    assertEquals(ConstantMap.NUMBER_SIZE_ONE, getTextSizeForCount(verySmallSizeText))
    assertEquals(ConstantMap.NUMBER_SIZE_ONE, getTextSizeForCount(smallSizeText))
  }

  @Test
  fun getTextSizeForCount_returns_correct_size_for_double_digit() {
    assertEquals(ConstantMap.NUMBER_SIZE_TWO, getTextSizeForCount(midSmallSizeText))
    assertEquals(ConstantMap.NUMBER_SIZE_TWO, getTextSizeForCount(midSizeText))
  }

  @Test
  fun getTextSizeForCount_returns_correct_size_for_triple_digit() {
    assertEquals(ConstantMap.NUMBER_SIZE_THREE, getTextSizeForCount(bigSmallSizeText))
    assertEquals(ConstantMap.NUMBER_SIZE_THREE, getTextSizeForCount(bigSizeText))
  }

  @Test
  fun createTextPaint_returns_paint_with_correct_properties() {
    val paint = createTextPaint(verySmallSizeText)

    assertEquals(Color.WHITE, paint.color)
    assertEquals(ConstantMap.NUMBER_SIZE_ONE, paint.textSize)
    assertEquals(Paint.Align.CENTER, paint.textAlign)
    assertEquals(true, paint.isAntiAlias)
  }

  @Test
  fun createMarkerBitmap_returns_bitmap_with_correct_dimensions() {
    val bitmap = createMarkerBitmap(verySmallSizeText)

    assertEquals(ConstantMap.SIZE_OF_MARKER, bitmap.width)
    assertEquals(ConstantMap.SIZE_OF_MARKER, bitmap.height)
    assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
  }

  @Test
  fun createMarkerBitmap_handles_different_count_values() {
    val bitmap1 = createMarkerBitmap(one)
    val bitmap2 = createMarkerBitmap(fifty)
    val bitmap3 = createMarkerBitmap(bigSizeText)

    assertNotNull(bitmap1)
    assertNotNull(bitmap2)
    assertNotNull(bitmap3)

    // Verify dimensions are correct for all
    assertEquals(ConstantMap.SIZE_OF_MARKER, bitmap1.width)
    assertEquals(ConstantMap.SIZE_OF_MARKER, bitmap2.width)
    assertEquals(ConstantMap.SIZE_OF_MARKER, bitmap3.width)
  }

  @Test
  fun createMarkerBitmap_is_properly_configured() {
    val bitmap = createMarkerBitmap(verySmallSizeText)

    // Verify the bitmap exists and has proper configuration
    assertNotNull(bitmap)
    assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    assertEquals(ConstantMap.SIZE_OF_MARKER, bitmap.width)
    assertEquals(ConstantMap.SIZE_OF_MARKER, bitmap.height)

    // Verify the bitmap is mutable (can be drawn on)
    assertEquals(true, bitmap.isMutable)
  }
}
