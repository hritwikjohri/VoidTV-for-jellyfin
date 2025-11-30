package com.hritwik.avoid.presentation.ui.components.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.utils.constants.AppConstants.DEFAULT_NAVIGATION_INDEX
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun SideNavigationBar(
    modifier: Modifier = Modifier,
    selectedItem: Int?,
    onItemSelected: (Int) -> Unit = {},
    focusRequester: FocusRequester
) {
    val mainNavItems = remember {
        listOf(
            NavItem(
                label = R.string.search,
                icon = Icons.Default.Search,
                route = "search",
                customIcon = R.drawable.search
            ),
            NavItem(
                label = R.string.nav_home,
                icon = Icons.Default.Home,
                route = "home",
                customIcon = R.drawable.home
            ),
            NavItem(
                label = R.string.nav_Movies,
                icon = Icons.Default.Movie,
                route = "movies",
                customIcon = R.drawable.movies
            ),
            NavItem(
                label = R.string.nav_TVShow,
                icon = Icons.Default.Tv,
                route = "shows",
                customIcon = R.drawable.shows
            ),
            NavItem(
                label = R.string.nav_library,
                icon = Icons.Default.VideoLibrary,
                route = "library",
                customIcon = R.drawable.library
            )
        )
    }

    val profileItem = remember {
        NavItem(
            label = R.string.bottom_nav_profile,
            icon = Icons.Default.Person,
            route = "Profile",
            customIcon = R.drawable.void_personalize
        )
    }

    val allNavItems = remember { mainNavItems + profileItem }

    val focusManager = LocalFocusManager.current
    val itemFocusRequesters = remember { List(allNavItems.size) { FocusRequester() } }

    var focusedItemIndex by remember { mutableIntStateOf(-1) }
    var pendingFocusIndex by rememberSaveable {
        mutableIntStateOf(selectedItem ?: DEFAULT_NAVIGATION_INDEX)
    }
    var lastSelectedIndex by rememberSaveable {
        mutableIntStateOf(selectedItem ?: DEFAULT_NAVIGATION_INDEX)
    }
    var restoreFocusOnNextGain by remember {
        mutableStateOf(false)
    }

    val safeIndex = pendingFocusIndex.coerceIn(0, allNavItems.lastIndex)
    if (safeIndex != pendingFocusIndex) {
        pendingFocusIndex = safeIndex
    }

    val navigationHasActiveFocus = focusedItemIndex != -1

    LaunchedEffect(selectedItem, navigationHasActiveFocus) {
        selectedItem?.let { currentSelection ->
            lastSelectedIndex = currentSelection
            if (!navigationHasActiveFocus) {
                pendingFocusIndex = currentSelection
            }
        }
    }

    LaunchedEffect(navigationHasActiveFocus) {
        if (!navigationHasActiveFocus) {
            pendingFocusIndex = lastSelectedIndex
            restoreFocusOnNextGain = true
        }
    }

    LaunchedEffect(navigationHasActiveFocus, pendingFocusIndex) {
        if (navigationHasActiveFocus) {
            itemFocusRequesters.getOrNull(pendingFocusIndex)?.requestFocus()
        }
    }

    fun requestItemFocus(
        targetIndex: Int,
        shouldNavigate: Boolean,
        forceNavigate: Boolean = false,
        ensureFocus: Boolean = navigationHasActiveFocus
    ): Boolean {
        if (targetIndex !in allNavItems.indices) {
            return false
        }

        val resolvedIndex = targetIndex.coerceIn(0, allNavItems.lastIndex)
        pendingFocusIndex = resolvedIndex

        if (shouldNavigate) {
            lastSelectedIndex = resolvedIndex
        }

        if (ensureFocus) {
            focusedItemIndex = resolvedIndex
            itemFocusRequesters[resolvedIndex].requestFocus()
        }

        if (shouldNavigate && (forceNavigate || selectedItem != resolvedIndex)) {
            onItemSelected(resolvedIndex)
        }

        return true
    }

    LaunchedEffect(navigationHasActiveFocus, restoreFocusOnNextGain, lastSelectedIndex) {
        if (navigationHasActiveFocus && restoreFocusOnNextGain) {
            requestItemFocus(
                targetIndex = lastSelectedIndex,
                shouldNavigate = false,
                ensureFocus = true
            )
            restoreFocusOnNextGain = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(calculateRoundedValue(80).sdp)
            .focusGroup()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = calculateRoundedValue(16).sdp)
                    .padding(bottom = calculateRoundedValue(24).sdp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = R.drawable.void_icon,
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier.size(calculateRoundedValue(80).sdp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                mainNavItems.forEachIndexed { index, item ->
                    val isFocused = focusedItemIndex == index
                    val isSelected = selectedItem == index

                    SideNavigationItem(
                        item = item,
                        isFocused = isFocused,
                        isSelected = isSelected,
                        modifier = Modifier
                            .focusRequester(itemFocusRequesters[index])
                            .let { baseModifier ->
                                if (index == pendingFocusIndex) {
                                    baseModifier.focusRequester(focusRequester)
                                } else {
                                    baseModifier
                                }
                            },
                        onFocus = { pendingFocusIndex = index },
                        onFocusStateChanged = { hasFocus ->
                            if (hasFocus) {
                                focusedItemIndex = index
                            } else if (focusedItemIndex == index) {
                                focusedItemIndex = -1
                            }
                        },
                        onActivate = {
                            requestItemFocus(
                                index,
                                shouldNavigate = true,
                                forceNavigate = true,
                                ensureFocus = true
                            )
                        },
                        onNavigateUp = {
                            if (index > 0) {
                                requestItemFocus(
                                    index - 1,
                                    shouldNavigate = false,
                                    ensureFocus = true
                                )
                            } else {
                                false
                            }
                        },
                        onNavigateDown = {
                            if (index < mainNavItems.lastIndex) {
                                requestItemFocus(
                                    index + 1,
                                    shouldNavigate = false,
                                    ensureFocus = true
                                )
                            } else {
                                requestItemFocus(
                                    allNavItems.lastIndex,
                                    shouldNavigate = false,
                                    ensureFocus = true
                                )
                            }
                        },
                        onMoveRight = {
                            focusManager.moveFocus(FocusDirection.Right)
                        }
                    )

                    if (index < mainNavItems.lastIndex) {
                        Spacer(modifier = Modifier.height(calculateRoundedValue(4).sdp))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(vertical = calculateRoundedValue(16).sdp),
                contentAlignment = Alignment.Center
            ) {
                val profileIndex = allNavItems.lastIndex
                val isFocused = focusedItemIndex == profileIndex
                val isSelected = selectedItem == profileIndex

                SideNavigationItem(
                    item = profileItem,
                    isFocused = isFocused,
                    isSelected = isSelected,
                    modifier = Modifier
                        .focusRequester(itemFocusRequesters[profileIndex])
                        .let { baseModifier ->
                            if (profileIndex == pendingFocusIndex) {
                                baseModifier.focusRequester(focusRequester)
                            } else {
                                baseModifier
                            }
                        },
                    onFocus = { pendingFocusIndex = profileIndex },
                    onFocusStateChanged = { hasFocus ->
                        if (hasFocus) {
                            focusedItemIndex = profileIndex
                        } else if (focusedItemIndex == profileIndex) {
                            focusedItemIndex = -1
                        }
                    },
                    onActivate = {
                        requestItemFocus(
                            profileIndex,
                            shouldNavigate = true,
                            forceNavigate = true,
                            ensureFocus = true
                        )
                    },
                    onNavigateUp = {
                        requestItemFocus(
                            mainNavItems.lastIndex,
                            shouldNavigate = false,
                            ensureFocus = true
                        )
                    },
                    onNavigateDown = {
                        false
                    },
                    onMoveRight = {
                        focusManager.moveFocus(FocusDirection.Right)
                    }
                )
            }
        }
    }
}

@Composable
private fun SideNavigationItem(
    item: NavItem,
    isFocused: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit,
    onFocusStateChanged: (Boolean) -> Unit,
    onActivate: () -> Unit,
    onNavigateUp: () -> Boolean,
    onNavigateDown: () -> Boolean,
    onMoveRight: () -> Boolean
) {
    var hasFocus by remember { mutableStateOf(false) }
    val isActive = hasFocus || isSelected

    val animatedScale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = tween(200),
        label = "nav_item_scale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.6f,
        animationSpec = tween(200),
        label = "nav_item_alpha"
    )

    val iconSize by animateDpAsState(
        targetValue = if (isActive) {
            calculateRoundedValue(28).sdp
        } else {
            calculateRoundedValue(24).sdp
        },
        animationSpec = tween(200),
        label = "nav_item_icon_size"
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = when {
            hasFocus -> 0.25f
            isSelected -> 0.12f
            else -> 0f
        },
        animationSpec = tween(150),
        label = "nav_item_background"
    )

    Box(
        modifier = modifier
            .padding(vertical = calculateRoundedValue(4).sdp)
            .dpadNavigation(
                shape = MaterialTheme.shapes.medium,
                onClick = onActivate,
                onFocusChange = { focused ->
                    hasFocus = focused
                    onFocusStateChanged(focused)
                    if (focused) {
                        onFocus()
                    }
                },
                onMoveFocus = { direction ->
                    when (direction) {
                        FocusDirection.Up -> onNavigateUp()
                        FocusDirection.Down -> onNavigateDown()
                        FocusDirection.Right -> onMoveRight()
                        FocusDirection.Left -> true
                        else -> false
                    }
                }
            )
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = backgroundAlpha))
            .padding(all = calculateRoundedValue(10).sdp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .scale(animatedScale)
                .alpha(contentAlpha),
            contentAlignment = Alignment.Center
        ) {
            if (item.customIcon != null && isActive) {
                Image(
                    painter = painterResource(id = item.customIcon),
                    contentDescription = stringResource(id = item.label),
                    modifier = Modifier.size(iconSize)
                )
            } else {
                Icon(
                    imageVector = item.icon,
                    contentDescription = stringResource(id = item.label),
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
            }
        }
    }
}
