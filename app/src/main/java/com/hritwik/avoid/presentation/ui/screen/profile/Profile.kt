package com.hritwik.avoid.presentation.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hritwik.avoid.R
import com.hritwik.avoid.domain.model.library.UserData
import com.hritwik.avoid.presentation.ui.components.common.LogoutDialog
import com.hritwik.avoid.presentation.ui.components.common.SubtleShinySignature
import com.hritwik.avoid.presentation.ui.common.focusToSideNavigationOnLeftEdge
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun Profile(
    onSwitchUser: () -> Unit = {},
    authViewModel: AuthServerViewModel = hiltViewModel(),
    userDataViewModel: UserDataViewModel = hiltViewModel(),
    sideNavigationFocusRequester: FocusRequester? = null,
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val favorites by userDataViewModel.favorites.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var logoutInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(authState.authSession) {
        authState.authSession?.let { session ->
            userDataViewModel.loadFavorites(session.userId.id, session.accessToken)
            userDataViewModel.loadPlayedItems(session.userId.id)
        }
    }

    val userData = UserData(
        name = authState.authSession?.userId?.name,
        email = authState.authSession?.userId?.id,
        serverName = authState.authSession?.server?.name,
        serverUrl = authState.authSession?.server?.url,
        favoriteMovies = favorites.count { it.type == ApiConstants.ITEM_TYPE_MOVIE },
        favoriteShows = favorites.count { it.type == ApiConstants.ITEM_TYPE_SERIES },
    )

    LaunchedEffect(authState.isLoading) {
        if (authState.isLoading && showLogoutDialog) {
            logoutInProgress = true
        } else if (!authState.isLoading && logoutInProgress) {
            logoutInProgress = false
            showLogoutDialog = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = calculateRoundedValue(80).sdp)
            .focusToSideNavigationOnLeftEdge(sideNavigationFocusRequester),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box (
            modifier = Modifier.fillMaxSize().weight(1f)
        ){
            VoidTabContent(
                onSwitchUser = onSwitchUser,
                onLogoutClick = { showLogoutDialog = true }
            )
        }

        Box (
            modifier = Modifier.fillMaxSize().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            UserHeaderSection(userData = userData)
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = calculateRoundedValue(30).sdp),
                contentAlignment = Alignment.BottomCenter
            ){
                AsyncImage(
                    model = R.drawable.void_logo,
                    contentDescription = "Server Logo",
                    modifier = Modifier.size(calculateRoundedValue(120).sdp)
                )
                SubtleShinySignature(
                    modifier = Modifier
                )
            }
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                authViewModel.logout()
                authViewModel.clearError()
                userDataViewModel.clearCache()
                userDataViewModel.clearDownloads()
                userDataViewModel.clearUserData()
                showLogoutDialog = false
            },
            onDismiss = { showLogoutDialog = false },
            isLoading = logoutInProgress
        )
    }
}