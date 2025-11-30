package com.hritwik.avoid.presentation.ui.screen.search

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.core.content.ContextCompat
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.input.RecentSearches
import com.hritwik.avoid.presentation.ui.components.input.SearchBar
import com.hritwik.avoid.presentation.ui.components.input.TvKeyboard
import com.hritwik.avoid.presentation.ui.components.search.SearchResultRow
import com.hritwik.avoid.presentation.ui.common.focusToSideNavigationOnLeftEdge
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.presentation.ui.state.SearchCategory
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.search.SearchViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import java.util.Locale

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Search(
    onMediaItemClick: (MediaItem) -> Unit = {},
    authViewModel: AuthServerViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    sideNavigationFocusRequester: FocusRequester? = null,
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val searchState by searchViewModel.searchState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val searchResults = searchViewModel.searchResults.collectAsLazyPagingItems()
    val searchBarFocusRequester = remember { FocusRequester() }
    val keyboardFocusRequester = remember { FocusRequester() }
    val filtersFocusRequester = remember { FocusRequester() }
    val resultsFocusRequester = remember { FocusRequester() }
    val micFocusRequester = remember { FocusRequester() }
    val clearFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingStartListening by remember { mutableStateOf(false) }
    val currentAuthState = rememberUpdatedState(authState)
    val currentSearchViewModel = rememberUpdatedState(searchViewModel)
    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    ?: SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        } else {
            null
        }
    }

    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                val transcript = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!transcript.isNullOrBlank()) {
                    currentSearchViewModel.value.updateSearchQuery(transcript)
                    if (transcript.length >= 2) {
                        currentAuthState.value.authSession?.let { session ->
                            currentSearchViewModel.value.fetchSearchSuggestions(
                                query = transcript,
                                userId = session.userId.id,
                                accessToken = session.accessToken
                            )
                        }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val transcript = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!transcript.isNullOrBlank()) {
                    currentSearchViewModel.value.updateSearchQuery(transcript)
                    currentSearchViewModel.value.performImmediateSearch(transcript)
                }
                isListening = false
            }

            override fun onError(error: Int) {
                Log.w("SearchVoice", "Speech recognition error: $error")
                isListening = false
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
    }

    val toggleSpeechRecognition: () -> Unit = remember(speechRecognizer, recognitionIntent, isListening, hasRecordAudioPermission) {
        {
            val recognizer = speechRecognizer
            if (recognizer == null) {
                Log.w("SearchVoice", "Speech recognition not supported on this device.")
            } else if (!hasRecordAudioPermission) {
                Log.w("SearchVoice", "RECORD_AUDIO permission not granted.")
            } else {
                try {
                    if (isListening) {
                        recognizer.stopListening()
                        recognizer.cancel()
                        isListening = false
                    } else {
                        recognizer.startListening(recognitionIntent)
                        isListening = true
                    }
                } catch (securityException: SecurityException) {
                    Log.e("SearchVoice", "Missing RECORD_AUDIO permission.", securityException)
                    isListening = false
                } catch (illegalState: IllegalStateException) {
                    Log.e("SearchVoice", "Speech recognizer is not ready.", illegalState)
                    recognizer.cancel()
                    isListening = false
                }
            }
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordAudioPermission = granted
        val shouldTrigger = granted && pendingStartListening
        if (!granted && pendingStartListening) {
            Toast.makeText(
                context,
                "Microphone permission is required for voice search.",
                Toast.LENGTH_SHORT
            ).show()
        }
        pendingStartListening = false
        if (shouldTrigger) {
            toggleSpeechRecognition()
        }
    }

    LaunchedEffect(Unit) {
        keyboardFocusRequester.requestFocus()
    }

    val handleMicClick: () -> Unit = {
        if (hasRecordAudioPermission) {
            toggleSpeechRecognition()
        } else {
            pendingStartListening = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Row (
        modifier = Modifier
            .fillMaxSize()
            .padding(start = calculateRoundedValue(80).sdp)
            .padding(calculateRoundedValue(16).sdp)
            .focusToSideNavigationOnLeftEdge(sideNavigationFocusRequester),
        verticalAlignment = Alignment.Top
    ) {
        Column (
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.3f)
        ){
            SearchBar(
                query = searchState.searchQuery,
                onSearch = { query ->
                    searchViewModel.performImmediateSearch(query)
                },
                onClear = {
                    searchViewModel.clearSearch()
                    focusManager.clearFocus()
                },
                focusRequester = searchBarFocusRequester,
                clearFocusRequester = clearFocusRequester,
                downFocusRequester = keyboardFocusRequester,
                onQueryChange = { query ->
                    searchViewModel.updateSearchQuery(query)
                    if (query.length >= 2) {
                        authState.authSession?.let { session ->
                            searchViewModel.fetchSearchSuggestions(
                                query = query,
                                userId = session.userId.id,
                                accessToken = session.accessToken
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties {
                        down = keyboardFocusRequester
                    }
            )

            TvKeyboard(
                onKeyPress = { char ->
                    val newQuery = when (char) {
                        '\b' -> searchState.searchQuery.dropLast(1)
                        '\n' -> searchState.searchQuery
                        else -> searchState.searchQuery + char
                    }
                    searchViewModel.updateSearchQuery(newQuery)
                },
                firstKeyFocusRequester = keyboardFocusRequester,
                upFocusRequester = searchBarFocusRequester,
                modifier = Modifier.focusProperties {
                    up = searchBarFocusRequester
                    down = micFocusRequester
                    right = resultsFocusRequester
                },
                onClear = {
                    searchViewModel.clearSearch()
                    focusManager.clearFocus()
                }
            )

            VoiceSearchButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                isListening = isListening,
                onMicClick = handleMicClick,
                focusRequester = micFocusRequester,
                upFocusRequester = keyboardFocusRequester,
                leftFocusRequester = searchBarFocusRequester,
                downFocusRequester = filtersFocusRequester
            )

            if (!searchState.isSearchActive && searchState.recentSearches.isNotEmpty()) {
                RecentSearches(
                    modifier = Modifier.padding(calculateRoundedValue(12).sdp),
                    recentSearches = searchState.recentSearches,
                    onSearchClick = { query ->
                        searchViewModel.updateSearchQuery(query)
                        searchViewModel.performImmediateSearch(query)
                    },
                    onClearAll = {
                        searchViewModel.clearRecentSearches()
                    }
                )
            }
        }

        Column (
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.7f)
        ) {
            val selectedTabIndex = SearchCategory.entries.indexOf(searchState.selectedCategory)

            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .focusRequester(filtersFocusRequester)
                    .focusProperties {
                        up = keyboardFocusRequester
                        right = resultsFocusRequester
                    },
                containerColor = Color.Transparent,
                contentColor = PrimaryText,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex)
                    )
                },
                divider = { null }
            ) {
                SearchCategory.entries.forEach { category ->
                    Tab(
                        selected = searchState.selectedCategory == category,
                        onClick = { searchViewModel.updateSelectedCategory(category) },
                        text = { Text(category.label()) }
                    )
                }
            }

            when {
                searchState.isSearchActive && searchResults.loadState.refresh is LoadState.Loading -> {
                    LoadingContent()
                }

                searchState.isSearchActive && searchState.suggestions.isNotEmpty() && searchResults.itemCount == 0 -> {
                    SuggestionsContent(
                        suggestions = searchState.suggestions,
                        onSuggestionClick = { suggestion ->
                            searchViewModel.updateSearchQuery(suggestion)
                            searchViewModel.performImmediateSearch(suggestion)
                        },
                        firstItemFocusRequester = resultsFocusRequester,
                        leftFocusRequester = keyboardFocusRequester,
                        upFocusRequester = searchBarFocusRequester
                    )
                }

                searchState.isSearchActive && searchResults.itemCount == 0 && searchResults.loadState.refresh !is LoadState.Loading -> {
                    EmptySearchResults(
                        query = searchState.searchQuery
                    )
                }

                searchResults.itemCount > 0 -> {
                    SearchResultsGrid(
                        searchResults = searchResults,
                        searchQuery = searchState.searchQuery,
                        serverUrl = authState.authSession?.server?.url.orEmpty(),
                        onMediaItemClick = onMediaItemClick,
                        firstItemFocusRequester = resultsFocusRequester,
                        leftFocusRequester = keyboardFocusRequester,
                        upFocusRequester = searchBarFocusRequester
                    )
                }

                else -> {
                    WelcomeContent()
                }
            }
        }
    }
}

@Composable
private fun VoiceSearchButton(
    modifier: Modifier = Modifier,
    isListening: Boolean,
    onMicClick: () -> Unit,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
) {
    var isFocused by remember { mutableStateOf(false) }

    val surfaceColor = if (isListening) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isListening || isFocused) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .padding(top = calculateRoundedValue(8).sdp)
            .height(calculateRoundedValue(40).sdp)
            .defaultMinSize(minWidth = calculateRoundedValue(150).sdp)
            .dpadNavigation(
                shape = RoundedCornerShape(calculateRoundedValue(6).sdp),
                focusRequester = focusRequester,
                onFocusChange = { focused -> isFocused = focused },
                onClick = onMicClick,
                onMoveFocus = { direction ->
                    when (direction) {
                        FocusDirection.Up -> {
                            upFocusRequester.requestFocus()
                            true
                        }
                        FocusDirection.Down -> {
                            downFocusRequester.requestFocus()
                            true
                        }
                        FocusDirection.Left -> {
                            leftFocusRequester.requestFocus()
                            true
                        }
                        else -> false
                    }
                }
            ),
        shape = RoundedCornerShape(calculateRoundedValue(6).sdp),
        color = surfaceColor,
        border = BorderStroke(
            width = calculateRoundedValue(1).sdp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = calculateRoundedValue(12).sdp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Stop voice input" else "Start voice input",
                tint = contentColor,
                modifier = Modifier.size(calculateRoundedValue(20).sdp)
            )
            Text(
                text = if (isListening) "Listening…" else "Voice Search",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

private fun SearchCategory.label(): String = when (this) {
    SearchCategory.TopResults -> "Top Results"
    SearchCategory.Movies -> "Movies"
    SearchCategory.Shows -> "Show"
    SearchCategory.Episodes -> "Episode"
}

@Composable
private fun SearchResultsGrid(
    searchResults: LazyPagingItems<MediaItem>,
    searchQuery: String,
    serverUrl: String,
    onMediaItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "${searchResults.itemCount} result${if (searchResults.itemCount != 1) "s" else ""} for \"$searchQuery\"",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = PrimaryText,
            modifier = Modifier.padding(calculateRoundedValue(12).sdp)
        )

        Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(calculateRoundedValue(4).sdp),
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
        ) {
            items(
                count = searchResults.itemCount,
                key = { index -> searchResults[index]?.id ?: index }
            ) { index ->
                searchResults[index]?.let { mediaItem ->
                    SearchResultRow(
                        mediaItem = mediaItem,
                        serverUrl = serverUrl,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onMediaItemClick,
                        focusRequester = if (index == 0) firstItemFocusRequester else null,
                        leftFocusRequester = if (index == 0) leftFocusRequester else null,
                        upFocusRequester = if (index == 0) upFocusRequester else null
                    )
                }
            }

            if (searchResults.loadState.append is LoadState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(calculateRoundedValue(16).sdp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(calculateRoundedValue(24).sdp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(calculateRoundedValue(24).sdp)
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))
            Text(
                text = "Searching...",
                style = MaterialTheme.typography.bodyLarge,
                color = PrimaryText
            )
        }
    }
}

@Composable
private fun SuggestionsContent(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(calculateRoundedValue(4).sdp),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
    ) {
        itemsIndexed(suggestions) { index, suggestion ->
            val focusModifier = if (index == 0 && firstItemFocusRequester != null) {
                Modifier
                    .focusRequester(firstItemFocusRequester)
                    .focusProperties {
                        if (leftFocusRequester != null) {
                            left = leftFocusRequester
                        }
                        if (upFocusRequester != null) {
                            up = upFocusRequester
                        }
                    }
            } else {
                Modifier
            }
            Text(
                text = suggestion,
                color = PrimaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .dpadNavigation(
                        shape = RoundedCornerShape(calculateRoundedValue(12).sdp),
                        onClick = { onSuggestionClick(suggestion) }
                    )
                    .padding(
                        horizontal = calculateRoundedValue(16).sdp,
                        vertical = calculateRoundedValue(12).sdp
                    ),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun WelcomeContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(calculateRoundedValue(24).sdp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(calculateRoundedValue(48).sdp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))
            Text(
                text = "Start typing to search",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))
            Text(
                text = "Use the on-screen keyboard or your remote",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
