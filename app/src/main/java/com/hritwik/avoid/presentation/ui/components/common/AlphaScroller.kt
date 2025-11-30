package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer
import kotlin.math.min
import com.hritwik.avoid.utils.helpers.normalizeChar

@Composable
fun AlphaScroller(
    titles: List<String>,
    gridState: LazyGridState,
    totalCount: Int = 0,
    precomputedIndexMap: Map<Char, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onActiveLetterChange: (Char?) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    
    val indexMap = remember(titles, precomputedIndexMap) {
        if (precomputedIndexMap.isNotEmpty()) {
            precomputedIndexMap
        } else {
            buildMap<Char, Int> {
                titles.forEachIndexed { index, title ->
                    
                    val firstChar = title.trim().firstOrNull()
                    val normalizedChar = normalizeChar(firstChar)

                    
                    
                    putIfAbsent(normalizedChar, index)
                }
            }
        }
    }

    val letters = remember { listOf('#') + ('A'..'Z') }

    
    val defaultActiveIndex = remember(indexMap) {
        if (indexMap.isEmpty()) {
            
            0
        } else {
            
            letters.indexOfFirst { indexMap.containsKey(it) }.takeIf { it >= 0 } ?: 0
        }
    }

    var activeLetterIndex by remember { mutableStateOf<Int?>(defaultActiveIndex) }

    
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(defaultActiveIndex) {
        activeLetterIndex = defaultActiveIndex
    }

    
    fun jumpTo(letter: Char) {
        
        scrollJob?.cancel()

        try {
            indexMap[letter]?.let { idx ->
                val bound = if (titles.isNotEmpty()) titles.size else maxOf(totalCount, idx + 1)
                
                if (idx >= 0 && idx < bound) {
                    scrollJob = scope.launch {
                        try {
                            
                            delay(500)
                            gridState.scrollToItem(idx)
                        } catch (e: Exception) {
                            
                            
                            if (e !is kotlinx.coroutines.CancellationException) {
                                println("AlphaScroller: Failed to scroll to index $idx for letter '$letter': ${e.message}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("AlphaScroller: Error in jumpTo for letter '$letter': ${e.message}")
        }
    }

    fun focusLetterAt(index: Int) {
        
        if (letters.isEmpty()) return 

        val boundedIndex = index.coerceIn(0, letters.lastIndex)
        activeLetterIndex = boundedIndex
        val letter = letters[boundedIndex]
        onActiveLetterChange(letter)
        jumpTo(letter)
    }

    fun moveToNextAvailable(step: Int): Boolean {
        
        if (indexMap.isEmpty() || letters.isEmpty()) return false 

        val currentIndex = activeLetterIndex ?: defaultActiveIndex
        var nextIndex = currentIndex + step

        while (nextIndex in letters.indices) {
            val candidate = letters[nextIndex]
            if (indexMap.containsKey(candidate)) {
                focusLetterAt(nextIndex)
                return true
            }
            nextIndex += step
        }
        return false
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            val index = activeLetterIndex ?: defaultActiveIndex
            focusLetterAt(index)
        } else {
            onActiveLetterChange(null)
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(200))
    ) {
        Surface(
            modifier = modifier
                .width(calculateRoundedValue(28).sdp)
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .clip(RoundedCornerShape(50))
                .focusable(interactionSource = interactionSource)
                .onKeyEvent {
                    if (it.type != KeyEventType.KeyDown) {
                        return@onKeyEvent false
                    }

                    when (it.key) {
                        Key.DirectionLeft -> {
                            focusManager.moveFocus(FocusDirection.Left)
                            true
                        }
                        Key.DirectionDown -> {
                            if (!moveToNextAvailable(1)) {
                                activeLetterIndex?.let { focusLetterAt(it) }
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (!moveToNextAvailable(-1)) {
                                activeLetterIndex?.let { focusLetterAt(it) }
                            }
                            true
                        }
                        else -> false
                    }
                },
            tonalElevation = calculateRoundedValue(2).sdp
        ) {
            Column(
                modifier = Modifier.padding(calculateRoundedValue(2).sdp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                letters.forEachIndexed { index, ch ->
                    val isActive = index == activeLetterIndex
                    val backgroundColor = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                    val contentColor = if (isActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(backgroundColor)
                            .padding(
                                horizontal = calculateRoundedValue(4).sdp,
                                vertical = calculateRoundedValue(1).sdp
                            )
                    ) {
                        Text(
                            text = ch.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = contentColor
                        )
                    }
                }
            }
        }

    }
}















@Composable
fun AlphaScrollerWithSections(
    sectionHeaderIndices: Map<Char, Int>,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onActiveLetterChange: (Char?) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val letters = remember { listOf('#') + ('A'..'Z') }

    
    val availableLetters = remember(sectionHeaderIndices) {
        sectionHeaderIndices.keys
    }

    
    val defaultActiveIndex = remember(availableLetters) {
        if (availableLetters.isEmpty()) {
            0 
        } else {
            
            letters.indexOfFirst { availableLetters.contains(it) }.takeIf { it >= 0 } ?: 0
        }
    }

    var activeLetterIndex by remember { mutableStateOf<Int?>(defaultActiveIndex) }

    
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(defaultActiveIndex) {
        activeLetterIndex = defaultActiveIndex
    }

    
    fun jumpToHeader(letter: Char) {
        
        scrollJob?.cancel()

        try {
            sectionHeaderIndices[letter]?.let { headerIdx ->
                
                if (headerIdx >= 0) {
                    scrollJob = scope.launch {
                        try {
                            
                            delay(500)
                            gridState.scrollToItem(headerIdx)
                        } catch (e: Exception) {
                            
                            
                            if (e !is kotlinx.coroutines.CancellationException) {
                                println("AlphaScrollerWithSections: Failed to scroll to header $headerIdx for letter '$letter': ${e.message}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("AlphaScrollerWithSections: Error in jumpToHeader for letter '$letter': ${e.message}")
        }
    }

    fun focusLetterAt(index: Int) {
        
        if (letters.isEmpty()) return 

        val boundedIndex = index.coerceIn(0, letters.lastIndex)
        activeLetterIndex = boundedIndex
        val letter = letters[boundedIndex]
        onActiveLetterChange(letter)
        jumpToHeader(letter)
    }

    fun moveToNextAvailable(step: Int): Boolean {
        
        if (availableLetters.isEmpty() || letters.isEmpty()) return false 

        val currentIndex = activeLetterIndex ?: defaultActiveIndex
        var nextIndex = currentIndex + step

        while (nextIndex in letters.indices) {
            val candidate = letters[nextIndex]
            if (availableLetters.contains(candidate)) {
                focusLetterAt(nextIndex)
                return true
            }
            nextIndex += step
        }
        return false
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            val index = activeLetterIndex ?: defaultActiveIndex
            focusLetterAt(index)
        } else {
            onActiveLetterChange(null)
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(200))
    ) {
        Surface(
            modifier = modifier
                .width(calculateRoundedValue(28).sdp)
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .clip(RoundedCornerShape(50))
                .focusable(interactionSource = interactionSource)
                .onKeyEvent {
                    if (it.type != KeyEventType.KeyDown) {
                        return@onKeyEvent false
                    }

                    when (it.key) {
                        Key.DirectionLeft -> {
                            focusManager.moveFocus(FocusDirection.Left)
                            true
                        }
                        Key.DirectionDown -> {
                            if (!moveToNextAvailable(1)) {
                                activeLetterIndex?.let { focusLetterAt(it) }
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (!moveToNextAvailable(-1)) {
                                activeLetterIndex?.let { focusLetterAt(it) }
                            }
                            true
                        }
                        else -> false
                    }
                },
            tonalElevation = calculateRoundedValue(2).sdp
        ) {
            Column(
                modifier = Modifier.padding(calculateRoundedValue(2).sdp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                letters.forEachIndexed { index, ch ->
                    val isActive = index == activeLetterIndex
                    val isAvailable = availableLetters.contains(ch)

                    
                    
                    
                    
                    val backgroundColor = if (isActive && isAvailable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }

                    val contentColor = when {
                        isActive && isAvailable -> MaterialTheme.colorScheme.onPrimary
                        isAvailable -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) 
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(backgroundColor)
                            .padding(
                                horizontal = calculateRoundedValue(4).sdp,
                                vertical = calculateRoundedValue(1).sdp
                            )
                    ) {
                        Text(
                            text = ch.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
