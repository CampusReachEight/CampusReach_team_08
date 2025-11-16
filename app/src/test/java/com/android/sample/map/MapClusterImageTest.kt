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

  @Test
  fun getTextSizeForCount_returns_correct_size_for_single_digit() {
    assertEquals(ConstantMap.NUMBER_SIZE_ONE, getTextSizeForCount(5))
    assertEquals(ConstantMap.NUMBER_SIZE_ONE, getTextSizeForCount(9))
  }

  @Test
  fun getTextSizeForCount_returns_correct_size_for_double_digit() {
    assertEquals(ConstantMap.NUMBER_SIZE_TWO, getTextSizeForCount(10))
    assertEquals(ConstantMap.NUMBER_SIZE_TWO, getTextSizeForCount(99))
  }

  @Test
  fun getTextSizeForCount_returns_correct_size_for_triple_digit() {
    assertEquals(ConstantMap.NUMBER_SIZE_THREE, getTextSizeForCount(100))
    assertEquals(ConstantMap.NUMBER_SIZE_THREE, getTextSizeForCount(999))
  }

  @Test
  fun createTextPaint_returns_paint_with_correct_properties() {
    val paint = createTextPaint(5)

    assertEquals(Color.WHITE, paint.color)
    assertEquals(ConstantMap.NUMBER_SIZE_ONE, paint.textSize)
    assertEquals(Paint.Align.CENTER, paint.textAlign)
    assertEquals(true, paint.isAntiAlias)
  }

  @Test
  fun createMarkerBitmap_returns_bitmap_with_correct_dimensions() {
    val bitmap = createMarkerBitmap(5)

    assertEquals(100, bitmap.width)
    assertEquals(100, bitmap.height)
    assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
  }

  @Test
  fun createMarkerBitmap_handles_different_count_values() {
    val bitmap1 = createMarkerBitmap(1)
    val bitmap2 = createMarkerBitmap(50)
    val bitmap3 = createMarkerBitmap(999)

    assertNotNull(bitmap1)
    assertNotNull(bitmap2)
    assertNotNull(bitmap3)

    // Verify dimensions are correct for all
    assertEquals(100, bitmap1.width)
    assertEquals(100, bitmap2.width)
    assertEquals(100, bitmap3.width)
  }

  @Test
  fun createMarkerBitmap_is_properly_configured() {
    val bitmap = createMarkerBitmap(5)

    // Verify the bitmap exists and has proper configuration
    assertNotNull(bitmap)
    assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    assertEquals(100, bitmap.width)
    assertEquals(100, bitmap.height)

    // Verify the bitmap is mutable (can be drawn on)
    assertEquals(true, bitmap.isMutable)
  }
}
