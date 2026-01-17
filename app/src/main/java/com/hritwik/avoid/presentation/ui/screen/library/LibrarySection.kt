package com.hritwik.avoid.presentation.ui.screen.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.presentation.ui.components.common.ScreenHeader
import com.hritwik.avoid.domain.model.library.LibraryType
import com.hritwik.avoid.presentation.ui.components.media.LibraryGridCard
import com.hritwik.avoid.presentation.ui.common.focusToSideNavigationOnLeftEdge
import com.hritwik.avoid.presentation.ui.components.common.states.EmptyState
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun LibrarySection(
    onLibraryClick: (String, String, LibraryType) -> Unit = { _, _, _ -> },
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel,
    sideNavigationFocusRequester: FocusRequester? = null,
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.libraryState.collectAsStateWithLifecycle()
    val isNetworkAvailable by userDataViewModel.isConnected.collectAsStateWithLifecycle()

    LaunchedEffect(authState.authSession) {
        val session = authState.authSession ?: return@LaunchedEffect
        libraryViewModel.loadLibraries(
            userId = session.userId.id,
            accessToken = session.accessToken,
            forceRefresh = true
        )
    }

    if (!isNetworkAvailable) {
        EmptyState(
            icon = Icons.Default.WifiOff,
            title = "Offline",
            description = "Looks like you don't have a stable internet connection."
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = calculateRoundedValue(80).sdp)
                    .focusToSideNavigationOnLeftEdge(sideNavigationFocusRequester)
            ) {
                authState.server?.name?.let {
                    ScreenHeader(
                        title = "$it's Libraries",
                        showBackButton = false,
                        modifier = Modifier.padding(top = calculateRoundedValue(8).sdp)
                    )
                }

                if (libraryState.libraries.isNotEmpty()) {
                    val firstFocusRequester = remember { FocusRequester() }

                    LaunchedEffect(Unit) {
                        firstFocusRequester.requestFocus()
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusToSideNavigationOnLeftEdge(sideNavigationFocusRequester),
                        contentPadding = PaddingValues(calculateRoundedValue(10).sdp),
                        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                    ) {
                        itemsIndexed(libraryState.libraries.chunked(5)) { rowIndex, libraryRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
                            ) {
                                libraryRow.forEachIndexed { colIndex, library ->
                                    LibraryGridCard(
                                        library = library,
                                        serverUrl = authState.authSession?.server?.url ?: "",
                                        onClick = { onLibraryClick(it.id, it.name, it.type) },
                                        modifier = Modifier.weight(1f),
                                        focusRequester = if (rowIndex == 0 && colIndex == 0) firstFocusRequester else null
                                    )
                                }

                                repeat(5 - libraryRow.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusToSideNavigationOnLeftEdge(sideNavigationFocusRequester),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LibraryAdd,
                            contentDescription = "Add LibrarySection",
                            modifier = Modifier.size(calculateRoundedValue(80).sdp)
                        )

                        Spacer(modifier = Modifier.height(calculateRoundedValue(10).sdp))

                        Text(
                            text = "No LibrarySection found!",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
        }
    }
}
