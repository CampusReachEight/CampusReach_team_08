package com.android.sample.ui.utils

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RangeFacetTest {

  private data class TestItem(val value: Int)

  private lateinit var definition: RangeFilterDefinitions.RangeFilterDefinition<TestItem>
  private lateinit var facet: RangeFacet<TestItem>

  @Before
  fun setUp() {
    definition =
        RangeFilterDefinitions.RangeFilterDefinition(
            id = "testRange",
            title = "Test Range",
            minBound = 0,
            maxBound = 100,
            step = 1,
            extract = { it.value },
            buttonTestTag = "testButton",
            panelTestTag = "testPanel",
            sliderTestTag = "testSlider",
            minFieldTestTag = "testMinField",
            maxFieldTestTag = "testMaxField")
    facet = RangeFacet(definition)
  }

  @Test
  fun `initial state has full range`() = runTest {
    assertEquals(0..100, facet.currentRange.first())
    assertEquals(0..100, facet.fullRange)
    assertFalse(facet.isActive)
  }

  @Test
  fun `setRange updates current range`() = runTest {
    facet.setRange(20..80)
    assertEquals(20..80, facet.currentRange.first())
    assertTrue(facet.isActive)
  }

  @Test
  fun `setRange clamps to bounds`() = runTest {
    facet.setRange(-10..150)
    assertEquals(0..100, facet.currentRange.first())
  }

  @Test
  fun `setMin updates only minimum`() = runTest {
    facet.setRange(10..90)
    facet.setMin(30)
    assertEquals(30..90, facet.currentRange.first())
  }

  @Test
  fun `setMin clamps to current max`() = runTest {
    facet.setRange(10..50)
    facet.setMin(70) // Should clamp to 50
    assertEquals(50..50, facet.currentRange.first())
  }

  @Test
  fun `setMin clamps to minBound`() = runTest {
    facet.setRange(20..80)
    facet.setMin(-10) // Should clamp to 0
    assertEquals(0..80, facet.currentRange.first())
  }

  @Test
  fun `setMax updates only maximum`() = runTest {
    facet.setRange(10..90)
    facet.setMax(60)
    assertEquals(10..60, facet.currentRange.first())
  }

  @Test
  fun `setMax clamps to current min`() = runTest {
    facet.setRange(50..90)
    facet.setMax(30) // Should clamp to 50
    assertEquals(50..50, facet.currentRange.first())
  }

  @Test
  fun `setMax clamps to maxBound`() = runTest {
    facet.setRange(20..80)
    facet.setMax(150) // Should clamp to 100
    assertEquals(20..100, facet.currentRange.first())
  }

  @Test
  fun `reset returns to full range`() = runTest {
    facet.setRange(20..80)
    assertTrue(facet.isActive)

    facet.reset()
    assertEquals(0..100, facet.currentRange.first())
    assertFalse(facet.isActive)
  }

  @Test
  fun `matches returns true when filter inactive`() {
    assertFalse(facet.isActive)
    assertTrue(facet.matches(TestItem(-50))) // Out of bounds but filter inactive
    assertTrue(facet.matches(TestItem(50)))
    assertTrue(facet.matches(TestItem(150))) // Out of bounds but filter inactive
  }

  @Test
  fun `matches returns true for items within range`() {
    facet.setRange(20..80)
    assertTrue(facet.matches(TestItem(20)))
    assertTrue(facet.matches(TestItem(50)))
    assertTrue(facet.matches(TestItem(80)))
  }

  @Test
  fun `matches returns false for items outside range`() {
    facet.setRange(20..80)
    assertFalse(facet.matches(TestItem(10)))
    assertFalse(facet.matches(TestItem(19)))
    assertFalse(facet.matches(TestItem(81)))
    assertFalse(facet.matches(TestItem(100)))
  }

  @Test
  fun `properties delegate to definition`() {
    assertEquals("testRange", facet.id)
    assertEquals("Test Range", facet.title)
    assertEquals(0, facet.minBound)
    assertEquals(100, facet.maxBound)
    assertEquals(1, facet.step)
    assertEquals("testButton", facet.buttonTestTag)
    assertEquals("testPanel", facet.panelTestTag)
    assertEquals("testSlider", facet.sliderTestTag)
    assertEquals("testMinField", facet.minFieldTestTag)
    assertEquals("testMaxField", facet.maxFieldTestTag)
  }

  @Test
  fun `extract function works correctly`() {
    val item = TestItem(42)
    assertEquals(42, facet.extract(item))
  }

  @Test
  fun `edge case - single value range`() = runTest {
    facet.setRange(50..50)
    assertEquals(50..50, facet.currentRange.first())
    assertTrue(facet.isActive)
    assertTrue(facet.matches(TestItem(50)))
    assertFalse(facet.matches(TestItem(49)))
    assertFalse(facet.matches(TestItem(51)))
  }

  @Test
  fun `edge case - full range is not active`() = runTest {
    facet.setRange(0..100)
    assertFalse(facet.isActive)
  }

  @Test
  fun `step property is correctly exposed`() {
    val customDef =
        RangeFilterDefinitions.RangeFilterDefinition(
            id = "custom",
            title = "Custom",
            minBound = 0,
            maxBound = 1000,
            step = 10,
            extract = { it: TestItem -> it.value },
            buttonTestTag = "btn",
            panelTestTag = "panel",
            sliderTestTag = "slider",
            minFieldTestTag = "min",
            maxFieldTestTag = "max")
    val customFacet = RangeFacet(customDef)
    assertEquals(10, customFacet.step)
  }
}
