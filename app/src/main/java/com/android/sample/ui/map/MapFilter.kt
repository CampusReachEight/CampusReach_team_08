package com.android.sample.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import com.android.sample.model.request.RequestOwnership
import com.android.sample.ui.request.FilterMenuButton
import com.android.sample.ui.request.FilterMenuPanel
import com.android.sample.ui.request.RequestSearchFilterViewModel

@Composable
fun MapFilter(
    searchFilterViewModel: RequestSearchFilterViewModel,
    selectedOwnership: RequestOwnership,
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
  val facets = searchFilterViewModel.facets
  val selectedSets = facets.map { it.selected.collectAsState() }
  var openFacetId: String? by remember { mutableStateOf(null) }

  Column(modifier = modifier) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation =
            CardDefaults.cardElevation(defaultElevation = ConstantMap.CARD_DEFAULT_ELEVATION),
        shape = RectangleShape) {
          LazyRow(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(
                          horizontal = ConstantMap.CARD_HORIZONTAL_PADDING,
                          vertical = ConstantMap.CARD_VERTICAL_PADDING)
                      .testTag(MapTestTags.MAP_LIST_FILTER),
              horizontalArrangement = Arrangement.spacedBy(ConstantMap.BUTTON_SPACING)) {
                item(key = ConstantMap.OWNERSHIP_KEY) {
                  FilterMenuButton(
                      title = selectedOwnership.displayString(),
                      selectedCount = 0,
                      testTag = MapTestTags.MAP_FILTER_OWNER,
                      onClick = {
                        openFacetId =
                            if (openFacetId == ConstantMap.OWNERSHIP_KEY) {
                              null
                            } else {
                              ConstantMap.OWNERSHIP_KEY
                            }
                      },
                      modifier = Modifier.height(ConstantMap.FILTER_BUTTON_HEIGHT))
                }

                facets.forEachIndexed { index, facet ->
                  val selectedCount = selectedSets[index].value.size
                  item(key = facet.id) {
                    FilterMenuButton(
                        title = facet.title,
                        selectedCount = selectedCount,
                        testTag = facet.dropdownButtonTag,
                        onClick = { openFacetId = if (openFacetId == facet.id) null else facet.id },
                        modifier = Modifier.height(ConstantMap.FILTER_BUTTON_HEIGHT))
                  }
                }
              }
        }

    if (openFacetId == ConstantMap.OWNERSHIP_KEY) {
      OwnershipFilterPanel(
          selected = selectedOwnership,
          onSelect = { ownership ->
            viewModel.updateFilterOwnerShip(ownership)
            openFacetId = null
          })
    }

    facets
        .find { it.id == openFacetId }
        ?.let { openFacet ->
          val countsState = openFacet.counts.collectAsState()
          FilterMenuPanel(
              values = openFacet.values,
              selected = openFacet.selected.collectAsState().value,
              counts = countsState.value,
              labelOf = { openFacet.labelOf(it) },
              onToggle = { openFacet.toggle(it) },
              dropdownSearchBarTestTag = openFacet.searchBarTag,
              rowTestTagOf = { openFacet.rowTagOf(it) })
        }
  }
}

@Composable
private fun OwnershipFilterPanel(selected: RequestOwnership, onSelect: (RequestOwnership) -> Unit) {
  Column(
      modifier =
          Modifier.fillMaxWidth().padding(horizontal = ConstantMap.PANEL_HORIZONTAL_PADDING)) {
        Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT))
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = ConstantMap.SURFACE_TONAL_ELEVATION,
            shadowElevation = ConstantMap.SURFACE_SHADOW_ELEVATION) {
              Column(
                  modifier = Modifier.fillMaxWidth().padding(ConstantMap.PANEL_INTERNAL_PADDING)) {
                    RequestOwnership.entries.forEach { ownership ->
                      Row(
                          modifier =
                              Modifier.fillMaxWidth()
                                  .clickable { onSelect(ownership) }
                                  .padding(vertical = ConstantMap.ROW_VERTICAL_PADDING)
                                  .testTag(MapTestTags.testTagForRequestOwnership(ownership)),
                          verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = ownership == selected, onClick = { onSelect(ownership) })
                            Spacer(modifier = Modifier.width(ConstantMap.RADIO_BUTTON_SPACING))
                            Text(text = ownership.displayString())
                          }
                    }
                  }
            }
      }
}
