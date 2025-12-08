package com.android.sample.ui.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Range filter configuration for numeric-based filtering. This utility allows filtering items by
 * numeric ranges (e.g., kudos 0-100, help received 50-500).
 *
 * Usage:
 * 1. Define a `RangeFilterDefinition<T>` for your data type T
 * 2. Create a `RangeFacet<T>` instance from the definition
 * 3. Use `RangeFacet.currentRange` StateFlow to observe, `setRange()` to update
 *
 * @param T The type of item being filtered (e.g., UserProfile)
 */
object RangeFilterDefinitions {

  /**
   * Defines a single numeric range filter for items of type T.
   *
   * @param T The type of item being filtered
   * @property id Unique identifier for this range filter (e.g., "kudos", "helpReceived")
   * @property title Display title for the filter button (e.g., "Kudos", "Help Received")
   * @property minBound The minimum possible value for this range
   * @property maxBound The maximum possible value for this range
   * @property step The increment step for the slider (e.g., 1, 10, 100)
   * @property extract Function to extract the numeric value from an item of type T
   * @property buttonTestTag Test tag for the range filter button
   * @property panelTestTag Test tag for the range filter panel
   * @property sliderTestTag Test tag for the slider component
   * @property minFieldTestTag Test tag for the minimum value text field
   * @property maxFieldTestTag Test tag for the maximum value text field
   */
  data class RangeFilterDefinition<T>(
      val id: String,
      val title: String,
      val minBound: Int,
      val maxBound: Int,
      val step: Int = 1,
      val extract: (T) -> Int,
      val buttonTestTag: String,
      val panelTestTag: String,
      val sliderTestTag: String,
      val minFieldTestTag: String,
      val maxFieldTestTag: String,
  )
}

/**
 * Runtime range filter state holder for numeric filtering.
 *
 * Manages the currently selected range and provides reactive state for UI binding. When the range
 * equals the full bounds (min to max), the filter is considered inactive.
 *
 * @param T The type of item being filtered
 * @property def The range filter definition this instance is based on
 */
class RangeFacet<T>(val def: RangeFilterDefinitions.RangeFilterDefinition<T>) {
  val id: String
    get() = def.id

  val title: String
    get() = def.title

  val minBound: Int
    get() = def.minBound

  val maxBound: Int
    get() = def.maxBound

  val step: Int
    get() = def.step

  val extract: (T) -> Int
    get() = def.extract

  val buttonTestTag: String
    get() = def.buttonTestTag

  val panelTestTag: String
    get() = def.panelTestTag

  val sliderTestTag: String
    get() = def.sliderTestTag

  val minFieldTestTag: String
    get() = def.minFieldTestTag

  val maxFieldTestTag: String
    get() = def.maxFieldTestTag

  /** The full range representing no filter applied. */
  val fullRange: IntRange
    get() = minBound..maxBound

  private val _currentRange = MutableStateFlow(fullRange)

  /** Observable current range selection. When equals `fullRange`, filter is inactive. */
  val currentRange: StateFlow<IntRange>
    get() = _currentRange

  /** Whether the filter is currently active (range differs from full bounds). */
  val isActive: Boolean
    get() = _currentRange.value != fullRange

  /**
   * Updates the current range selection.
   *
   * @param range The new range, will be clamped to bounds
   */
  fun setRange(range: IntRange) {
    val clamped =
        IntRange(
            start = range.first.coerceIn(minBound, maxBound),
            endInclusive = range.last.coerceIn(minBound, maxBound))
    _currentRange.value = clamped
  }

  /** Sets only the minimum value, keeping the current maximum. */
  fun setMin(min: Int) {
    val clampedMin = min.coerceIn(minBound, _currentRange.value.last)
    _currentRange.value = clampedMin.._currentRange.value.last
  }

  /** Sets only the maximum value, keeping the current minimum. */
  fun setMax(max: Int) {
    val clampedMax = max.coerceIn(_currentRange.value.first, maxBound)
    _currentRange.value = _currentRange.value.first..clampedMax
  }

  /** Resets the range to full bounds (filter inactive). */
  fun reset() {
    _currentRange.value = fullRange
  }

  /**
   * Checks if an item passes this range filter.
   *
   * @param item The item to check
   * @return true if the item's value is within the current range, or if filter is inactive
   */
  fun matches(item: T): Boolean {
    if (!isActive) return true
    val value = extract(item)
    return value in _currentRange.value
  }
}
