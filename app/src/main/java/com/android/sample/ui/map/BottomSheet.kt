package com.android.sample.ui.map

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.sample.model.request.Request
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.theme.AppPalette


/**
 * A bottom sheet component with drag-to-resize functionality and smooth animations.
 *
 * @param viewModel The MapViewModel to handle state updates
 * @param appPalette The color palette for theming
 * @param modifier Optional modifier for customization
 * @param content The content to display inside the bottom sheet
 */
@Composable
fun AnimatedBottomSheet(
    viewModel: MapViewModel,
    appPalette: AppPalette,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val minHeight = ConstantMap.MIN_HEIGHT

    val density = LocalDensity.current
    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val minHeightPx = with(density) { minHeight.toPx() }
    val thirdOfScreen = screenHeight * ConstantMap.PROPORTION_FOR_INITIALIZE_SHEET
    var offsetY by remember { mutableFloatStateOf(thirdOfScreen) }

    val currentHeight by
    animateDpAsState(
        targetValue =
            with(density) {
                (screenHeight -
                        offsetY.coerceIn(ConstantMap.MIN_OFFSET_Y, screenHeight - minHeightPx))
                    .toDp()
            },
        animationSpec = tween(durationMillis = ConstantMap.DURATION_ANIMATION))

    Box(modifier = modifier.fillMaxWidth().testTag(MapTestTags.DRAG_DOWN_MENU)) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(currentHeight),
            shape =
                RoundedCornerShape(
                    topStart = ConstantMap.BOTTOM_SHEET_SHAPE, topEnd = ConstantMap.BOTTOM_SHEET_SHAPE),
            color = appPalette.primary,
            shadowElevation = ConstantMap.BOTTOM_SHEET_ELEVATION) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag Handle + Button
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Drag Handle
                    Box(
                        modifier =
                            Modifier.width(ConstantMap.DRAG_HANDLE_WIDTH)
                                .height(ConstantMap.DRAG_HANDLE_HEIGHT)
                                .background(
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = ConstantMap.DRAG_HANDLE_ALPHA),
                                    shape = RoundedCornerShape(ConstantMap.DRAG_HANDLE_CORNER_RADIUS))
                                .align(Alignment.Center)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { _, dragAmount ->
                                            offsetY =
                                                (offsetY + dragAmount).coerceIn(
                                                    ConstantMap.MIN_OFFSET_Y, screenHeight - minHeightPx)
                                        })
                                }
                                .testTag(MapTestTags.DRAG))

                    // Button X
                    IconButton(
                        onClick = { viewModel.updateNoRequests() },
                        modifier = Modifier.align(Alignment.CenterEnd).testTag(MapTestTags.BUTTON_X)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = ConstantMap.CLOSE_BUTTON_DESCRIPTION,
                            tint =
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = ConstantMap.CLOSE_BUTTON_ALPHA))
                    }
                }

                content()
            }
        }
    }
}

/**
 * Displays an animated bottom sheet with detailed information about the selected request. Contains
 * tabs for request details and profile information.
 *
 * @param uiState Current UI state containing the selected request
 * @param viewModel ViewModel for map operations
 * @param navigationActions Navigation actions for screen transitions
 * @param appPalette Color palette for styling
 * @param modifier Modifier for positioning the sheet
 */
@Composable
fun CurrentRequestBottomSheet(
    uiState: MapUIState,
    viewModel: MapViewModel,
    navigationActions: NavigationActions?,
    appPalette: AppPalette,
    modifier: Modifier = Modifier
) {
    uiState.currentRequest?.let { req ->
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf(ConstantMap.DETAILS, ConstantMap.PROFILE)

        AnimatedBottomSheet(viewModel = viewModel, appPalette = appPalette, modifier = modifier) {

            // Tab Row
            BottomSheetTabRow(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                appPalette = appPalette)

            HorizontalDivider(
                thickness = ConstantMap.DIVIDER_THICKNESS,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = ConstantMap.ALPHA_DIVIDER))

            // Tab Content
            BottomSheetContent(
                selectedTab = selectedTab,
                request = req,
                uiState = uiState,
                navigationActions = navigationActions,
                viewModel = viewModel,
                appPalette = appPalette)
        }
    }
}


/**
 * Displays a scrollable row of tabs for switching between different views in the bottom sheet.
 *
 * @param tabs List of tab titles (e.g., ["Details", "Profile"])
 * @param selectedTab Index of the currently selected tab
 * @param onTabSelected Callback when a tab is selected
 * @param appPalette Color palette for styling
 */
@Composable
private fun BottomSheetTabRow(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    appPalette: AppPalette
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        modifier = Modifier.fillMaxWidth(),
        edgePadding = ConstantMap.TAB_ROW_EDGE_PADDING,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = appPalette.accent)
        },
        divider = {}) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color =
                            if (selectedTab == index) appPalette.accent
                            else
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = ConstantMap.ALPHA_TEXT_UNSELECTED))
                },
                modifier = Modifier.testTag(MapTestTags.testTagForTab(title)))
        }
    }
}

/**
 * Displays the content of the bottom sheet based on the selected tab.
 *
 * @param selectedTab The index of the currently selected tab (0 = Details, 1 = Profile)
 * @param request The request being displayed
 * @param uiState Current UI state containing ownership and profile info
 * @param navigationActions Actions for navigating between screens
 * @param viewModel ViewModel for map operations
 * @param appPalette Color palette for styling
 */
@Composable
private fun ColumnScope.BottomSheetContent(
    selectedTab: Int,
    request: Request,
    uiState: MapUIState,
    navigationActions: NavigationActions?,
    viewModel: MapViewModel,
    appPalette: AppPalette
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .weight(ConstantMap.WEIGHT_FILL)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = ConstantMap.PADDING_HORIZONTAL_STANDARD,
                    vertical = ConstantMap.PADDING_STANDARD)) {
        when (selectedTab) {
            0 -> { // Details
                RequestDetailsTab(
                    request = request,
                    uiState = uiState,
                    navigationActions = navigationActions,
                    viewModel = viewModel,
                    appPalette = appPalette)
            }
            1 -> { // Profile
                CurrentProfileUI(
                    uiState.isOwner,
                    navigationActions,
                    uiState.currentProfile,
                    viewModel,
                    this,
                    request,
                    appPalette)
            }
        }
    }
}
