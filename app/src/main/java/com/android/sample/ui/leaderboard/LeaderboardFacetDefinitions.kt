package com.android.sample.ui.leaderboard

import com.android.sample.model.profile.UserProfile
import com.android.sample.ui.profile.UserSections
import com.android.sample.ui.utils.EnumFacet
import com.android.sample.ui.utils.EnumFacetDefinitions
import com.android.sample.ui.utils.RangeFacet
import com.android.sample.ui.utils.RangeFilterDefinitions

/**
 * Leaderboard-specific facet and filter definitions.
 *
 * Provides:
 * - Enum facet for filtering by Section (UserSections)
 * - Range filters for Kudos and Help Received
 *
 * Usage:
 * ```
 * val sectionFacets = LeaderboardFacetDefinitions.users_all.map { EnumFacet(it) }
 * val rangeFilters = LeaderboardRangeFilters.all.map { RangeFacet(it) }
 * ```
 */
object LeaderboardFacetDefinitions {

  /** All enum facet definitions for UserProfile filtering. */
  val users_all: List<EnumFacetDefinitions.FacetDefinition<UserProfile>> =
      listOf(
          EnumFacetDefinitions.FacetDefinition(
              id = "section",
              title = "Section",
              values = UserSections.entries.filter { it != UserSections.NONE },
              extract = { listOf(it.section) },
              dropdownButtonTag = LeaderboardTestTags.SECTION_FILTER_DROPDOWN_BUTTON,
              searchBarTag = LeaderboardTestTags.SECTION_FILTER_SEARCH_BAR,
              rowTagOf = { v -> LeaderboardTestTags.getSectionFilterTag(v as UserSections) },
              labelOf = { (it as UserSections).label },
          ),
      )
}

/**
 * Range filter definitions for numeric UserProfile fields.
 *
 * These filters allow users to filter the leaderboard by kudos and help received ranges. The bounds
 * are set to reasonable maximums that should cover most use cases; the UI will dynamically show the
 * current data range.
 */
object LeaderboardRangeFilters {

  /** Default maximum bound for kudos range filter. */
  const val DEFAULT_MAX_KUDOS = 50

  /** Default maximum bound for help received range filter. */
  const val DEFAULT_MAX_HELP = 50

  /** Step increment for range sliders. */
  const val RANGE_STEP = 1

  /** Minimum bound shared by leaderboard numeric filters. */
  const val RANGE_MIN_BOUND = 0

  /** Kudos range filter definition. */
  val kudosFilter =
      RangeFilterDefinitions.RangeFilterDefinition<UserProfile>(
          id = "kudos",
          title = "Kudos",
          minBound = RANGE_MIN_BOUND,
          maxBound = DEFAULT_MAX_KUDOS,
          step = RANGE_STEP,
          extract = { it.kudos },
          buttonTestTag = LeaderboardTestTags.KUDOS_RANGE_BUTTON,
          panelTestTag = LeaderboardTestTags.KUDOS_RANGE_PANEL,
          sliderTestTag = LeaderboardTestTags.KUDOS_RANGE_SLIDER,
          minFieldTestTag = LeaderboardTestTags.KUDOS_RANGE_MIN_FIELD,
          maxFieldTestTag = LeaderboardTestTags.KUDOS_RANGE_MAX_FIELD,
      )

  /** Help received range filter definition. */
  val helpReceivedFilter =
      RangeFilterDefinitions.RangeFilterDefinition<UserProfile>(
          id = "helpReceived",
          title = "Help Received",
          minBound = RANGE_MIN_BOUND,
          maxBound = DEFAULT_MAX_HELP,
          step = RANGE_STEP,
          extract = { it.helpReceived },
          buttonTestTag = LeaderboardTestTags.HELP_RANGE_BUTTON,
          panelTestTag = LeaderboardTestTags.HELP_RANGE_PANEL,
          sliderTestTag = LeaderboardTestTags.HELP_RANGE_SLIDER,
          minFieldTestTag = LeaderboardTestTags.HELP_RANGE_MIN_FIELD,
          maxFieldTestTag = LeaderboardTestTags.HELP_RANGE_MAX_FIELD,
      )

  /** All range filter definitions for the leaderboard. */
  val all: List<RangeFilterDefinitions.RangeFilterDefinition<UserProfile>> =
      listOf(kudosFilter, helpReceivedFilter)
}

/** Type alias for UserProfile-specific EnumFacet. */
typealias LeaderboardFacet = EnumFacet<UserProfile>

/** Type alias for UserProfile-specific RangeFacet. */
typealias LeaderboardRangeFacet = RangeFacet<UserProfile>
