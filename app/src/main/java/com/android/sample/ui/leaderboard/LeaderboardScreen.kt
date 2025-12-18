package com.android.sample.ui.leaderboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileCache
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.getFilterAndSortButtonColors
import com.android.sample.ui.getTextFieldColors
import com.android.sample.ui.leaderboard.LeaderboardAddOns.crown
import com.android.sample.ui.leaderboard.LeaderboardAddOns.cutiePatootie
import com.android.sample.ui.leaderboard.LeaderboardBadgeThemes.CutieColor
import com.android.sample.ui.leaderboard.LeaderboardBadgeThemes.forRank
import com.android.sample.ui.navigation.BottomNavigationMenu
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTab
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.navigation.TopNavigationBar
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.profile.UserSections
import com.android.sample.ui.theme.appPalette
import com.android.sample.ui.utils.EnumFilterButton
import com.android.sample.ui.utils.EnumFilterPanel
import com.android.sample.ui.utils.RangeFilterButton
import com.android.sample.ui.utils.RangeFilterPanel

object LeaderBoardScreenUILabels {
  const val OFFLINE_MODE_MESSAGE = "You are in offline mode. Displaying cached profiles."
  const val NO_PROFILES_FOUND = "No profiles found."
  const val SEARCH_PLACEHOLDER = "Search by name"
  const val CLEAR_BUTTON = "Clear"
  const val KUDOS_LABEL = "Kudos"
  const val WAS_HELPED_LABEL = "Was helped"
  const val MEDAL_DESCRIPTION = "Medal"
  const val ERROR_DIALOG_TITLE = "An error occurred"
  const val OK_BUTTON = "OK"
  const val CROWN_DESCRIPTION = "Top crown"
  const val CUTIE_PATOOTIE_DESCRIPTION = "Cutie Patootie filter"
  const val PROFILE_ADDON_DESCRIPTION = "Profile add-on"
}

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
  val leaderboardViewModel =
      leaderboardViewModel
          ?: viewModel(
              factory =
                  remember(context) {
                    LeaderboardViewModelFactory(profileCache = UserProfileCache(context))
                  })

  val state by leaderboardViewModel.state.collectAsState()

  // Updated to Material 3 PullToRefresh state
  val refreshState = rememberPullToRefreshState()

  LaunchedEffect(Unit) { leaderboardViewModel.loadProfiles() }

  // Keep Lucene index in sync with loaded profiles
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
            onProfileClick = {
              val currentUserId = leaderboardViewModel.profileRepository.getCurrentUserId()
              if (currentUserId.isNotBlank()) {
                navigationActions?.navigateTo(Screen.Profile(currentUserId))
              }
            },
            navigationActions = navigationActions)
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedNavigationTab = NavigationTab.Leaderboard,
            navigationActions = navigationActions)
      }) { innerPadding ->
        state.errorMessage?.let { msg ->
          ErrorDialog(message = msg, onDismiss = { leaderboardViewModel.clearError() })
        }

        // Updated: Using Material 3 PullToRefreshBox
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { leaderboardViewModel.refresh() },
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
                      text = LeaderBoardScreenUILabels.OFFLINE_MODE_MESSAGE,
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
                        text = LeaderBoardScreenUILabels.NO_PROFILES_FOUND,
                        modifier =
                            Modifier.fillMaxSize()
                                .padding(ConstantLeaderboard.PaddingLarge)
                                .testTag(LeaderboardTestTags.EMPTY_LIST_MESSAGE),
                        textAlign = TextAlign.Center)
                  }
                  else -> {
                    LeaderboardList(
                        profiles = displayedProfiles,
                        positions = state.positions,
                        profileRepository = leaderboardViewModel.profileRepository,
                        navigationActions = navigationActions)
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
      placeholder = { Text(LeaderBoardScreenUILabels.SEARCH_PLACEHOLDER) },
      trailingIcon = {
        when {
          query.isNotEmpty() ->
              TextButton(onClick = onClearQuery) { Text(LeaderBoardScreenUILabels.CLEAR_BUTTON) }
          isSearching ->
              CircularProgressIndicator(
                  modifier = Modifier.size(ConstantLeaderboard.SmallIndicatorSize),
                  strokeWidth = ConstantLeaderboard.SmallIndicatorStroke)
        }
      },
      colors = getTextFieldColors())

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
            modifier = Modifier.height(ConstantLeaderboard.FilterButtonHeight),
        )

        facets.forEachIndexed { index, facet ->
          val selectedCount = selectedSets[index].value.size
          EnumFilterButton(
              facet = facet,
              selectedCount = selectedCount,
              onClick = {
                openRangeId = null
                openEnumId = if (openEnumId == facet.id) null else facet.id
              },
              modifier = Modifier.height(ConstantLeaderboard.FilterButtonHeight),
          )
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
                vertical = ConstantLeaderboard.SortButtonPaddingVertical),
        colors = getFilterAndSortButtonColors()) {
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
    positions: Map<String, Int>,
    profileRepository: com.android.sample.model.profile.UserProfileRepository,
    navigationActions: NavigationActions? = null
) {
  LazyColumn(
      modifier =
          Modifier.fillMaxSize()
              .padding(horizontal = ConstantLeaderboard.ListPadding)
              .testTag(LeaderboardTestTags.LEADERBOARD_LIST),
      verticalArrangement = Arrangement.spacedBy(ConstantLeaderboard.ListItemSpacing)) {
        itemsIndexed(items = profiles, key = { _, item -> item.id }) { index, profile ->
          Box(modifier = Modifier.testTag(LeaderboardTestTags.LEADERBOARD_CARD)) {
            val actualPosition =
                positions[profile.id] ?: (index + ConstantLeaderboard.ListIndexOffset)
            LeaderboardCard(
                position = actualPosition,
                profile = profile,
                profileRepository = profileRepository,
                navigationActions = navigationActions)
          }
        }
      }
}

@Composable
private fun LeaderboardCard(
    position: Int,
    profile: UserProfile,
    profileRepository: UserProfileRepository,
    navigationActions: NavigationActions? = null
) {
  val badgeTheme = forRank(position)
  val addon = resolveAddon(position, profile.id)
  val cardBorder = resolveCardBorder(badgeTheme, addon)

  var targetScale by remember { mutableFloatStateOf(ConstantLeaderboard.CardInitialSizeRatio) }
  LaunchedEffect(Unit) { targetScale = 1f }
  val scale by
      animateFloatAsState(
          targetValue = targetScale,
          animationSpec =
              spring(
                  dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))

  Card(
      shape = RoundedCornerShape(ConstantLeaderboard.CardCornerRadius),
      border = cardBorder,
      colors = CardDefaults.cardColors(containerColor = appPalette().surface),
      modifier =
          Modifier.fillMaxWidth()
              .height(ConstantLeaderboard.CardHeight)
              .testTag(LeaderboardTestTags.getCardTag(profile.id))
              .graphicsLayer(scaleX = scale, scaleY = scale)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(ConstantLeaderboard.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically) {
              PositionWithMedal(position, badgeTheme)

              Spacer(modifier = Modifier.width(ConstantLeaderboard.RowSpacing))

              ProfilePictureWithAddon(
                  profile = profile,
                  badgeTheme = badgeTheme,
                  addon = addon,
                  profileRepository = profileRepository,
                  navigationActions = navigationActions)

              Spacer(modifier = Modifier.width(ConstantLeaderboard.RowSpacing))

              Column(modifier = Modifier.weight(ConstantLeaderboard.WeightFill)) {
                // Main identity block stretches to take available horizontal space
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = ConstantLeaderboard.SingleLineMax,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(LeaderboardTestTags.CARD_NAME))
                Text(
                    text = profile.lastName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = ConstantLeaderboard.SingleLineMax,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(LeaderboardTestTags.CARD_NAME),
                    color = appPalette().onSurface)
                Text(
                    text = sectionLabel(profile.section),
                    style = MaterialTheme.typography.bodySmall,
                    color = appPalette().text.copy(alpha = ConstantLeaderboard.SecondaryTextAlpha),
                    maxLines = ConstantLeaderboard.SingleLineMax,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(LeaderboardTestTags.CARD_SECTION))
              }

              Spacer(modifier = Modifier.width(ConstantLeaderboard.RowSpacing))

              StatsColumn(
                  label = LeaderBoardScreenUILabels.KUDOS_LABEL,
                  value = profile.kudos,
                  testTag = LeaderboardTestTags.CARD_KUDOS_VALUE)
              Spacer(modifier = Modifier.width(ConstantLeaderboard.RowSpacing))
              StatsColumn(
                  label = LeaderBoardScreenUILabels.WAS_HELPED_LABEL,
                  value = profile.helpReceived,
                  testTag = LeaderboardTestTags.CARD_HELP_VALUE)
            }
      }
}

@Composable
private fun PositionWithMedal(position: Int, theme: BadgeTheme?) {
  if (theme == null) {
    // When no theme, center the position text vertically
    Box(
        modifier = Modifier.width(ConstantLeaderboard.MedalIconSize),
        contentAlignment = Alignment.Center) {
          PositionText(position, theme)
        }
  } else {
    // When theme exists, position text above medal
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(ConstantLeaderboard.MedalIconSize)) {
          PositionText(position, theme)

          Spacer(modifier = Modifier.height(ConstantLeaderboard.PaddingSmall))

          val base =
              Modifier.size(ConstantLeaderboard.MedalIconSize)
                  .clip(CircleShape)
                  .background(theme.haloColor)
          val tagged = theme.testTag?.let { base.testTag(it) } ?: base

          Box(modifier = tagged, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = theme.icon,
                contentDescription = "${LeaderBoardScreenUILabels.MEDAL_DESCRIPTION} $position",
                tint = theme.primaryColor)
          }
        }
  }
}

@Composable
private fun PositionText(position: Int, theme: BadgeTheme?) {
  Text(
      text = "#$position",
      style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
      fontWeight = FontWeight.Bold,
      color = theme?.primaryColor ?: Color.Gray,
      modifier = Modifier.testTag(LeaderboardTestTags.CARD_POSITION))
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
            modifier = Modifier.testTag(testTag),
            color = appPalette().onSurface)
      }
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(LeaderBoardScreenUILabels.ERROR_DIALOG_TITLE) },
      text = {
        Text(message, modifier = Modifier.testTag(LeaderboardTestTags.ERROR_MESSAGE_DIALOG))
      },
      confirmButton = {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(LeaderboardTestTags.OK_BUTTON_ERROR_DIALOG)) {
              Text(LeaderBoardScreenUILabels.OK_BUTTON)
            }
      })
}

@Composable
private fun ProfilePictureWithAddon(
    profile: UserProfile,
    badgeTheme: BadgeTheme?,
    addon: ProfileAddon?,
    profileRepository: com.android.sample.model.profile.UserProfileRepository,
    navigationActions: NavigationActions? = null
) {
  val crownTint = badgeTheme?.primaryColor ?: MaterialTheme.colorScheme.primary

  Box(modifier = Modifier.size(ConstantLeaderboard.ProfilePictureSize)) {
    ProfilePicture(
        profileRepository = profileRepository,
        profileId = profile.id,
        navigationActions = navigationActions,
        modifier =
            Modifier.matchParentSize()
                .clip(RoundedCornerShape(ConstantLeaderboard.CardCornerRadius))
                .testTag(LeaderboardTestTags.CARD_PROFILE_PICTURE))

    when (addon) {
      null -> Unit
      crown -> {
        Icon(
            imageVector = addon.image,
            contentDescription = LeaderBoardScreenUILabels.CROWN_DESCRIPTION,
            tint = crownTint,
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .offset(y = ConstantLeaderboard.CrownOffsetY)
                    .size(addon.size))
      }
      cutiePatootie -> {
        Icon(
            imageVector = addon.image,
            contentDescription = LeaderBoardScreenUILabels.CUTIE_PATOOTIE_DESCRIPTION,
            tint = Color.Unspecified,
            modifier =
                Modifier.matchParentSize()
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(ConstantLeaderboard.CardCornerRadius))
                    .testTag(LeaderboardTestTags.CUTIE_PATOOTIE_FILTER))
      }
      else -> {
        Icon(
            imageVector = addon.image,
            contentDescription = LeaderBoardScreenUILabels.PROFILE_ADDON_DESCRIPTION,
            tint = Color.Unspecified,
            modifier =
                Modifier.matchParentSize()
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(ConstantLeaderboard.CardCornerRadius)))
      }
    }
  }
}

private fun resolveAddon(position: Int, profileId: String): ProfileAddon? {
  return when {
    AddonEligibility.crownPositions.contains(position) -> crown
    AddonEligibility.cutiePatootieHashes.contains(hashIdSha256(profileId)) -> cutiePatootie
    else -> null
  }
}

@Composable
private fun resolveCardBorder(badgeTheme: BadgeTheme?, addon: ProfileAddon?): BorderStroke {
  return when {
    badgeTheme != null -> BorderStroke(badgeTheme.cardBorderWidth, badgeTheme.borderColor)
    addon == cutiePatootie -> BorderStroke(BadgeThemeDefaults.CardBorderWidth, CutieColor)
    else ->
        BorderStroke(ConstantLeaderboard.CardBorderWidth, MaterialTheme.colorScheme.outlineVariant)
  }
}

private fun sectionLabel(section: UserSections): String = section.label
