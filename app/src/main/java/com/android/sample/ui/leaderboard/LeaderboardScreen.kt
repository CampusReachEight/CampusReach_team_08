package com.android.sample.ui.leaderboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.leaderboard.LeaderboardCache
import com.android.sample.model.profile.UserProfile
import com.android.sample.ui.navigation.BottomNavigationMenu
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTab
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.navigation.TopNavigationBar
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.theme.appPalette
import com.android.sample.ui.utils.EnumFilterButton
import com.android.sample.ui.utils.EnumFilterPanel
import com.android.sample.ui.utils.RangeFilterButton
import com.android.sample.ui.utils.RangeFilterPanel

/** Top-level screen composable for the leaderboard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    leaderboardViewModel: LeaderboardViewModel? = null,
    searchFilterViewModel: LeaderboardSearchFilterViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val context = LocalContext.current
  val vm =
      leaderboardViewModel
          ?: viewModel(
              factory =
                  remember(context) {
                    LeaderboardViewModelFactory(leaderboardCache = LeaderboardCache(context))
                  })

  val state by vm.state.collectAsState()
  val refreshState = rememberPullToRefreshState()

  LaunchedEffect(Unit) { vm.loadProfiles() }

  LaunchedEffect(state.profiles) {
    if (state.profiles.isNotEmpty()) searchFilterViewModel.initializeWithProfiles(state.profiles)
  }

  val displayedProfiles by searchFilterViewModel.displayedProfiles.collectAsState()
  val query by searchFilterViewModel.searchQuery.collectAsState()
  val isSearching by searchFilterViewModel.isSearching.collectAsState()

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(LeaderboardTestTags.LEADERBOARD_SCREEN),
      topBar = {
        TopNavigationBar(
            selectedTab = NavigationTab.Leaderboard,
            onProfileClick = { navigationActions?.navigateTo(Screen.Profile("TODO")) },
            navigationActions = navigationActions)
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedNavigationTab = NavigationTab.Leaderboard,
            navigationActions = navigationActions)
      }) { innerPadding ->
        state.errorMessage?.let { msg ->
          ErrorDialog(message = msg, onDismiss = { vm.clearError() })
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { vm.refresh() },
            state = refreshState,
            modifier = Modifier.fillMaxSize().padding(innerPadding)) {
              Column(modifier = Modifier.fillMaxSize()) {
                LeaderboardFilters(
                    searchFilterViewModel = searchFilterViewModel,
                    query = query,
                    isSearching = isSearching,
                    onQueryChange = { searchFilterViewModel.updateSearchQuery(it) },
                    onClearQuery = { searchFilterViewModel.clearSearch() })

                Spacer(modifier = Modifier.height(ConstantLeaderboard.PaddingMedium))

                if (state.offlineMode) {
                  Text(
                      text = "You are in offline mode. Displaying cached profiles.",
                      color = appPalette().error,
                      style = MaterialTheme.typography.bodyMedium,
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(horizontal = ConstantLeaderboard.PaddingLarge))
                  Spacer(modifier = Modifier.height(ConstantLeaderboard.PaddingSmall))
                }

                when {
                  state.isLoading -> {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().testTag(LeaderboardTestTags.LOADING_INDICATOR),
                        contentAlignment = Alignment.Center) {
                          CircularProgressIndicator()
                        }
                  }
                  displayedProfiles.isEmpty() -> {
                    Text(
                        text = "No profiles found.",
                        modifier =
                            Modifier.fillMaxSize()
                                .padding(ConstantLeaderboard.PaddingLarge)
                                .testTag(LeaderboardTestTags.EMPTY_LIST_MESSAGE),
                        textAlign = TextAlign.Center)
                  }
                  else -> {
                    LeaderboardList(
                        profiles = displayedProfiles, profileRepository = vm.profileRepository)
                  }
                }
              }
            }
      }
}

@Composable
private fun LeaderboardFilters(
    searchFilterViewModel: LeaderboardSearchFilterViewModel,
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
) {
  val sort by searchFilterViewModel.sortCriteria.collectAsState()
  val facets = searchFilterViewModel.facets
  val rangeFacets = searchFilterViewModel.rangeFacets
  val selectedSets = facets.map { it.selected.collectAsState() }
  var openEnumId by rememberSaveable { mutableStateOf<String?>(null) }
  var openRangeId by rememberSaveable { mutableStateOf<String?>(null) }
  val filterScrollState = rememberScrollState()

  OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = ConstantLeaderboard.PaddingLarge)
              .testTag(LeaderboardTestTags.SEARCH_BAR),
      singleLine = true,
      placeholder = { Text("Search by name") },
      trailingIcon = {
        when {
          query.isNotEmpty() -> TextButton(onClick = onClearQuery) { Text("Clear") }
          isSearching ->
              CircularProgressIndicator(
                  modifier = Modifier.size(ConstantLeaderboard.SmallIndicatorSize),
                  strokeWidth = ConstantLeaderboard.SmallIndicatorStroke)
        }
      })

  Spacer(modifier = Modifier.height(ConstantLeaderboard.PaddingMedium))

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .horizontalScroll(filterScrollState)
              .padding(horizontal = ConstantLeaderboard.PaddingLarge),
      horizontalArrangement = Arrangement.spacedBy(ConstantLeaderboard.RowSpacing)) {
        SortButton(
            current = sort,
            onSelect = { searchFilterViewModel.setSortCriteria(it) },
            modifier = Modifier.height(ConstantLeaderboard.FilterButtonHeight))

        facets.forEachIndexed { index, facet ->
          val selectedCount = selectedSets[index].value.size
          EnumFilterButton(
              facet = facet,
              selectedCount = selectedCount,
              onClick = {
                openRangeId = null
                openEnumId = if (openEnumId == facet.id) null else facet.id
              },
              modifier = Modifier.height(ConstantLeaderboard.FilterButtonHeight))
        }

        rangeFacets.forEach { rangeFacet ->
          RangeFilterButton(
              rangeFacet = rangeFacet,
              onClick = {
                openEnumId = null
                openRangeId = if (openRangeId == rangeFacet.id) null else rangeFacet.id
              },
              modifier = Modifier.height(ConstantLeaderboard.FilterButtonHeight))
        }
      }

  facets
      .find { it.id == openEnumId }
      ?.let { openFacet ->
        val countsState = openFacet.counts.collectAsState()
        EnumFilterPanel(
            facet = openFacet,
            selected = openFacet.selected.collectAsState().value,
            counts = countsState.value,
            onToggle = { openFacet.toggle(it) })
      }

  rangeFacets
      .find { it.id == openRangeId }
      ?.let { openRange ->
        Spacer(modifier = Modifier.height(ConstantLeaderboard.PaddingSmall))
        RangeFilterPanel(
            rangeFacet = openRange,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = ConstantLeaderboard.PaddingLarge)
                    .testTag(openRange.panelTestTag))
      }
}

@Composable
private fun SortButton(
    current: LeaderboardSort,
    onSelect: (LeaderboardSort) -> Unit,
    modifier: Modifier = Modifier
) {
  var expanded by rememberSaveable { mutableStateOf(false) }

  Box(modifier = modifier) {
    FilledTonalButton(
        onClick = { expanded = true },
        modifier = Modifier.testTag(LeaderboardTestTags.SORT_BUTTON),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = ConstantLeaderboard.SortButtonPaddingHorizontal,
                vertical = ConstantLeaderboard.SortButtonPaddingVertical)) {
          Text(current.displayLabel())
          Spacer(Modifier.width(ConstantLeaderboard.FilterRowSpacingSmall))
          Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.testTag(LeaderboardTestTags.SORT_MENU)) {
          LeaderboardSort.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.displayLabel()) },
                onClick = {
                  expanded = false
                  if (option != current) onSelect(option)
                },
                leadingIcon =
                    if (option == current) {
                      { Icon(Icons.Filled.ArrowUpward, contentDescription = null) }
                    } else null,
                modifier = Modifier.testTag(LeaderboardTestTags.getSortOptionTag(option)))
          }
        }
  }
}

@Composable
private fun LeaderboardList(
    profiles: List<UserProfile>,
    profileRepository: com.android.sample.model.profile.UserProfileRepository
) {
  LazyColumn(
      modifier =
          Modifier.fillMaxSize()
              .padding(horizontal = ConstantLeaderboard.ListPadding)
              .testTag(LeaderboardTestTags.LEADERBOARD_LIST),
      verticalArrangement = Arrangement.spacedBy(ConstantLeaderboard.ListItemSpacing)) {
        itemsIndexed(items = profiles, key = { _, item -> item.id }) { index, profile ->
          LeaderboardCard(
              position = index + ConstantLeaderboard.ListIndexOffset,
              profile = profile,
              profileRepository = profileRepository)
        }
      }
}

@Composable
private fun LeaderboardCard(
    position: Int,
    profile: UserProfile,
    profileRepository: com.android.sample.model.profile.UserProfileRepository,
) {
  Card(
      shape = RoundedCornerShape(ConstantLeaderboard.CardCornerRadius),
      border =
          BorderStroke(
              ConstantLeaderboard.CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier =
          Modifier.fillMaxWidth()
              .height(ConstantLeaderboard.CardHeight)
              .testTag(LeaderboardTestTags.getCardTag(profile.id))) {
        Row(
            modifier = Modifier.fillMaxSize().padding(ConstantLeaderboard.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically) {
              // Rank number
              Text(
                  text = "#$position",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.width(40.dp))

              Spacer(modifier = Modifier.width(ConstantLeaderboard.RowSpacing))

              // Profile picture
              ProfilePicture(
                  profileRepository = profileRepository,
                  profileId = profile.id,
                  modifier =
                      Modifier.size(ConstantLeaderboard.ProfilePictureSize)
                          .clip(RoundedCornerShape(ConstantLeaderboard.CardCornerRadius))
                          .testTag(LeaderboardTestTags.CARD_PROFILE_PICTURE))

              Spacer(modifier = Modifier.width(ConstantLeaderboard.RowSpacing))

              // Name and section
              Column(modifier = Modifier.weight(ConstantLeaderboard.WeightFill)) {
                Text(
                    text = "${profile.name} ${profile.lastName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = ConstantLeaderboard.SingleLineMax,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(LeaderboardTestTags.CARD_NAME))
                Text(
                    text = profile.section.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = appPalette().text.copy(alpha = ConstantLeaderboard.SecondaryTextAlpha),
                    modifier = Modifier.testTag(LeaderboardTestTags.CARD_SECTION))
              }

              Spacer(modifier = Modifier.width(ConstantLeaderboard.RowSpacing))

              // Stats
              StatsColumn(
                  label = "Kudos",
                  value = profile.kudos,
                  testTag = LeaderboardTestTags.CARD_KUDOS_VALUE)
              Spacer(modifier = Modifier.width(ConstantLeaderboard.RowSpacing))
              StatsColumn(
                  label = "Helped",
                  value = profile.helpReceived,
                  testTag = LeaderboardTestTags.CARD_HELP_VALUE)
            }
      }
}

@Composable
private fun StatsColumn(label: String, value: Int, testTag: String) {
  Column(
      horizontalAlignment = Alignment.End,
      modifier = Modifier.width(ConstantLeaderboard.StatsColumnWidth)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = appPalette().text.copy(alpha = ConstantLeaderboard.SecondaryTextAlpha))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(testTag))
      }
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("An error occurred") },
      text = {
        Text(message, modifier = Modifier.testTag(LeaderboardTestTags.ERROR_MESSAGE_DIALOG))
      },
      confirmButton = {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(LeaderboardTestTags.OK_BUTTON_ERROR_DIALOG)) {
              Text("OK")
            }
      })
}
