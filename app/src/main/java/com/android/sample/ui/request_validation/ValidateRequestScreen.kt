package com.android.sample.ui.request_validation

import androidx.compose.animation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.request_validation.ValidateRequestConstants.SCREEN_TITLE

/**
 * Main screen for validating and closing a request. Allows the request creator to select helpers
 * and award kudos.
 *
 * @param state The current validation state
 * @param userProfileRepository Repository for loading user profiles (for ProfilePicture)
 * @param callbacks Callbacks for user actions
 * @param modifier Modifier for styling and testing
 */
@Composable
fun ValidateRequestScreen(
    state: ValidationState,
    userProfileRepository: UserProfileRepository,
    callbacks: ValidateRequestCallbacks,
    modifier: Modifier = Modifier
) {
  // scope for launching suspend repository operations from UI events
  val coroutineScope = rememberCoroutineScope()

  // Handle success navigation
  LaunchedEffect(state) {
    if (state is ValidationState.Success) {
      callbacks.onRequestClosed()
    }
  }

  // Main scaffold
  Scaffold(
      modifier = modifier.fillMaxSize(),
      topBar = {
        ValidateRequestTopBar(
            onNavigateBack = callbacks.onNavigateBack,
            canNavigateBack = state !is ValidationState.Processing)
      }) { paddingValues ->
        // Screen content based on state
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          when (state) {
            is ValidationState.Loading -> {
              LoadingContent()
            }
            // Ready state with helpers list
            is ValidationState.Ready -> {
              ReadyContent(
                  request = state.request,
                  helpers = state.helpers,
                  selectedHelperIds = state.selectedHelperIds,
                  userProfileRepository = userProfileRepository,
                  onToggleHelper = callbacks.onToggleHelper,
                  onValidate = callbacks.onShowConfirmation)
            }
            is ValidationState.Confirming -> {
              ConfirmationDialog(
                  selectedHelpers = state.selectedHelpers,
                  kudosToAward = state.kudosToAward,
                  onConfirm = {
                    android.util.Log.d(
                        "ValidateRequestScreen",
                        "Confirm dialog clicked -> delegate close+award to ViewModel")
                    // Delegate to ViewModel / use case which runs the atomic close + awards.
                    callbacks.onConfirmAndClose()
                  },
                  onDismiss = callbacks.onCancelConfirmation)
            }
            is ValidationState.Processing -> {
              ProcessingContent()
            }
            is ValidationState.Error -> {
              ErrorContent(
                  message = state.message,
                  canRetry = state.canRetry,
                  onRetry = callbacks.onRetry,
                  onBack = callbacks.onNavigateBack)
            }
            is ValidationState.Success -> {
              // This should trigger navigation via LaunchedEffect
              SuccessContent()
            }
          }
        }
      }
}

/**
 * Top app bar for the Validate Request screen.
 *
 * @param onNavigateBack Callback to navigate back
 * @param canNavigateBack Whether navigation back is allowed
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ValidateRequestTopBar(
    onNavigateBack: () -> Unit,
    canNavigateBack: Boolean,
    modifier: Modifier = Modifier
) {
  TopAppBar(
      title = {
        Text(
            text = SCREEN_TITLE,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
      },
      navigationIcon = {
        if (canNavigateBack) {
          IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag(ValidateRequestConstants.TAG_BACK_BUTTON)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = ValidateRequestConstants.CD_GO_BACK)
              }
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface,
              titleContentColor = MaterialTheme.colorScheme.onSurface),
      modifier = modifier)
}

/**
 * Loading content shown while request and helpers are being loaded.
 *
 * @param modifier Modifier for styling
 */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ValidateRequestConstants.SPACING_XLARGE_DP.dp)) {
          CircularProgressIndicator(
              modifier = Modifier.testTag(ValidateRequestConstants.TAG_LOADING_INDICATOR))
          Text(
              text = ValidateRequestConstants.LOADING_REQUEST,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
  }
}

/**
 * Content shown in the Ready state, displaying the list of helpers and validate button.
 *
 * @param request The request being validated
 * @param helpers List of potential helpers
 * @param selectedHelperIds Set of selected helper IDs
 * @param userProfileRepository Repository for loading user profiles
 * @param onToggleHelper Callback when a helper is toggled
 * @param onValidate Callback when validate button is clicked
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadyContent(
    request: com.android.sample.model.request.Request,
    helpers: List<UserProfile>,
    selectedHelperIds: Set<String>,
    userProfileRepository: UserProfileRepository,
    onToggleHelper: (String) -> Unit,
    onValidate: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize().padding(ValidateRequestConstants.PADDING_SCREEN_DP.dp)) {
    // Header section
    Text(
        text = ValidateRequestConstants.HEADER_SELECT_HELPERS,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = ValidateRequestConstants.SPACING_HEADER_BOTTOM_DP.dp))

    Text(
        text = ValidateRequestConstants.getHeaderDescription(request.title),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier.padding(bottom = ValidateRequestConstants.SPACING_DESCRIPTION_BOTTOM_DP.dp))

    if (helpers.isEmpty()) {
      // No helpers case
      EmptyHelpersContent(onValidate = onValidate, modifier = Modifier.fillMaxSize())
    } else {
      // Helpers list
      LazyColumn(
          modifier =
              Modifier.weight(ValidateRequestConstants.LAZY_COLUMN_WEIGHT)
                  .testTag(ValidateRequestConstants.TAG_HELPERS_LIST),
          // Spacing between items
          verticalArrangement =
              Arrangement.spacedBy(ValidateRequestConstants.SPACING_LARGE_DP.dp)) {
            items(items = helpers, key = { it.id }) { helper ->
              HelperCard(
                  helper = helper,
                  isSelected = helper.id in selectedHelperIds,
                  userProfileRepository = userProfileRepository,
                  onClick = { onToggleHelper(helper.id) },
                  modifier = Modifier.animateItem())
            }
          }

      Spacer(modifier = Modifier.height(ValidateRequestConstants.SPACING_XLARGE_DP.dp))

      // Kudos summary
      KudosSummary(
          selectedCount = selectedHelperIds.size,
          modifier =
              Modifier.padding(vertical = ValidateRequestConstants.SPACING_SUMMARY_VERTICAL_DP.dp))

      // Validate button
      Button(
          onClick = onValidate,
          modifier =
              Modifier.fillMaxWidth()
                  .height(ValidateRequestConstants.BUTTON_HEIGHT_DP.dp)
                  .testTag(ValidateRequestConstants.TAG_VALIDATE_BUTTON),
          // Styling
          shape = RoundedCornerShape(ValidateRequestConstants.CORNER_RADIUS_MEDIUM_DP.dp),
          colors =
              ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            // Button content
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(ValidateRequestConstants.BUTTON_ICON_SIZE_LARGE_DP.dp))
            Spacer(modifier = Modifier.width(ValidateRequestConstants.SPACING_MEDIUM_DP.dp))
            Text(
                text = ValidateRequestConstants.BUTTON_VALIDATE,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
          }
    }
  }
}

/**
 * Content shown when there are no helpers available.
 *
 * @param onValidate Callback when validate button is clicked
 * @param modifier Modifier for styling
 */
@Composable
private fun EmptyHelpersContent(onValidate: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      // Center content
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(ValidateRequestConstants.LARGE_ICON_SIZE_DP.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(ValidateRequestConstants.SPACING_XLARGE_DP.dp))

        Text(
            text = ValidateRequestConstants.EMPTY_NO_HELPERS,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(ValidateRequestConstants.SPACING_MEDIUM_DP.dp))

        Text(
            text = ValidateRequestConstants.EMPTY_CAN_CLOSE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.padding(
                    horizontal = ValidateRequestConstants.PADDING_HORIZONTAL_LARGE_DP.dp))
        //
        Spacer(modifier = Modifier.height(ValidateRequestConstants.SPACING_XXXLARGE_DP.dp))

        Button(
            onClick = onValidate,
            modifier =
                Modifier.fillMaxWidth(ValidateRequestConstants.WIDTH_FRACTION_EMPTY_BUTTON)
                    .height(ValidateRequestConstants.BUTTON_HEIGHT_DP.dp)
                    .testTag(ValidateRequestConstants.TAG_VALIDATE_BUTTON),
            shape = RoundedCornerShape(ValidateRequestConstants.CORNER_RADIUS_MEDIUM_DP.dp)) {
              Text(
                  text = ValidateRequestConstants.BUTTON_CLOSE,
                  style = MaterialTheme.typography.titleMedium)
            }
      }
}

/**
 * Card representing a helper user profile.
 *
 * @param helper The user profile of the helper
 * @param isSelected Whether the helper is selected
 * @param userProfileRepository Repository for loading user profiles
 * @param onClick Callback when the card is clicked
 * @param modifier Modifier for styling
 */
@Composable
private fun HelperCard(
    helper: UserProfile,
    isSelected: Boolean,
    userProfileRepository: UserProfileRepository,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val borderColor by
      animateColorAsState(
          targetValue =
              if (isSelected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.outlineVariant
              },
          animationSpec = tween(ValidateRequestConstants.COLOR_ANIMATION_DURATION_MS),
          label = "borderColor")

  // Background color animation
  val backgroundColor by
      animateColorAsState(
          // Target background color based on selection
          targetValue =
              // Selected state uses primaryContainer with alpha, unselected uses surface
              if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = ValidateRequestConstants.ALPHA_SELECTED_BACKGROUND)
                // Unselected state
              } else {
                MaterialTheme.colorScheme.surface
              },
          animationSpec = tween(ValidateRequestConstants.COLOR_ANIMATION_DURATION_MS),
          label = "backgroundColor")
  // Card container
  Card(
      modifier =
          modifier.fillMaxWidth().testTag(ValidateRequestConstants.getHelperCardTag(helper.id)),
      shape = RoundedCornerShape(ValidateRequestConstants.CORNER_RADIUS_LARGE_DP.dp),
      colors = CardDefaults.cardColors(containerColor = backgroundColor),
      border =
          // Border changes based on selection
          BorderStroke(
              width =
                  if (isSelected) {
                    ValidateRequestConstants.CARD_BORDER_WIDTH_SELECTED_DP.dp
                  } else {
                    ValidateRequestConstants.CARD_BORDER_WIDTH_UNSELECTED_DP.dp
                  },
              color = borderColor)) {
        // Card content
        Row(
            modifier =
                Modifier.clickable(onClick = onClick)
                    .padding(ValidateRequestConstants.PADDING_CARD_DP.dp)
                    .fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(ValidateRequestConstants.SPACING_XLARGE_DP.dp),
            verticalAlignment = Alignment.CenterVertically) {
              // Profile picture section
              Box(modifier = Modifier.size(ValidateRequestConstants.PROFILE_PICTURE_SIZE_DP.dp)) {
                ProfilePicture(
                    profileRepository = userProfileRepository,
                    profileId = helper.id,
                    onClick = { /* no-op, handled by parent */},
                    modifier = Modifier.fillMaxSize())

                // Selection indicator - using visibility instead of animation
                if (isSelected) {
                  Box(
                      modifier =
                          Modifier.align(Alignment.BottomEnd)
                              .size(ValidateRequestConstants.SELECTION_INDICATOR_SIZE_DP.dp)
                              .background(
                                  color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                              .border(
                                  width = ValidateRequestConstants.SELECTION_INDICATOR_BORDER_DP.dp,
                                  color = MaterialTheme.colorScheme.surface,
                                  shape = CircleShape),
                      contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = ValidateRequestConstants.CD_SELECTED,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier =
                                Modifier.size(
                                    ValidateRequestConstants.SELECTION_INDICATOR_ICON_SIZE_DP.dp))
                      }
                }
              }

              // User info section
              Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement =
                      Arrangement.spacedBy(ValidateRequestConstants.SPACING_SMALL_DP.dp)) {
                    Text(
                        text = "${helper.name} ${helper.lastName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(
                        text = ValidateRequestConstants.getUserKudos(helper.kudos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }

              // Kudos badge section - using Crossfade
              Crossfade(targetState = isSelected) { selected ->
                if (selected) {
                  // Show badge when selected
                  Surface(
                      shape =
                          RoundedCornerShape(ValidateRequestConstants.CORNER_RADIUS_SMALL_DP.dp),
                      color = MaterialTheme.colorScheme.primary,
                      // Padding for badge
                      modifier =
                          Modifier.padding(
                              start = ValidateRequestConstants.PADDING_BADGE_START_DP.dp)) {
                        Row(
                            modifier =
                                Modifier.padding(
                                    horizontal =
                                        ValidateRequestConstants.PADDING_BADGE_HORIZONTAL_DP.dp,
                                    vertical =
                                        ValidateRequestConstants.PADDING_BADGE_VERTICAL_DP.dp),
                            horizontalArrangement =
                                // Spacing between text and icon
                                Arrangement.spacedBy(ValidateRequestConstants.SPACING_SMALL_DP.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                              // Kudos text and icon
                              Text(
                                  text =
                                      ValidateRequestConstants.getKudosBadge(
                                          KudosConstants.KUDOS_PER_HELPER),
                                  style = MaterialTheme.typography.labelLarge,
                                  fontWeight = FontWeight.Bold,
                                  color = MaterialTheme.colorScheme.onPrimary)
                              // Kudos star icon
                              Icon(
                                  imageVector = Icons.Default.Star,
                                  contentDescription = ValidateRequestConstants.CD_KUDOS,
                                  tint = MaterialTheme.colorScheme.onPrimary,
                                  modifier =
                                      Modifier.size(ValidateRequestConstants.KUDOS_ICON_SIZE_DP.dp))
                            }
                      }
                }
              }
            }
      }
}

/**
 * Summary card showing total kudos to be awarded based on selected helpers.
 *
 * @param selectedCount Number of selected helpers
 * @param modifier Modifier for styling
 */
@Composable
private fun KudosSummary(selectedCount: Int, modifier: Modifier = Modifier) {
  val totalKudos = selectedCount * KudosConstants.KUDOS_PER_HELPER

  AnimatedVisibility(
      visible = selectedCount > ValidateRequestConstants.VALUE_ZERO,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically(),
      modifier = modifier) {
        // Summary card
        Card(
            shape = RoundedCornerShape(ValidateRequestConstants.CORNER_RADIUS_MEDIUM_DP.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
              Row(
                  // Summary content
                  modifier =
                      Modifier.fillMaxWidth().padding(ValidateRequestConstants.PADDING_CARD_DP.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    // Left side: Total label and helper count
                    Column {
                      Text(
                          text = ValidateRequestConstants.SUMMARY_TOTAL_LABEL,
                          style = MaterialTheme.typography.labelMedium,
                          color = MaterialTheme.colorScheme.onSecondaryContainer)
                      // Helper count
                      Text(
                          text = ValidateRequestConstants.getSummaryHelperCount(selectedCount),
                          style = MaterialTheme.typography.bodySmall,
                          color =
                              MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                  alpha = ValidateRequestConstants.ALPHA_SECONDARY_TEXT))
                    }
                    // Right side: Total kudos
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(ValidateRequestConstants.SPACING_SMALL_DP.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                          Text(
                              text = "$totalKudos",
                              style = MaterialTheme.typography.headlineMedium,
                              fontWeight = FontWeight.Bold,
                              color = MaterialTheme.colorScheme.onSecondaryContainer)
                          Icon(
                              imageVector = Icons.Default.Star,
                              contentDescription = ValidateRequestConstants.CD_KUDOS,
                              tint = MaterialTheme.colorScheme.primary,
                              modifier =
                                  Modifier.size(
                                      ValidateRequestConstants.KUDOS_ICON_LARGE_SIZE_DP.dp))
                        }
                  }
            }
      }
}

/**
 * Confirmation dialog shown before finalizing the request closure.
 *
 * @param selectedHelpers List of selected helper profiles
 * @param kudosToAward Total kudos to be awarded
 * @param onConfirm Callback when confirm button is clicked
 * @param onDismiss Callback when dismiss button is clicked
 * @param modifier Modifier for styling
 */
@Composable
private fun ConfirmationDialog(
    selectedHelpers: List<UserProfile>,
    kudosToAward: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
  // Alert dialog container
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(text = ValidateRequestConstants.CONFIRM_TITLE, fontWeight = FontWeight.Bold) },
      text = {
        Column(
            verticalArrangement =
                Arrangement.spacedBy(ValidateRequestConstants.SPACING_LARGE_DP.dp)) {
              if (selectedHelpers.isEmpty()) {
                Text(ValidateRequestConstants.CONFIRM_NO_HELPERS)
                Text(
                    text = ValidateRequestConstants.CONFIRM_NO_KUDOS,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error)
              } else { // Helpers selected case
                Text(ValidateRequestConstants.CONFIRM_AWARD_TO)
                // List of selected helpers
                selectedHelpers.forEach { helper ->
                  Row(
                      horizontalArrangement =
                          Arrangement.spacedBy(ValidateRequestConstants.SPACING_MEDIUM_DP.dp),
                      verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier.size(ValidateRequestConstants.DIALOG_ICON_SIZE_DP.dp))
                        Text(
                            text = "${helper.name} ${helper.lastName}",
                            style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text =
                                "(${ValidateRequestConstants.getKudosBadge(KudosConstants.KUDOS_PER_HELPER)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                      }
                }
                // Total kudos section
                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            vertical = ValidateRequestConstants.PADDING_VERTICAL_DIALOG_DP.dp))
                // Total kudos row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      Text(
                          text = ValidateRequestConstants.CONFIRM_TOTAL_KUDOS,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold)
                      Text(
                          text = "$kudosToAward",
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary)
                    }
              }

              Text(
                  text = ValidateRequestConstants.CONFIRM_CANNOT_UNDO,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                  fontWeight = FontWeight.Medium)
            }
      },
      confirmButton = {
        Button(
            onClick = onConfirm,
            modifier = Modifier.testTag(ValidateRequestConstants.TAG_CONFIRM_BUTTON)) {
              Text(ValidateRequestConstants.BUTTON_CONFIRM)
            }
      },
      dismissButton = {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(ValidateRequestConstants.TAG_CANCEL_BUTTON)) {
              Text(ValidateRequestConstants.BUTTON_CANCEL)
            }
      },
      modifier = modifier.testTag(ValidateRequestConstants.TAG_CONFIRMATION_DIALOG))
}

/**
 * Content shown while the request is being processed.
 *
 * @param modifier Modifier for styling
 */
@Composable
private fun ProcessingContent(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ValidateRequestConstants.SPACING_XLARGE_DP.dp)) {
          CircularProgressIndicator(
              modifier = Modifier.testTag(ValidateRequestConstants.TAG_PROCESSING_INDICATOR))
          Text(
              text = ValidateRequestConstants.PROCESSING_REQUEST,
              style = MaterialTheme.typography.bodyLarge,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
              text = ValidateRequestConstants.PROCESSING_WAIT,
              style = MaterialTheme.typography.bodyMedium,
              color =
                  MaterialTheme.colorScheme.onSurfaceVariant.copy(
                      alpha = ValidateRequestConstants.ALPHA_SECONDARY_TEXT))
        }
  }
}
/**
 * Content shown when the request has been successfully closed.
 *
 * @param modifier Modifier for styling
 */
@Composable
private fun SuccessContent(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ValidateRequestConstants.SPACING_XLARGE_DP.dp)) {
          Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = ValidateRequestConstants.CD_SUCCESS,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(ValidateRequestConstants.LARGE_ICON_SIZE_DP.dp))
          Text(
              text = ValidateRequestConstants.SUCCESS_TITLE,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center)
          Text(
              text = ValidateRequestConstants.SUCCESS_SUBTITLE,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center)
        }
  }
}

/**
 * Content shown when an error occurs during validation.
 *
 * @param message The error message to display
 * @param canRetry Whether retrying is allowed
 * @param onRetry Callback when retry button is clicked
 * @param onBack Callback when back button is clicked
 * @param modifier Modifier for styling
 */
@Composable
private fun ErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
  // Error content container
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ValidateRequestConstants.SPACING_XLARGE_DP.dp),
        modifier = Modifier.padding(ValidateRequestConstants.PADDING_HORIZONTAL_LARGE_DP.dp)) {
          Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = ValidateRequestConstants.CD_ERROR,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(ValidateRequestConstants.LARGE_ICON_SIZE_DP.dp))

          Text(
              text = ValidateRequestConstants.ERROR_TITLE,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.error)

          Text(
              text = message,
              style = MaterialTheme.typography.bodyLarge,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
          // Action buttons row
          Row(
              horizontalArrangement =
                  Arrangement.spacedBy(ValidateRequestConstants.SPACING_LARGE_DP.dp),
              modifier = Modifier.padding(top = ValidateRequestConstants.SPACING_ERROR_TOP_DP.dp)) {
                if (canRetry) {
                  Button(
                      onClick = onRetry,
                      modifier = Modifier.testTag(ValidateRequestConstants.TAG_RETRY_BUTTON)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier =
                                Modifier.size(ValidateRequestConstants.BUTTON_ICON_SIZE_DP.dp))
                        Spacer(
                            modifier =
                                Modifier.width(ValidateRequestConstants.SPACING_MEDIUM_DP.dp))
                        Text(ValidateRequestConstants.BUTTON_RETRY)
                      }
                }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(ValidateRequestConstants.TAG_BACK_BUTTON)) {
                      Text(ValidateRequestConstants.BUTTON_GO_BACK)
                    }
              }
        }
  }
}
