package com.android.sample.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.sample.ui.getFilterAndSortButtonColors
import com.android.sample.ui.getTextFieldColors
import com.android.sample.ui.theme.appPalette

/** Constants for EnumFilterUI dimensions. */
object EnumFilterUIDimens {
  val PaddingSmall = 8.dp
  val PaddingMedium = 12.dp
  val PaddingLarge = 16.dp
  val RowSpacing = 8.dp
  val ButtonHeight = 40.dp
  val FilterRowHeight = 48.dp
  val FilterRowHorizontalPadding = 8.dp
  val DropdownMaxHeight = 200.dp
  val SurfaceTonalElevation = 2.dp
  val SurfaceShadowElevation = 2.dp

  val EnumFilterSpacing = 1f
}

object EnumFilterUIText {
  val SearchOptionsLabel = "Search options"
}

/**
 * Generic filter button that displays the facet title and selected count. When clicked, it should
 * toggle the visibility of the filter panel.
 *
 * @param T The type of item being filtered
 * @param facet The enum facet to display
 * @param selectedCount Number of selected values
 * @param onClick Callback when button is clicked
 * @param modifier Optional modifier
 */
@Composable
fun <T> EnumFilterButton(
    facet: EnumFacet<T>,
    selectedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  EnumFilterButtonSimple(
      title = facet.title,
      selectedCount = selectedCount,
      testTag = facet.dropdownButtonTag,
      onClick = onClick,
      modifier = modifier)
}

/**
 * Generic filter panel that displays all values for a facet with checkboxes. Includes a search bar
 * to filter options and shows counts for each value.
 *
 * @param T The type of item being filtered
 * @param facet The enum facet to display
 * @param selected Currently selected values
 * @param counts Count of items matching each value
 * @param onToggle Callback when a value is toggled
 * @param modifier Optional modifier
 * @param horizontalPadding Horizontal padding for the panel
 * @param maxHeight Maximum height of the values list
 */
@Composable
fun <T> EnumFilterPanel(
    facet: EnumFacet<T>,
    selected: Set<Enum<*>>,
    counts: Map<Enum<*>, Int>,
    onToggle: (Enum<*>) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = EnumFilterUIDimens.PaddingLarge,
    maxHeight: Dp = EnumFilterUIDimens.DropdownMaxHeight
) {
  EnumFilterPanelSimple(
      values = facet.values,
      selected = selected,
      counts = counts,
      labelOf = { facet.labelOf(it) },
      onToggle = onToggle,
      dropdownSearchBarTestTag = facet.searchBarTag,
      rowTestTagOf = { facet.rowTagOf(it) },
      modifier = modifier,
      horizontalPadding = horizontalPadding,
      maxHeight = maxHeight)
}

/**
 * Simplified filter panel that works with raw values instead of a facet object. Useful for screens
 * that need more control over the filter UI.
 *
 * @param values All possible enum values
 * @param selected Currently selected values
 * @param counts Count of items matching each value
 * @param labelOf Function to get display label for a value
 * @param onToggle Callback when a value is toggled
 * @param dropdownSearchBarTestTag Test tag for the search bar
 * @param rowTestTagOf Function to get test tag for each row
 * @param modifier Optional modifier
 * @param horizontalPadding Horizontal padding for the panel
 * @param maxHeight Maximum height of the values list
 */
@Composable
fun EnumFilterPanelSimple(
    values: List<Enum<*>>,
    selected: Set<Enum<*>>,
    counts: Map<Enum<*>, Int>,
    labelOf: (Enum<*>) -> String,
    onToggle: (Enum<*>) -> Unit,
    dropdownSearchBarTestTag: String,
    rowTestTagOf: (Enum<*>) -> String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = EnumFilterUIDimens.PaddingLarge,
    maxHeight: Dp = EnumFilterUIDimens.DropdownMaxHeight
) {
  Column(modifier = modifier.fillMaxWidth().padding(horizontal = horizontalPadding)) {
    Spacer(modifier = Modifier.height(EnumFilterUIDimens.PaddingSmall))
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = EnumFilterUIDimens.SurfaceTonalElevation,
        shadowElevation = EnumFilterUIDimens.SurfaceShadowElevation) {
          Column(modifier = Modifier.fillMaxWidth().padding(EnumFilterUIDimens.PaddingMedium)) {
            var localQuery by rememberSaveable { mutableStateOf("") }

            OutlinedTextField(
                value = localQuery,
                onValueChange = { localQuery = it },
                modifier = Modifier.fillMaxWidth().testTag(dropdownSearchBarTestTag),
                singleLine = true,
                placeholder = { Text(EnumFilterUIText.SearchOptionsLabel) },
                colors = getTextFieldColors())

            Spacer(modifier = Modifier.height(EnumFilterUIDimens.PaddingSmall))

            val filteredValues =
                remember(localQuery, values, counts) {
                  values
                      .filter { labelOf(it).contains(localQuery, ignoreCase = true) }
                      .sortedByDescending { counts[it] ?: 0 }
                }

            Box(modifier = Modifier.heightIn(max = maxHeight)) {
              Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                filteredValues.forEach { v ->
                  val isChecked = selected.contains(v)
                  val count = counts[v] ?: 0
                  Row(
                      modifier =
                          Modifier.fillMaxWidth()
                              .clickable { onToggle(v) }
                              .padding(horizontal = EnumFilterUIDimens.FilterRowHorizontalPadding)
                              .testTag(rowTestTagOf(v))
                              .height(EnumFilterUIDimens.FilterRowHeight),
                      horizontalArrangement = Arrangement.Start,
                      verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = null,
                            colors =
                                CheckboxDefaults.colors(
                                    uncheckedColor = appPalette().onSurface,
                                    checkedColor = appPalette().accent))
                        Spacer(modifier = Modifier.width(EnumFilterUIDimens.RowSpacing))
                        Text(text = labelOf(v), color = appPalette().onSurface)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = count.toString(), color = appPalette().onSurface)
                      }
                }
              }
            }
          }
        }
  }
}

/**
 * Simple filter button without facet object dependency. Useful for screens that need more control
 * over the filter UI.
 *
 * @param title Display title for the button
 * @param selectedCount Number of selected values
 * @param testTag Test tag for the button
 * @param onClick Callback when button is clicked
 * @param modifier Optional modifier
 */
@Composable
fun EnumFilterButtonSimple(
    title: String,
    selectedCount: Int,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  OutlinedButton(
      onClick = onClick,
      modifier = modifier.testTag(testTag),
      colors = getFilterAndSortButtonColors()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(EnumFilterUIDimens.RowSpacing),
            verticalAlignment = Alignment.CenterVertically) {
              val label = if (selectedCount > 0) "$title ($selectedCount)" else title
              Text(label)
              Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
            }
      }
}
