package com.android.sample.ui.utils

import android.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.android.sample.ui.getFilterAndSortButtonColors
import com.android.sample.ui.getTextFieldColors
import com.android.sample.ui.theme.appPalette

/** Constants for RangeFilterUI dimensions. */
object RangeFilterUIDimens {
  val PanelPadding = 16.dp
  val PanelVerticalSpacing = 12.dp
  val FieldWidth = 80.dp
  val FieldSpacing = 8.dp
  val ButtonHeight = 40.dp
  val SliderHeight = 32.dp
  val MinimalInteractiveSize = 12.dp
  val SurfaceTonalElevation = 2.dp
  val SurfaceShadowElevation = 2.dp
  val SliderStepOffset = 1
  val SliderMinSteps = 0
}

/** Test tags for range filter components. */
object RangeFilterTestTags {
  const val RESET_BUTTON_SUFFIX = "_reset"

  fun getResetButtonTag(filterId: String): String = "${filterId}${RESET_BUTTON_SUFFIX}"
}

object RangeFilterUIText {
  const val ResetButtonLabel = "Reset"
  const val MinLabel = "Min"
  const val MaxLabel = "Max"
  const val ToLabel = "to"
}

/**
 * Button that displays a range filter with current selection info. When clicked, opens the range
 * filter panel.
 *
 * @param T The type of item being filtered
 * @param rangeFacet The range facet to display
 * @param onClick Called when the button is clicked to open the panel
 * @param modifier Modifier for the button
 */
@Composable
fun <T> RangeFilterButton(
    rangeFacet: RangeFacet<T>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val currentRange by rangeFacet.currentRange.collectAsState()
  val isActive = currentRange != rangeFacet.fullRange

  val label =
      if (isActive) {
        "${rangeFacet.title} (${currentRange.first}-${currentRange.last})"
      } else {
        rangeFacet.title
      }

  OutlinedButton(
      onClick = onClick,
      modifier = modifier.testTag(rangeFacet.buttonTestTag),
      colors = getFilterAndSortButtonColors()
      ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RangeFilterUIDimens.FieldSpacing),
        verticalAlignment = Alignment.CenterVertically) {
          Text(label)
          Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
        }
  }
}

/**
 * Panel that displays a range slider with min/max text fields for precise input.
 *
 * @param T The type of item being filtered
 * @param rangeFacet The range facet to control
 * @param modifier Modifier for the panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> RangeFilterPanel(rangeFacet: RangeFacet<T>, modifier: Modifier = Modifier) {
  val currentRange by rangeFacet.currentRange.collectAsState()

  // Local state for text fields to allow typing without immediate updates
  var minText by remember(currentRange.first) { mutableStateOf(currentRange.first.toString()) }
  var maxText by remember(currentRange.last) { mutableStateOf(currentRange.last.toString()) }

  Surface(
      shape = MaterialTheme.shapes.medium,
      tonalElevation = RangeFilterUIDimens.SurfaceTonalElevation,
      shadowElevation = RangeFilterUIDimens.SurfaceShadowElevation,
      color = appPalette().surface,
      modifier = modifier.testTag(rangeFacet.panelTestTag)) {
        Column(modifier = Modifier.fillMaxWidth().padding(RangeFilterUIDimens.PanelPadding)) {
          // Title row with reset button
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(text = rangeFacet.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = appPalette().onSurface
                )
                TextButton(
                    onClick = {
                      rangeFacet.reset()
                      minText = rangeFacet.minBound.toString()
                      maxText = rangeFacet.maxBound.toString()
                    },
                    modifier =
                        Modifier.testTag(RangeFilterTestTags.getResetButtonTag(rangeFacet.id)),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = appPalette().accent
                    )
                ) {
                      Text(RangeFilterUIText.ResetButtonLabel)
                    }
              }

          Spacer(modifier = Modifier.height(RangeFilterUIDimens.PanelVerticalSpacing))

          // Range slider with compact thumb/track height
          CompositionLocalProvider(
              LocalMinimumInteractiveComponentSize provides
                  RangeFilterUIDimens.MinimalInteractiveSize) {
                RangeSlider(
                    value = currentRange.first.toFloat()..currentRange.last.toFloat(),
                    onValueChange = { range ->
                      val newMin = range.start.toInt()
                      val newMax = range.endInclusive.toInt()
                      rangeFacet.setRange(newMin..newMax)
                      minText = newMin.toString()
                      maxText = newMax.toString()
                    },
                    valueRange = rangeFacet.minBound.toFloat()..rangeFacet.maxBound.toFloat(),
                    steps =
                        ((rangeFacet.maxBound - rangeFacet.minBound) / rangeFacet.step -
                                RangeFilterUIDimens.SliderStepOffset)
                            .coerceAtLeast(RangeFilterUIDimens.SliderMinSteps),
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(RangeFilterUIDimens.SliderHeight)
                            .testTag(rangeFacet.sliderTestTag),
                    colors = SliderDefaults.colors(
                        thumbColor = appPalette().accent,
                        activeTrackColor = appPalette().accent,
                        inactiveTrackColor = appPalette().secondary,
                        activeTickColor = appPalette().secondary,
                        inactiveTickColor = appPalette().accent
                    )
                )
              }

          Spacer(modifier = Modifier.height(RangeFilterUIDimens.PanelVerticalSpacing))

          // Min/Max text fields
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = { newValue ->
                      minText = newValue
                      newValue.toIntOrNull()?.let { rangeFacet.setMin(it) }
                    },
                    label = { Text(RangeFilterUIText.MinLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier =
                        Modifier.width(RangeFilterUIDimens.FieldWidth)
                            .testTag(rangeFacet.minFieldTestTag),
                    colors = getTextFieldColors()
                    )

                Text(
                    text = RangeFilterUIText.ToLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = RangeFilterUIDimens.FieldSpacing),
                    color = appPalette().onSurface
                )

                OutlinedTextField(
                    value = maxText,
                    onValueChange = { newValue ->
                      maxText = newValue
                      newValue.toIntOrNull()?.let { rangeFacet.setMax(it) }
                    },
                    label = { Text(RangeFilterUIText.MaxLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier =
                        Modifier.width(RangeFilterUIDimens.FieldWidth)
                            .testTag(rangeFacet.maxFieldTestTag),
                    colors = getTextFieldColors()
                    )
              }
        }
      }
}
