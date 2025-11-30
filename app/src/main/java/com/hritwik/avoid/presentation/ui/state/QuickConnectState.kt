package com.hritwik.avoid.presentation.ui.state

data class QuickConnectState(
    val code: String? = null,
    val secret: String? = null,
    val isPolling: Boolean = false,
    val error: String? = null,
    val authorizationToken: String? = null,
    val authorizationUrl: String? = null,
    val shouldNavigateToAuthorization: Boolean = false,
    val authorizationInitiated: Boolean = false,
    val authorizationRequestInFlight: Boolean = false,
    val authorizationCompleted: Boolean = false,
    val authorizationAttempted: Boolean = false
)