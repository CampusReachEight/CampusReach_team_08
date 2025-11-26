package com.android.sample.ui.request_validation

/**
 * Callbacks for ValidateRequestScreen interactions. Groups all navigation and action callbacks to
 * reduce parameter count.
 */
data class ValidateRequestCallbacks(
    val onToggleHelper: (String) -> Unit,
    val onShowConfirmation: () -> Unit,
    val onCancelConfirmation: () -> Unit,
    val onConfirmAndClose: () -> Unit,
    val onRetry: () -> Unit,
    val onRequestClosed: () -> Unit,
    val onNavigateBack: () -> Unit
)
