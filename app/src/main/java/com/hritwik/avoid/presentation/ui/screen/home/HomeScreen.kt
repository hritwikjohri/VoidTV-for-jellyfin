package com.hritwik.avoid.presentation.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel

@Composable
fun HomeScreen(
    onMediaItemClick: (MediaItem) -> Unit = {},
    onMediaItemFocus: (MediaItem) -> Unit = {},
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel,
    userDataViewModel: UserDataViewModel = hiltViewModel(),
    sideNavigationFocusRequester: FocusRequester
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.libraryState.collectAsStateWithLifecycle()

    var featured by remember { mutableStateOf<MediaItem?>(null) }

    
    
    val isLoading by remember { derivedStateOf { libraryState.isLoading } }
    val libraries by remember { derivedStateOf { libraryState.libraries } }
    val latestItems by remember { derivedStateOf { libraryState.latestItems } }

    val featureItems by remember {
        derivedStateOf {
            latestItems
                .filter { it.type == "Movie" || it.type == "Series" }
                .take(5)
        }
    }

    val sessionUserId = authState.authSession?.userId?.id
    val sessionAccessToken = authState.authSession?.accessToken

    LaunchedEffect(sessionUserId, sessionAccessToken) {
        val session = authState.authSession
        if (session == null || sessionUserId == null || sessionAccessToken == null) return@LaunchedEffect

        with(libraryViewModel) {
            
            loadLibraries(sessionUserId, sessionAccessToken, refreshResume = true)
            
        }

        with(userDataViewModel) {
            loadFavorites(sessionUserId, sessionAccessToken)
            loadPlayedItems(sessionUserId)
        }
    }

    if(isLoading){
        LoadingState()
    } else if(libraries.isNotEmpty()){
        FeatureHeader(
            items = featureItems,
            selected = featured,
            serverUrl = authState.authSession?.server?.url ?: "",
            onItemSelected = { item ->
                featured = item
                onMediaItemClick(item)
            },
            onItemFocused = { item ->
                featured = item
                onMediaItemFocus(item)
            },
            onMediaItemClick = onMediaItemClick,
            onMediaItemFocus = onMediaItemFocus,
            showThumbnails = false,
            authViewModel = authViewModel,
            libraryViewModel = libraryViewModel,
            sideNavigationFocusRequester = sideNavigationFocusRequester
        )
    } else {
        EmptyHomeState()
    }
}

@Composable
private fun EmptyHomeState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No content available",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}
