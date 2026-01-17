package com.hritwik.avoid.presentation.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.presentation.ui.components.common.SettingItem
import com.hritwik.avoid.presentation.ui.components.common.SettingItemWithSwitch
import com.hritwik.avoid.presentation.ui.components.dialogs.DeviceInfoDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.DisplayModeSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.HdrFormatSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.MpvConfigDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.PlayerSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.PlayerProgressColorDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.SubtitleSizeDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.ThemeSongFallbackDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.ThemeSongVolumeDialog
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.ui.theme.resolvePlayerProgressColorLabel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun VoidTabContent(
    onSwitchUser: () -> Unit,
    onLogoutClick: () -> Unit,
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val playbackSettings by userDataViewModel.playbackSettings.collectAsStateWithLifecycle()
    val themeSongsEnabled = playbackSettings.playThemeSongs
    val themeSongVolume = playbackSettings.themeSongVolume
    val themeSongFallbackUrl = playbackSettings.themeSongFallbackUrl
    val displayMode = playbackSettings.displayMode
    val playerType = playbackSettings.playerType
    val audioPassthroughEnabled = playbackSettings.audioPassthroughEnabled
    val autoSkip = playbackSettings.autoSkipSegments
    val externalPlayerEnabled = playbackSettings.externalPlayerEnabled
    val directPlayEnabled = playbackSettings.directPlayEnabled
    val hdrFormatPreference = playbackSettings.hdrFormatPreference
    val mpvConfig by userDataViewModel.mpvConfig.collectAsStateWithLifecycle()
    val subtitleSize by userDataViewModel.subtitleSize.collectAsStateWithLifecycle()
    val progressBarColorKey by userDataViewModel.playerProgressColor.collectAsStateWithLifecycle()
    val seekProgressBarColorKey by userDataViewModel.playerProgressSeekColor.collectAsStateWithLifecycle()

    val playThemeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        playThemeFocusRequester.requestFocus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
    ) {
        item {
            Text(
                text = "Void Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.MusicNote,
                title = "Play Theme Songs",
                subtitle = "Automatically play theme music on details",
                checked = themeSongsEnabled,
                onCheckedChange = { userDataViewModel.setPlayThemeSongs(it) },
                focusRequester = playThemeFocusRequester
            )
        }

        if (themeSongsEnabled) {
            item {
                var showVolumeDialog by remember { mutableStateOf(false) }

                SettingItem(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Theme Song Volume",
                    subtitle = "Adjust how loud theme songs play",
                    onClick = { showVolumeDialog = true },
                    trailingText = "Volume $themeSongVolume"
                )

                if (showVolumeDialog) {
                    ThemeSongVolumeDialog(
                        currentVolume = themeSongVolume,
                        onVolumeSelected = { volume ->
                            userDataViewModel.setThemeSongVolume(volume)
                            showVolumeDialog = false
                        },
                        onDismiss = { showVolumeDialog = false }
                    )
                }
            }

            item {
                var showFallbackDialog by remember { mutableStateOf(false) }
                val fallbackDisplayText = themeSongFallbackUrl.ifBlank { "Not set" }

                SettingItem(
                    icon = Icons.Default.Link,
                    title = "Theme Song Fallback URL",
                    subtitle = "Base URL used when no theme is provided",
                    onClick = { showFallbackDialog = true },
                    trailingText = fallbackDisplayText
                )

                if (showFallbackDialog) {
                    ThemeSongFallbackDialog(
                        initialUrl = themeSongFallbackUrl,
                        onSave = { url ->
                            userDataViewModel.setThemeSongFallbackUrl(url)
                            showFallbackDialog = false
                        },
                        onDismiss = { showFallbackDialog = false }
                    )
                }
            }
        }

        item {
            Text(
                text = "Playback Setting",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }

        item {
            var showPlayerDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.Default.PlayCircle,
                title = "Preferred Player",
                subtitle = "Choose between Auto, MPV, or ExoPlayer",
                onClick = { showPlayerDialog = true },
                trailingText = playerType.value
            )

            if (showPlayerDialog) {
                PlayerSelectionDialog(
                    currentPlayer = playerType,
                    onPlayerSelected = { player ->
                        userDataViewModel.setPlayerType(player)
                        showPlayerDialog = false
                    },
                    onDismiss = { showPlayerDialog = false }
                )
            }
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                title = "Use External Player",
                subtitle = "Play media in another video app",
                checked = externalPlayerEnabled,
                onCheckedChange = { userDataViewModel.setExternalPlayerEnabled(it) }
            )
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.CheckCircle,
                title = "Direct Play",
                subtitle = "When enabled, automatic transcoding will be disabled",
                checked = directPlayEnabled,
                onCheckedChange = {
                    userDataViewModel.setDirectPlayEnabled(it)
                }
            )
        }

        item {
            var showHdrDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.Default.HdrOn,
                title = "HDR Format",
                subtitle = "Choose whether to use HDR10+ or Dolby Vision",
                onClick = { showHdrDialog = true },
                trailingText = hdrFormatPreference.displayName
            )

            if (showHdrDialog) {
                HdrFormatSelectionDialog(
                    currentPreference = hdrFormatPreference,
                    onPreferenceSelected = { preference ->
                        userDataViewModel.setHdrFormatPreference(preference)
                    },
                    onDismiss = { showHdrDialog = false }
                )
            }
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.SurroundSound,
                title = "Audio Passthrough",
                subtitle = "Send supported audio formats directly to your receiver",
                checked = audioPassthroughEnabled,
                onCheckedChange = { userDataViewModel.setAudioPassthroughEnabled(it) }
            )
        }

        item {
            var showDisplayModeDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.Default.Monitor,
                title = "Display Mode",
                subtitle = "Fit screen, Crop, Stretch, Original",
                onClick = { showDisplayModeDialog = true },
                trailingText = displayMode.value
            )

            if (showDisplayModeDialog) {
                DisplayModeSelectionDialog(
                    currentMode = displayMode,
                    onModeSelected = { mode ->
                        userDataViewModel.setDisplayMode(mode)
                        showDisplayModeDialog = false
                    },
                    onDismiss = { showDisplayModeDialog = false }
                )
            }
        }

        item {
            var showMpvConfigDialog by remember { mutableStateOf(false) }
            val configLines = mpvConfig.trim().takeIf { it.isNotEmpty() }?.lineSequence()?.count() ?: 0
            val trailingText = if (configLines == 0) "Default" else "$configLines lines"

            SettingItem(
                icon = Icons.Default.Tune,
                title = "MPV Config",
                subtitle = "Edit the mpv.conf used by the MPV player",
                onClick = {
                    userDataViewModel.refreshMpvConfig()
                    showMpvConfigDialog = true
                },
                trailingText = trailingText
            )

            if (showMpvConfigDialog) {
                MpvConfigDialog(
                    initialValue = mpvConfig,
                    onSave = { config ->
                        userDataViewModel.saveMpvConfig(config)
                        showMpvConfigDialog = false
                    },
                    onDismiss = { showMpvConfigDialog = false }
                )
            }
        }

        item {
            var showSubtitleSizeDialog by remember { mutableStateOf(false) }
            val sizeDisplayText = when (subtitleSize) {
                "small" -> "Small"
                "medium" -> "Medium"
                "large" -> "Large"
                "extra_large" -> "Extra Large"
                else -> "Medium"
            }

            SettingItem(
                icon = Icons.Default.TextFields,
                title = "Subtitle Size",
                subtitle = "Adjust subtitle text size for all players",
                onClick = { showSubtitleSizeDialog = true },
                trailingText = sizeDisplayText
            )

            if (showSubtitleSizeDialog) {
                SubtitleSizeDialog(
                    currentSize = subtitleSize,
                    onSizeChange = { size ->
                        userDataViewModel.setSubtitleSize(size)
                        showSubtitleSizeDialog = false
                    },
                    onDismiss = { showSubtitleSizeDialog = false }
                )
            }
        }

        item {
            var showProgressColorDialog by remember { mutableStateOf(false) }
            val progressColorLabel = resolvePlayerProgressColorLabel(progressBarColorKey)

            SettingItem(
                icon = Icons.Default.ColorLens,
                title = "Progress Bar Color",
                subtitle = "Change the player seek bar color",
                onClick = { showProgressColorDialog = true },
                trailingText = progressColorLabel
            )

            if (showProgressColorDialog) {
                PlayerProgressColorDialog(
                    currentColorKey = progressBarColorKey,
                    currentSeekColorKey = seekProgressBarColorKey,
                    onColorSaved = { colorKey, seekColorKey ->
                        userDataViewModel.setPlayerProgressColor(colorKey)
                        userDataViewModel.setPlayerProgressSeekColor(seekColorKey)
                        showProgressColorDialog = false
                    },
                    onDismiss = { showProgressColorDialog = false }
                )
            }
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.SkipNext,
                title = "Auto Skip Intros/Credits",
                subtitle = "Automatically skip opening and ending segments",
                checked = autoSkip,
                onCheckedChange = {
                    userDataViewModel.setAutoSkipSegments(it)
                }
            )
        }

        item {
            Text(
                text = "Server Setting",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Person,
                title = "Switch User",
                subtitle = "Login with another account on this server",
                onClick = { onSwitchUser() }
            )
        }

        item {
            SettingItem(
                icon = Icons.AutoMirrored.Outlined.Logout,
                title = "Sign Out",
                subtitle = "Sign out from this device",
                onClick = { onLogoutClick() },
                destructive = true
            )
        }

        item {
            Text(
                text = "Device Info",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }

        item {
            var showDeviceInfoDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.Default.Info,
                title = "Device Capabilities",
                subtitle = "View supported codecs, HDR, and Dolby Vision profiles",
                onClick = { showDeviceInfoDialog = true }
            )

            if (showDeviceInfoDialog) {
                DeviceInfoDialog(
                    onDismiss = { showDeviceInfoDialog = false }
                )
            }
        }

        item {
            Spacer(
                modifier = Modifier.height(calculateRoundedValue(130).sdp)
            )
        }
    }
}
